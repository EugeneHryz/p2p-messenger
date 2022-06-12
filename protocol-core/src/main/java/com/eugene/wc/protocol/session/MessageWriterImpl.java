package com.eugene.wc.protocol.session;

import com.eugene.wc.protocol.ProtocolComponent;
import com.eugene.wc.protocol.api.Pair;
import com.eugene.wc.protocol.api.client.ClientHelper;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.crypto.exception.CryptoException;
import com.eugene.wc.protocol.api.data.WdfList2;
import com.eugene.wc.protocol.api.session.Ack;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageFactory;
import com.eugene.wc.protocol.api.session.MessageWriter;
import com.eugene.wc.protocol.api.transport.EncryptedPacket;
import com.eugene.wc.protocol.api.transport.KeyManager;
import com.eugene.wc.protocol.api.transport.Tag;
import com.eugene.wc.protocol.api.transport.TransportWriter;
import com.eugene.wc.protocol.transport.TransportWriterImpl;

import static com.eugene.wc.protocol.api.session.MessageTypes.*;
import static com.eugene.wc.protocol.api.util.ByteUtils.*;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;

@NotThreadSafe
public class MessageWriterImpl implements MessageWriter {

    @Inject
    MessageFactory messageFactory;
    @Inject
    ClientHelper clientHelper;
    @Inject
    CryptoComponent crypto;

    private final TransportWriter writer;
    private final KeyManager transportKeyManager;

    public MessageWriterImpl(OutputStream output, KeyManager transportKeyManager,
                             ProtocolComponent component) {
        writer = new TransportWriterImpl(output);

        this.transportKeyManager = transportKeyManager;
        component.inject(this);
    }

    @Override
    public void sendMessage(Message message) throws IOException, CryptoException {
        byte[] rawMessage = messageFactory.getRawMessage(message);

        byte[] content = new byte[1 + INT_32_BYTES + rawMessage.length];
        content[0] = MESSAGE;
        writeUint32(rawMessage.length, content, 1);
        System.arraycopy(rawMessage, 0, content, 1 + INT_32_BYTES, rawMessage.length);

        encryptAndSend(content);
    }

    @Override
    public void sendAck(Ack ack) throws IOException, CryptoException {
        WdfList2 list = new WdfList2();
        list.add(ack.getRemoteId().getBytes());
        list.add(ack.getLocalId().getBytes());

        byte[] ackRaw = clientHelper.toByteArray(list);
        byte[] content = new byte[1 + INT_32_BYTES + ackRaw.length];
        content[0] = ACK;
        writeUint32(ackRaw.length, content, 1);
        System.arraycopy(ackRaw, 0, content, 1 + INT_32_BYTES, ackRaw.length);

        encryptAndSend(content);
    }

    @Override
    public void sendEndOfSession() throws IOException, CryptoException {
        byte[] content = new byte[1];
        content[0] = END_OF_SESSION;

        encryptAndSend(content);
    }

    private void encryptAndSend(byte[] data) throws IOException, CryptoException {
        Pair<SecretKey, Tag> keyWithTag = transportKeyManager.retrieveNextOutgoingKeyAndTag();
        byte[] encryptedContent = crypto.encryptWithKey(keyWithTag.getFirst(), data);

        EncryptedPacket packet = new EncryptedPacket(keyWithTag.getSecond(), encryptedContent);
        writer.writePacket(packet);
    }
}
