package ru.andrew.errorfreetext.speller;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

@Component
public class TextChunker {
    static final int LIMIT = 10_000;

    public List<Chunk> split(String text) {
        List<Chunk> chunks = new ArrayList<>();
        Matcher urls = TextOptions.URL.matcher(text);
        int start = 0;
        while (urls.find()) {
            if (urls.end() - urls.start() > LIMIT) {
                splitRange(text.substring(start, urls.start()), chunks);
                // Слишком длинный URL сохраняем целиком: его обрывок нельзя проверять как слово.
                chunks.add(new Chunk(urls.group(), false));
                start = urls.end();
            }
        }
        splitRange(text.substring(start), chunks);
        return List.copyOf(chunks);
    }

    private void splitRange(String text, List<Chunk> result) {
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + LIMIT, text.length());
            if (end < text.length()) {
                if (Character.isHighSurrogate(text.charAt(end - 1))
                        && Character.isLowSurrogate(text.charAt(end))) {
                    end--;
                }
                // Пробел остаётся в предыдущем фрагменте, поэтому склейка не меняет исходный текст.
                for (int i = end - 1; i > start; i--) {
                    if (Character.isWhitespace(text.charAt(i))) {
                        end = i + 1;
                        break;
                    }
                }
            }
            String part = text.substring(start, end);
            result.add(new Chunk(part, !part.isBlank()));
            start = end;
        }
    }

    public record Chunk(String text, boolean check) {}
}
