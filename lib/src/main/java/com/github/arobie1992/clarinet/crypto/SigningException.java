package com.github.arobie1992.clarinet.crypto;

import java.security.GeneralSecurityException;

public class SigningException extends RuntimeException {
    public SigningException(String message) {
        super(message);
    }
    public SigningException(GeneralSecurityException cause) {
        super(cause);
    }
}
