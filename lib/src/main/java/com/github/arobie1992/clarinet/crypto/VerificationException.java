package com.github.arobie1992.clarinet.crypto;

import java.security.GeneralSecurityException;

public class VerificationException extends RuntimeException {
    public VerificationException(GeneralSecurityException cause) {
        super(cause);
    }
}
