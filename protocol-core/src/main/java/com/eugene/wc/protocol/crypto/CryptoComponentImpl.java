package com.eugene.wc.protocol.crypto;

import com.eugene.wc.protocol.api.crypto.AuthenticatedCipher;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.DHKeyExchange;
import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.crypto.PasswordBasedKdf;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.crypto.exception.CryptoException;
import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.crypto.exception.EncryptionException;
import com.eugene.wc.protocol.api.crypto.exception.InvalidParameterException;

import org.apache.commons.lang3.ArrayUtils;

import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

public class CryptoComponentImpl implements CryptoComponent {

    private static final int SALT_LENGTH = 32;

    private final AuthenticatedCipher authCipher;
    private final PasswordBasedKdf pbKdf;
    private final DHKeyExchange dhKeyExchange;

    private final SecureRandom secureRandom;

    @Inject
    public CryptoComponentImpl(AuthenticatedCipher authCipher, PasswordBasedKdf pbKdf,
                               DHKeyExchange dhKeyExchange) {
        this.authCipher = authCipher;
        this.pbKdf = pbKdf;
        this.dhKeyExchange = dhKeyExchange;

        secureRandom = new SecureRandom();
    }

    @Override
    public SecretKey generateSecretKey() {
        byte[] randomBytes = new byte[SecretKey.SECRET_KEY_SIZE];
        secureRandom.nextBytes(randomBytes);
        return new SecretKey(randomBytes);
    }

    @Override
    public byte[] encryptWithPassword(byte[] plaintext, char[] password) {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        byte[] ivBytes = new byte[AesHmacAuthenticatedCipher.IV_LENGTH];
        secureRandom.nextBytes(ivBytes);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        try {
            byte[] derivedKeyFromPassword = pbKdf.deriveKey(password, salt);
            SecretKeySpec masterKey = new SecretKeySpec(derivedKeyFromPassword, "AES");

            authCipher.init(masterKey);
            byte[] ciphertext = authCipher.encrypt(plaintext, ivSpec);
            return ArrayUtils.addAll(salt, ciphertext);

        } catch (InvalidParameterException | EncryptionException e) {
            throw new RuntimeException("Error while encrypting data", e);
        }
    }

    @Override
    public byte[] decryptWithPassword(byte[] ciphertext, char[] password) throws DecryptionException {
        byte[] salt = Arrays.copyOf(ciphertext, SALT_LENGTH);
        byte[] bytesToDecrypt = Arrays.copyOfRange(ciphertext, SALT_LENGTH, ciphertext.length);

        try {
            byte[] keyBytes = pbKdf.deriveKey(password, salt);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            authCipher.init(secretKey);
            return authCipher.decrypt(bytesToDecrypt);

        } catch (CryptoException e) {
            throw new DecryptionException("Error while decrypting with password", e);
        }
    }

    @Override
    public KeyPair generateAgreementKeyPair() {
        return dhKeyExchange.generateKeyPair();
    }

    @Override
    public SecretKey deriveSharedSecret(KeyPair localKeyPair, PublicKey remoteKey)
            throws CryptoException {

        byte[] sharedSecret = dhKeyExchange.deriveSharedSecret(localKeyPair, remoteKey);
        return new SecretKey(sharedSecret);
    }
}
