package ru.andrew.errorfreetext.speller;

public class SpellerException extends RuntimeException {
    private final int code;

    public SpellerException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }
}
