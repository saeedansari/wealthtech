package com.nevis.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        return ex.toProblemDetail();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ValidationException ve = new ValidationException(
                HttpStatus.BAD_REQUEST, "Validation Error", "Request validation failed", errors);
        return ve.toProblemDetail();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex) {
        ValidationException ve = new ValidationException(
                HttpStatus.BAD_REQUEST, "Missing Parameter", ex.getMessage());
        return ve.toProblemDetail();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException ex) {
        ValidationException ve = new ValidationException(
                HttpStatus.BAD_REQUEST, "Bad Request", "Request body is missing or malformed");
        return ve.toProblemDetail();
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        ServerException se = new ServerException("An unexpected error occurred", ex);
        return se.toProblemDetail();
    }
}
