package com.eugene.wc.protocol.crypto;

import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.crypto.PrivateKey;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.crypto.Signature;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Logger;

import javax.inject.Inject;

public class ECDSASignature implements Signature {

    private static final Logger logger = Logger.getLogger(ECDSASignature.class.getName());

    private static final String KEY_PAIR_GENERATOR_ALGORITHM = "EC";
    // key size in bits
    private static final int KEY_SIZE = 256;

    private KeyPairGenerator keyPairGenerator;

    @Inject
    public ECDSASignature() {
        SecureRandom random = new SecureRandom();
        try {
            keyPairGenerator = KeyPairGenerator.getInstance(KEY_PAIR_GENERATOR_ALGORITHM);

            keyPairGenerator.initialize(KEY_SIZE, random);
        } catch (NoSuchAlgorithmException e) {
            logger.severe("No such key pair generator algorithm found");
        }
    }

    @Override
    public KeyPair generateSignatureKeyPair() {

        java.security.KeyPair keyPair = keyPairGenerator.generateKeyPair();
        byte[] pubBytes = keyPair.getPublic().getEncoded();
        byte[] prBytes = keyPair.getPrivate().getEncoded();

        PublicKey pubKey = new PublicKey(pubBytes);
        PrivateKey privateKey = new PrivateKey(prBytes);

        return new KeyPair(privateKey, pubKey);
    }
}
