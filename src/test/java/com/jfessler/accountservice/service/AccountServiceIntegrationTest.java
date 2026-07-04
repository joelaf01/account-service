package com.jfessler.accountservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jfessler.accountservice.AbstractIntegrationTest;
import com.jfessler.accountservice.model.Account;
import com.jfessler.accountservice.model.Status;
import com.jfessler.accountservice.repository.AccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class AccountServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoBean
    private AccountCacheService accountCacheService;

    @MockitoBean
    private DirtyFlagService dirtyFlagService;

    @Test
    void createPersistsToDatabase() {
        Account account =
                Account.builder().name("Checking").status(Status.ACTIVE).build();

        Account createdAccount = accountService.create(account);

        Optional<Account> fromDB = accountRepository.findById(createdAccount.getId());

        assertTrue(fromDB.isPresent());
        assertEquals(account.getName(), fromDB.get().getName());
    }
}
