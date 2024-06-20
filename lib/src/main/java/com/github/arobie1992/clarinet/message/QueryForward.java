package com.github.arobie1992.clarinet.message;

import java.util.Arrays;
import java.util.Objects;

public record QueryForward(QueryResponse queryResponse, byte[] signature) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryForward that = (QueryForward) o;
        return Objects.deepEquals(signature, that.signature) && Objects.equals(queryResponse, that.queryResponse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryResponse, Arrays.hashCode(signature));
    }
}
