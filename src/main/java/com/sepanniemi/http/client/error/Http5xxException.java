package com.sepanniemi.http.client.error;

/**
 * Created by sepanniemi on 17/02/2018.
 */
public class Http5xxException extends HttpException {
    public Http5xxException(String message, int status, byte[] body) {
        super(message, status, body);
    }
}
