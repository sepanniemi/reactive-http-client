package com.sepanniemi.http.client.error;

/**
 * Created by sepanniemi on 17/02/2018.
 */
public class Http4xxException extends HttpException {
    public Http4xxException(int status, byte[] body) {
        super(status, body);
    }

    public Http4xxException(int status) {
        super(status);
    }
}
