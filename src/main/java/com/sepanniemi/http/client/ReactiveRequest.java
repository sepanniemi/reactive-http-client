package com.sepanniemi.http.client;

import com.sepanniemi.http.client.configuration.ClientConfiguration;
import com.sepanniemi.http.client.content.CompletedResponse;
import com.sepanniemi.http.client.content.Headers;
import com.sepanniemi.http.client.content.Parameters;
import com.sepanniemi.http.client.error.Http4xxException;
import com.sepanniemi.http.client.error.Http5xxException;
import com.sepanniemi.http.client.error.HttpException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleOperator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpStatus;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Created by sepanniemi on 28/02/2018.
 */
@Slf4j
public class ReactiveRequest {

    private enum ContentType {
        JSON, XML
    }

    private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json;charset=UTF-8";

    private Request request;

    private ClientConfiguration clientConfiguration;

    private CircuitBreaker circuitBreaker;

    private ContentType contentType;


    public ReactiveRequest(Request request, ClientConfiguration clientConfiguration, CircuitBreaker circuitBreaker) {
        this.request = request;
        this.clientConfiguration = clientConfiguration;
        this.circuitBreaker = circuitBreaker;
    }

    public ReactiveRequest json(Object content) {
        request.content(new BytesContentProvider(APPLICATION_JSON_CHARSET_UTF_8, writeJsonBytes(content)));
        //we expect the result to be json also.
        this.contentType = ContentType.JSON;
        return this;
    }

    public ReactiveRequest headers(Headers headers) {
        headers.getHeaders().forEach(request::header);
        return this;
    }

    public ReactiveRequest parameters(Parameters parameters) {
        parameters.getParameters().forEach(request::param);
        return this;
    }

    public <T> Single<CompletedResponse<T>> response(Class<T> responseType) {
        return Single
                .create(sendForResponse(request, responseType))
                .lift(fused());
    }

    private <T> SingleOperator<T, T> fused() {
        if (circuitBreaker != null) {
            return CircuitBreakerOperator.of(circuitBreaker);
        } else {
            return downstream -> downstream;
        }
    }

    @SneakyThrows
    private byte[] writeJsonBytes(Object content) {
        return clientConfiguration.getObjectMapper().writer().writeValueAsBytes(content);
    }

    @SneakyThrows
    private <T> T readBytes(byte[] bytes, Class<T> responseType) {
        if (ContentType.JSON.equals(contentType)) {
            return readJsonBytes(bytes, responseType);
        } else {
            throw new UnsupportedOperationException("XML Deserialization not supported yet.");
        }
    }

    @SneakyThrows
    private <T> T readJsonBytes(byte[] bytes, Class<T> responseType) {
        return clientConfiguration.getObjectMapper().readValue(bytes, responseType);
    }

    private <T> SingleOnSubscribe<CompletedResponse<T>> sendForResponse(Request request,
                                                                    Class<T> responseType) {
        return emitter -> request.send(onResponse(emitter, responseType));
    }

    private <T> Response.Listener.Adapter onResponse(SingleEmitter<? super CompletedResponse<T>> emitter,
                                                 Class<T> responseType) {

        return new Response.Listener.Adapter() {
            private final List<byte[]> buffers = new ArrayList<>();

            @Override
            public void onContent(Response response, ByteBuffer buffer) {
                log.debug("Content received for response={}", response);
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                buffers.add(bytes);
            }

            @Override
            public void onComplete(Result result) {
                log.debug("Completed result={}", result);
                Response response = result.getResponse();
                HttpStatus.Code statusCode = HttpStatus.getCode(response.getStatus());
                if (statusCode.isSuccess()) {
                    try {
                        Map<String, String> responseHeaders =
                                result.getResponse()
                                        .getHeaders()
                                        .stream()
                                        .collect(toMap(HttpField::getName, HttpField::getValue));
                        CompletedResponse<T> completedResponse =
                                CompletedResponse
                                        .<T>builder()
                                        .status(response.getStatus())
                                        .body(readJsonBytes(getContent(), responseType))
                                        .headers(responseHeaders)
                                        .build();

                        log.debug("Request compeleted with response={}", completedResponse);
                        emitter.onSuccess(completedResponse);
                    } catch (Exception e) {
                        emitter.onError(e);
                    }

                } else if (statusCode.isClientError()) {
                    emitter.onError(new Http4xxException(response.getReason(),
                            statusCode.getCode(),
                            getContent()));
                } else if (statusCode.isServerError()) {
                    emitter.onError(new Http5xxException(response.getReason(),
                            statusCode.getCode(),
                            getContent()));
                } else {
                    emitter.onError(new HttpException(response.getReason(),
                            statusCode.getCode(),
                            getContent()));
                }
            }

            private byte[] getContent() {
                if (buffers.isEmpty()) {
                    return null;
                }
                int totalLength = buffers.stream().mapToInt(bytes -> bytes.length).sum();
                byte[] bytes = new byte[totalLength];
                int offset = 0;
                for (byte[] chunk : buffers) {
                    int length = chunk.length;
                    System.arraycopy(chunk, 0, bytes, offset, length);
                    offset += length;
                }
                return bytes;
            }

            @Override
            public void onFailure(Response response, Throwable failure) {
                log.debug("Request failed for response={} with failure={}", response, failure);
                emitter.onError(failure);
            }
        };
    }


}
