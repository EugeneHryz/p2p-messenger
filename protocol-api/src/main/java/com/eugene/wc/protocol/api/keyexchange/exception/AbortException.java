package com.eugene.wc.protocol.api.keyexchange.exception;

public class AbortException extends Exception {

    public AbortException() {
        super();
    }

    public AbortException(String message) {
        super(message);
    }

    public AbortException(String message, Throwable cause) {
        super(message, cause);
    }

    public AbortException(Throwable cause) {
        super(cause);
    }
}
