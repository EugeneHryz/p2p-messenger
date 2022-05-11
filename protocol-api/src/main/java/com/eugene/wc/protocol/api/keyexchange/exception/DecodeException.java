package com.eugene.wc.protocol.api.keyexchange.exception;

import java.io.IOException;

public class DecodeException extends IOException {

    public DecodeException() {
        super();
    }

    public DecodeException(String message) {
        super(message);
    }

    public DecodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public DecodeException(Throwable cause) {
        super(cause);
    }
}
