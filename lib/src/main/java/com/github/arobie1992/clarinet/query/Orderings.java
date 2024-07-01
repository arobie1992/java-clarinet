package com.github.arobie1992.clarinet.query;

import java.util.Comparator;
import java.util.Random;

public class Orderings {
    private Orderings() {}

    public static <T> Comparator<T> random() {
        return new Comparator<>() {
            private final Random random = new Random();
            @SuppressWarnings("ComparatorMethodParameterNotUsed")
            @Override
            public int compare(T o1, T o2) {
                return random.nextInt(-1, 2);
            }
        };
    }
}
