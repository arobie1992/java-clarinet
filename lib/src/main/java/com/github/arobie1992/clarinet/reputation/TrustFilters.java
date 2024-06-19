package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    public static BiFunction<Stream<? extends PeerId>, Function<PeerId, Double>, Stream<? extends PeerId>> minAndStandardDeviation(double minValue) {
        return (peerIds, repFn) -> {
            var peersAndRep = peerIds.collect(Collectors.toMap(p -> p, repFn));
            if(peersAndRep.isEmpty()) {
                return Stream.empty();
            }
            var mean = mean(peersAndRep.values());
            var standardDeviation = standardDeviation(peersAndRep.values());
            return peersAndRep.keySet().stream().filter(p -> {
                var rep = peersAndRep.get(p);
                return rep >= minValue && rep >= mean - standardDeviation;
            });
        };
    }

}
