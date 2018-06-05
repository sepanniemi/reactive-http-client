package com.sepanniemi.http;

import com.github.tomakehurst.wiremock.global.RequestDelaySpec;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sepanniemi.http.client.ReactiveHttpClient;
import com.sepanniemi.http.client.configuration.CircuitProperties;
import com.sepanniemi.http.client.configuration.ClientConfiguration;
import com.sepanniemi.http.client.configuration.ClientProperties;
import com.sepanniemi.http.client.configuration.ConfigurableCircuitBreaker;
import com.sepanniemi.http.client.content.CompletedResponse;
import com.sepanniemi.http.client.content.Headers;
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
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class RxHttpClientTests {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8888);

    private ReactiveHttpClient reactiveHttpClient =
            ReactiveHttpClient
                    .builder()
                    .baseUrl(URI.create("http://localhost:8888"))
                    .circuitBreaker(
                            ConfigurableCircuitBreaker.builder()
                                    .name("http-client-circuit")
                                    .circuitProperties(new CircuitProperties().setRingBufferSizeInClosedState(2))
                                    .build()
                                    .getCircuitBreaker())
                    .build();

    @Test
    @SneakyThrows
    public void testGet() {
        wireMockRule.stubFor(any(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"foo\":\"bar\"}")));
        Mono<FooBar> foo =
                reactiveHttpClient
                        .get("/test")
                        .response(FooBar.class)
                        .map(CompletedResponse::getBody);

        StepVerifier
                .create(foo)
                .expectNextMatches(fb -> fb.getFoo().equals("bar"))
                .expectComplete()
                .log()
                .verify();
    }

//    @Test
//    @SneakyThrows
//    public void testPost() {
//        wireMockRule
//                .stubFor(any(urlEqualTo("/test"))
//                        .withRequestBody(equalToJson("{\"foo\":\"special foo\"}"))
//                        .withHeader("x-y", equalTo("1234"))
//                        .willReturn(aResponse()
//                                .withStatus(200)
//                                .withBody("{\"foo\":\"bar\"}")));
//
//        Single<FooBar> foo =
//                reactiveHttpClient
//                        .post("/test")
//                        .headers(Headers.builder().header("x-y", "1234").build())
//                        .json(new FooBar("special foo"))
//                        .response(FooBar.class)
//                        .map(CompletedResponse::getBody);
//
//        TestObserver<FooBar> testSubscriber = new TestObserver<>();
//        foo.toObservable().subscribe(testSubscriber);
//        testSubscriber.awaitTerminalEvent();
//        testSubscriber.assertComplete();
//        testSubscriber.assertValue(fb -> fb.getFoo().equals("bar"));
//    }

//    @Test
//    @SneakyThrows
//    public void testGetBadRequest() {
//        wireMockRule.stubFor(any(urlEqualTo("/test")).willReturn(aResponse().withStatus(400).withBody("{\"error\":\"bad_request\"}")));
//
//        Single<FooBar> foo =
//                reactiveHttpClient
//                        .get("/test")
//                        .response(FooBar.class)
//                        .map(CompletedResponse::getBody);
//
//        TestObserver<FooBar> testSubscriber = new TestObserver<>();
//        foo.toObservable().subscribe(testSubscriber);
//        testSubscriber.awaitTerminalEvent();
//        testSubscriber.assertError(Http4xxException.class);
//    }
//
//    @Test
//    @SneakyThrows
//    public void testGetNotFound() {
//
//        Single<FooBar> foo =
//                reactiveHttpClient
//                        .get("/not_found")
//                        .response(FooBar.class)
//                        .map(CompletedResponse::getBody);
//
//        TestObserver<FooBar> testSubscriber = new TestObserver<>();
//        foo.toObservable().subscribe(testSubscriber);
//        testSubscriber.awaitTerminalEvent();
//        testSubscriber.assertError(Http4xxException.class);
//    }
//
//    @Test
//    @SneakyThrows
//    public void testConnectionTimeoutFailure() {
//        wireMockRule.addSocketAcceptDelay(new RequestDelaySpec(5000));
//
//        ReactiveHttpClient reactiveHttpClient =
//                ReactiveHttpClient
//                        .builder()
//                        .baseUrl(URI.create("http://localhost:8889"))
//                        .clientConfiguration(
//                                ClientConfiguration.builder().clientProperties(
//                                        new ClientProperties().setConnectionTimeout(0))
//                                        .build())
//                        .build();
//
//        Single<FooBar> foo =
//                reactiveHttpClient
//                        .get("/test")
//                        .response(FooBar.class)
//                        .map(CompletedResponse::getBody);
//
//        TestObserver<FooBar> testSubscriber = new TestObserver<>();
//        foo.toObservable().subscribe(testSubscriber);
//        testSubscriber.awaitTerminalEvent();
//        testSubscriber.assertError(SocketTimeoutException.class);
//    }
//
//
//    @Test
//    @SneakyThrows
//    public void testRequestTimeoutFailure() {
//        wireMockRule.stubFor(any(urlEqualTo("/test")).willReturn(
//                aResponse()
//                        .withStatus(200)
//                        .withFixedDelay(3000)));
//
//        ReactiveHttpClient reactiveHttpClient =
//                ReactiveHttpClient
//                        .builder()
//                        .baseUrl(URI.create("http://localhost:8888"))
//                        .clientConfiguration(
//                                ClientConfiguration.builder().clientProperties(
//                                        new ClientProperties().setRequestTimeout(1000))
//                                        .build())
//                        .build();
//
//        Single<FooBar> foo =
//                reactiveHttpClient
//                        .get("/test")
//                        .response(FooBar.class)
//                        .map(CompletedResponse::getBody);
//
//        TestObserver<FooBar> testSubscriber = new TestObserver<>();
//        foo.toObservable().subscribe(testSubscriber);
//        testSubscriber.awaitTerminalEvent();
//        testSubscriber.assertError(TimeoutException.class);
//    }
//
//    @Test
//    @SneakyThrows
//    public void testConnectionRefusedFailure() {
//
//        ReactiveHttpClient reactiveHttpClient =
//                ReactiveHttpClient
//                        .builder()
//                        .baseUrl(URI.create("http://localhost:1001"))
//                        .build();
//
//        Single<FooBar> foo =
//                reactiveHttpClient
//                        .get("/test")
//                        .response(FooBar.class)
//                        .map(CompletedResponse::getBody);
//
//        TestObserver<FooBar> testSubscriber = new TestObserver<>();
//        foo.toObservable().subscribe(testSubscriber);
//        testSubscriber.awaitTerminalEvent();
//        testSubscriber.assertError(ConnectException.class);
//    }
//
//
//    @Test
//    @SneakyThrows
//    public void testGetFailureCircuitOpens() {
//        wireMockRule.stubFor(any(urlEqualTo("/test")).willReturn(aResponse().withStatus(500).withBody("{\"error\":\"server_error\"}")));
//
//        //One
//        Single<FooBar> foo =
//                reactiveHttpClient
//                        .get("/test")
//                        .response(FooBar.class)
//                        .map(CompletedResponse::getBody);
//
//        TestObserver<FooBar> testSubscriber = new TestObserver<>();
//        foo.subscribe(testSubscriber);
//        testSubscriber.awaitTerminalEvent();
//        testSubscriber.assertError(Http5xxException.class);
//
//        //Two
//        foo = reactiveHttpClient
//                .get("/test")
//                .response(FooBar.class)
//                .map(CompletedResponse::getBody);
//
//        testSubscriber = new TestObserver<>();
//        foo.subscribe(testSubscriber);
//        testSubscriber.awaitTerminalEvent();
//        testSubscriber.assertError(Http5xxException.class);
//
//        //Broken
//        foo = reactiveHttpClient
//                .get("/test")
//                .response(FooBar.class)
//                .map(CompletedResponse::getBody);
//
//        testSubscriber = new TestObserver<>();
//        foo.subscribe(testSubscriber);
//        testSubscriber.awaitTerminalEvent();
//        testSubscriber.assertError(CircuitBreakerOpenException.class);
//    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class FooBar {
        private String foo;
    }
}
