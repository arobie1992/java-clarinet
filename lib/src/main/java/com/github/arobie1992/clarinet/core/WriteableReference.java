package com.github.arobie1992.clarinet.core;

sealed interface WriteableReference extends Connection.Reference permits Connection.Absent, Writeable {
}
