package com.github.arobie1992.clarinet.crypto;

import java.security.GeneralSecurityException;

public class KeyCreationException extends RuntimeException {
    public KeyCreationException(GeneralSecurityException cause) {
        super(cause);
    }
}
