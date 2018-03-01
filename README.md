# Reactive Http Client implementation

Proof of concept implementation of a ```rxjava2``` wrapper for ```jetty-client``` to be used in a real world use case to consume HTTP JSON APIs.

Supports circuit breaking with https://github.com/resilience4j/resilience4j Circuit Breaker

## Usage

Initializing the client
```java
ReactiveHttpClient reactiveHttpClient =
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
```
Sending request

```java
Single<MyResponseBody> response =
                reactiveHttpClient
                        .post("/myobjects")
                        .headers(Headers
                                    .builder()
                                    .header("header-x", "value-x")
                                    .build())
                        .json(new MyRequestBody())
                        .response(MyResponseBody.class)
                        .map(CompletedResponse::getBody);
```

## Building

The project can be build with ```./mvnw clean install``` or ```./mvnw.cmd clean install``` if you happen to run in Windows environment.