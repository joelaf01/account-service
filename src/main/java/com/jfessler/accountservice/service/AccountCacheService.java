package com.jfessler.accountservice.service;

import com.jfessler.accountservice.model.Account;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountCacheService {

    private final RedisTemplate<String, Account> accountRedisTemplate;

    public Optional<Account> getCachedAccount(UUID id) {
        return Optional.ofNullable(accountRedisTemplate.opsForValue().get(key(id)));
    }

    public void putCachedAccount(Account account) {
        accountRedisTemplate.opsForValue().set(key(account.getId()), account);
    }

    public void evictCachedAccount(UUID id) {
        accountRedisTemplate.delete(key(id));
    }

    private String key(UUID id) {
        return "account:" + id.toString();
    }
}
