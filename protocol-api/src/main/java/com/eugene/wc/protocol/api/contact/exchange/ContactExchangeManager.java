package com.eugene.wc.protocol.api.contact.exchange;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.exception.ContactExchangeException;

public interface ContactExchangeManager {

    ContactId startContactExchange() throws ContactExchangeException;
}
