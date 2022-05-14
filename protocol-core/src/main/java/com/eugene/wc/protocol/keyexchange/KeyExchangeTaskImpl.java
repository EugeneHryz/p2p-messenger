package com.eugene.wc.protocol.keyexchange;

import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.KeyExchangeCrypto;
import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.data.StreamDataWriter;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.keyexchange.ConnectionChooser;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeTask;
import com.eugene.wc.protocol.api.keyexchange.Payload;
import com.eugene.wc.protocol.api.keyexchange.PayloadEncoder;
import com.eugene.wc.protocol.api.keyexchange.TransportDescriptor;
import com.eugene.wc.protocol.api.keyexchange.exception.AbortException;
import com.eugene.wc.protocol.api.keyexchange.exception.EncodeException;
import com.eugene.wc.protocol.api.keyexchange.exception.TransportException;
import com.eugene.wc.protocol.api.plugin.PluginManager;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.data.StreamDataWriterImpl;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.inject.Inject;

public class KeyExchangeTaskImpl extends Thread implements KeyExchangeTask {

    private static final Logger logger = Logger.getLogger(KeyExchangeTaskImpl.class.getName());

    private KeyExchangeConnector connector;
    private final EventBus eventBus;

    private Payload localPayload;
    private Payload remotePayload;
    private final CryptoComponent crypto;
    private final KeyExchangeCrypto kec;

    private KeyPair localKeyPair;

    @Inject
    public KeyExchangeTaskImpl(EventBus eventBus, CryptoComponent crypto, KeyExchangeCrypto kec,
                               PluginManager pluginManager, ConnectionChooser connChooser) {
        this.eventBus = eventBus;
        this.crypto = crypto;
        this.kec = kec;

        connector = new KeyExchangeConnector(pluginManager, connChooser, kec);
    }

    @Override
    public void run() {
        boolean isAlice = localPayload.compareTo(remotePayload) < 0;

        try {
            KeyExchangeTransport transport = connector.connect(remotePayload, isAlice);
            logger.info("RECEIVED KeyExchangeTransport!!!");
            if (transport == null) {
                throw new AbortException("Unable to establish remote connection");
            }

            KeyExchangeProtocol protocol = new KeyExchangeProtocol(transport, crypto, kec,
                    localPayload, remotePayload, localKeyPair, isAlice);
            SecretKey sharedSecret = protocol.perform();


            // create KeyExchangeResult and broadcast corresponding event
        } catch (AbortException e) {
            logger.warning("Key Exchange task was aborted " + e);
            // broadcast aborted event
        } catch (TransportException e) {
            logger.warning("Unable to perform key exchange task " + e);
        }
    }

    @Override
    public synchronized void listen(KeyPair ephemeralKeyPair) {
        if (localPayload == null) {
            localKeyPair = ephemeralKeyPair;
            localPayload = connector.listen(localKeyPair);

            logger.info("Received localPayload ");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamDataWriter writer = new StreamDataWriterImpl(baos);
            PayloadEncoder payloadEncoder = new PayloadEncoderImpl(writer);

            try {
                payloadEncoder.encode(localPayload);
            } catch (EncodeException e) {
                e.printStackTrace();
            }

            logger.info(Arrays.toString(baos.toByteArray()));
            eventBus.broadcast(new ReceivedLocalPayloadEvent(localPayload));

            for (TransportDescriptor td : localPayload.getDescriptors()) {
                logger.info(td.getTransportId().toString());
            }
        }
    }

    @Override
    public synchronized void stopListening() {
        connector.stopListening();
    }

    @Override
    public synchronized void connectAndPerformKeyExchange(Payload remotePayload) {
        if (localPayload == null) {
            throw new IllegalStateException("Need to listen first and get local payload");
        }
        this.remotePayload = remotePayload;
        start();
    }
}
