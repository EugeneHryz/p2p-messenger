package com.eugene.wc.protocol.api.contact.exchange;

import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.keyexchange.exception.EncodeException;

public interface IdentityEncoder {

    byte[] encode(Identity identity) throws EncodeException;
}
