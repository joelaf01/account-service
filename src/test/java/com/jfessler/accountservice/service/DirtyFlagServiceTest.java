package com.jfessler.accountservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class DirtyFlagServiceTest {

    private static final String DIRTY_FLAG = "dirtyFlag:";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private DirtyFlagService dirtyFlagService;

    @Nested
    class IsDirtyTests {

        @Test
        public void whenFoundReturnsTrue() {
            UUID id = UUID.randomUUID();
            doReturn(true).when(stringRedisTemplate).hasKey(DIRTY_FLAG + id);

            boolean result = dirtyFlagService.isDirty(id);
            assertTrue(result);
        }

        @Test
        public void whenNotFoundReturnsFalse() {
            UUID id = UUID.randomUUID();
            doReturn(false).when(stringRedisTemplate).hasKey(DIRTY_FLAG + id);

            boolean result = dirtyFlagService.isDirty(id);
            assertFalse(result);
        }
    }

    @Nested
    class MarkDirtyTests {

        @BeforeEach
        void setUp() {
            doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
        }

        @Test
        void markDirtySavesDirtyFlag() {
            UUID id = UUID.randomUUID();
            doNothing().when(valueOperations).set(DIRTY_FLAG + id, String.valueOf(true));

            dirtyFlagService.markDirty(id);

            verify(stringRedisTemplate, times(1)).opsForValue();
            verify(valueOperations, times(1)).set(DIRTY_FLAG + id, String.valueOf(true));
        }
    }

    @Nested
    class ClearDirtyTests {

        @Test
        void clearDirtyDeletesDirtyFlag() {
            UUID id = UUID.randomUUID();

            dirtyFlagService.clearDirty(id);

            verify(stringRedisTemplate, times(1)).delete(DIRTY_FLAG + id);
        }
    }
}
