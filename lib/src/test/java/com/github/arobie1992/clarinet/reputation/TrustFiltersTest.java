package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.impl.peer.StringPeerId;
import com.github.arobie1992.clarinet.peer.PeerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TrustFiltersTest {

    private final PeerId minId = new StringPeerId("min");
    private final PeerId badId = new StringPeerId("bad");
    private final PeerId stdId = new StringPeerId("std");
    /*
    mean    = 0.78181818181818
    std dev = 0.37372372347444
    thresh  = 0.408094458344
     */
    // Map.of can only accept 10 kv pairs so need to use Map.ofEntries which takes varargs.
    private final Map<? extends PeerId, Double> reps = Map.ofEntries(
            Map.entry(new StringPeerId("good1"), 1.0),
            Map.entry(new StringPeerId("good2"), 1.0),
            Map.entry(new StringPeerId("good3"), 1.0),
            Map.entry(new StringPeerId("good4"), 1.0),
            Map.entry(new StringPeerId("good5"), 1.0),
            Map.entry(new StringPeerId("good6"), 1.0),
            Map.entry(new StringPeerId("good7"), 1.0),
            Map.entry(new StringPeerId("good8"), 1.0),
            Map.entry(minId, 0.5),
            Map.entry(badId, 0.0),
            Map.entry(stdId, 0.1)
    );
    private final Function<PeerId, Double> repFn = reps::get;

    @BeforeEach
    void setUp() {

    }

    @Test
    void testMinAndStandardDeviationMin() {
        var result = TrustFilters.minAndStandardDeviation(0.51).apply(reps.keySet().stream(), repFn).toList();
        assertEquals(8, result.size());
        assertFalse(result.stream().anyMatch(minId::equals));
        assertFalse(result.stream().anyMatch(badId::equals));
        assertFalse(result.stream().anyMatch(stdId::equals));
    }

    @Test
    void testMinAndStandardDeviationStd() {
        var result = TrustFilters.minAndStandardDeviation(0.05).apply(reps.keySet().stream(), repFn).toList();
        assertEquals(9, result.size());
        assertFalse(result.stream().anyMatch(badId::equals));
        assertFalse(result.stream().anyMatch(stdId::equals));
    }

    @Test
    void testMinAndStandardDeviationAllGood() {
        var pid = new StringPeerId("good");
        assertEquals(List.of(pid), TrustFilters.minAndStandardDeviation(0.5).apply(Stream.of(pid), p -> 1.0).toList());
    }

    @Test
    void testMinAndStandardDeviationEmpty() {
        assertEquals(List.of(), TrustFilters.minAndStandardDeviation(0.05).apply(Stream.of(), p -> 1.0).toList());
    }

}