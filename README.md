# Reactive Http Client implementation

Proof of concept implementation of wrapper for ```jetty-reactive-httpclient``` and ```rxjava2``` to be used in a real world use case to consume HTTP JSON APIs.

Supports circuit breaking with https://github.com/resilience4j/resilience4j Circuit Breaker

Far from being tested or ready to be used in action.

The project can be build with ```./mvnw clean install``` or ```./mvnw.cmd clean install``` if you happen to run in Windows environment.