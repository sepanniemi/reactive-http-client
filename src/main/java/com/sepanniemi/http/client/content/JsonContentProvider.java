package com.sepanniemi.http.client.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sepanniemi.http.client.context.ClientContext;
import io.reactivex.Flowable;
import lombok.Builder;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.Value;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@Builder
public class JsonContentProvider<T> implements ContentBodyProvider {

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
    public ReactiveRequest.Content getContent() {
        return ReactiveRequest.Content.fromPublisher(provide(),
                "application/json",
                StandardCharsets.UTF_8);
    }

    private Publisher<ContentChunk> provide() {
        return Flowable.just(content)
                .map(this::writeBytes)
                .map(ByteBuffer::wrap)
                .map(ContentChunk::new);
    }

    @SneakyThrows
    private byte[] writeBytes(T content) {
        return objectMapper.writer().writeValueAsBytes(content);
    }
}
