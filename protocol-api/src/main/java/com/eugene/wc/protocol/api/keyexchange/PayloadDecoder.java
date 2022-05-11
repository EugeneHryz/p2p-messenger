package com.eugene.wc.protocol.api.keyexchange;

import com.eugene.wc.protocol.api.keyexchange.exception.DecodeException;

public interface PayloadDecoder {

    Payload decode() throws DecodeException;
}
