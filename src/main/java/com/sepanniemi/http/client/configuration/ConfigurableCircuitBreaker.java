package com.sepanniemi.http.client.configuration;

import com.sepanniemi.http.client.error.Http4xxException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.Builder;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Created by sepanniemi on 20/02/2018.
 */
@Slf4j
public class ConfigurableCircuitBreaker {

    private String name;

    @Singular
    private Set<Class<? extends Throwable>> ignoredExceptions;

    @Builder
    public ConfigurableCircuitBreaker(String name, Set<Class<? extends Throwable>> ignoredExceptions, CircuitProperties circuitProperties) {
        this.name = name != null ? name : "http-client-circuit";
        this.ignoredExceptions = ignoredExceptions != null ? ignoredExceptions : new HashSet<>();
        this.ignoredExceptions.add(Http4xxException.class);
        this.circuitProperties = circuitProperties != null ? circuitProperties : new CircuitProperties();

    }

    @Builder.Default
    private CircuitProperties circuitProperties;

    public CircuitBreaker getCircuitBreaker() {
        CircuitBreakerConfig config = new CircuitBreakerConfig.Builder()
                .recordFailure(shouldRecord())
                .failureRateThreshold(circuitProperties.getFailureRateThreshold())
                .ringBufferSizeInClosedState(circuitProperties.getRingBufferSizeInClosedState())
                .waitDurationInOpenState(circuitProperties.getWaitDurationInOpenState())
                .build();

        return CircuitBreaker.of(name, config);
    }

    private Predicate<Throwable> shouldRecord() {
        return throwable -> {
            boolean failure = !ignoredExceptions.contains(throwable.getClass());
            log.debug("Circuit breaker handling an exception={}, failure recorded={}", throwable, failure);
            return failure;
        };
    }
}
