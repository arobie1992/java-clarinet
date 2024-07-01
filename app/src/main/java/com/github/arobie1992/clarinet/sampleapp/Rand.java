package com.github.arobie1992.clarinet.sampleapp;

import java.util.Random;

public class Rand {
    private Rand() {}

    /**
     * Returns a random integer in the range ({@code min}, {@code max}).
     * @param min The lower bound, inclusive.
     * @param max The upper bound, inclusive.
     * @return the random {@code int}.
     */
    public static int in(int min, int max) {
        return new Random().nextInt(min, max + 1);
    }

    public static boolean flip() {
        return in(0, 1) == 1;
    }

}
