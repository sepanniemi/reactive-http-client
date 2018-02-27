package com.sepanniemi.http.client.error;

import lombok.Data;

import java.util.Optional;


@Data
public class HttpException extends RuntimeException {
    private final int status;
    private byte[] body;

    public HttpException(String message, int status, byte[] body) {
        super(message);
        this.status = status;
        this.body = body;
    }

    public Optional<byte[]> getBody() {
        return Optional.ofNullable(body);
    }
}
