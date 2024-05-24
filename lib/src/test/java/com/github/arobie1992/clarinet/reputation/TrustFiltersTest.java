package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.impl.peer.StringPeerId;
import com.github.arobie1992.clarinet.peer.PeerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TrustFiltersTest {

    private record TestReputation(PeerId peerId, double value) implements Reputation {
        @Override
        public void strongPenalize() {}

        @Override
        public void weakPenalize() {}

        @Override
        public void reward() {}

        @Override
        public Reputation copy() {
            return null;
        }
    }

    private final PeerId minId = new StringPeerId("min");
    private final PeerId badId = new StringPeerId("bad");
    private final PeerId stdId = new StringPeerId("std");
    private Stream<Reputation> reputationStream;

    @BeforeEach
    void setUp() {
        /*
        mean    = 0.78181818181818
        std dev = 0.37372372347444
        thresh  = 0.408094458344
         */
        reputationStream = Stream.of(
                new TestReputation(new StringPeerId("good1"), 1),
                new TestReputation(new StringPeerId("good2"), 1),
                new TestReputation(new StringPeerId("good3"), 1),
                new TestReputation(new StringPeerId("good4"), 1),
                new TestReputation(new StringPeerId("good5"), 1),
                new TestReputation(new StringPeerId("good6"), 1),
                new TestReputation(new StringPeerId("good7"), 1),
                new TestReputation(new StringPeerId("good8"), 1),
                new TestReputation(minId, 0.5),
                new TestReputation(badId, 0),
                new TestReputation(stdId, 0.1)
        );
    }

    @Test
    void testMinAndStandardDeviationMin() {
        var result = TrustFilters.minAndStandardDeviation(0.51).apply(reputationStream).toList();
        assertEquals(8, result.size());
        assertFalse(result.stream().anyMatch(minId::equals));
        assertFalse(result.stream().anyMatch(badId::equals));
        assertFalse(result.stream().anyMatch(stdId::equals));
    }

    @Test
    void testMinAndStandardDeviationStd() {
        var result = TrustFilters.minAndStandardDeviation(0.05).apply(reputationStream).toList();
        assertEquals(9, result.size());
        assertFalse(result.stream().anyMatch(badId::equals));
        assertFalse(result.stream().anyMatch(stdId::equals));
    }

    @Test
    void testMinAndStandardDeviationAllGood() {
        var rep = new TestReputation(new StringPeerId("good"), 1);
        assertEquals(List.of(rep.peerId), TrustFilters.minAndStandardDeviation(0.5).apply(Stream.of(rep)).toList());
    }

    @Test
    void testMinAndStandardDeviationEmpty() {
        assertEquals(List.of(), TrustFilters.minAndStandardDeviation(0.05).apply(Stream.of()).toList());
    }

}