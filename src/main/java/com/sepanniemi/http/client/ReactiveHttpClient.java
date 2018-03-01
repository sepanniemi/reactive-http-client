package com.sepanniemi.http.client;

import com.sepanniemi.http.client.configuration.ClientConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.URIUtil;

import java.net.URI;
import java.util.concurrent.TimeUnit;

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

        httpClient = new HttpClient();
        if(clientConfiguration != null ){
            this.clientConfiguration = clientConfiguration;
        }
        httpClient.setConnectTimeout(this.clientConfiguration.getClientProperties().getConnectionTimeout());
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
    public ReactiveRequest get(String path) {
        return newRequest(HttpMethod.GET.name(), path);
    }

    @SneakyThrows
    public ReactiveRequest delete(String path) {
        return newRequest(HttpMethod.DELETE.name(), path);
    }

    public ReactiveRequest post(String path) {
        return newRequest(HttpMethod.POST.name(), path);
    }

    public ReactiveRequest put(String path) {
        return newRequest(HttpMethod.PUT.name(), path);
    }

    public ReactiveRequest patch(String path) {
        return newRequest(PATCH, path);
    }


    @SneakyThrows
    private ReactiveRequest newRequest(String method, String path) {
        Request request =
                httpClient
                        .newRequest(URIUtil.addPath(baseUrl, path))
                        .timeout(clientConfiguration.getClientProperties().getRequestTimeout(), TimeUnit.MILLISECONDS)
                        .method(method);

        return new ReactiveRequest(request, clientConfiguration, circuitBreaker);
    }
}
