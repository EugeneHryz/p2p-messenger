package com.eugene.wc.protocol.keyexchange;

import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.KeyExchangeCrypto;
import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.keyexchange.ConnectionChooser;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeResult;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeTask;
import com.eugene.wc.protocol.api.keyexchange.Payload;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeAbortedEvent;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeFinishedEvent;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeListeningEvent;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeStartedEvent;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeStoppedListeningEvent;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeWaitingEvent;
import com.eugene.wc.protocol.api.keyexchange.exception.AbortException;
import com.eugene.wc.protocol.api.transport.exception.TransportException;
import com.eugene.wc.protocol.api.plugin.PluginManager;

import java.util.logging.Logger;

import javax.inject.Inject;

public class KeyExchangeTaskImpl extends Thread implements KeyExchangeTask,
        KeyExchangeProtocol.Callback {

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
            if (transport == null) {
                throw new AbortException("Unable to establish remote connection");
            }
            KeyExchangeProtocol protocol = new KeyExchangeProtocol(this, transport, crypto,
                    kec, localPayload, remotePayload, localKeyPair, isAlice);
            SecretKey sharedSecret = protocol.perform();

            KeyExchangeResult result = new KeyExchangeResult(isAlice, transport.getConnection(),
                    sharedSecret);
            logger.info("About to broadcast KeyExchangeFinishedEvent...");
            eventBus.broadcast(new KeyExchangeFinishedEvent(result));

        } catch (AbortException e) {
            logger.warning("Key Exchange task was aborted " + e);
            eventBus.broadcast(new KeyExchangeAbortedEvent());
        } catch (TransportException e) {
            logger.warning("Unable to perform key exchange task " + e);
            eventBus.broadcast(new KeyExchangeAbortedEvent());
        }
    }

    @Override
    public synchronized void listen(KeyPair ephemeralKeyPair) {
        if (localPayload == null) {
            localKeyPair = ephemeralKeyPair;
            localPayload = connector.listen(localKeyPair);

            eventBus.broadcast(new KeyExchangeListeningEvent(localPayload));
        }
    }

    @Override
    public synchronized void stopListening() {
        if (localPayload != null) {
            connector.stopListening();
            eventBus.broadcast(new KeyExchangeStoppedListeningEvent());
        }
    }

    @Override
    public synchronized void connectAndPerformKeyExchange(Payload remotePayload) {
        if (localPayload == null) {
            throw new IllegalStateException("Need to listen first and get local payload");
        }
        this.remotePayload = remotePayload;
        start();
    }

    @Override
    public void waiting() {
        eventBus.broadcast(new KeyExchangeWaitingEvent());
    }

    @Override
    public void started() {
        eventBus.broadcast(new KeyExchangeStartedEvent());
    }
}
