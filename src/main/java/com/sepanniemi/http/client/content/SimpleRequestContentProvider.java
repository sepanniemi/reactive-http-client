package com.sepanniemi.http.client.content;

import com.sepanniemi.http.client.context.ClientContext;
import lombok.Builder;
import lombok.Singular;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sepanniemi on 20/02/2018.
 */
@Builder
public class SimpleRequestContentProvider implements RequestContentProvider {

    private ClientContext clientContext;

    @Singular
    private Map<String,String> headers = new HashMap<>();
    @Singular
    private Map<String,String> parameters = new HashMap<>();

    @Override
    public Map<String,String> getHeaders(){
        Map<String, String> merged = new HashMap<>(headers);
        if(clientContext!=null){
            merged.putAll(clientContext.getHeaders());
        }
        return merged;
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }
}
