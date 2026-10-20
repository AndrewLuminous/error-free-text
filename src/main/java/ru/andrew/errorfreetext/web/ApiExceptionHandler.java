package ru.andrew.errorfreetext.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
        return response(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> handleValidation(Exception ex, HttpServletRequest request) {
        return response(400, 40001, "Проверьте текст, язык RU/EN и формат идентификатора задачи", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNotFound(Exception ex, HttpServletRequest request) {
        return response(404, 40401, "Ресурс не найден", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> handleMethod(Exception ex, HttpServletRequest request) {
        return response(405, 40501, "Метод не поддерживается", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> handleMediaType(Exception ex, HttpServletRequest request) {
        return response(415, 41501, "Используйте Content-Type: application/json", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Ошибка API: тип={}, путь={}", ex.getClass().getSimpleName(), request.getRequestURI());
        return response(500, 50001, "Внутренняя ошибка приложения", request);
    }

    private ResponseEntity<ApiError> response(int status, int code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ApiError(message, code, Instant.now(), request.getRequestURI()));
    }

    public record ApiError(String errorMessage, int errorCode, Instant timestamp, String path) {}
}
