package com.jfessler.accountservice.controller;

import com.jfessler.accountservice.exception.AccountNotFoundException;
import com.jfessler.accountservice.model.Account;
import com.jfessler.accountservice.service.AccountService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public List<Account> findAll() {
        return accountService.findAll();
    }

    @GetMapping("/{id}")
    public Account findById(@PathVariable UUID id) {
        return accountService.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    @PostMapping
    public Account create(@RequestBody Account account) {
        return accountService.create(account);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Account> update(@PathVariable UUID id, @RequestBody Account account) {
        return ResponseEntity.ok(accountService.update(id, account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        accountService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
