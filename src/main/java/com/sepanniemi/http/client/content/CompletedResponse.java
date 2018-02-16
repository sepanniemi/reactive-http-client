package com.sepanniemi.http.client.content;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

/**
 * Created by sepanniemi on 15/02/2018.
 */
@Builder
@Value
public class CompletedResponse {
    private int status;
    private String reason;
    private byte[] body;
    @Singular
    private Map<String,String> headers;
}
