package com.sepanniemi.http.client.publisher;

import io.reactivex.internal.subscriptions.EmptySubscription;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.eclipse.jetty.reactive.client.ReactiveResponse;
import org.eclipse.jetty.reactive.client.internal.ResponseListenerPublisher;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.util.function.BiFunction;

/**
 * Created by sepanniemi on 17/02/2018.
 */
public class CancellableResponseListenerPublisher<T> extends ResponseListenerPublisher<T> {

    public static <T> Publisher<T> forRequest(ReactiveRequest request, BiFunction<ReactiveResponse, Publisher<ContentChunk>, Publisher<T>> contentFn) {
        return new CancellableResponseListenerPublisher<>(request, contentFn);
    }

    private CancellableResponseListenerPublisher(ReactiveRequest request, BiFunction<ReactiveResponse, Publisher<ContentChunk>, Publisher<T>> contentFn) {
        super(request, contentFn);
    }

    @Override
    public void onComplete(Result result) {
        if (result.isSucceeded()) {
            super.onComplete(result);
        } else {
//            content.fail(result.getFailure());
            downStream().onError(result.getFailure());
        }
    }

    @Override
    protected Subscription upStream() {
        Subscription upstream = super.upStream();
        if( upstream == null ){
            //workaround to avoid null pointer exception when cancelling before subscription.
            upstream = EmptySubscription.INSTANCE;
        }
        return upstream;
    }
}
