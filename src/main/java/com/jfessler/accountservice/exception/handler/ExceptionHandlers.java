package com.jfessler.accountservice.exception.handler;

import com.jfessler.accountservice.exception.AccountNotFoundException;
import com.jfessler.accountservice.exception.InvalidAccountIdException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionHandlers {

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail accountNotFoundException(AccountNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidAccountIdException.class)
    public ProblemDetail invalidAccountIdException(InvalidAccountIdException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
