package com.sepanniemi.http;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sepanniemi.http.client.content.ResponseHandler;
import com.sepanniemi.http.client.ReactiveHttpClient;
import com.sepanniemi.http.client.error.HttpException;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class RxHttpClientTests {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8888);

    private ResponseHandler<FooBar> parser = new ResponseHandler<>(FooBar.class);

    @Test
    @SneakyThrows
    public void testGet(){
        wireMockRule.stubFor(any(urlEqualTo("/test")).willReturn(aResponse().withStatus(200).withBody("{\"foo\":\"bar\"}")));
        ReactiveHttpClient reactiveHttpClient = ReactiveHttpClient.builder().baseUrl(URI.create("http://localhost:8888")).build();
        Single<FooBar> foo = reactiveHttpClient.get("/test", parser);

        TestObserver<FooBar> testSubscriber = new TestObserver<>();
        foo.toObservable().subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertComplete();
        testSubscriber.assertValue( fb -> fb.getFoo().equals("bar") );
    }

    @Test
    @SneakyThrows
    public void testGetFailure(){
        wireMockRule.stubFor(any(urlEqualTo("/test")).willReturn(aResponse().withStatus(400).withBody("{\"error\":\"bad_request\"}")));
        ReactiveHttpClient reactiveHttpClient = ReactiveHttpClient.builder().baseUrl(URI.create("http://localhost:8888")).build();
        Single<FooBar> foo = reactiveHttpClient.get("/test", parser);

        TestObserver<FooBar> testSubscriber = new TestObserver<>();
        foo.toObservable().subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(HttpException.class);
    }

    @Data
    @NoArgsConstructor
    private static class FooBar {
        private String foo;
    }
}
