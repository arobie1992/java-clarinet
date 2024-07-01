package com.github.arobie1992.clarinet.query;

import java.util.Comparator;
import java.util.function.Predicate;

public record QueryTerms<T>(Predicate<T> where, Comparator<T> order) {
}
