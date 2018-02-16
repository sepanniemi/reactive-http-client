package com.sepanniemi.http.client;

import com.sepanniemi.http.client.content.CompletedResponse;
import com.sepanniemi.http.client.content.JsonContentProvider;
import com.sepanniemi.http.client.content.ResponseHandler;
import com.sepanniemi.http.client.content.ResponseProcessor;
import io.reactivex.Single;
import lombok.Builder;
import lombok.SneakyThrows;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.eclipse.jetty.reactive.client.ReactiveResponse;
import org.eclipse.jetty.util.URIUtil;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.function.BiFunction;


public class ReactiveHttpClient {

    private URI baseUrl;

    private final HttpClient httpClient;

    @Builder
    @SneakyThrows
    public ReactiveHttpClient(URI baseUrl) {
        httpClient = new HttpClient();
        httpClient.start();
        this.baseUrl = baseUrl;
    }

    @SneakyThrows
    public <T> Single<T> get(String path,
                             ResponseHandler<T> parser) {

        ReactiveRequest reactiveRequest = ReactiveRequest
                .newBuilder(newRequest(path, HttpMethod.GET))
                .build();

        Publisher<CompletedResponse> publisher = reactiveRequest.response(complete());

        return Single
                .fromPublisher(publisher)
                .map(parser::parseResponse);

    }

    @SneakyThrows
    public <T> Single<T> delete(String path,
                                ResponseHandler<T> parser) {

        ReactiveRequest reactiveRequest = ReactiveRequest
                .newBuilder(newRequest(path, HttpMethod.DELETE))
                .build();

        Publisher<CompletedResponse> publisher = reactiveRequest.response(complete());

        return Single
                .fromPublisher(publisher)
                .map(parser::parseResponse);

    }

    public <T, I> Single<T> post(Class<T> type,
                                 String path,
                                 JsonContentProvider<I> contentProvider,
                                 ResponseHandler<T> responseParser) {

        Request request = newRequest(path, HttpMethod.POST);

        contentProvider.getHeaders().forEach(request::header);

        ReactiveRequest reactiveRequest = ReactiveRequest
                .newBuilder(request)
                .content(contentProvider.getContent())
                .build();

        Publisher<CompletedResponse> publisher = reactiveRequest.response(complete());

        return Single
                .fromPublisher(publisher)
                .map(responseParser::parseResponse);

    }

    @SneakyThrows
    private Request newRequest(String path, HttpMethod method) {

        return httpClient
                .newRequest(URIUtil.addPath(baseUrl, path))
                .method(method);
    }

    private BiFunction<ReactiveResponse, Publisher<ContentChunk>, Publisher<CompletedResponse>> complete() {
        return (response, content) -> {
            ResponseProcessor responseProcessor = new ResponseProcessor(response);
            content.subscribe(responseProcessor);
            return responseProcessor;
        };
    }
}
