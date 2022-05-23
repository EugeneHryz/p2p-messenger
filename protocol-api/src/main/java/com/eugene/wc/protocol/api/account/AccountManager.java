package com.eugene.wc.protocol.api.account;

import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.crypto.exception.CryptoException;
import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.crypto.exception.EncryptionException;

public interface AccountManager {

    boolean accountExists();

    void createAccount(String nickname, String password) throws CryptoException;

    void signIn(String password) throws DecryptionException;

    SecretKey getSecretKey();

    // change password method
}
