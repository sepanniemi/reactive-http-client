package com.sepanniemi.http.client.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Optional;


@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class HttpException extends RuntimeException {
    private final int status;
    private byte[] body;

    public Optional<byte []> getBody(){
        return Optional.ofNullable(body);
    }
}
