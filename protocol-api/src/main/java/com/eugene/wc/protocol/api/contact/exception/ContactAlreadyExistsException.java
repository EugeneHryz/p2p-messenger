package com.eugene.wc.protocol.api.contact.exception;

public class ContactAlreadyExistsException extends Exception {

    public ContactAlreadyExistsException() {
        super();
    }

    public ContactAlreadyExistsException(String message) {
        super(message);
    }

    public ContactAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContactAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
