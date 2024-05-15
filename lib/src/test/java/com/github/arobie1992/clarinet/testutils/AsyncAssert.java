package com.github.arobie1992.clarinet.testutils;

public class AsyncAssert {

    private final Thread thread;
    private AssertionError exception;

    public AsyncAssert(Runnable runnable) {
        this.thread = Thread.ofVirtual().unstarted(() -> {
            try {
                runnable.run();
            } catch(AssertionError e) {
                exception = e;
            }
        });
    }

    public void start() {
        thread.start();
    }

    public void join() throws InterruptedException {
        thread.join();
        if(exception != null) {
            throw exception;
        }
    }

    public static AsyncAssert started(Runnable runnable) {
        var async = new AsyncAssert(runnable);
        async.start();
        return async;
    }

}
