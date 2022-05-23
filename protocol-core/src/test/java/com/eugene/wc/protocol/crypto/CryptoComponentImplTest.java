package com.eugene.wc.protocol.crypto;

import com.eugene.wc.protocol.api.crypto.AuthenticatedCipher;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.DHKeyExchange;
import com.eugene.wc.protocol.api.crypto.PasswordBasedKdf;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.crypto.Signature;
import static com.eugene.wc.protocol.api.util.ByteUtils.INT_32_BYTES;
import static com.eugene.wc.protocol.api.util.ByteUtils.writeUint32;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CryptoComponentImplTest {

    private CryptoComponent crypto;

    @Before
    public void setUp() {
        AuthenticatedCipher cipher = new AesHmacAuthenticatedCipher();
        PasswordBasedKdf pbkdf = new HmacPasswordBasedKdf();
        DHKeyExchange dhKeyExchange = new DHKeyExchangeImpl();
        Signature signature = new ECDSASignature();

        crypto = new CryptoComponentImpl(cipher, pbkdf, dhKeyExchange, signature);
    }

    @Test
    public void keyDerivationShouldBeCorrect() {
        SecretKey key = crypto.generateSecretKey();

        String namespace = "contact_exchange";

        int number = 1;
        byte[] input = new byte[INT_32_BYTES];
        writeUint32(number, input, 0);

        byte[] derivedKey1 = crypto.deriveKey(key, namespace, input);
        byte[] derivedKey2 = crypto.deriveKey(key, namespace, input);

        Assert.assertArrayEquals(derivedKey1, derivedKey2);
    }
}
