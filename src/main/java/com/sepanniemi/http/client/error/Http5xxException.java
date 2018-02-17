package com.sepanniemi.http.client.error;

/**
 * Created by sepanniemi on 17/02/2018.
 */
public class Http5xxException extends HttpException {
    public Http5xxException(int status, byte[] body) {
        super(status, body);
    }

    public Http5xxException(int status) {
        super(status);
    }
}
