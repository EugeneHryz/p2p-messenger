package com.eugene.wc.protocol.api.crypto;

import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.crypto.exception.EncryptionException;
import com.eugene.wc.protocol.api.crypto.exception.InvalidParameterException;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public interface AuthenticatedCipher {

    void init(SecretKey key);

    byte[] encrypt(final byte[] plaintext, IvParameterSpec ivSpec) throws InvalidParameterException,
            EncryptionException;

    byte[] decrypt(final byte[] ciphertext) throws InvalidParameterException, DecryptionException;
}
