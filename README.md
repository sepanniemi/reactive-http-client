# Reactive Http Client implementation

Proof of concept implementation of wrapper for ```jetty-reactive-httpclient``` and ```rxjava2``` to be used in a real world use case to consume HTTP JSON APIs.

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

ResponseHandler<MyResponseBody> responseHandler = new ResponseHandler<>(MyResponseBody.class);

JsonContentProvider<MyRequestBody> content =
                JsonContentProvider.<MyRequestBody>builder()
                        .content(new MyRequestBody())
                        .clientContext(ClientContext
                                .builder()
                                .header("header-x", "value-x")
                                .build())
                        .build();
Single<MyResponseBody> response = reactiveHttpClient.post("/myobjects", content, responseHandler);
```

## Building

The project can be build with ```./mvnw clean install``` or ```./mvnw.cmd clean install``` if you happen to run in Windows environment.