package com.sepanniemi.http.client;

import com.sepanniemi.http.client.configuration.ClientConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.client.HttpClientRequest;
import reactor.ipc.netty.http.client.HttpClientResponse;
import reactor.ipc.netty.resources.PoolResources;

import java.net.URI;
import java.time.Duration;
import java.util.function.Function;

@Slf4j
public class ReactiveHttpClient {

    private static final String PATCH = "PATCH";

    private URI baseUrl;

    private final HttpClient httpClient;

    private CircuitBreaker circuitBreaker;

    @Builder.Default
    private ClientConfiguration clientConfiguration = ClientConfiguration.builder().build();

    @Builder
    @SneakyThrows
    private ReactiveHttpClient(URI baseUrl,
                               ClientConfiguration clientConfiguration,
                               CircuitBreaker circuitBreaker) {
        if (clientConfiguration != null) {
            this.clientConfiguration = clientConfiguration;
        }
        this.circuitBreaker = circuitBreaker;

        httpClient = HttpClient.create(opts ->
                opts.poolResources(
                        PoolResources
                                .fixed("netty-client-pool",
                                        10,
                                        this.clientConfiguration.getClientProperties().getConnectionTimeout()))
                        .host(baseUrl.getHost())
                        .port(baseUrl.getPort()));
    }

    /**
     * Gets the configured http client to allow direct customization of the client.
     *
     * @return Http client.
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    @SneakyThrows
    public ReactiveRequest get(String path) {
        return newRequest(httpClient.get(path));
    }

    @SneakyThrows
    public ReactiveRequest delete(String path) {
        return newRequest(httpClient.delete(path));
    }

    public ReactiveRequest post(String path, Function<? super HttpClientRequest, ? extends Publisher<Void>> handler) {
        return newRequest(httpClient.post(path, handler));
    }

    public ReactiveRequest put(String path, Function<? super HttpClientRequest, ? extends Publisher<Void>> handler) {
        return newRequest(httpClient.put(path, handler));
    }

    public ReactiveRequest patch(String path, Function<? super HttpClientRequest, ? extends Publisher<Void>> handler) {
        return newRequest(httpClient.patch(path, handler));
    }

    @SneakyThrows
    private ReactiveRequest newRequest(Mono<HttpClientResponse> request) {
        return new ReactiveRequest(request, clientConfiguration, circuitBreaker);


    }


}
