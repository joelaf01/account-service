package com.jfessler.accountservice.service;

import com.jfessler.accountservice.circuitbreaker.CircuitBreakerFallbackExecutor;
import com.jfessler.accountservice.circuitbreaker.ResilientResult;
import com.jfessler.accountservice.exception.AccountNotFoundException;
import com.jfessler.accountservice.model.Account;
import com.jfessler.accountservice.repository.AccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountCacheService accountCacheService;
    private final DirtyFlagService dirtyFlagService;

    private final CircuitBreakerFallbackExecutor<ResilientResult<Optional<Account>>>
            repositoryCircuitBreakerFallbackExecutor;
    private final CircuitBreakerFallbackExecutor<ResilientResult<Optional<Account>>>
            cacheCircuitBreakerFallbackExecutor;

    public AccountService(
            AccountRepository accountRepository,
            AccountCacheService accountCacheService,
            DirtyFlagService dirtyFlagService,
            @Qualifier("repositoryCircuitBreakerFallbackExecutor")
                    CircuitBreakerFallbackExecutor<ResilientResult<Optional<Account>>>
                            repositoryCircuitBreakerFallbackExecutor,
            @Qualifier("cacheCircuitBreakerFallbackExecutor")
                    CircuitBreakerFallbackExecutor<ResilientResult<Optional<Account>>>
                            cacheCircuitBreakerFallbackExecutor) {
        this.accountRepository = accountRepository;
        this.accountCacheService = accountCacheService;
        this.dirtyFlagService = dirtyFlagService;
        this.repositoryCircuitBreakerFallbackExecutor = repositoryCircuitBreakerFallbackExecutor;
        this.cacheCircuitBreakerFallbackExecutor = cacheCircuitBreakerFallbackExecutor;
    }

    public List<Account> findAll() {
        // FindAll skips cache, since there is no way to know everything is in cache
        return accountRepository.findAll();
    }

    public Optional<ResilientResult<Account>> findById(UUID id) {
        if (dirtyFlagService.isDirty(id)) {
            return retrieveFromRepositoryWithFallback(id);
        } else {
            return retrieveFromCache(id);
        }
    }

    private Optional<ResilientResult<Account>> retrieveFromCache(UUID id) {
        Supplier<ResilientResult<Optional<Account>>> cacheSupplier =
                () -> new ResilientResult<>(accountCacheService.getCachedAccount(id), false);

        Supplier<ResilientResult<Optional<Account>>> repositorySupplier =
                () -> new ResilientResult<>(retrieveFromRepository(id), false);

        ResilientResult<Optional<Account>> result =
                cacheCircuitBreakerFallbackExecutor.execute(cacheSupplier, repositorySupplier);

        return result.value().map(account -> new ResilientResult<>(account, result.stale()));
    }

    private Optional<ResilientResult<Account>> retrieveFromRepositoryWithFallback(UUID id) {
        Supplier<ResilientResult<Optional<Account>>> repositorySupplier =
                () -> new ResilientResult<>(retrieveFromRepository(id), false);

        Supplier<ResilientResult<Optional<Account>>> cacheSupplier =
                () -> new ResilientResult<>(accountCacheService.getCachedAccount(id), true);

        ResilientResult<Optional<Account>> result =
                repositoryCircuitBreakerFallbackExecutor.execute(repositorySupplier, cacheSupplier);

        return result.value().map(account -> new ResilientResult<>(account, result.stale()));
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
        dirtyFlagService.markDirty(account.getId());
        return accountRepository.save(account);
    }

    @Transactional(readOnly = false)
    public Account update(Account account) {
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
