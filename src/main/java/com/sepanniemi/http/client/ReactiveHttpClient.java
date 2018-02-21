package com.sepanniemi.http.client;

import com.sepanniemi.http.client.configuration.ClientProperties;
import com.sepanniemi.http.client.content.*;
import com.sepanniemi.http.client.publisher.CancellableResponseListenerPublisher;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.reactivex.Single;
import io.reactivex.SingleOperator;
import lombok.Builder;
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
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

@Slf4j
public class ReactiveHttpClient {

    public static final String PATCH = "PATCH";

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
        if (clientProperties != null) {
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
                             ContentProvider contentProvider,
                             ResponseHandler<T> responseParser) {

        return send(responseParser,
                newReactiveRequest(HttpMethod.GET.name(), path, contentProvider));
    }

    @SneakyThrows
    public <T> Single<T> delete(String path,
                                ContentProvider contentProvider,
                                ResponseHandler<T> responseParser) {

        return send(responseParser,
                newReactiveRequest(HttpMethod.DELETE.name(), path, contentProvider));
    }

    public <T> Single<T> post(String path,
                              ContentBodyProvider contentProvider,
                              ResponseHandler<T> responseParser) {

        return send(responseParser, newReactiveRequestWithBody(path, HttpMethod.POST.name(), contentProvider));
    }

    public <T> Single<T> put(String path,
                              ContentBodyProvider contentProvider,
                              ResponseHandler<T> responseParser) {

        return send(responseParser, newReactiveRequestWithBody(path, HttpMethod.PUT.name(), contentProvider));
    }

    public <T> Single<T> patch(String path,
                             ContentBodyProvider contentProvider,
                             ResponseHandler<T> responseParser) {

        return send(responseParser, newReactiveRequestWithBody(path, PATCH, contentProvider));
    }

    private ReactiveRequest newReactiveRequestWithBody(String path,
                                                       String method,
                                                       ContentBodyProvider contentProvider) {
        return ReactiveRequest
                .newBuilder(newRequest(path, method, contentProvider))
                .content(contentProvider.getContent())
                .build();
    }

    private ReactiveRequest newReactiveRequest(String method,
                                               String path,
                                               ContentProvider contentProvider) {
        return ReactiveRequest
                .newBuilder(newRequest(path, method, contentProvider))
                .build();
    }

    private <T> Single<T> send(ResponseHandler<T> responseParser,
                               ReactiveRequest reactiveRequest) {
        Publisher<CompletedResponse> publisher = CancellableResponseListenerPublisher.forRequest(reactiveRequest, complete());

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
    private Request newRequest(String path, String method, ContentProvider contentProvider) {
        Request request =
                httpClient
                        .newRequest(URIUtil.addPath(baseUrl, path))
                        .timeout(clientProperties.getRequestTimeout(), TimeUnit.MILLISECONDS)
                        .method(method);

        contentProvider.getHeaders().forEach(request::header);

        contentProvider.getParameters().forEach(request::param);

        return request;

    }

    private BiFunction<ReactiveResponse, Publisher<ContentChunk>, Publisher<CompletedResponse>> complete() {
        return (response, content) -> {
            ResponseProcessor responseProcessor = new ResponseProcessor(response);
            content.subscribe(responseProcessor);
            return responseProcessor;
        };
    }

}
