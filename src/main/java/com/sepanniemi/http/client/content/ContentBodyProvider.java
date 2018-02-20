package com.sepanniemi.http.client.content;

import org.eclipse.jetty.reactive.client.ReactiveRequest;

import java.util.Map;

/**
 * Created by sepanniemi on 08/02/2018.
 */
public interface ContentBodyProvider extends ContentProvider {

    ReactiveRequest.Content getContent();
}
