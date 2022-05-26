package com.eugene.wc.protocol.crypto;

import static com.eugene.wc.protocol.api.util.ByteUtils.INT_32_BYTES;

import com.eugene.wc.protocol.api.crypto.AuthenticatedCipher;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.DHKeyExchange;
import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.crypto.PasswordBasedKdf;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.crypto.Signature;
import com.eugene.wc.protocol.api.crypto.exception.CryptoException;
import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.crypto.exception.EncryptionException;
import com.eugene.wc.protocol.api.crypto.exception.InvalidParameterException;
import com.eugene.wc.protocol.api.util.ByteUtils;
import com.eugene.wc.protocol.api.util.StringUtils;

import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.Blake2bDigest;

import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

public class CryptoComponentImpl implements CryptoComponent {

    private static final int SALT_LENGTH = 32;
    private static final int MAC_LENGTH = 32;

    private final AuthenticatedCipher authCipher;
    private final PasswordBasedKdf pbKdf;
    private final DHKeyExchange dhKeyExchange;
    private final Signature signature;

    private final SecureRandom secureRandom;

    @Inject
    public CryptoComponentImpl(AuthenticatedCipher authCipher, PasswordBasedKdf pbKdf,
                               DHKeyExchange dhKeyExchange, Signature signature) {
        this.authCipher = authCipher;
        this.pbKdf = pbKdf;
        this.dhKeyExchange = dhKeyExchange;
        this.signature = signature;

        secureRandom = new SecureRandom();
    }

    @Override
    public SecureRandom getSecureRandom() {
        return secureRandom;
    }

    @Override
    public SecretKey generateSecretKey() {
        byte[] randomBytes = new byte[SecretKey.SECRET_KEY_SIZE];
        secureRandom.nextBytes(randomBytes);
        return new SecretKey(randomBytes);
    }

    @Override
    public byte[] encryptWithPassword(byte[] plaintext, char[] password) throws CryptoException {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        byte[] ivBytes = new byte[AesHmacAuthenticatedCipher.IV_LENGTH];
        secureRandom.nextBytes(ivBytes);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        try {
            byte[] derivedKeyFromPassword = pbKdf.deriveKey(password, salt);
            SecretKeySpec masterKey = new SecretKeySpec(derivedKeyFromPassword,
                    HmacPasswordBasedKdf.ALGORITHM_NAME);

            authCipher.init(masterKey);
            byte[] ciphertext = authCipher.encrypt(plaintext, ivSpec);
            return ArrayUtils.addAll(salt, ciphertext);

        } catch (InvalidParameterException | EncryptionException e) {
            throw new CryptoException("Error while encrypting data", e);
        }
    }

    @Override
    public byte[] decryptWithPassword(byte[] ciphertext, char[] password) throws DecryptionException {
        byte[] salt = Arrays.copyOf(ciphertext, SALT_LENGTH);
        byte[] bytesToDecrypt = Arrays.copyOfRange(ciphertext, SALT_LENGTH, ciphertext.length);

        try {
            byte[] keyBytes = pbKdf.deriveKey(password, salt);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, HmacPasswordBasedKdf.ALGORITHM_NAME);

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

    @Override
    public KeyPair generateSignatureKeyPair() {
        return signature.generateSignatureKeyPair();
    }

    @Override
    public byte[] mac(SecretKey secretKey, String namespace, byte[]... inputs) {
        Digest mac = new Blake2bDigest(secretKey.getBytes(), MAC_LENGTH, null, null);

        if (namespace != null) {
            byte[] namespaceLength = new byte[INT_32_BYTES];
            ByteUtils.writeUint32(namespace.length(), namespaceLength, 0);
            mac.update(namespaceLength, 0, namespaceLength.length);
        }
        for (byte[] input : inputs) {
            byte[] lengthArray = new byte[INT_32_BYTES];
            ByteUtils.writeUint32(input.length, lengthArray, 0);

            mac.update(lengthArray, 0, lengthArray.length);
            mac.update(input, 0, input.length);
        }
        byte[] digest = new byte[MAC_LENGTH];
        mac.doFinal(digest, 0);
        return digest;
    }

    @Override
    public byte[] hash(String label, byte[]... inputs) {
        byte[] labelBytes = StringUtils.toUtf8(label);
        Digest digest = new Blake2bDigest(256);
        byte[] length = new byte[INT_32_BYTES];
        ByteUtils.writeUint32(labelBytes.length, length, 0);
        digest.update(length, 0, length.length);
        digest.update(labelBytes, 0, labelBytes.length);
        for (byte[] input : inputs) {
            ByteUtils.writeUint32(input.length, length, 0);
            digest.update(length, 0, length.length);
            digest.update(input, 0, input.length);
        }
        byte[] output = new byte[digest.getDigestSize()];
        digest.doFinal(output, 0);
        return output;
    }

    @Override
    public SecretKey deriveKey(SecretKey secretKey, String namespace, byte[]... inputs) {
        return new SecretKey(mac(secretKey, namespace, inputs));
    }

    @Override
    public byte[] encryptWithKey(SecretKey key, byte[] plaintext) throws CryptoException {

        byte[] ivBytes = new byte[AesHmacAuthenticatedCipher.IV_LENGTH];
        secureRandom.nextBytes(ivBytes);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        try {
            SecretKeySpec masterKey = new SecretKeySpec(key.getBytes(),
                    DHKeyExchangeImpl.ALGORITHM_NAME);

            authCipher.init(masterKey);
            byte[] ciphertext = authCipher.encrypt(plaintext, ivSpec);
            return ciphertext;

        } catch (InvalidParameterException | EncryptionException e) {
            throw new CryptoException("Unable to encrypt data", e);
        }
    }

    @Override
    public byte[] decryptWithKey(SecretKey key, byte[] ciphertext) throws DecryptionException {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), DHKeyExchangeImpl.ALGORITHM_NAME);
        authCipher.init(secretKey);

        try {
            return authCipher.decrypt(ciphertext);

        } catch (InvalidParameterException e) {
            throw new DecryptionException("Unable to decrypt data", e);
        }
    }
}
