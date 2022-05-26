package com.eugene.wc.protocol.api.contact.exchange;

import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.properties.TransportProperties;

import java.util.Map;

public class ContactInfo {

    private final Identity identity;
    private final Map<TransportId, TransportProperties> properties;

    public ContactInfo(Identity identity, Map<TransportId, TransportProperties> properties) {
        this.identity = identity;
        this.properties = properties;
    }

    public Identity getIdentity() {
        return identity;
    }

    public Map<TransportId, TransportProperties> getProperties() {
        return properties;
    }
}
