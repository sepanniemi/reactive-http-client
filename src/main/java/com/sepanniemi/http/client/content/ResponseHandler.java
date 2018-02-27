package com.sepanniemi.http.client.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

/**
 * Created by sepanniemi on 07/02/2018.
 */
@Slf4j
@Data
public class ResponseHandler<T> {

    private final Class<T> type;
    private ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public T parseResponse(byte[] body) {
        return objectMapper.readValue(body, type);
    }
}
