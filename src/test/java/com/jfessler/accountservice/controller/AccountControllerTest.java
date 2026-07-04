package com.jfessler.accountservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jfessler.accountservice.exception.AccountNotFoundException;
import com.jfessler.accountservice.exception.InvalidAccountIdException;
import com.jfessler.accountservice.model.Account;
import com.jfessler.accountservice.service.AccountService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private AccountService accountService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    class FindAllTests {

        @Test
        void testFindAll() throws Exception {
            UUID id = UUID.randomUUID();
            doReturn(List.of(Account.builder().id(id).build()))
                    .when(accountService)
                    .findAll();

            assertThat(mockMvc.get().uri("/account"))
                    .hasStatusOk()
                    .hasContentType("application/json")
                    .bodyJson()
                    .extractingPath("$[0].id")
                    .isEqualTo(id.toString());

            verify(accountService, times(1)).findAll();
        }

        @Test
        void emptyListReturnsEmptyArray() throws Exception {
            doReturn(List.of()).when(accountService).findAll();

            assertThat(mockMvc.get().uri("/account"))
                    .hasStatusOk()
                    .hasContentType("application/json")
                    .bodyJson()
                    .extractingPath("$")
                    .asArray()
                    .isEmpty();

            verify(accountService, times(1)).findAll();
        }
    }

    @Nested
    class FindByIdTests {
        @Test
        void returnsSuccessfully() throws Exception {
            UUID id = UUID.randomUUID();
            doReturn(Optional.of(Account.builder().id(id).build()))
                    .when(accountService)
                    .findById(id);

            assertThat(mockMvc.get().uri("/account/" + id))
                    .hasStatusOk()
                    .hasContentType("application/json")
                    .bodyJson()
                    .extractingPath("$.id")
                    .isEqualTo(id.toString());

            verify(accountService, times(1)).findById(id);
        }

        @Test
        void notFound() throws Exception {
            UUID id = UUID.randomUUID();
            doReturn(Optional.empty()).when(accountService).findById(id);

            assertThat(mockMvc.get().uri("/account/" + id))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .hasContentType("application/problem+json")
                    .bodyJson()
                    .extractingPath("detail")
                    .isEqualTo("Account not found for id: " + id);

            verify(accountService, times(1)).findById(id);
        }
    }

    @Nested
    class CreateTests {

        @Test
        void returnsSuccessfully() throws Exception {
            Account account = Account.builder().name("name").build();

            doAnswer((Answer<Account>) invocation -> {
                        Account account1 = (Account) invocation.getArguments()[0];
                        account1.setId(UUID.randomUUID());

                        return account1;
                    })
                    .when(accountService)
                    .create(any());

            assertThat(mockMvc.post()
                            .uri("/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(account)))
                    .hasStatusOk()
                    .hasContentType("application/json")
                    .bodyJson()
                    .extractingPath("$.id")
                    .isNotEmpty();

            verify(accountService, times(1)).create(any());
        }

        @Test
        void withIdThrowsException() throws Exception {
            UUID id = UUID.randomUUID();
            Account account = Account.builder().id(id).build();

            InvalidAccountIdException invalidAccountIdException = new InvalidAccountIdException(id);
            doThrow(invalidAccountIdException).when(accountService).create(any());

            assertThat(mockMvc.post()
                            .uri("/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(account)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType("application/problem+json")
                    .bodyJson()
                    .extractingPath("detail")
                    .isEqualTo(invalidAccountIdException.getMessage());

            verify(accountService, times(1)).create(any());
        }
    }

    @Nested
    class UpdateTests {
        @Test
        void idInBodyReturnsSuccessfully() throws Exception {
            UUID id = UUID.randomUUID();
            Account account = Account.builder().id(id).name("name").build();
            doReturn(account).when(accountService).update(any(), any());

            assertThat(mockMvc.put()
                            .uri("/account/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(account)))
                    .hasStatusOk()
                    .hasContentType("application/json")
                    .bodyJson()
                    .extractingPath("$.id")
                    .isEqualTo(id.toString());

            verify(accountService, times(1)).update(any(), any());
        }

        @Test
        void noIdInBodyReturnsSuccessfully() throws Exception {
            UUID id = UUID.randomUUID();
            Account account = Account.builder().name("name").build();
            doAnswer((Answer<Account>) invocation -> {
                        Account account1 = (Account) invocation.getArguments()[1];
                        account1.setId(id);

                        return account1;
                    })
                    .when(accountService)
                    .update(any(), any());

            assertThat(mockMvc.put()
                            .uri("/account/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(account)))
                    .hasStatusOk()
                    .hasContentType("application/json")
                    .bodyJson()
                    .extractingPath("$.id")
                    .isEqualTo(id.toString());

            verify(accountService, times(1)).update(any(), any());
        }

        @Test
        void withNonMatchingIdThrowsException() throws Exception {
            UUID id = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            Account account = Account.builder().id(otherId).build();

            InvalidAccountIdException invalidAccountIdException = new InvalidAccountIdException(otherId);
            doThrow(invalidAccountIdException).when(accountService).update(any(), any());

            assertThat(mockMvc.put()
                            .uri("/account/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(account)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType("application/problem+json")
                    .bodyJson()
                    .extractingPath("detail")
                    .isEqualTo(invalidAccountIdException.getMessage());

            verify(accountService, times(1)).update(any(), any());
        }

        @Test
        void accountNotFoundThrowsException() throws Exception {
            UUID id = UUID.randomUUID();
            Account account = Account.builder().id(id).build();

            AccountNotFoundException accountNotFoundException = new AccountNotFoundException(id);
            doThrow(accountNotFoundException).when(accountService).update(any(), any());

            assertThat(mockMvc.put()
                            .uri("/account/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(account)))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .hasContentType("application/problem+json")
                    .bodyJson()
                    .extractingPath("detail")
                    .isEqualTo(accountNotFoundException.getMessage());

            verify(accountService, times(1)).update(any(), any());
        }
    }

    @Nested
    class DeleteTests {
        @Test
        void returnsSuccessfully() throws Exception {
            UUID id = UUID.randomUUID();

            assertThat(mockMvc.delete().uri("/account/" + id)).hasStatus(HttpStatus.NO_CONTENT);

            verify(accountService, times(1)).deleteById(id);
        }
    }
}
