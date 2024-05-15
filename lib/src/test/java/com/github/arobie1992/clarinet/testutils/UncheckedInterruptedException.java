package com.github.arobie1992.clarinet.testutils;

public class UncheckedInterruptedException extends RuntimeException {
    public UncheckedInterruptedException(InterruptedException cause) {
        super(cause);
    }
}
