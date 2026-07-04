package com.jfessler.accountservice.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    private static final String MESSAGE = "Account not found for id: %s";

    public AccountNotFoundException(UUID id) {
        super(MESSAGE.formatted(id));
    }
}
