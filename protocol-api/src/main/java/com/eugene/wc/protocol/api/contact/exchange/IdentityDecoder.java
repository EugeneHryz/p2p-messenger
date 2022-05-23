package com.eugene.wc.protocol.api.contact.exchange;

import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.keyexchange.exception.DecodeException;

public interface IdentityDecoder {

    Identity decode(byte[] encoded) throws DecodeException;
}
