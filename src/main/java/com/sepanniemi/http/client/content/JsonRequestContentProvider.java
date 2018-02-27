package com.sepanniemi.http.client.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sepanniemi.http.client.context.ClientContext;
import lombok.Builder;
import lombok.Singular;
import lombok.SneakyThrows;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.util.BytesContentProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Builder
public class JsonRequestContentProvider<T> implements RequestContentProvider {

    public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json;charset=UTF-8";

    @Builder.Default
    private ObjectMapper objectMapper = new ObjectMapper();

    private ClientContext clientContext;

    private T content;

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

    @Override
    public Optional<ContentProvider.Typed> getContent() {
        return Optional.of(new BytesContentProvider(APPLICATION_JSON_CHARSET_UTF_8, writeBytes(content)));
    }


    @SneakyThrows
    private byte[] writeBytes(T content) {
        return objectMapper.writer().writeValueAsBytes(content);
    }
}
