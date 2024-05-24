package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * An assortment of general predefined trust filter functions.
 */
public class TrustFilters {
    private TrustFilters() {}

    private static double standardDeviation(Collection<Double> values) {
        if(values.size() <= 1) {
            return 0;
        }

        var mean = mean(values);

        var variance = 0.0;
        for (var i : values) {
            variance += Math.pow(i - mean, 2);
        }
        variance /= (values.size() - 1);

        return Math.sqrt(variance);
    }

    private static double mean(Collection<Double> values) {
        // want this to throw if there aren't any
        //noinspection OptionalGetWithoutIsPresent
        return values.stream().mapToDouble(i -> i).average().getAsDouble();
    }

    public static Function<Stream<? extends Reputation>, Stream<PeerId>> minAndStandardDeviation(double minValue) {
        return reputationStream -> {
            var asList = reputationStream.toList();
            if(asList.isEmpty()) {
                return Stream.empty();
            }
            var repValues = asList.stream().map(Reputation::value).toList();
            var mean = mean(repValues);
            var standardDeviation = standardDeviation(repValues);
            return asList.stream().filter(rep -> rep.value() >= minValue && rep.value() >= mean - standardDeviation).map(Reputation::peerId);
        };
    }

}
