package com.eugene.wc.protocol.api.connection;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;

public interface ConnectionManager {

    void manageIncomingConnection(DuplexTransportConnection c, TransportId transportId);

    void manageOutgoingConnection(DuplexTransportConnection c, TransportId transportId,
                                  ContactId contactId);
}
