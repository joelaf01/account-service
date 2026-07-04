package com.jfessler.accountservice.configuration;

import com.jfessler.accountservice.model.Account;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

    @Bean
    public RedisTemplate<String, Account> accountRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Account> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        redisTemplate.setValueSerializer(new JacksonJsonRedisSerializer<>(Account.class));
        redisTemplate.setHashValueSerializer(new JacksonJsonRedisSerializer<>(Account.class));

        return redisTemplate;
    }
}
