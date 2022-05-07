package com.eugene.wc.protocol.account;

import com.eugene.wc.protocol.api.account.AccountManager;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.crypto.exception.EncryptionException;
import com.eugene.wc.protocol.api.db.DatabaseConfig;
import com.eugene.wc.protocol.api.util.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.inject.Inject;

public class AccountManagerImpl implements AccountManager {

    private static final String DB_KEY_FILENAME = "db.key";

    private DatabaseConfig dbConfig;
    private CryptoComponent cryptoComponent;
    private File dbFeyFile;

    private SecretKey secretKey;

    public AccountManagerImpl(DatabaseConfig dbConfig, CryptoComponent crypto) {
        this.dbConfig = dbConfig;
        cryptoComponent = crypto;

        dbFeyFile = new File(dbConfig.getDatabaseKeyDirectory(), DB_KEY_FILENAME);
    }

    @Override
    public boolean accountExists() {
        return dbFeyFile.exists();
    }

    @Override
    public void createAccount(String nickname, String password) throws EncryptionException {
        // todo: create account in the database

        SecretKey secretKey = cryptoComponent.generateSecretKey();
        encryptAndStoreDatabaseKey(secretKey, password);
        this.secretKey = secretKey;
    }

    @Override
    public void signIn(String password) throws DecryptionException {
        byte[] decryptedKey = readAndDecryptDatabaseKey(password);
        secretKey = new SecretKey(decryptedKey);
    }

    @Override
    public SecretKey getSecretKey() {
        return secretKey;
    }

    private byte[] readAndDecryptDatabaseKey(String password) throws DecryptionException {
        if (!dbFeyFile.exists()) {
            throw new DecryptionException("Db key file doesn't exist");
        }
        String key;
        try {
            key = readEncryptedKeyFromFile();
        } catch (IOException e) {
            throw new DecryptionException("Unable to read Db key", e);
        }
        byte[] keyBytes = StringUtils.fromHexString(key);

        byte[] actualKey = cryptoComponent.decryptWithPassword(keyBytes, password.toCharArray());
        return actualKey;
    }

    private void encryptAndStoreDatabaseKey(SecretKey secretKey, String password) throws EncryptionException {
        byte[] encryptedSecretKey = cryptoComponent.encryptWithPassword(secretKey.getBytes(),
                password.toCharArray());

        String hexEncryptedKey = StringUtils.toHexString(encryptedSecretKey);
        try {
            writeEncryptedKeyToFile(hexEncryptedKey);
        } catch (IOException e) {
            throw new EncryptionException("Error occured while writing to key file");
        }
    }

    private String readEncryptedKeyFromFile() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(dbFeyFile))) {
            String key = br.readLine();
            return key;
        }
    }

    private void writeEncryptedKeyToFile(String hex) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dbFeyFile, false))) {
            bw.write(hex);
        }
    }
}
