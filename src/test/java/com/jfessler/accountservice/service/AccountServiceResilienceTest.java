package com.jfessler.accountservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jfessler.accountservice.AbstractIntegrationTest;
import com.jfessler.accountservice.circuitbreaker.CircuitBreakerFallbackExecutor;
import com.jfessler.accountservice.circuitbreaker.ResilientResult;
import com.jfessler.accountservice.exception.FallbackExhaustedException;
import com.jfessler.accountservice.model.Account;
import com.jfessler.accountservice.model.Status;
import com.jfessler.accountservice.repository.AccountRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class AccountServiceResilienceTest extends AbstractIntegrationTest {

    @Autowired
    private AccountService accountService;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private AccountCacheService accountCacheService;

    @MockitoBean
    private DirtyFlagService dirtyFlagService;

    @Autowired
    private CircuitBreakerFallbackExecutor<ResilientResult<Optional<Account>>> repositoryCircuitBreakerFallbackExecutor;

    @Autowired
    private CircuitBreakerFallbackExecutor<ResilientResult<Optional<Account>>> cacheCircuitBreakerFallbackExecutor;

    @Qualifier("repositoryCircuitBreaker")
    @Autowired
    private CircuitBreaker repositoryCircuitBreaker;

    @Qualifier("cacheCircuitBreaker")
    @Autowired
    private CircuitBreaker cacheCircuitBreaker;

    @Nested
    class DirtyTests {

        @BeforeEach
        void setUp() {
            doReturn(true).when(dirtyFlagService).isDirty(any());
        }

        @AfterEach
        void tearDown() {
            repositoryCircuitBreaker.transitionToClosedState();
            cacheCircuitBreaker.transitionToClosedState();
        }

        @Test
        void successfulReturnFromRepository() {
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .name("name")
                    .status(Status.INACTIVE)
                    .build();
            doReturn(Optional.of(account)).when(accountRepository).findById(any());

            Optional<ResilientResult<Account>> result = accountService.findById(account.getId());

            assertNotNull(result);
            assertTrue(result.isPresent());
            ResilientResult<Account> resilientResult = result.get();
            Account resultAccount = resilientResult.value();
            assertNotNull(resultAccount);
            assertEquals(account.getId(), resultAccount.getId());
            assertEquals(account.getName(), resultAccount.getName());
            assertEquals(Status.INACTIVE, resultAccount.getStatus());
            assertFalse(resilientResult.stale());

            verify(accountRepository, times(1)).findById(account.getId());
            verify(accountCacheService, never()).getCachedAccount(any());
            verify(accountCacheService, times(1)).putCachedAccount(account);
            verify(dirtyFlagService, times(1)).clearDirty(account.getId());
        }

        @Test
        void emptyReturnFromRepository() {
            doReturn(Optional.empty()).when(accountRepository).findById(any());

            Optional<ResilientResult<Account>> result = accountService.findById(UUID.randomUUID());

            assertNotNull(result);
            assertFalse(result.isPresent());

            verify(accountRepository, times(1)).findById(any());
            verify(accountCacheService, never()).getCachedAccount(any());
            verify(accountCacheService, never()).putCachedAccount(any());
            verify(dirtyFlagService, never()).clearDirty(any());
        }

        @Test
        void repositoryThrowsDataAccessExceptionReturnsFromCache() {
            doThrow(new DataAccessResourceFailureException(""))
                    .when(accountRepository)
                    .findById(any());
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .name("name")
                    .status(Status.INACTIVE)
                    .build();
            doReturn(Optional.of(account)).when(accountCacheService).getCachedAccount(any());

            Optional<ResilientResult<Account>> result = accountService.findById(account.getId());

            assertNotNull(result);
            assertTrue(result.isPresent());
            ResilientResult<Account> resilientResult = result.get();
            Account resultAccount = resilientResult.value();
            assertNotNull(resultAccount);
            assertEquals(account.getId(), resultAccount.getId());
            assertEquals(account.getName(), resultAccount.getName());
            assertEquals(Status.INACTIVE, resultAccount.getStatus());
            assertTrue(resilientResult.stale());

            verify(accountRepository, times(1)).findById(any());
            verify(accountCacheService, times(1)).getCachedAccount(any());
            verify(accountCacheService, never()).putCachedAccount(any());
            verify(dirtyFlagService, never()).clearDirty(any());
        }

        @Test
        void repositoryThrowsDataAccessExceptionEmptyFromCache() {
            doThrow(new DataAccessResourceFailureException(""))
                    .when(accountRepository)
                    .findById(any());
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .name("name")
                    .status(Status.INACTIVE)
                    .build();
            doReturn(Optional.empty()).when(accountCacheService).getCachedAccount(any());

            Optional<ResilientResult<Account>> result = accountService.findById(account.getId());

            assertNotNull(result);
            assertFalse(result.isPresent());

            verify(accountRepository, times(1)).findById(any());
            verify(accountCacheService, times(1)).getCachedAccount(any());
            verify(accountCacheService, never()).putCachedAccount(any());
            verify(dirtyFlagService, never()).clearDirty(any());
        }

        @Test
        void primaryCircuitBreakerOpenReturnsFromCache() {
            repositoryCircuitBreaker.transitionToForcedOpenState();
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .name("name")
                    .status(Status.INACTIVE)
                    .build();
            doReturn(Optional.of(account)).when(accountCacheService).getCachedAccount(any());

            Optional<ResilientResult<Account>> result = accountService.findById(account.getId());

            assertNotNull(result);
            assertTrue(result.isPresent());
            ResilientResult<Account> resilientResult = result.get();
            Account resultAccount = resilientResult.value();
            assertNotNull(resultAccount);
            assertEquals(account.getId(), resultAccount.getId());
            assertEquals(account.getName(), resultAccount.getName());
            assertEquals(Status.INACTIVE, resultAccount.getStatus());
            assertTrue(resilientResult.stale());

            verify(accountRepository, never()).findById(any());
            verify(accountCacheService, times(1)).getCachedAccount(account.getId());
            verify(accountCacheService, never()).putCachedAccount(any());
            verify(dirtyFlagService, never()).clearDirty(any());
        }

        @Test
        void bothCircuitBreakersOpenThrowsFallbackExhaustedException() {
            repositoryCircuitBreaker.transitionToForcedOpenState();
            cacheCircuitBreaker.transitionToForcedOpenState();

            FallbackExhaustedException exhaustedException =
                    assertThrows(FallbackExhaustedException.class, () -> accountService.findById(UUID.randomUUID()));

            assertNotNull(exhaustedException);
            assertInstanceOf(CallNotPermittedException.class, exhaustedException.getCause());

            verify(accountRepository, never()).findById(any());
            verify(accountCacheService, never()).getCachedAccount(any());
        }

        @Test
        void cacheThrowsDataAccessExceptionThrowsFallbackExhaustedException() {
            repositoryCircuitBreaker.transitionToForcedOpenState();
            doThrow(new DataAccessResourceFailureException(""))
                    .when(accountCacheService)
                    .getCachedAccount(any());

            FallbackExhaustedException exhaustedException =
                    assertThrows(FallbackExhaustedException.class, () -> accountService.findById(UUID.randomUUID()));

            assertNotNull(exhaustedException);
            assertInstanceOf(DataAccessResourceFailureException.class, exhaustedException.getCause());

            verify(accountRepository, never()).findById(any());
            verify(accountCacheService, times(1)).getCachedAccount(any());
        }
    }

    @Nested
    class CleanTests {

        @BeforeEach
        void setUp() {
            doReturn(false).when(dirtyFlagService).isDirty(any());
        }

        @AfterEach
        void tearDown() {
            repositoryCircuitBreaker.transitionToClosedState();
            cacheCircuitBreaker.transitionToClosedState();
        }

        @Test
        void successfulReturnFromCache() {
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .name("name")
                    .status(Status.INACTIVE)
                    .build();
            doReturn(Optional.of(account)).when(accountCacheService).getCachedAccount(any());

            Optional<ResilientResult<Account>> result = accountService.findById(account.getId());

            assertNotNull(result);
            assertTrue(result.isPresent());
            ResilientResult<Account> resilientResult = result.get();
            Account resultAccount = resilientResult.value();
            assertNotNull(resultAccount);
            assertEquals(account.getId(), resultAccount.getId());
            assertEquals(account.getName(), resultAccount.getName());
            assertEquals(Status.INACTIVE, resultAccount.getStatus());
            assertFalse(resilientResult.stale());

            verify(accountRepository, never()).findById(any());
            verify(accountCacheService, times(1)).getCachedAccount(account.getId());
            verify(accountCacheService, never()).putCachedAccount(account);
            verify(dirtyFlagService, never()).clearDirty(account.getId());
        }

        @Test
        void emptyFromCacheReturnFromRepository() {
            doReturn(Optional.empty()).when(accountCacheService).getCachedAccount(any());

            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .name("name")
                    .status(Status.INACTIVE)
                    .build();
            doReturn(Optional.of(account)).when(accountRepository).findById(any());

            Optional<ResilientResult<Account>> result = accountService.findById(account.getId());

            assertNotNull(result);
            assertTrue(result.isPresent());
            ResilientResult<Account> resilientResult = result.get();
            Account resultAccount = resilientResult.value();
            assertNotNull(resultAccount);
            assertEquals(account.getId(), resultAccount.getId());
            assertEquals(account.getName(), resultAccount.getName());
            assertEquals(Status.INACTIVE, resultAccount.getStatus());
            assertFalse(resilientResult.stale());

            verify(accountRepository, times(1)).findById(any());
            verify(accountCacheService, times(1)).getCachedAccount(any());
            verify(accountCacheService, times(1)).putCachedAccount(any());
            verify(dirtyFlagService, times(1)).clearDirty(any());
        }

        @Test
        void emptyFromCacheAndEmptyReturnFromRepository() {
            doReturn(Optional.empty()).when(accountCacheService).getCachedAccount(any());
            doReturn(Optional.empty()).when(accountRepository).findById(any());

            Optional<ResilientResult<Account>> result = accountService.findById(UUID.randomUUID());

            assertNotNull(result);
            assertFalse(result.isPresent());

            verify(accountRepository, times(1)).findById(any());
            verify(accountCacheService, times(1)).getCachedAccount(any());
            verify(accountCacheService, never()).putCachedAccount(any());
            verify(dirtyFlagService, never()).clearDirty(any());
        }

        @Test
        void cacheThrowsDataAccessExceptionReturnsFromRepository() {
            doThrow(new DataAccessResourceFailureException(""))
                    .when(accountCacheService)
                    .getCachedAccount(any());
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .name("name")
                    .status(Status.INACTIVE)
                    .build();
            doReturn(Optional.of(account)).when(accountRepository).findById(any());

            Optional<ResilientResult<Account>> result = accountService.findById(account.getId());

            assertNotNull(result);
            assertTrue(result.isPresent());
            ResilientResult<Account> resilientResult = result.get();
            Account resultAccount = resilientResult.value();
            assertNotNull(resultAccount);
            assertEquals(account.getId(), resultAccount.getId());
            assertEquals(account.getName(), resultAccount.getName());
            assertEquals(Status.INACTIVE, resultAccount.getStatus());
            assertFalse(resilientResult.stale());

            verify(accountRepository, times(1)).findById(any());
            verify(accountCacheService, times(1)).getCachedAccount(any());
            verify(accountCacheService, times(1)).putCachedAccount(any());
            verify(dirtyFlagService, times(1)).clearDirty(any());
        }

        @Test
        void cacheThrowsDataAccessExceptionEmptyFromRepository() {
            doThrow(new DataAccessResourceFailureException(""))
                    .when(accountCacheService)
                    .getCachedAccount(any());
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .name("name")
                    .status(Status.INACTIVE)
                    .build();
            doReturn(Optional.empty()).when(accountRepository).findById(any());

            Optional<ResilientResult<Account>> result = accountService.findById(account.getId());

            assertNotNull(result);
            assertFalse(result.isPresent());

            verify(accountRepository, times(1)).findById(any());
            verify(accountCacheService, times(1)).getCachedAccount(any());
            verify(accountCacheService, never()).putCachedAccount(any());
            verify(dirtyFlagService, never()).clearDirty(any());
        }

        @Test
        void primaryCircuitBreakerOpenReturnsFromRepository() {
            cacheCircuitBreaker.transitionToForcedOpenState();
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .name("name")
                    .status(Status.INACTIVE)
                    .build();
            doReturn(Optional.of(account)).when(accountRepository).findById(any());

            Optional<ResilientResult<Account>> result = accountService.findById(account.getId());

            assertNotNull(result);
            assertTrue(result.isPresent());
            ResilientResult<Account> resilientResult = result.get();
            Account resultAccount = resilientResult.value();
            assertNotNull(resultAccount);
            assertEquals(account.getId(), resultAccount.getId());
            assertEquals(account.getName(), resultAccount.getName());
            assertEquals(Status.INACTIVE, resultAccount.getStatus());
            assertFalse(resilientResult.stale());

            verify(accountRepository, times(1)).findById(any());
            verify(accountCacheService, never()).getCachedAccount(account.getId());
            verify(accountCacheService, times(1)).putCachedAccount(any());
            verify(dirtyFlagService, times(1)).clearDirty(any());
        }

        @Test
        void bothCircuitBreakersOpenThrowsFallbackExhaustedException() {
            repositoryCircuitBreaker.transitionToForcedOpenState();
            cacheCircuitBreaker.transitionToForcedOpenState();

            FallbackExhaustedException exhaustedException =
                    assertThrows(FallbackExhaustedException.class, () -> accountService.findById(UUID.randomUUID()));

            assertNotNull(exhaustedException);
            assertInstanceOf(CallNotPermittedException.class, exhaustedException.getCause());

            verify(accountRepository, never()).findById(any());
            verify(accountCacheService, never()).getCachedAccount(any());
        }

        @Test
        void repositoryThrowsDataAccessExceptionThrowsFallbackExhaustedException() {
            cacheCircuitBreaker.transitionToForcedOpenState();
            doThrow(new DataAccessResourceFailureException(""))
                    .when(accountRepository)
                    .findById(any());

            FallbackExhaustedException exhaustedException =
                    assertThrows(FallbackExhaustedException.class, () -> accountService.findById(UUID.randomUUID()));

            assertNotNull(exhaustedException);
            assertInstanceOf(DataAccessResourceFailureException.class, exhaustedException.getCause());

            verify(accountRepository, times(1)).findById(any());
            verify(accountCacheService, never()).getCachedAccount(any());
        }
    }
}
