package com.jfessler.accountservice.service;

import com.jfessler.accountservice.model.Account;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccountCacheService {

    private final RedisTemplate<String, Account> accountRedisTemplate;
    private final int ttlHours;

    public AccountCacheService(
            RedisTemplate<String, Account> accountRedisTemplate,
            @Value("${account-service.cache.ttl-hours:1}") int ttlHours) {
        this.accountRedisTemplate = accountRedisTemplate;
        this.ttlHours = ttlHours;
    }

    public Optional<Account> getCachedAccount(UUID id) {
        return Optional.ofNullable(accountRedisTemplate.opsForValue().get(key(id)));
    }

    public void putCachedAccount(Account account) {
        accountRedisTemplate.opsForValue().set(key(account.getId()), account, Duration.ofHours(ttlHours));
    }

    public void evictCachedAccount(UUID id) {
        accountRedisTemplate.delete(key(id));
    }

    private String key(UUID id) {
        return "account:" + id.toString();
    }
}
