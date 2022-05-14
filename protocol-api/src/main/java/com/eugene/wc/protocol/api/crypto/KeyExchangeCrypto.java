package com.eugene.wc.protocol.api.crypto;

public interface KeyExchangeCrypto {

    byte[] deriveCommitment(byte[] keyBytes);
}
