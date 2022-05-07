package com.eugene.wc.protocol.api.crypto;

import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.crypto.exception.EncryptionException;

public interface CryptoComponent {

    SecretKey generateSecretKey();

    byte[] encryptWithPassword(byte[] plaintext, char[] password);

    byte[] decryptWithPassword(byte[] ciphertext, char[] password) throws DecryptionException;
}
