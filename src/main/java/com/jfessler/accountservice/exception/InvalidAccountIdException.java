package com.jfessler.accountservice.exception;

import java.util.UUID;

public class InvalidAccountIdException extends RuntimeException {

    private static final String MESSAGE = "Invalid account ID: %s";

    public InvalidAccountIdException(UUID id) {
        super(MESSAGE.formatted(id));
    }
}
