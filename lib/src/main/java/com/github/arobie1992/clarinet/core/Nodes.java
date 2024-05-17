package com.github.arobie1992.clarinet.core;

public class Nodes {
    private Nodes() {}

    public static NodeBuilder newBuilder() {
        return new SimpleNode.Builder();
    }
}
