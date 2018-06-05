package com.sepanniemi.http.client;

import com.sepanniemi.http.client.configuration.ClientConfiguration;
import com.sepanniemi.http.client.content.CompletedResponse;
import com.sepanniemi.http.client.error.Http4xxException;
import com.sepanniemi.http.client.error.Http5xxException;
import com.sepanniemi.http.client.error.HttpException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClientResponse;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

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

    private Mono<HttpClientResponse> request;

    private ClientConfiguration clientConfiguration;

    private CircuitBreaker circuitBreaker;

    private ContentType contentType;


    public ReactiveRequest(Mono<HttpClientResponse> request,
                           ClientConfiguration clientConfiguration,
                           CircuitBreaker circuitBreaker) {
        this.request = request;
        this.clientConfiguration = clientConfiguration;
        this.circuitBreaker = circuitBreaker;
    }


    public <T> Mono<CompletedResponse<T>> response(Class<T> responseType) {
        return request
                .timeout(Duration.ofMillis(clientConfiguration.getClientProperties().getRequestTimeout()))
                .transform(fused())
                .map(this::validatedResponse)
                .flatMap(cr -> readResponse(cr, responseType));

    }

    private <T> Function<Publisher<T>, Publisher<T>> fused() {
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

    private <T> Mono<CompletedResponse<T>> readResponse(HttpClientResponse clientResponse,
                                                        Class<T> responseType) {
        Map<String, String> responseHeaders =
                clientResponse.responseHeaders()
                        .entries()
                        .stream()
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        return readBytes(clientResponse.receive().aggregate().asByteArray(), responseType)
                .map(body ->
                        buildCompletedResponse(
                                clientResponse.status().code(),
                                body,
                                responseHeaders)
                );
    }

    private <T> CompletedResponse<T> buildCompletedResponse(int statusCode,
                                                            T body,
                                                            Map<String, String> responseHeaders) {
        return CompletedResponse
                .<T>builder()
                .status(statusCode)
                .body(body)
                .headers(responseHeaders)
                .build();
    }

    @SneakyThrows
    private <T> Mono<T> readBytes(Mono<byte[]> bm, Class<T> responseType) {
//        if (ContentType.JSON.equals(contentType)) {
            return bm.map(bytes -> readJsonBytes(bytes, responseType));
//        } else {
//            throw new UnsupportedOperationException("XML Deserialization not supported yet.");
//        }
    }

    @SneakyThrows
    private <T> T readJsonBytes(byte[] bytes, Class<T> responseType) {
        return clientConfiguration.getObjectMapper().readValue(bytes, responseType);
    }

    private HttpClientResponse validatedResponse(HttpClientResponse response) {
        HttpResponseStatus status = response.status();
        if (HttpStatusClass.SUCCESS.equals(status.codeClass())) {
            return response;
        }
        if (HttpStatusClass.CLIENT_ERROR.equals(status.codeClass())) {
            throw new Http4xxException(status.reasonPhrase(),
                    status.code(),
                    response.receive().aggregate().asByteArray());
        } else if (HttpStatusClass.SERVER_ERROR.equals(status.codeClass())) {
            throw new Http5xxException(status.reasonPhrase(),
                    status.code(),
                    response.receive().aggregate().asByteArray());
        } else {
            throw new HttpException(status.reasonPhrase(),
                    status.code(),
                    response.receive().aggregate().asByteArray());
        }
    }

}
