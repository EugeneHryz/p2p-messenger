package com.eugene.wc.protocol.crypto;

import com.eugene.wc.protocol.api.crypto.AuthenticatedCipher;
import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.crypto.exception.EncryptionException;
import com.eugene.wc.protocol.api.crypto.exception.InvalidParameterException;
import com.eugene.wc.protocol.api.util.ArrayUtil;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import at.favre.lib.crypto.HKDF;

public class AesHmacAuthenticatedCipher implements AuthenticatedCipher {

    private static final String TRANSFORMATION_NAME = "AES/CBC/PKCS5Padding";
    private static final String MAC_ALGORITHM = "HmacSHA256";

    // 32 bytes for HmacSHA256
    private static final int MAC_LENGTH = 32;
    // 16 bytes default block size for AES
    public static final int IV_LENGTH = 16;

    private static final int DERIVED_SUBKEY_LENGTH = 32;

    private SecretKey cipherSecretKey;
    private SecretKey macSecretKey;

    @Override
    public void init(SecretKey key) {
        HKDF hkdf = HKDF.fromHmacSha256();

        byte[] cipherInfo = "cipher-key".getBytes(StandardCharsets.UTF_8);
        byte[] macInfo = "mac-key".getBytes(StandardCharsets.UTF_8);

        byte[] cipherKeyBytes = hkdf.expand(key.getEncoded(), cipherInfo, DERIVED_SUBKEY_LENGTH);
        byte[] macKeyBytes = hkdf.expand(key.getEncoded(), macInfo, DERIVED_SUBKEY_LENGTH);

        cipherSecretKey = new SecretKeySpec(cipherKeyBytes, "AES");
        macSecretKey = new SecretKeySpec(macKeyBytes, "HMAC");
    }

    @Override
    public byte[] encrypt(final byte[] plaintext, IvParameterSpec ivSpec) throws InvalidParameterException,
            EncryptionException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(TRANSFORMATION_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, cipherSecretKey, ivSpec);
        } catch (GeneralSecurityException e) {
            throw new InvalidParameterException("Invalid cipher parameter specified", e);
        }

        byte[] ciphertext;
        try {
            ciphertext = cipher.doFinal(plaintext);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new EncryptionException("Unable to encrypt the message", e);
        }
        byte[] ivBytes = ivSpec.getIV();
        byte[] ivAndCiphertext = ArrayUtils.addAll(ivBytes, ciphertext);

        Mac mac;
        try {
            mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(macSecretKey);
        } catch (GeneralSecurityException e) {
            throw new InvalidParameterException("Invalid MAC parameter specified", e);
        }
        byte[] authTag = mac.doFinal(ivAndCiphertext);

        return ArrayUtils.addAll(ivAndCiphertext, authTag);
    }

    @Override
    public byte[] decrypt(final byte[] ciphertext) throws InvalidParameterException,
            DecryptionException {
        Mac mac;
        try {
            mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(macSecretKey);
        } catch (GeneralSecurityException e) {
            throw new InvalidParameterException("Invalid MAC parameter specified");
        }

        mac.update(ciphertext, 0, ciphertext.length - MAC_LENGTH);
        byte[] calculatedAuthTag = mac.doFinal();

        if (ArrayUtil.compare(ciphertext, ciphertext.length - MAC_LENGTH,
                calculatedAuthTag, 0, MAC_LENGTH) != 0) {
            throw new DecryptionException("Unable to decrypt: calculated and stored MACs don't match");
        }

        byte[] ivBytes = new byte[IV_LENGTH];
        System.arraycopy(ciphertext, 0, ivBytes, 0, IV_LENGTH);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher;
        try {
            cipher = Cipher.getInstance(TRANSFORMATION_NAME);
            cipher.init(Cipher.DECRYPT_MODE, cipherSecretKey, ivSpec);

            int payloadLength = ciphertext.length - IV_LENGTH - MAC_LENGTH;
            return cipher.doFinal(ciphertext, IV_LENGTH, payloadLength);

        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new DecryptionException("Unable to decrypt the message", e);
        } catch (GeneralSecurityException e) {
            throw new InvalidParameterException("Invalid cipher parameter specified", e);
        }
    }
}
