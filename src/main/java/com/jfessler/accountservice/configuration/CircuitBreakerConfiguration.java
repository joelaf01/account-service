package com.jfessler.accountservice.configuration;

import com.jfessler.accountservice.circuitbreaker.CircuitBreakerFallbackExecutor;
import com.jfessler.accountservice.circuitbreaker.ResilientResult;
import com.jfessler.accountservice.model.Account;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;

@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry)
                .bindTo(meterRegistry);

        return circuitBreakerRegistry;
    }

    @Bean
    public CircuitBreaker dirtyFlagCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        // CircuitBreaker configs are placeholders. Actual values for a production deployment would be dialed in through
        // load and performance testing.
        return circuitBreakerRegistry.circuitBreaker(
                "dirty-flag-circuit-breaker",
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .build());
    }

    @Bean
    public CircuitBreaker cacheCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        // CircuitBreaker configs are placeholders. Actual values for a production deployment would be dialed in through
        // load and performance testing.
        return circuitBreakerRegistry.circuitBreaker(
                "cache-circuit-breaker",
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .build());
    }

    @Bean
    public CircuitBreaker repositoryCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        // CircuitBreaker configs are placeholders. Actual values for a production deployment would be dialed in through
        // load and performance testing.
        return circuitBreakerRegistry.circuitBreaker(
                "repository-circuit-breaker",
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .build());
    }

    @Bean
    public CircuitBreakerFallbackExecutor<ResilientResult<Optional<Account>>> repositoryCircuitBreakerFallbackExecutor(
            @Qualifier("repositoryCircuitBreaker") CircuitBreaker repositoryCircuitBreaker,
            @Qualifier("cacheCircuitBreaker") CircuitBreaker cacheCircuitBreaker) {
        // Database is the system of record, so not setting a fallback on empty predicate.
        return new CircuitBreakerFallbackExecutor<>(
                repositoryCircuitBreaker, cacheCircuitBreaker, e -> e instanceof DataAccessException);
    }

    @Bean
    public CircuitBreakerFallbackExecutor<ResilientResult<Optional<Account>>> cacheCircuitBreakerFallbackExecutor(
            @Qualifier("cacheCircuitBreaker") CircuitBreaker cacheCircuitBreaker,
            @Qualifier("repositoryCircuitBreaker") CircuitBreaker repositoryCircuitBreaker) {

        return new CircuitBreakerFallbackExecutor<>(
                cacheCircuitBreaker,
                repositoryCircuitBreaker,
                e -> e instanceof DataAccessException,
                result -> result.value().isEmpty());
    }
}
