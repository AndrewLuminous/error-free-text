package ru.andrew.errorfreetext.speller;

import java.util.List;

public record SpellingError(int pos, int len, String word, List<String> suggestions) {}
