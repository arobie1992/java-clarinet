package com.github.arobie1992.clarinet.sampleapp;

import com.google.api.core.ApiFuture;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class ThreadUtils {
    private ThreadUtils() {}
    public static void uncheckedSleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public static <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
