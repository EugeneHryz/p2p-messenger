package com.eugene.wc.protocol.api.keyexchange.exception;

import java.io.IOException;

public class EncodeException extends IOException {

    public EncodeException() {
        super();
    }

    public EncodeException(String message) {
        super(message);
    }

    public EncodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public EncodeException(Throwable cause) {
        super(cause);
    }
}
