package com.jfessler.accountservice.service;

import com.jfessler.accountservice.exception.AccountNotFoundException;
import com.jfessler.accountservice.exception.InvalidAccountIdException;
import com.jfessler.accountservice.model.Account;
import com.jfessler.accountservice.repository.AccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountCacheService accountCacheService;
    private final DirtyFlagService dirtyFlagService;

    public List<Account> findAll() {
        // FindAll skips cache, since there is no way to know everything is in cache
        return accountRepository.findAll();
    }

    public Optional<Account> findById(UUID id) {
        if (dirtyFlagService.isDirty(id)) {
            return retrieveFromRepository(id);
        } else {
            Optional<Account> account = accountCacheService.getCachedAccount(id);
            return account.isPresent() ? account : retrieveFromRepository(id);
        }
    }

    private Optional<Account> retrieveFromRepository(UUID id) {
        Optional<Account> account = accountRepository.findById(id);
        if (account.isPresent()) {
            accountCacheService.putCachedAccount(account.get());
            dirtyFlagService.clearDirty(id);
        }
        return account;
    }

    @Transactional(readOnly = false)
    public Account create(Account account) {
        if (account.getId() != null) {
            throw new InvalidAccountIdException(account.getId());
        }
        account.setId(UUID.randomUUID());
        dirtyFlagService.markDirty(account.getId());
        return accountRepository.save(account);
    }

    @Transactional(readOnly = false)
    public Account update(UUID id, Account account) {
        if (!id.equals(account.getId()) && account.getId() != null) {
            throw new InvalidAccountIdException(account.getId());
        }
        account.setId(id);
        if (!accountRepository.existsById(account.getId())) {
            throw new AccountNotFoundException(account.getId());
        }

        dirtyFlagService.markDirty(account.getId());
        return accountRepository.save(account);
    }

    @Transactional(readOnly = false)
    public void deleteById(UUID id) {
        dirtyFlagService.markDirty(id);
        accountRepository.deleteById(id);
        accountCacheService.evictCachedAccount(id);
    }
}
