package com.sepanniemi.http.client.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sepanniemi.http.client.error.Http4xxException;
import com.sepanniemi.http.client.error.Http5xxException;
import com.sepanniemi.http.client.error.HttpException;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by sepanniemi on 07/02/2018.
 */
@Slf4j
@Data
public class ResponseHandler<T> {

    private final Class<T> type;
    private ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public T parseResponse(CompletedResponse response) {
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            return objectMapper.readValue(response.getBody(), type);
        } else if (response.getStatus() >= 400 && response.getStatus() < 500) {
            log.debug("Client error");
            throw new Http4xxException(response.getStatus(), response.getBody());
        } else {
            log.debug("Server error");
            throw new Http5xxException(response.getStatus(), response.getBody());
        }
    }


}
