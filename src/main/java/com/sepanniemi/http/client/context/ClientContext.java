package com.sepanniemi.http.client.context;


import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@Builder
@Value
public class ClientContext {
    @Singular
    Map<String,String> headers;

}
