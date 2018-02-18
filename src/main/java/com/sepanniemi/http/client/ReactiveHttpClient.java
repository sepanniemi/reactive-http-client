package com.sepanniemi.http.client;

import com.sepanniemi.http.client.configuration.CircuitProperties;
import com.sepanniemi.http.client.configuration.ClientProperties;
import com.sepanniemi.http.client.content.CompletedResponse;
import com.sepanniemi.http.client.content.JsonContentProvider;
import com.sepanniemi.http.client.content.ResponseHandler;
import com.sepanniemi.http.client.content.ResponseProcessor;
import com.sepanniemi.http.client.error.Http4xxException;
import com.sepanniemi.http.client.publisher.CancellableResponseListenerPublisher;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.reactivex.Single;
import io.reactivex.SingleOperator;
import lombok.Builder;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.eclipse.jetty.reactive.client.ReactiveResponse;
import org.eclipse.jetty.util.URIUtil;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;

@Slf4j
public class ReactiveHttpClient {

    private URI baseUrl;

    private final HttpClient httpClient;

    private CircuitBreaker circuitBreaker;

    @Builder.Default
    private ClientProperties clientProperties = new ClientProperties();


    @Builder
    @SneakyThrows
    private ReactiveHttpClient(URI baseUrl,
                               ClientProperties clientProperties,
                               CircuitBreaker circuitBreaker) {

        httpClient = new HttpClient();
        if( clientProperties != null ){
            this.clientProperties = clientProperties;
        }
        httpClient.setConnectTimeout(this.clientProperties.getConnectionTimeout());
        httpClient.start();
        this.baseUrl = baseUrl;
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * Gets the current proxy configuration to allow setting up proxy rules.
     *
     * @return Http client proxy configuration.
     */
    public ProxyConfiguration getProxyConfiguration() {
        return httpClient.getProxyConfiguration();
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
    public <T> Single<T> get(String path,
                             ResponseHandler<T> responseParser) {

        ReactiveRequest reactiveRequest = ReactiveRequest
                .newBuilder(newRequest(path, HttpMethod.GET))
                .build();

        Publisher<CompletedResponse> publisher = CancellableResponseListenerPublisher.forRequest(reactiveRequest, complete());

        return observe(responseParser, publisher);
    }

    @SneakyThrows
    public <T> Single<T> delete(String path,
                                ResponseHandler<T> responseParser) {

        ReactiveRequest reactiveRequest = ReactiveRequest
                .newBuilder(newRequest(path, HttpMethod.DELETE))
                .build();

        Publisher<CompletedResponse> publisher = CancellableResponseListenerPublisher.forRequest(reactiveRequest, complete());

        return observe(responseParser, publisher);
    }

    public <T, I> Single<T> post(String path,
                                 JsonContentProvider<I> contentProvider,
                                 ResponseHandler<T> responseParser) {

        Request request = newRequest(path, HttpMethod.POST);

        contentProvider.getHeaders().forEach(request::header);

        ReactiveRequest reactiveRequest = ReactiveRequest
                .newBuilder(request)
                .content(contentProvider.getContent())
                .build();

        Publisher<CompletedResponse> publisher = reactiveRequest.response(complete());

        return observe(responseParser, publisher);
    }

    private <T> Single<T> observe(ResponseHandler<T> responseParser, Publisher<CompletedResponse> publisher) {
        return Single
                .fromPublisher(publisher)
                .map(responseParser::parseResponse)
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
    private Request newRequest(String path, HttpMethod method) {
        return httpClient
                .newRequest(URIUtil.addPath(baseUrl, path))
                .timeout(clientProperties.getRequestTimeout(), TimeUnit.MILLISECONDS)
                .method(method);
    }

    private BiFunction<ReactiveResponse, Publisher<ContentChunk>, Publisher<CompletedResponse>> complete() {
        return (response, content) -> {
            ResponseProcessor responseProcessor = new ResponseProcessor(response);
            content.subscribe(responseProcessor);
            return responseProcessor;
        };
    }

    @lombok.Builder
    public static class ConfigurableCircuitBreaker {

        private String name;
        @Singular
        private Set<Class<? extends Throwable>> ignoredExceptions = new HashSet<Class<? extends Throwable>>() {{
            add(Http4xxException.class);
        }};
        private CircuitProperties circuitProperties = new CircuitProperties();

        public CircuitBreaker getCircuitBreaker() {
            CircuitBreakerConfig config = new CircuitBreakerConfig.Builder()
                    .recordFailure(shouldRecord())
                    .failureRateThreshold(circuitProperties.getFailureRateThreshold())
                    .ringBufferSizeInClosedState(circuitProperties.getRingBufferSizeInClosedState())
                    .waitDurationInOpenState(circuitProperties.getWaitDurationInOpenState())
                    .build();

            return CircuitBreaker.of(name, config);

        }

        private Predicate<Throwable> shouldRecord() {
            return throwable -> {
                boolean failure = !ignoredExceptions.contains(throwable.getClass());
                log.debug("Circuit breaker handling failure={}, failure recorded={}", throwable, failure);
                return failure;
            };
        }
    }
}
