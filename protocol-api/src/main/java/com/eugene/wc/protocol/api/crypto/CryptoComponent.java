package com.eugene.wc.protocol.api.crypto;

import com.eugene.wc.protocol.api.crypto.exception.CryptoException;
import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;

import java.security.SecureRandom;

public interface CryptoComponent {

    SecureRandom getSecureRandom();

    SecretKey generateSecretKey();

    byte[] encryptWithPassword(byte[] plaintext, char[] password) throws CryptoException;

    byte[] decryptWithPassword(byte[] ciphertext, char[] password) throws DecryptionException;

    KeyPair generateAgreementKeyPair();

    SecretKey deriveSharedSecret(KeyPair localKeyPair, PublicKey remoteKey) throws CryptoException;

    KeyPair generateSignatureKeyPair();

    byte[] mac(SecretKey secretKey, String namespace, byte[]... inputs);

    byte[] hash(String label, byte[]... inputs);

    SecretKey deriveKey(SecretKey secretKey, String namespace, byte[]... inputs);

    byte[] encryptWithKey(SecretKey key, byte[] plaintext) throws CryptoException;

    byte[] decryptWithKey(SecretKey key, byte[] ciphertext) throws DecryptionException;
}
