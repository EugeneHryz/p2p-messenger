package com.eugene.wc.protocol.api.crypto;

import com.eugene.wc.protocol.api.crypto.exception.CryptoException;
import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.crypto.exception.InvalidParameterException;

public interface CryptoComponent {

    SecretKey generateSecretKey();

    byte[] encryptWithPassword(byte[] plaintext, char[] password);

    byte[] decryptWithPassword(byte[] ciphertext, char[] password) throws DecryptionException;

    KeyPair generateAgreementKeyPair();

    SecretKey deriveSharedSecret(KeyPair localKeyPair, PublicKey remoteKey)
            throws CryptoException;
}
