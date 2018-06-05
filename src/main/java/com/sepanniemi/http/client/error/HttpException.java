package com.sepanniemi.http.client.error;

import lombok.Data;
import reactor.core.publisher.Mono;

import java.util.Optional;


@Data
public class HttpException extends RuntimeException {
    private final int status;
    private Mono<byte[]> body;

    public HttpException(String message, int status, Mono<byte[]> body) {
        super(message);
        this.status = status;
        this.body = body;
    }
}
