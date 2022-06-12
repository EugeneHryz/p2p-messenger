package com.eugene.wc.protocol.session;

import com.eugene.wc.protocol.ProtocolComponent;
import com.eugene.wc.protocol.api.client.ClientHelper;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.data.WdfList2;
import com.eugene.wc.protocol.api.identity.IdentityId;
import com.eugene.wc.protocol.api.session.Ack;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageFactory;
import com.eugene.wc.protocol.api.session.MessageReader;
import com.eugene.wc.protocol.api.session.MessageTypes;
import com.eugene.wc.protocol.api.transport.EncryptedPacket;
import com.eugene.wc.protocol.api.transport.KeyManager;
import com.eugene.wc.protocol.api.transport.TransportReader;
import com.eugene.wc.protocol.transport.TransportReaderImpl;
import static com.eugene.wc.protocol.api.util.ByteUtils.*;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;

@NotThreadSafe
public class MessageReaderImpl implements MessageReader {

    @Inject
    MessageFactory messageFactory;
    @Inject
    ClientHelper clientHelper;
    @Inject
    CryptoComponent crypto;

    private final KeyManager transportKeyManager;
    private final TransportReader reader;

    private byte[] nextContent;
    private byte nextType;
    private boolean readNextPacket;

    public MessageReaderImpl(InputStream input, KeyManager tkm,
                             ProtocolComponent component) {
        reader = new TransportReaderImpl(input);

        transportKeyManager = tkm;
        component.inject(this);
    }

    @Override
    public boolean hasMessage() throws IOException, DecryptionException {
        if (!readNextPacket) {
            readNextAndDecrypt();
        }
        return nextType == MessageTypes.MESSAGE;
    }

    @Override
    public boolean hasAck() throws IOException, DecryptionException {
        if (!readNextPacket) {
            readNextAndDecrypt();
        }
        return nextType == MessageTypes.ACK;
    }

    @Override
    public boolean isEndOfSession() throws IOException, DecryptionException {
        if (!readNextPacket) {
            readNextAndDecrypt();
        }
        return nextType == MessageTypes.END_OF_SESSION;
    }

    @Override
    public Message readNextMessage() throws IOException, DecryptionException {
        if (!readNextPacket) {
            readNextAndDecrypt();
        }
        if (nextType != MessageTypes.MESSAGE) {
            throw new IllegalStateException("Next packet does not contain a Message!");
        }
        Message msg = messageFactory.createMessage(nextContent);
        readNextPacket = false;
        return msg;
    }

    @Override
    public Ack readNextAck() throws IOException, DecryptionException {
        if (!readNextPacket) {
            readNextAndDecrypt();
        }
        if (nextType != MessageTypes.ACK) {
            throw new IllegalStateException("Next packet does not contain an Ack!");
        }
        WdfList2 list = clientHelper.toList(nextContent);
        IdentityId remoteId = new IdentityId(list.getRaw(0));
        IdentityId localId = new IdentityId(list.getRaw(1));

        readNextPacket = false;
        return new Ack(localId, remoteId);
    }

    private void readNextAndDecrypt() throws IOException, DecryptionException {
        if (readNextPacket) {
            throw new IllegalStateException();
        }
        EncryptedPacket packet = reader.readNextPacket();

        SecretKey nextKey = transportKeyManager.retrieveIncomingKey(packet.getTag());
        byte[] decryptedRaw = crypto.decryptWithKey(nextKey, packet.getContent());

        nextType = decryptedRaw[0];
        if (decryptedRaw.length > 1 + INT_32_BYTES) {
            int length = (int) readUint32(decryptedRaw, 1);
            if (1 + INT_32_BYTES + length == decryptedRaw.length) {
                byte[] contentRaw = new byte[length];
                System.arraycopy(decryptedRaw, 1 + INT_32_BYTES, contentRaw, 0, length);
                nextContent = contentRaw;
            }
        }
        readNextPacket = true;
    }
}
