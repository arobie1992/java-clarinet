package com.github.arobie1992.clarinet.connection;

import java.util.List;

public record ConnectResponse(ConnectionId connectionId, List<String> errors, boolean accepted, List<String> rejectReasons) {}
