package com.eugene.wc.protocol.api.contact.exception;

public class ContactExchangeException extends Exception {

    public ContactExchangeException() {
        super();
    }

    public ContactExchangeException(String message) {
        super(message);
    }

    public ContactExchangeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContactExchangeException(Throwable cause) {
        super(cause);
    }
}
