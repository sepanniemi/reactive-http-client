package com.sepanniemi.http.client.content;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sepanniemi on 28/02/2018.
 */
@Builder
@Getter
@ToString
public class Parameters {
    @Singular
    private Map<String,String> parameters = new HashMap<>();
}
