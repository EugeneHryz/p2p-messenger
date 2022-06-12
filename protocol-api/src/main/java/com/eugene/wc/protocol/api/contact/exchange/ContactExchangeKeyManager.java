package com.eugene.wc.protocol.api.contact.exchange;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.transport.KeyManager;

import java.sql.Connection;

public interface ContactExchangeKeyManager extends KeyManager {

    int DERIVE_KEY_NUMBER = 5;

    void generateInitialKeys(SecretKey key);

    void saveLastGeneratedKeys(Connection txn, ContactId contactId) throws DbException;
}
