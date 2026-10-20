package ru.andrew.errorfreetext.speller;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class TextOptions {
    static final Pattern URL = Pattern.compile("(?i)(?:https?://|www\\.)\\S+");
    private static final int IGNORE_DIGITS = 2;
    private static final int IGNORE_URLS = 4;

    public int resolve(String text) {
        int options = text.codePoints().anyMatch(Character::isDigit) ? IGNORE_DIGITS : 0;
        if (URL.matcher(text).find()) {
            options |= IGNORE_URLS;
        }
        return options;
    }
}
