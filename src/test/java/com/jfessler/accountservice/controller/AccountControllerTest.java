package com.jfessler.accountservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jfessler.accountservice.circuitbreaker.ResilientResult;
import com.jfessler.accountservice.exception.AccountNotFoundException;
import com.jfessler.accountservice.mapper.AccountMapper;
import com.jfessler.accountservice.model.Account;
import com.jfessler.accountservice.model.Status;
import com.jfessler.accountservice.representation.AccountRequest;
import com.jfessler.accountservice.service.AccountService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AccountController.class)
@Import(AccountMapper.class)
class AccountControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Autowired
    private AccountMapper accountMapper;

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
            doReturn(Optional.of(new ResilientResult<>(Account.builder().id(id).build(), false)))
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

        @Test
        void returnsStaleIndicatorWhenResultIsStale() throws Exception {
            UUID id = UUID.randomUUID();
            doReturn(Optional.of(new ResilientResult<>(Account.builder().id(id).build(), true)))
                    .when(accountService)
                    .findById(id);

            assertThat(mockMvc.get().uri("/account/" + id))
                    .hasStatusOk()
                    .hasContentType("application/json")
                    .bodyJson()
                    .extractingPath("$.stale")
                    .isEqualTo(true);
        }

        @Test
        void invalidId() throws Exception {
            String invalidId = "invalidId";

            assertThat(mockMvc.get().uri("/account/" + invalidId))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType("application/problem+json")
                    .bodyJson()
                    .extractingPath("detail")
                    .isEqualTo("Method parameter 'id': Failed to convert value of type 'java.lang.String' " +
                            "to required type 'java.util.UUID'; Invalid UUID string: invalidId");
        }
    }

    @Nested
    class CreateTests {

        @Test
        void returnsSuccessfully() throws Exception {
            AccountRequest accountRequest = new AccountRequest("name", Status.ACTIVE);

            doAnswer((Answer<Account>) invocation -> (Account) invocation.getArguments()[0])
                    .when(accountService)
                    .create(any());

            assertThat(mockMvc.post()
                            .uri("/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accountRequest)))
                    .hasStatusOk()
                    .hasContentType("application/json")
                    .bodyJson()
                    .extractingPath("$.id")
                    .isNotEmpty();

            verify(accountService, times(1)).create(any());
        }

        @Test
        void invalidName() throws Exception {
            AccountRequest accountRequest = new AccountRequest("name".repeat(100), Status.INACTIVE);

            assertThat(mockMvc.post()
                    .uri("/account")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(accountRequest)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType("application/problem+json")
                    .bodyJson()
                    .extractingPath("detail")
                    .isEqualTo("name: size must be between 0 and 255");
        }

        @Test
        void blankName() throws Exception {
            AccountRequest accountRequest = new AccountRequest("", Status.INACTIVE);

            assertThat(mockMvc.post()
                    .uri("/account")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(accountRequest)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType("application/problem+json").bodyJson()
                    .extractingPath("detail")
                    .isEqualTo("name: must not be blank");
        }

        @Test
        void nullStatus() throws Exception {
            AccountRequest accountRequest = new AccountRequest("name", null);

            assertThat(mockMvc.post()
                    .uri("/account")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(accountRequest)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType("application/problem+json")
                    .bodyJson()
                    .extractingPath("detail")
                    .isEqualTo("status: must not be null");
        }
    }

    @Nested
    class UpdateTests {
        @Test
        void returnsSuccessfully() throws Exception {
            UUID id = UUID.randomUUID();
            AccountRequest accountRequest = new AccountRequest("name", Status.ACTIVE);
            doReturn(Account.builder().id(id).name("name").status(Status.ACTIVE).build())
                    .when(accountService)
                    .update(any());

            assertThat(mockMvc.put()
                            .uri("/account/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accountRequest)))
                    .hasStatusOk()
                    .hasContentType("application/json")
                    .bodyJson()
                    .extractingPath("$.id")
                    .isEqualTo(id.toString());

            verify(accountService, times(1)).update(any());
        }

        @Test
        void accountNotFoundThrowsException() throws Exception {
            UUID id = UUID.randomUUID();
            AccountRequest accountRequest = new AccountRequest("name", Status.ACTIVE);

            AccountNotFoundException accountNotFoundException = new AccountNotFoundException(id);
            doThrow(accountNotFoundException).when(accountService).update(any());

            assertThat(mockMvc.put()
                            .uri("/account/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accountRequest)))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .hasContentType("application/problem+json")
                    .bodyJson()
                    .extractingPath("detail")
                    .isEqualTo(accountNotFoundException.getMessage());

            verify(accountService, times(1)).update(any());
        }

        @Test
        void invalidId() throws Exception {
            String invalidId = "invalidId";

            assertThat(mockMvc.put().uri("/account/" + invalidId))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType("application/problem+json")
                    .bodyJson()
                    .extractingPath("detail")
                    .isEqualTo("Method parameter 'id': Failed to convert value of type 'java.lang.String' " +
                            "to required type 'java.util.UUID'; Invalid UUID string: invalidId");
        }

        @Test
        void invalidName() throws Exception {
            UUID id = UUID.randomUUID();
            AccountRequest accountRequest = new AccountRequest("name".repeat(100), Status.INACTIVE);

            assertThat(mockMvc.put()
                    .uri("/account/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(accountRequest)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType("application/problem+json")
                    .bodyJson()
                    .extractingPath("detail")
                    .isEqualTo("name: size must be between 0 and 255");
        }

        @Test
        void blankName() throws Exception {
            UUID id = UUID.randomUUID();
            AccountRequest accountRequest = new AccountRequest("", Status.INACTIVE);

            assertThat(mockMvc.put()
                    .uri("/account/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(accountRequest)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType("application/problem+json").bodyJson()
                    .extractingPath("detail")
                    .isEqualTo("name: must not be blank");
        }

        @Test
        void nullStatus() throws Exception {
            UUID id = UUID.randomUUID();
            AccountRequest accountRequest = new AccountRequest("name", null);

            assertThat(mockMvc.put()
                    .uri("/account/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(accountRequest)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType("application/problem+json")
                    .bodyJson()
                    .extractingPath("detail")
                    .isEqualTo("status: must not be null");
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

        @Test
        void invalidId() throws Exception {
            String invalidId = "invalidId";

            assertThat(mockMvc.delete().uri("/account/" + invalidId))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType("application/problem+json")
                    .bodyJson()
                    .extractingPath("detail")
                    .isEqualTo("Method parameter 'id': Failed to convert value of type 'java.lang.String' " +
                            "to required type 'java.util.UUID'; Invalid UUID string: invalidId");
        }
    }
}
