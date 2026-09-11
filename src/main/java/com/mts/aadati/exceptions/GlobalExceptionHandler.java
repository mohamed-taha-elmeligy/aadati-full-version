package com.mts.aadati.exceptions;

import com.mts.aadati.exceptions.exception.*;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OperationFailedException.class)
    public ProblemDetail operationFailedException(
            OperationFailedException ex,
            HttpServletRequest request
    ) {
        return problemDetailBuilder(
                "Operation Failed",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal error occurred. Please try again later.",
                request,
                null
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail invalidCredentialsException(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {
        return problemDetailBuilder(
                "Invalid Credentials",
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail businessRuleViolationException(
            BusinessRuleViolationException ex,
            HttpServletRequest request
    ) {
        return problemDetailBuilder(
                "Business Rule Violation",
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail invalidRequestException(
            InvalidRequestException ex,
            HttpServletRequest request
    ) {
        return problemDetailBuilder(
                "Invalid Request",
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail duplicateResourceException(
            DuplicateResourceException ex,
            HttpServletRequest request
    ) {
        return problemDetailBuilder(
                "Duplicate Resource",
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail dataIntegrityViolationException(
            DuplicateResourceException ex,
            HttpServletRequest request
    ) {
        return problemDetailBuilder(
                "Data Integrity Violation",
                HttpStatus.CONFLICT,
                "A conflict occurred - the data violates a uniqueness or integrity constraint",
                request,
                null
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail resourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return problemDetailBuilder(
                "Resource Not Found",
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request,
                null
        );
    }


    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        return problemDetailBuilder(
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request,
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));

        return problemDetailBuilder(
                "Validation Failed",
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request,
                fieldErrors
        );
    }


    private static ProblemDetail problemDetailBuilder(
            String title,
            HttpStatus httpStatus,
            String message,
            HttpServletRequest request,
            @Nullable Map<String, String> fieldErrors
    ){
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(
                        httpStatus,
                        message
                );

        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));

        if (fieldErrors != null) {
            problem.setProperty("errors", fieldErrors);
        }

        return problem;
    }
}
