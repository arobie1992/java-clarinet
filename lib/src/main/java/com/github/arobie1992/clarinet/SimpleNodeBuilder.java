package com.github.arobie1992.clarinet;

public class SimpleNodeBuilder implements NodeBuilder {

    @Override
    public Node build() {
        return new SimpleNode();
    }
}
