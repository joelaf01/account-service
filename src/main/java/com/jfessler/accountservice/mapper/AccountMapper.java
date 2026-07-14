package com.jfessler.accountservice.mapper;

import com.jfessler.accountservice.model.Account;
import com.jfessler.accountservice.representation.AccountRequest;
import com.jfessler.accountservice.representation.AccountResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toAccountResponse(Account account, boolean stale) {
        return new AccountResponse(account.getId(), account.getName(), account.getStatus(), stale);
    }

    public Account toEntity(AccountRequest request) {
        return new Account(UUID.randomUUID(), request.name(), request.status());
    }

    public Account toEntity(AccountRequest request, UUID id) {
        return new Account(id, request.name(), request.status());
    }
}
