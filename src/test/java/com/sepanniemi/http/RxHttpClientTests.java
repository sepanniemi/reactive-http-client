package com.sepanniemi.http;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sepanniemi.http.client.ReactiveHttpClient;
import com.sepanniemi.http.client.configuration.CircuitProperties;
import com.sepanniemi.http.client.content.JsonContentProvider;
import com.sepanniemi.http.client.content.ResponseHandler;
import com.sepanniemi.http.client.context.ClientContext;
import com.sepanniemi.http.client.error.Http4xxException;
import com.sepanniemi.http.client.error.Http5xxException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import lombok.AllArgsConstructor;
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

    private ReactiveHttpClient reactiveHttpClient =
            ReactiveHttpClient
                    .builder()
                    .baseUrl(URI.create("http://localhost:8888"))
                    .circuitBreaker(
                            ReactiveHttpClient.ConfigurableCircuitBreaker.builder()
                                    .name("http-client-circuit")
                                    .circuitProperties(new CircuitProperties().setRingBufferSizeInClosedState(2))
                                    .build()
                                    .getCircuitBreaker())
                    .build();

    @Test
    @SneakyThrows
    public void testGet() {
        wireMockRule.stubFor(any(urlEqualTo("/test")).willReturn(aResponse().withStatus(200).withBody("{\"foo\":\"bar\"}")));

        Single<FooBar> foo = reactiveHttpClient.get("/test", parser);

        TestObserver<FooBar> testSubscriber = new TestObserver<>();
        foo.toObservable().subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertComplete();
        testSubscriber.assertValue(fb -> fb.getFoo().equals("bar"));
    }

    @Test
    @SneakyThrows
    public void testPost() {
        wireMockRule
                .stubFor(any(urlEqualTo("/test"))
                        .withRequestBody(equalToJson("{\"foo\":\"special foo\"}"))
                        .withHeader("x-y", equalTo("1234"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody("{\"foo\":\"bar\"}")));

        JsonContentProvider<FooBar> fooToPost =
                JsonContentProvider.<FooBar>builder()
                        .content(new FooBar("special foo"))
                        .clientContext(ClientContext
                                .builder()
                                .header("x-y", "1234")
                                .build())
                        .build();
        Single<FooBar> foo = reactiveHttpClient.post("/test", fooToPost, parser);

        TestObserver<FooBar> testSubscriber = new TestObserver<>();
        foo.toObservable().subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertComplete();
        testSubscriber.assertValue(fb -> fb.getFoo().equals("bar"));
    }

    @Test
    @SneakyThrows
    public void testGetFailure() {
        wireMockRule.stubFor(any(urlEqualTo("/test")).willReturn(aResponse().withStatus(400).withBody("{\"error\":\"bad_request\"}")));

        Single<FooBar> foo = reactiveHttpClient.get("/test", parser);

        TestObserver<FooBar> testSubscriber = new TestObserver<>();
        foo.toObservable().subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(Http4xxException.class);
    }

    @Test
    @SneakyThrows
    public void testGetFailureCircuitOpens() {
        wireMockRule.stubFor(any(urlEqualTo("/test")).willReturn(aResponse().withStatus(500).withBody("{\"error\":\"server_error\"}")));

        //One
        Single<FooBar> foo = reactiveHttpClient.get("/test", parser);

        TestObserver<FooBar> testSubscriber = new TestObserver<>();
        foo.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(Http5xxException.class);

        //Two
        foo = reactiveHttpClient.get("/test", parser);

        testSubscriber = new TestObserver<>();
        foo.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(Http5xxException.class);

        //Broken
        foo = reactiveHttpClient.get("/test", parser);

        testSubscriber = new TestObserver<>();
        foo.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(CircuitBreakerOpenException.class);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class FooBar {
        private String foo;
    }
}
