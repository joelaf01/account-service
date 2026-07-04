package com.jfessler.accountservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jfessler.accountservice.model.Account;
import com.jfessler.accountservice.model.Status;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AccountCacheServiceTest {

    private static final String ACCOUNT = "account:";

    @Mock
    private RedisTemplate<String, Account> accountRedisTemplate;

    @Mock
    private ValueOperations<String, Account> valueOperations;

    @InjectMocks
    private AccountCacheService accountCacheService;

    @Nested
    class GetCachedAccountTests {

        @BeforeEach
        void setUp() {
            doReturn(valueOperations).when(accountRedisTemplate).opsForValue();
        }

        @Test
        void notFoundReturnsEmptyOptional() {
            UUID id = UUID.randomUUID();
            doReturn(null).when(valueOperations).get(ACCOUNT + id);

            Optional<Account> result = accountCacheService.getCachedAccount(id);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void foundReturnsAccount() {
            UUID id = UUID.randomUUID();
            Account account =
                    Account.builder().id(id).name("name").status(Status.ACTIVE).build();
            doReturn(account).when(valueOperations).get(ACCOUNT + id);

            Optional<Account> result = accountCacheService.getCachedAccount(id);
            assertNotNull(result);
            assertTrue(result.isPresent());
            assertEquals(account, result.get());
        }
    }

    @Nested
    class PutCachedAccountTests {

        @BeforeEach
        void setUp() {
            doReturn(valueOperations).when(accountRedisTemplate).opsForValue();
        }

        @Test
        void putSavesAccount() {
            UUID id = UUID.randomUUID();
            Account account =
                    Account.builder().id(id).name("name").status(Status.ACTIVE).build();
            doNothing().when(valueOperations).set(ACCOUNT + id, account);

            accountCacheService.putCachedAccount(account);

            verify(accountRedisTemplate, times(1)).opsForValue();
            verify(valueOperations, times(1)).set(ACCOUNT + id, account);
        }
    }

    @Nested
    class EvictCachedAccountTests {

        @Test
        void evictDeletesAccount() {
            UUID id = UUID.randomUUID();

            accountCacheService.evictCachedAccount(id);

            verify(accountRedisTemplate, times(1)).delete(ACCOUNT + id);
        }
    }
}
