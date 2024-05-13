package com.github.arobie1992.clarinet.connection;

import java.util.List;

public record WitnessResponse(ConnectionId connectionId, List<String> errors, boolean accepted, List<String> rejectReasons) {}
