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
public class CompletedResponse<T> {
    private int status;
    private T body;
    @Singular
    private Map<String,String> headers;
}
