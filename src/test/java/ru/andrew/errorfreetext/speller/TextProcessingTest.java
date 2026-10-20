package ru.andrew.errorfreetext.speller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class TextProcessingTest {
    private final TextChunker chunker = new TextChunker();
    private final CorrectionApplier applier = new CorrectionApplier();

    @ParameterizedTest
    @CsvSource({"Привет,0", "Текст123,2", "https://example.org,4", "https://example.org/12,6"})
    void resolvesOnlyRequiredOptions(String text, int expected) {
        assertThat(new TextOptions().resolve(text)).isEqualTo(expected);
    }

    @Test
    void splitsLongTextWithoutLosingWhitespace() {
        String text = "Строка текста.\r\n\t".repeat(2000);
        var chunks = chunker.split(text);
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> assertThat(c.text().length()).isLessThanOrEqualTo(10_000));
        assertThat(join(chunks)).isEqualTo(text);
    }

    @Test
    void doesNotSplitSurrogatePairAtBoundary() {
        String text = "а".repeat(9999) + "😀" + "б".repeat(20);
        var chunks = chunker.split(text);
        assertThat(chunks.getFirst().text()).hasSize(9999);
        assertThat(chunks.get(1).text()).startsWith("😀");
        assertThat(join(chunks)).isEqualTo(text);
    }

    @Test
    void preservesOversizedUrlWithoutCheckingItsFragments() {
        String url = "https://example.org/" + "x".repeat(15000);
        String text = "Привет " + url + "\nМир";
        var chunks = chunker.split(text);
        assertThat(chunks).anySatisfy(c -> {
            assertThat(c.text()).isEqualTo(url);
            assertThat(c.check()).isFalse();
        });
        assertThat(join(chunks)).isEqualTo(text);
        assertThat(chunks.stream().filter(TextChunker.Chunk::check))
                .allSatisfy(c -> assertThat(c.text().length()).isLessThanOrEqualTo(10_000));
    }

    @Test
    void skipsWhitespaceOnlyFragments() {
        assertThat(chunker.split(" \n\t")).allSatisfy(c -> assertThat(c.check()).isFalse());
    }

    @Test
    void appliesDifferentLengthCorrectionsFromRightToLeft() {
        var errors = List.of(new SpellingError(0, 4, "Helo", List.of("Hello")),
                new SpellingError(5, 4, "wrld", List.of("world")));
        assertThat(applier.apply("Helo wrld!", errors)).isEqualTo("Hello world!");
    }

    @Test
    void usesUtf16PositionsAndRemovesSuggestionPadding() {
        var errors = List.of(new SpellingError(3, 6, "Превет", List.of("   Привет")));
        assertThat(applier.apply("😀 Превет,\nмир!", errors)).isEqualTo("😀 Привет,\nмир!");
    }

    @Test
    void keepsWordWithoutSuggestions() {
        assertThat(applier.apply("абв", List.of(new SpellingError(0, 3, "абв", List.of()))))
                .isEqualTo("абв");
    }

    @Test
    void rejectsOverlappingCorrections() {
        var errors = List.of(new SpellingError(0, 3, "abc", List.of("x")),
                new SpellingError(2, 2, "cd", List.of("y")));
        assertThatThrownBy(() -> applier.apply("abcd", errors)).isInstanceOf(SpellerException.class);
    }

    @Test
    void rejectsOutOfRangeOrMismatchedWord() {
        for (SpellingError error : List.of(new SpellingError(-1, 1, "a", List.of("b")),
                new SpellingError(2, Integer.MAX_VALUE, "c", List.of("x")),
                new SpellingError(0, 3, "xyz", List.of("x")))) {
            assertThatThrownBy(() -> applier.apply("abc", List.of(error)))
                    .isInstanceOf(SpellerException.class);
        }
    }

    private String join(List<TextChunker.Chunk> chunks) {
        return chunks.stream().map(TextChunker.Chunk::text).reduce("", String::concat);
    }
}
