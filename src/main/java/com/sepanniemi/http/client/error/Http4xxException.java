package com.sepanniemi.http.client.error;

import reactor.core.publisher.Mono;

/**
 * Created by sepanniemi on 17/02/2018.
 */
public class Http4xxException extends HttpException {
    public Http4xxException(String message, int status, Mono<byte[]> body) {
        super(message, status, body);
    }
}
