package com.sepanniemi.http.client.content;

import org.eclipse.jetty.client.api.ContentProvider;

import java.util.Map;
import java.util.Optional;

/**
 * Created by sepanniemi on 08/02/2018.
 */
public interface RequestContentProvider {

    Map<String,String> getHeaders();

    Map<String,String> getParameters();

    default Optional<ContentProvider.Typed> getContent(){
        return Optional.empty();
    }

}
