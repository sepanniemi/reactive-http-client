package com.sepanniemi.http.client.configuration;

import com.sepanniemi.http.client.configuration.CircuitProperties;
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
@Builder
public class ConfigurableCircuitBreaker {

    private String name;
    @Singular
    private Set<Class<? extends Throwable>> ignoredExceptions = new HashSet<Class<? extends Throwable>>() {{
        add(Http4xxException.class);
    }};
    private CircuitProperties circuitProperties = new CircuitProperties();

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
