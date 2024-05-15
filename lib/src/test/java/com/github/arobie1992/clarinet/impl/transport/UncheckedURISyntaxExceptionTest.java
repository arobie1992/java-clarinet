package com.github.arobie1992.clarinet.impl.transport;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class UncheckedURISyntaxExceptionTest {
    @Test
    void test() {
        try {
            new URI(":0.0.0.0");
        } catch(URISyntaxException e) {
            var ex = new UncheckedURISyntaxException(e);
            URISyntaxException cause = ex.getCause();
            assertSame(e, cause);
        }
    }
}