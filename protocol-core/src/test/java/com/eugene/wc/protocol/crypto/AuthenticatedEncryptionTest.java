//package com.eugene.wc.protocol.crypto;
//
//import com.eugene.wc.protocol.api.crypto.AuthenticatedCipher;
//import com.eugene.wc.protocol.api.crypto.CryptoComponent;
//import com.eugene.wc.protocol.api.crypto.PasswordBasedKdf;
//import com.eugene.wc.protocol.api.crypto.SecretKey;
//import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
//
//import org.junit.Assert;
//import org.junit.Test;
//
//public class AuthenticatedEncryptionTest {
//
//    private static final int SALT_LENGTH = 32;
//
//    private AuthenticatedCipher cipher = new AesHmacAuthenticatedCipher();
//    private PasswordBasedKdf pbKdf = new HmacPasswordBasedKdf();
//
//    private CryptoComponent crypto = new CryptoComponentImpl(cipher, pbKdf);
//
//    @Test
//    public void testAuthenticatedEncryption() throws DecryptionException {
//
//        SecretKey secretKey = crypto.generateSecretKey();
//        byte[] expected = secretKey.getBytes();
//        String password = "qweasdzxc123";
//
//        byte[] ciphertext = crypto.encryptWithPassword(expected, password.toCharArray());
//        byte[] actual = crypto.decryptWithPassword(ciphertext, password.toCharArray());
//
//        Assert.assertArrayEquals(expected, actual);
//    }
//}
