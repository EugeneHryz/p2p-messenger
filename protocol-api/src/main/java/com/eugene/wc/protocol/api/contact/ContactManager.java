package com.eugene.wc.protocol.api.contact;

import com.eugene.wc.protocol.api.contact.exception.ContactAlreadyExistsException;
import com.eugene.wc.protocol.api.db.exception.DbException;

import java.util.List;

public interface ContactManager {

    // is it good idea to ignore DbExceptions in these methods?

    boolean createContact(Contact contact) throws ContactAlreadyExistsException;

    List<Contact> getAllContacts();
}
