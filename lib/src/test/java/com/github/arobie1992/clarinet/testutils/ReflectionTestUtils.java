package com.github.arobie1992.clarinet.testutils;

public class ReflectionTestUtils {
    private ReflectionTestUtils() {}

    public static <T> T getFieldValue(Object target, String fieldName, Class<T> expectedType) throws NoSuchFieldException, IllegalAccessException {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return expectedType.cast(field.get(target));
    }
}
