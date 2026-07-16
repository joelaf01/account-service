package com.jfessler.accountservice.exception.handler;

import com.jfessler.accountservice.exception.AccountNotFoundException;
import com.jfessler.accountservice.exception.FallbackExhaustedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionHandlers {

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail accountNotFoundException(AccountNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(FallbackExhaustedException.class)
    public ProblemDetail fallbackExhaustedException(FallbackExhaustedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unhandledException(Exception e) {
        log.error("Unhandled exception", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}
