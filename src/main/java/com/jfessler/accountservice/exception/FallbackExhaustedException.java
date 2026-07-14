package com.jfessler.accountservice.exception;

public class FallbackExhaustedException extends RuntimeException {

    private static final String MESSAGE = "Fallback exhausted";

    public FallbackExhaustedException(Exception cause) {
        super(MESSAGE, cause);
    }
}
