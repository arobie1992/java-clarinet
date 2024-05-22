package com.github.arobie1992.clarinet.core;

import java.util.List;

public sealed interface Response {
    record Success(Object data) implements Response {}
    record Failure(List<String> errors) implements Response {}
}
