package com.sepanniemi.http.client.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sepanniemi.http.client.error.HttpException;
import lombok.SneakyThrows;

/**
 * Created by sepanniemi on 07/02/2018.
 */
public class ResponseHandler<T> {

    private final Class<T> type;
    private ObjectMapper objectMapper = new ObjectMapper();

    public ResponseHandler(Class<T> type) {
        this.type = type;
    }

    @SneakyThrows
    public T parseResponse(CompletedResponse response) {
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            return objectMapper.readValue(response.getBody(), type);
        } else {
            throw new HttpException(response.getStatus(), response.getBody());
        }
    }


}
