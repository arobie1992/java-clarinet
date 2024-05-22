package com.github.arobie1992.clarinet.testutils;

public class ThreadUtils {
    private ThreadUtils() {}

    public static void sleepUnchecked(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
