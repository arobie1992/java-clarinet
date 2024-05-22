package com.github.arobie1992.clarinet.transport;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class UncheckedURISyntaxExceptionTest {

    private final URISyntaxException cause = new URISyntaxException("test", "test");
    private final UncheckedURISyntaxException exception = new UncheckedURISyntaxException(cause);

    @Test
    void testCauseReturnType() {
        var cause = exception.getCause();
        assertEquals(URISyntaxException.class, cause.getClass());
        assertSame(this.cause, cause);
    }

}