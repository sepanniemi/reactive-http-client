package com.sepanniemi.http.client;

import com.sepanniemi.http.client.configuration.ClientProperties;
import com.sepanniemi.http.client.content.CompletedResponse;
import com.sepanniemi.http.client.content.RequestContentProvider;
import com.sepanniemi.http.client.content.ResponseHandler;
import com.sepanniemi.http.client.error.Http4xxException;
import com.sepanniemi.http.client.error.Http5xxException;
import com.sepanniemi.http.client.error.HttpException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleOperator;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.URIUtil;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toMap;

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
    public <T> Single<CompletedResponse<T>> get(String path,
                                                RequestContentProvider requestContentProvider,
                                                ResponseHandler<T> responseParser) {
        return send(responseParser,
                newRequest(HttpMethod.GET.name(), path, requestContentProvider));
    }

    @SneakyThrows
    public <T> Single<CompletedResponse<T>> delete(String path,
                                                   RequestContentProvider requestContentProvider,
                                                   ResponseHandler<T> responseParser) {
        return send(responseParser,
                newRequest(HttpMethod.DELETE.name(), path, requestContentProvider));
    }

    public <T> Single<CompletedResponse<T>> post(String path,
                                                 RequestContentProvider contentProvider,
                                                 ResponseHandler<T> responseParser) {
        return send(responseParser, newRequest(HttpMethod.POST.name(), path, contentProvider));
    }

    public <T> Single<CompletedResponse<T>> put(String path,
                                                RequestContentProvider contentProvider,
                                                ResponseHandler<T> responseParser) {
        return send(responseParser, newRequest(HttpMethod.PUT.name(), path, contentProvider));
    }

    public <T> Single<CompletedResponse<T>> patch(String path,
                                                  RequestContentProvider contentProvider,
                                                  ResponseHandler<T> responseParser) {
        return send(responseParser, newRequest(PATCH, path, contentProvider));
    }


    private <T> Single<CompletedResponse<T>> send(ResponseHandler<T> responseParser,
                                                  Request reactiveRequest) {
        return Single
                .create(sendForResponse(reactiveRequest, responseParser))
                .lift(fused());
    }

    private <T> SingleOnSubscribe<CompletedResponse<T>> sendForResponse(Request request,
                                                                        ResponseHandler<T> responseParser) {
        return emitter -> request.send(onResponse(emitter, responseParser));
    }

    private <T> Response.Listener.Adapter onResponse(SingleEmitter<? super CompletedResponse<T>> emitter,
                                                     ResponseHandler<T> responseParser) {

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
                                        .body(responseParser.parseResponse(getContent()))
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
                if( buffers.isEmpty()){
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


    private <T> SingleOperator<T, T> fused() {
        if (circuitBreaker != null) {
            return CircuitBreakerOperator.of(circuitBreaker);
        } else {
            return downstream -> downstream;
        }
    }

    @SneakyThrows
    private Request newRequest(String method, String path, RequestContentProvider requestContentProvider) {
        Request request =
                httpClient
                        .newRequest(URIUtil.addPath(baseUrl, path))
                        .timeout(clientProperties.getRequestTimeout(), TimeUnit.MILLISECONDS)
                        .method(method);

        requestContentProvider.getContent().ifPresent(request::content);

        requestContentProvider.getHeaders().forEach(request::header);

        requestContentProvider.getParameters().forEach(request::param);

        return request;
    }
}
