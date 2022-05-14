package com.eugene.wc.protocol.api.crypto;

import com.eugene.wc.protocol.api.crypto.exception.CryptoException;

public interface DHKeyExchange {

    KeyPair generateKeyPair();

    byte[] deriveSharedSecret(KeyPair ourKeyPair, PublicKey remoteKey)
            throws CryptoException;
}
