package com.jfessler.accountservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jfessler.accountservice.circuitbreaker.CircuitBreakerFallbackExecutor;
import com.jfessler.accountservice.circuitbreaker.ResilientResult;
import com.jfessler.accountservice.exception.AccountNotFoundException;
import com.jfessler.accountservice.model.Account;
import com.jfessler.accountservice.model.Status;
import com.jfessler.accountservice.repository.AccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountCacheService accountCacheService;

    @Mock
    private DirtyFlagService dirtyFlagService;

    @Mock
    private CircuitBreakerFallbackExecutor<ResilientResult<Optional<Account>>> repositoryCircuitBreakerFallbackExecutor;

    @Mock
    private CircuitBreakerFallbackExecutor<ResilientResult<Optional<Account>>> cacheCircuitBreakerFallbackExecutor;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(
                accountRepository,
                accountCacheService,
                dirtyFlagService,
                repositoryCircuitBreakerFallbackExecutor,
                cacheCircuitBreakerFallbackExecutor);
    }

    @Nested
    class FindAllTests {

        @Test
        void findAllShouldSkipCacheAndCallRepository() {
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .name("name")
                    .status(Status.ACTIVE)
                    .build();

            List<Account> accounts = List.of(account);
            doReturn(accounts).when(accountRepository).findAll();

            List<Account> result = accountService.findAll();

            assertNotNull(result);
            assertEquals(accounts.size(), result.size());
            assertEquals(account, result.getFirst());

            verify(accountRepository, times(1)).findAll();
            verify(accountCacheService, never()).getCachedAccount(any());
            verify(dirtyFlagService, never()).isDirty(any());
        }
    }

    @Nested
    class FindByIdTests {

        @Nested
        class DirtyTests {

            @BeforeEach
            void setUp() {
                doReturn(true).when(dirtyFlagService).isDirty(any());
            }

            @Test
            void returnsFromRepository() {
                UUID id = UUID.randomUUID();
                Account account = Account.builder()
                        .id(id)
                        .name("name")
                        .status(Status.ACTIVE)
                        .build();
                doReturn(Optional.of(account)).when(accountRepository).findById(id);
                doAnswer((Answer<ResilientResult<Optional<Account>>>) invocation ->
                                ((Supplier<ResilientResult<Optional<Account>>>) invocation.getArgument(0)).get())
                        .when(repositoryCircuitBreakerFallbackExecutor)
                        .execute(any(), any());

                Optional<ResilientResult<Account>> result = accountService.findById(id);

                assertNotNull(result);
                assertTrue(result.isPresent());
                assertEquals(account, result.get().value());
                assertFalse(result.get().stale());
                verify(accountRepository, times(1)).findById(id);
                verify(accountCacheService, never()).getCachedAccount(any());
                verify(accountCacheService, times(1)).putCachedAccount(any());
                verify(dirtyFlagService, times(1)).clearDirty(id);
            }

            @Test
            void emptyOptionalReturnsFromRepository() {
                UUID id = UUID.randomUUID();
                doReturn(Optional.empty()).when(accountRepository).findById(id);
                doAnswer((Answer<ResilientResult<Optional<Account>>>) invocation ->
                                ((Supplier<ResilientResult<Optional<Account>>>) invocation.getArgument(0)).get())
                        .when(repositoryCircuitBreakerFallbackExecutor)
                        .execute(any(), any());

                Optional<ResilientResult<Account>> result = accountService.findById(id);

                assertNotNull(result);
                assertTrue(result.isEmpty());
                verify(accountRepository, times(1)).findById(id);
                verify(accountCacheService, never()).getCachedAccount(any());
                verify(accountCacheService, never()).putCachedAccount(any());
                verify(dirtyFlagService, never()).clearDirty(any());
            }

            @Test
            void returnsFromCacheAfterFallback() {
                UUID id = UUID.randomUUID();
                Account account = Account.builder()
                        .id(id)
                        .name("name")
                        .status(Status.ACTIVE)
                        .build();
                doReturn(Optional.of(account)).when(accountCacheService).getCachedAccount(id);
                doAnswer((Answer<ResilientResult<Optional<Account>>>) invocation ->
                                ((Supplier<ResilientResult<Optional<Account>>>) invocation.getArgument(1)).get())
                        .when(repositoryCircuitBreakerFallbackExecutor)
                        .execute(any(), any());

                Optional<ResilientResult<Account>> result = accountService.findById(id);

                assertNotNull(result);
                assertTrue(result.isPresent());
                assertEquals(account, result.get().value());
                assertTrue(result.get().stale());
                verify(accountRepository, never()).findById(id);
                verify(accountCacheService, times(1)).getCachedAccount(any());
                verify(accountCacheService, never()).putCachedAccount(any());
                verify(dirtyFlagService, never()).clearDirty(id);
            }

            @Test
            void emptyOptionalReturnsAfterFallback() {
                UUID id = UUID.randomUUID();
                doReturn(Optional.empty()).when(accountCacheService).getCachedAccount(id);
                doAnswer((Answer<ResilientResult<Optional<Account>>>) invocation ->
                                ((Supplier<ResilientResult<Optional<Account>>>) invocation.getArgument(1)).get())
                        .when(repositoryCircuitBreakerFallbackExecutor)
                        .execute(any(), any());

                Optional<ResilientResult<Account>> result = accountService.findById(id);

                assertNotNull(result);
                assertTrue(result.isEmpty());
                verify(accountRepository, never()).findById(id);
                verify(accountCacheService, times(1)).getCachedAccount(any());
                verify(accountCacheService, never()).putCachedAccount(any());
                verify(dirtyFlagService, never()).clearDirty(any());
            }
        }

        @Nested
        class CleanTests {
            @BeforeEach
            void setUp() {
                doReturn(false).when(dirtyFlagService).isDirty(any());
            }

            @Test
            void returnsFromCache() {
                UUID id = UUID.randomUUID();
                Account account = Account.builder()
                        .id(id)
                        .name("name")
                        .status(Status.ACTIVE)
                        .build();
                doReturn(Optional.of(account)).when(accountCacheService).getCachedAccount(id);
                doAnswer((Answer<ResilientResult<Optional<Account>>>) invocation ->
                                ((Supplier<ResilientResult<Optional<Account>>>) invocation.getArgument(0)).get())
                        .when(cacheCircuitBreakerFallbackExecutor)
                        .execute(any(), any());

                Optional<ResilientResult<Account>> result = accountService.findById(id);
                assertNotNull(result);
                assertTrue(result.isPresent());
                assertEquals(account, result.get().value());
                assertFalse(result.get().stale());
                verify(accountRepository, never()).findById(id);
                verify(accountCacheService, times(1)).getCachedAccount(any());
                verify(accountCacheService, never()).putCachedAccount(any());
                verify(dirtyFlagService, never()).clearDirty(any());
            }

            @Test
            void notInCacheReturnsFromRepository() {
                UUID id = UUID.randomUUID();
                Account account = Account.builder()
                        .id(id)
                        .name("name")
                        .status(Status.ACTIVE)
                        .build();
                doReturn(Optional.of(account)).when(accountRepository).findById(id);
                doAnswer((Answer<ResilientResult<Optional<Account>>>) invocation ->
                                ((Supplier<ResilientResult<Optional<Account>>>) invocation.getArgument(1)).get())
                        .when(cacheCircuitBreakerFallbackExecutor)
                        .execute(any(), any());

                Optional<ResilientResult<Account>> result = accountService.findById(id);

                assertNotNull(result);
                assertTrue(result.isPresent());
                assertEquals(account, result.get().value());
                assertFalse(result.get().stale());
                verify(accountRepository, times(1)).findById(id);
                verify(accountCacheService, never()).getCachedAccount(any());
                verify(accountCacheService, times(1)).putCachedAccount(any());
                verify(dirtyFlagService, times(1)).clearDirty(id);
            }

            @Test
            void notIdCacheNotInRepository() {
                UUID id = UUID.randomUUID();
                doReturn(Optional.empty()).when(accountRepository).findById(id);
                doAnswer((Answer<ResilientResult<Optional<Account>>>) invocation ->
                                ((Supplier<ResilientResult<Optional<Account>>>) invocation.getArgument(1)).get())
                        .when(cacheCircuitBreakerFallbackExecutor)
                        .execute(any(), any());

                Optional<ResilientResult<Account>> result = accountService.findById(id);

                assertNotNull(result);
                assertTrue(result.isEmpty());
                verify(accountRepository, times(1)).findById(id);
                verify(accountCacheService, never()).getCachedAccount(any());
                verify(accountCacheService, never()).putCachedAccount(any());
                verify(dirtyFlagService, never()).clearDirty(any());
            }
        }
    }

    @Nested
    class CreateTests {

        @Test
        void createAccountShouldReturnCreatedAccount() {
            UUID id = UUID.randomUUID();
            Account account =
                    Account.builder().id(id).name("name").status(Status.ACTIVE).build();
            doReturn(account).when(accountRepository).save(any());

            Account result = accountService.create(account);
            assertNotNull(result);
            assertEquals(id, result.getId());
            assertEquals(account.getName(), result.getName());
            assertEquals(account.getStatus(), result.getStatus());

            verify(accountRepository, times(1)).save(any());
            verify(dirtyFlagService, times(1)).markDirty(result.getId());
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void updateAccountWithIdShouldReturnUpdatedAccount() {
            UUID id = UUID.randomUUID();
            Account account =
                    Account.builder().id(id).name("name").status(Status.ACTIVE).build();

            doReturn(true).when(accountRepository).existsById(id);
            doReturn(account).when(accountRepository).save(any());

            Account result = accountService.update(account);
            assertNotNull(result);
            assertEquals(id, result.getId());
            assertEquals(account.getName(), result.getName());
            assertEquals(account.getStatus(), result.getStatus());
            verify(accountRepository, times(1)).existsById(id);
            verify(accountRepository, times(1)).save(any());
            verify(dirtyFlagService, times(1)).markDirty(id);
        }

        @Test
        void exceptionWhenAccountIsNotFound() {
            UUID id = UUID.randomUUID();
            Account account =
                    Account.builder().id(id).name("name").status(Status.ACTIVE).build();

            doReturn(false).when(accountRepository).existsById(id);

            assertThrows(AccountNotFoundException.class, () -> accountService.update(account));
            verify(accountRepository, times(1)).existsById(id);
            verify(accountRepository, never()).save(any());
            verify(dirtyFlagService, never()).markDirty(id);
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void deleteTest() {
            UUID id = UUID.randomUUID();

            accountService.deleteById(id);
            verify(dirtyFlagService, times(1)).markDirty(id);
            verify(accountRepository, times(1)).deleteById(id);
            verify(accountCacheService, times(1)).evictCachedAccount(id);
        }
    }
}
