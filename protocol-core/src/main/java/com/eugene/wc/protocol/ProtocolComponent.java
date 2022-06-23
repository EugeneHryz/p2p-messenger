package com.eugene.wc.protocol;

import com.eugene.wc.protocol.session.MessageReaderImpl;
import com.eugene.wc.protocol.session.MessageWriterImpl;
import com.eugene.wc.protocol.transport.TransportKeyManager;

public interface ProtocolComponent {

    void inject(MessageReaderImpl messageReader);

    void inject(MessageWriterImpl messageWriter);

    void inject(TransportKeyManager tkm);
}
