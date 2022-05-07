package com.eugene.wc.protocol.api.crypto;

import com.eugene.wc.protocol.api.crypto.exception.InvalidParameterException;

public interface PasswordBasedKdf {

    byte[] deriveKey(char[] password, byte[] salt) throws InvalidParameterException;
}
