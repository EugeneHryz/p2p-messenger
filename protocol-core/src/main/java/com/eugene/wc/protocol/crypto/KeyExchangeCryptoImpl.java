package com.eugene.wc.protocol.crypto;

import com.eugene.wc.protocol.api.crypto.KeyExchangeCrypto;
import com.eugene.wc.protocol.api.keyexchange.Payload;
import com.eugene.wc.protocol.keyexchange.KeyExchangeProtocol;

import org.apache.commons.lang3.ArrayUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.inject.Inject;

public class KeyExchangeCryptoImpl implements KeyExchangeCrypto {

    private static final Logger logger = Logger.getLogger(KeyExchangeProtocol.class.getName());

    private static final String ALGORITHM_NAME = "SHA-256";

    // todo: use CryptoComponent to calculate hashes
    private MessageDigest digest;

    @Inject
    public KeyExchangeCryptoImpl() {
        try {
            digest = MessageDigest.getInstance(ALGORITHM_NAME);
        } catch (NoSuchAlgorithmException e) {
            logger.warning("Unable to find specified algorithm " + e);
        }
    }

    @Override
    public byte[] deriveCommitment(byte[] keyBytes) {
        byte[] result = digest.digest(keyBytes);

        return ArrayUtils.subarray(result, 0, Payload.COMMITMENT_LENGTH);
    }
}
