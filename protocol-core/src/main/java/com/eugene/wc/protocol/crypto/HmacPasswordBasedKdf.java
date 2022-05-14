package com.eugene.wc.protocol.crypto;

import com.eugene.wc.protocol.api.crypto.PasswordBasedKdf;
import com.eugene.wc.protocol.api.crypto.exception.InvalidParameterException;

import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class HmacPasswordBasedKdf implements PasswordBasedKdf {

    private static final String ALGORITHM_NAME = "PBKDF2withHmacSHA1";
    private static final String ANOTHER_ALGORITHM_NAME = "PBKDF2withHmacSHA1";

    // output key length in bits (32 bytes)
    public static final int KEY_LENGTH = 256;
    // a number of times password is hashed
    private static final int ITERATION_COUNT = 2048;

    @Override
    public byte[] deriveKey(char[] password, byte[] salt) throws InvalidParameterException {

        KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ANOTHER_ALGORITHM_NAME);
            return keyFactory.generateSecret(spec).getEncoded();

        } catch (GeneralSecurityException e) {
            throw new InvalidParameterException("Invalid key derivation parameter specified", e);
        }
    }
}
