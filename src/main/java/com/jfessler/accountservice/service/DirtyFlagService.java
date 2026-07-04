package com.jfessler.accountservice.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DirtyFlagService {

    private final StringRedisTemplate stringRedisTemplate;

    public boolean isDirty(UUID id) {
        return stringRedisTemplate.hasKey(key(id));
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
