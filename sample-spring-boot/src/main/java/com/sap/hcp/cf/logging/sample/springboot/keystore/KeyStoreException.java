package com.sap.hcp.cf.logging.sample.springboot.keystore;

public class KeyStoreException extends Exception {

    private static final long serialVersionUID = -1912864666741145608L;

    public KeyStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyStoreException(String message) {
        super(message);
    }
}
