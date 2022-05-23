package com.eugene.wc.protocol.keyexchange;

import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.KeyExchangeCrypto;
import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.crypto.exception.CryptoException;
import com.eugene.wc.protocol.api.keyexchange.Payload;
import com.eugene.wc.protocol.api.keyexchange.exception.AbortException;
import com.eugene.wc.protocol.api.transport.exception.TransportException;
import com.eugene.wc.protocol.api.util.ArrayUtil;

import java.util.Arrays;
import java.util.logging.Logger;

public class KeyExchangeProtocol {

    interface Callback {

        void waiting();

        void started();
    }

    private static final Logger logger = Logger.getLogger(KeyExchangeProtocol.class.getName());

    private final KeyExchangeTransport transport;
    private final CryptoComponent crypto;
    private final KeyExchangeCrypto kec;

    private final Payload localPayload, remotePayload;
    private final KeyPair localKeyPair;

    private final Callback callback;

    private final boolean isAlice;

    public KeyExchangeProtocol(Callback callback, KeyExchangeTransport transport, CryptoComponent crypto,
                               KeyExchangeCrypto kec, Payload local, Payload remote,
                               KeyPair localKeyPair, boolean isAlice) {
        this.callback = callback;
        this.transport = transport;
        this.crypto = crypto;
        this.kec = kec;
        localPayload = local;
        remotePayload = remote;
        this.localKeyPair = localKeyPair;

        this.isAlice = isAlice;
    }

    public SecretKey perform() throws AbortException, TransportException {
        try {
            PublicKey receivedKey;
            if (isAlice) {
                logger.info("Alice's own public key: " + Arrays.toString(localKeyPair
                        .getPublicKey().getBytes()));
                sendPublicKey(localKeyPair.getPublicKey());

                callback.waiting();
                logger.info("Alice sent her public key and about to receive bob's key");
                receivedKey = receivePublicKey();
            } else {
                callback.waiting();
                receivedKey = receivePublicKey();
                logger.info("Bob received alice key's key and about to send his own");
                logger.info("Alice's received key: " + Arrays.toString(receivedKey.getBytes()));
                sendPublicKey(localKeyPair.getPublicKey());
            }

            logger.info("About to generate shared secret");
            SecretKey sharedSecret = deriveSharedSecret(receivedKey);

            logger.info("Shared secret generated: " + Arrays.toString(sharedSecret.getBytes()));
            return sharedSecret;

        } catch (AbortException e) {
            transport.sendAbort();
            throw e;
        }
    }

    private SecretKey deriveSharedSecret(PublicKey theirPublicKey) throws AbortException {
        try {
            return crypto.deriveSharedSecret(localKeyPair, theirPublicKey);
        } catch (CryptoException e) {
            throw new AbortException(e);
        }
    }

    private PublicKey receivePublicKey() throws AbortException, TransportException {
        byte[] keyBytes = transport.receiveKey();
        byte[] ourCommitment = kec.deriveCommitment(keyBytes);
        if (ArrayUtil.compare(remotePayload.getCommitment(), 0, ourCommitment,
                0, ourCommitment.length) != 0) {
            throw new AbortException("Our calculated commitment of received public key does " +
                    "not match with what we have scanned");
        }
        callback.started();
        return new PublicKey(keyBytes);
    }

    private void sendPublicKey(PublicKey key) {
        try {
            transport.sendKey(key.getBytes());
        } catch (TransportException e) {
            logger.warning("Error while sending public key " + e);
        }
    }
}
