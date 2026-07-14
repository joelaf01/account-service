package com.jfessler.accountservice.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DirtyFlagService {

    private final StringRedisTemplate stringRedisTemplate;
    private final CircuitBreaker dirtyFlaCircuitBreaker;

    public DirtyFlagService(
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("dirtyFlagCircuitBreaker") CircuitBreaker dirtyFlagCircuitBreaker) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.dirtyFlaCircuitBreaker = dirtyFlagCircuitBreaker;
    }

    public boolean isDirty(UUID id) {
        try {
            return dirtyFlaCircuitBreaker.executeSupplier(() -> stringRedisTemplate.hasKey(key(id)));
        } catch (Exception e) {
            // In case of failure assume dirty rather than risk returning stale data from cache.
            log.warn("Dirty flag check failed for: {}", id, e);
            return true;
        }
    }

    public void markDirty(UUID id) {
        stringRedisTemplate.opsForValue().set(key(id), String.valueOf(true));
    }

    public void clearDirty(UUID id) {
        stringRedisTemplate.delete(key(id));
    }

    private String key(UUID id) {
        return "dirtyFlag:" + id.toString();
    }
}
