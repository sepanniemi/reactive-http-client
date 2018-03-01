package com.sepanniemi.http.client.content;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sepanniemi on 28/02/2018.
 */
@Getter
@ToString
@Builder
public class Headers {
    @Singular
    @NonNull
    private Map<String,String> headers;
}
