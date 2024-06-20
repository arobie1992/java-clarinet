package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.adt.Bytes;

import java.util.Objects;

public record QueryForward(QueryResponse queryResponse, Bytes signature) {
    public QueryForward {
        Objects.requireNonNull(queryResponse);
        Objects.requireNonNull(signature);
    }
}
