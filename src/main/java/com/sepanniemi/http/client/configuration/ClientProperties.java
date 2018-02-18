package com.sepanniemi.http.client.configuration;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created by sepanniemi on 18/02/2018.
 */
@Data
@Accessors(chain = true)
public class ClientProperties {

    /**
     * Connection timeout in milliseconds.
     */
    private int connectionTimeout = 10000;

    /**
     * Timeout for handling the request.
     */
    private int requestTimeout = 5000;
}
