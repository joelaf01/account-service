package com.jfessler.accountservice.circuitbreaker;

import com.jfessler.accountservice.exception.FallbackExhaustedException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CircuitBreakerFallbackExecutor<T> {

    private final CircuitBreaker primaryCircuitBreaker;
    private final CircuitBreaker secondaryCircuitBreaker;
    private final Predicate<Throwable> triggerFallbackPredicate;
    private final Predicate<T> emptyPredicate;

    public CircuitBreakerFallbackExecutor(
            CircuitBreaker primaryCircuitBreaker,
            CircuitBreaker secondaryCircuitBreaker,
            Predicate<Throwable> triggerFallbackPredicate) {
        this(primaryCircuitBreaker, secondaryCircuitBreaker, triggerFallbackPredicate, null);
    }

    public CircuitBreakerFallbackExecutor(
            CircuitBreaker primaryCircuitBreaker,
            CircuitBreaker secondaryCircuitBreaker,
            Predicate<Throwable> triggerFallbackPredicate,
            Predicate<T> emptyPredicate) {
        this.primaryCircuitBreaker = primaryCircuitBreaker;
        this.secondaryCircuitBreaker = secondaryCircuitBreaker;
        this.triggerFallbackPredicate = triggerFallbackPredicate;
        this.emptyPredicate = emptyPredicate;
    }

    public T execute(Supplier<T> primarySupplier, Supplier<T> secondarySupplier) {
        try {
            T result = primaryCircuitBreaker.executeSupplier(primarySupplier);
            if (emptyPredicate != null && emptyPredicate.test(result)) {
                return executeWithoutFallback(secondarySupplier, null);
            } else {
                return result;
            }
        } catch (CallNotPermittedException e) {
            return executeWithoutFallback(secondarySupplier, e);
        } catch (Exception e) {
            if (triggerFallbackPredicate != null && triggerFallbackPredicate.test(e)) {
                return executeWithoutFallback(secondarySupplier, e);
            }
            throw e;
        }
    }

    private T executeWithoutFallback(Supplier<T> secondarySupplier, Exception primaryException) {
        try {
            return secondaryCircuitBreaker.executeSupplier(secondarySupplier);
        } catch (Exception secondaryException) {
            FallbackExhaustedException exhaustedException = new FallbackExhaustedException(secondaryException);
            if (primaryException != null) {
                exhaustedException.addSuppressed(primaryException);
            }
            throw exhaustedException;
        }
    }
}
