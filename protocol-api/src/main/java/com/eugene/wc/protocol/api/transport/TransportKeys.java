package com.eugene.wc.protocol.api.transport;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.crypto.SecretKey;

import java.util.Arrays;

public class TransportKeys {

    private Integer keySetId;
    private final ContactId contactId;

    private final SecretKey outgoingKey;
    private final SecretKey incomingKey;

    public TransportKeys(ContactId contactId, SecretKey outgoingKey, SecretKey incomingKey) {
        this.contactId = contactId;
        this.outgoingKey = outgoingKey;
        this.incomingKey = incomingKey;
    }

    public TransportKeys(Integer keySetId, ContactId contactId, SecretKey outgoingKey,
                         SecretKey incomingKey) {
        this(contactId, outgoingKey, incomingKey);
        this.keySetId = keySetId;
    }

    public Integer getKeySetId() {
        return keySetId;
    }

    public ContactId getContactId() {
        return contactId;
    }

    public SecretKey getOutgoingKey() {
        return outgoingKey;
    }

    public SecretKey getIncomingKey() {
        return incomingKey;
    }

    @Override
    public String toString() {
        return "TransportKeys{" +
                "keySetId=" + keySetId +
                ", contactId=" + contactId.getInt() +
                ", outgoingKey=" + Arrays.toString(outgoingKey.getBytes()) +
                ", incomingKey=" + Arrays.toString(incomingKey.getBytes()) +
                '}';
    }
}
