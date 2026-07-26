package com.jfessler.accountservice.controller;

import com.jfessler.accountservice.circuitbreaker.ResilientResult;
import com.jfessler.accountservice.exception.AccountNotFoundException;
import com.jfessler.accountservice.mapper.AccountMapper;
import com.jfessler.accountservice.model.Account;
import com.jfessler.accountservice.representation.AccountRequest;
import com.jfessler.accountservice.representation.AccountResponse;
import com.jfessler.accountservice.service.AccountService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    @GetMapping
    public List<AccountResponse> findAll() {
        return accountService.findAll().stream()
                .map(account -> accountMapper.toAccountResponse(account, false))
                .toList();
    }

    @GetMapping("/{id}")
    public AccountResponse findById(@PathVariable UUID id) {
        Optional<ResilientResult<Account>> result = accountService.findById(id);
        if (result.isEmpty()) {
            throw new AccountNotFoundException(id);
        }
        return accountMapper.toAccountResponse(
                result.get().value(), result.get().stale());
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@RequestBody @Valid AccountRequest request) {

        AccountResponse response =
                accountMapper.toAccountResponse(accountService.create(accountMapper.toEntity(request)), false);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(@PathVariable UUID id, @RequestBody @Valid AccountRequest request) {
        return ResponseEntity.ok(
                accountMapper.toAccountResponse(accountService.update(accountMapper.toEntity(request, id)), false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        accountService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
