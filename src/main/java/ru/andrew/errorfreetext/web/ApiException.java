package ru.andrew.errorfreetext.web;

public class ApiException extends RuntimeException {
    private final int status;
    private final int code;

    public ApiException(int status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() { return status; }
    public int getCode() { return code; }
}
