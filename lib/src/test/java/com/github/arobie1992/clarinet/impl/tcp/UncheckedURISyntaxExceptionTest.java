package com.github.arobie1992.clarinet.impl.tcp;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class UncheckedURISyntaxExceptionTest {

    @Test
    void testCauseReturnType() {
        Method declaredMethod = UncheckedURISyntaxException.class.getDeclaredMethods()[0];
        assertEquals(URISyntaxException.class, declaredMethod.getReturnType());
    }

}