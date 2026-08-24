package com.influencerapp.infrastructure.adapter.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler that converts exceptions to RFC 7807 Problem Detail format.
 *
 * @author judaro122
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_TYPE_BASE = "https://api.influencerapp.com/problems/";

    /**
     * Handles validation errors from @Valid annotated request bodies.
     * Returns RFC 7807 Problem Detail with field-level error details.
     * Keeps the first error for each field to ensure deterministic behavior.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for the request"
        );
        problemDetail.setType(URI.create(PROBLEM_TYPE_BASE + "validation-error"));
        problemDetail.setTitle("Validation Error");

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        problemDetail.setProperty("fieldErrors", fieldErrors);

        return problemDetail;
    }
}
