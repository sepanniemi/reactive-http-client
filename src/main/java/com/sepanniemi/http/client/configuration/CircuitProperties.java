package com.sepanniemi.http.client.configuration;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Duration;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.*;

/**
 * Created by sepanniemi on 17/02/2018.
 */
@Data
@Accessors(chain = true)
public class CircuitProperties {

    private float failureRateThreshold = DEFAULT_MAX_FAILURE_THRESHOLD;
    private int ringBufferSizeInHalfOpenState = DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE;
    private int ringBufferSizeInClosedState = DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE;
    private int waitDurationInOpenState = DEFAULT_WAIT_DURATION_IN_OPEN_STATE;

    public Duration getWaitDurationInOpenState(){
        return Duration.ofSeconds(waitDurationInOpenState);
    }
}
