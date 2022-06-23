package com.eugene.wc.protocol.crypto;

import com.eugene.wc.protocol.api.crypto.DHKeyExchange;
import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.crypto.PrivateKey;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.crypto.exception.CryptoException;

import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

public class DHKeyExchangeImpl implements DHKeyExchange {

    public static final String ALGORITHM_NAME = "ECDH";

    private final Curve25519 curve25519;

    public DHKeyExchangeImpl() {
        curve25519 = Curve25519.getInstance("java");
    }

    @Override
    public KeyPair generateKeyPair() {
        Curve25519KeyPair keyPair = curve25519.generateKeyPair();

        KeyPair pair = new KeyPair(new PrivateKey(keyPair.getPrivateKey()),
                new PublicKey(keyPair.getPublicKey()));
        return pair;
    }

    @Override
    public byte[] deriveSharedSecret(KeyPair ourKeyPair, PublicKey remoteKey) throws CryptoException {
        byte[] secret = curve25519.calculateAgreement(remoteKey.getBytes(),
                ourKeyPair.getPrivateKey().getBytes());

        byte allZero = 0;
        for (byte b : secret) allZero |= b;
        if (allZero == 0) throw new CryptoException();

        return secret;
    }
}
