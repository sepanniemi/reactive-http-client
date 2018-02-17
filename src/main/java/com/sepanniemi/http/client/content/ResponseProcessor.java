package com.sepanniemi.http.client.content;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveResponse;
import org.eclipse.jetty.reactive.client.internal.AbstractSingleProcessor;
import org.reactivestreams.Subscription;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;


@Slf4j
public class ResponseProcessor extends AbstractSingleProcessor<ContentChunk, CompletedResponse> {
    private final List<byte[]> buffers = new ArrayList<>();
    private final ReactiveResponse response;

    public ResponseProcessor(ReactiveResponse response) {
        this.response = response;
    }

    @Override
    public void onNext(ContentChunk chunk) {
        ByteBuffer buffer = chunk.buffer;
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        buffers.add(bytes);
        chunk.callback.succeeded();
        upStream().request(1);
    }

    @Override
    public void onComplete() {
        int length = buffers.stream().mapToInt(bytes -> bytes.length).sum();
        byte[] bytes = new byte[length];
        int offset = 0;
        for (byte[] b : buffers) {
            int l = b.length;
            System.arraycopy(b, 0, bytes, offset, l);
            offset += l;
        }
        try {
            Map<String, String> responseHeaders =
                    response.getResponse()
                            .getHeaders()
                            .stream()
                            .collect(toMap(HttpField::getName, HttpField::getValue));
            CompletedResponse completedResponse =
                    CompletedResponse
                            .builder()
                            .status(response.getStatus())
                            .reason(response.getResponse().getReason())
                            .body(bytes)
                            .headers(responseHeaders)
                            .build();

            if (log.isDebugEnabled()) {
                log.debug("completed {}", completedResponse);
            }

            downStream().onNext(completedResponse);
        } catch (Exception e) {
            downStream().onError(e);
        }

        super.onComplete();
    }

    @Override
    public void cancel() {
        try {
            super.cancel();
        } catch (NullPointerException e) {
            log.debug("Upstream not available");
        }
    }
}
