package com.eugene.wc.protocol.api.keyexchange;

import com.eugene.wc.protocol.api.keyexchange.exception.EncodeException;

public interface PayloadEncoder {

    void encode(Payload payload) throws EncodeException;
}
