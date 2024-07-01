package com.github.arobie1992.clarinet.query;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderingsTest {

    // disabled because it is intrinsically random and I don't want to deal with random failures
    // enable for when changes are made and then disable again
    @Disabled
    @Test
    void testRandom() {
        var counts = new double[3];
        var rand = Orderings.random();
        for(int i = 0; i < 999; i++) {
            var val = rand.compare("a", "b");
            counts[val+1]++;
        }

        // approximately 1/3 +/- 3%
        assertInRange(counts[0]/999, 0.3, 0.367);
        assertInRange(counts[1]/999, 0.3, 0.367);
        assertInRange(counts[2]/999, 0.3, 0.367);
    }

    private void assertInRange(
            double value,
            @SuppressWarnings("SameParameterValue") double low,
            @SuppressWarnings("SameParameterValue") double high
    ) {
        assertTrue(value >= low && value <= high, "expected " + low + " <= value <= " + high + " but got " + value);
    }

}