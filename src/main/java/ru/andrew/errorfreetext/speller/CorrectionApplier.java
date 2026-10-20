package ru.andrew.errorfreetext.speller;

import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;

@Component
public class CorrectionApplier {
    public String apply(String text, List<SpellingError> errors) {
        List<SpellingError> ordered = errors.stream()
                .sorted(Comparator.comparingInt(SpellingError::pos).reversed()).toList();
        StringBuilder result = new StringBuilder(text);
        int previousStart = text.length();
        for (SpellingError error : ordered) {
            long end = (long) error.pos() + error.len();
            if (error.pos() < 0 || error.len() <= 0 || end > previousStart
                    || !text.substring(error.pos(), (int) end).equals(error.word())) {
                throw new SpellerException(50202, "Сервис проверки вернул некорректные позиции исправлений");
            }
            previousStart = error.pos();
            if (!error.suggestions().isEmpty()) {
                // Спеллер иногда дополняет вариант пробелами до позиции слова.
                String replacement = error.suggestions().getFirst().strip();
                if (!replacement.isEmpty()) {
                    result.replace(error.pos(), (int) end, replacement);
                }
            }
        }
        return result.toString();
    }
}
