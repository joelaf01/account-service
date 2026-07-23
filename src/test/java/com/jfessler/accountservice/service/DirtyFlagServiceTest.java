package com.jfessler.accountservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jfessler.accountservice.model.DirtyFlag;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@ExtendWith(MockitoExtension.class)
class DirtyFlagServiceTest {

    private static final String DIRTY_FLAG = "dirtyFlag:";

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<DirtyFlag> dirtyFlagTable;

    private CircuitBreaker circuitBreaker;
    private DirtyFlagService dirtyFlagService;

    @BeforeEach
    void setUp() {
        doReturn(dirtyFlagTable).when(enhancedClient).table(any(), any());
        circuitBreaker = CircuitBreaker.ofDefaults("test");
        dirtyFlagService = new DirtyFlagService(enhancedClient, "tableName", circuitBreaker, 24);
    }

    @AfterEach
    void tearDown() {
        circuitBreaker.transitionToClosedState();
    }

    @Nested
    class IsDirtyTests {

        @Test
        public void whenGetItemReturnsAnItem() {
            UUID id = UUID.randomUUID();
            doReturn(new DirtyFlag()).when(dirtyFlagTable).getItem(any(Key.class));

            boolean result = dirtyFlagService.isDirty(id);
            assertTrue(result);
        }

        @Test
        public void whenGetItemReturnsNull() {
            UUID id = UUID.randomUUID();
            doReturn(null).when(dirtyFlagTable).getItem(any(Key.class));

            boolean result = dirtyFlagService.isDirty(id);
            assertFalse(result);
        }

        @Test
        void whenDynamoThrowsExceptionAssumeDirty() {
            UUID id = UUID.randomUUID();
            doThrow(new RuntimeException("dynamoDb unavailable"))
                    .when(dirtyFlagTable)
                    .getItem(any(Key.class));

            assertTrue(dirtyFlagService.isDirty(id));
        }

        @Test
        void whenCircuitBreakerIsOpenAssumeDirty() {
            circuitBreaker.transitionToForcedOpenState();
            UUID id = UUID.randomUUID();

            assertTrue(dirtyFlagService.isDirty(id));

            verify(dirtyFlagTable, never()).getItem(any(Key.class));
        }
    }

    @Nested
    class MarkDirtyTests {

        @Test
        void markDirtySavesDirtyFlag() {
            UUID id = UUID.randomUUID();
            doNothing().when(dirtyFlagTable).putItem(any(DirtyFlag.class));

            dirtyFlagService.markDirty(id);

            verify(dirtyFlagTable, times(1)).putItem(any(DirtyFlag.class));
        }
    }

    @Nested
    class ClearDirtyTests {

        @Test
        void clearDirtyDeletesDirtyFlag() {
            UUID id = UUID.randomUUID();

            dirtyFlagService.clearDirty(id);

            verify(dirtyFlagTable, times(1)).deleteItem(any(Key.class));
        }
    }
}
