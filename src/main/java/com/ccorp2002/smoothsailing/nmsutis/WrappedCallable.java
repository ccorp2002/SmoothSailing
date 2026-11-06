package com.ccorp2002.smoothsailing.nmsutils;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class WrappedCallable<T> implements Runnable {

    private final Callable<T> callable;
    private final CompletableFuture<T> future = new CompletableFuture<>();

    public WrappedCallable(Callable<T> callable) {
        this.callable = callable;
    }

    @Override
    public final void run() {
        try {
            future.complete(callable.call());
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }
    }

    public final T get() {
        try {
            return future.get(30, TimeUnit.SECONDS); //.get();
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

}
