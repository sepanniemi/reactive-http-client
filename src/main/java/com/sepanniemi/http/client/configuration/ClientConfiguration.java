package com.sepanniemi.http.client.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

/**
 * Created by sepanniemi on 28/02/2018.
 */
@Builder
@Getter
@ToString
public class ClientConfiguration {
    @Builder.Default
    private ObjectMapper objectMapper = new ObjectMapper();
    @Builder.Default
    private ClientProperties clientProperties = new ClientProperties();
}
