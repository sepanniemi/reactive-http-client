package com.sepanniemi.http.client.error;

import com.sepanniemi.http.client.content.CompletedResponse;

/**
 * Created by sepanniemi on 15/02/2018.
 */
public class ErrorHandler {

    public Object handleError( CompletedResponse response ){
        return response.getBody();
    }
}
