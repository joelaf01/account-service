package com.jfessler.accountservice.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.jfessler.accountservice.model.Account;
import com.jfessler.accountservice.model.Status;
import com.jfessler.accountservice.representation.AccountRequest;
import com.jfessler.accountservice.representation.AccountResponse;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AccountMapperTest {

    private final AccountMapper accountMapper = new AccountMapper();

    @Nested
    class ToAccountResponseTests {

        @Test
        void happyPath() {
            Account account = new Account(UUID.randomUUID(), "account1", Status.ACTIVE);

            AccountResponse result = accountMapper.toAccountResponse(account, true);

            assertNotNull(result);
            assertEquals(account.getId(), result.id());
            assertEquals(account.getName(), result.name());
            assertEquals(account.getStatus(), result.status());
            assertTrue(result.stale());
        }
    }

    @Nested
    class ToEntityForCreateTests {

        @Test
        void happyPath() {
            AccountRequest request = new AccountRequest("account1", Status.ACTIVE);

            Account result = accountMapper.toEntity(request);

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals(request.name(), result.getName());
            assertEquals(request.status(), result.getStatus());
        }
    }

    @Nested
    class ToEntityForUpdateTests {

        @Test
        void happyPath() {
            UUID id = UUID.randomUUID();
            AccountRequest request = new AccountRequest("account1", Status.ACTIVE);

            Account result = accountMapper.toEntity(request, id);

            assertNotNull(result);
            assertEquals(id, result.getId());
            assertEquals(request.name(), result.getName());
            assertEquals(request.status(), result.getStatus());
        }
    }
}
