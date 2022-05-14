package com.eugene.wc.protocol.crypto;

import com.eugene.wc.protocol.api.crypto.DHKeyExchange;
import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.crypto.exception.InvalidParameterException;

import org.junit.Assert;
import org.junit.Test;

public class DHKeyExchangeTest {

    private final DHKeyExchange dhKeyExchange = new DHKeyExchangeImpl();

    @Test
    public void dhKeyExchangeTest() throws InvalidParameterException {

        KeyPair aliceKeyPair = dhKeyExchange.generateKeyPair();
        KeyPair bobKeyPair = dhKeyExchange.generateKeyPair();

        byte[] aliceSecret = dhKeyExchange.deriveSharedSecret(aliceKeyPair, bobKeyPair.getPublicKey());
        byte[] bobSecret = dhKeyExchange.deriveSharedSecret(bobKeyPair, aliceKeyPair.getPublicKey());

        Assert.assertArrayEquals(aliceSecret, bobSecret);
    }
}
