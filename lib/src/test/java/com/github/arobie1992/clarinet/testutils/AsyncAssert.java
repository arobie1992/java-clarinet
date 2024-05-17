package com.github.arobie1992.clarinet.testutils;

public class AsyncAssert {

    @FunctionalInterface
    public interface ThrowingRunnable extends Runnable {
        default void run() {
            try {
                throwableRun();
            } catch (AssertionError|RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        void throwableRun() throws Throwable;
    }

    private final Thread thread;
    private Throwable exception;

    public AsyncAssert(ThrowingRunnable runnable) {
        this.thread = Thread.ofVirtual().unstarted(() -> {
            try {
                runnable.run();
            } catch(Throwable e) {
                exception = e;
            }
        });
    }

    public void start() {
        thread.start();
    }

    public void join() throws Throwable {
        thread.join();
        if(exception != null) {
            throw exception;
        }
    }

    public static AsyncAssert started(ThrowingRunnable runnable) {
        var async = new AsyncAssert(runnable);
        async.start();
        return async;
    }

}