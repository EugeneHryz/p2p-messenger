package com.eugene.wc.protocol.keyexchange;

import static com.eugene.wc.protocol.api.keyexchange.KeyAgreementConstants.CONNECTION_TIMEOUT;
import static com.eugene.wc.protocol.api.util.LogUtils.logException;
import static java.util.Arrays.asList;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

import com.eugene.wc.protocol.api.Pair;
import com.eugene.wc.protocol.api.crypto.KeyExchangeCrypto;
import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.data.WdfList;
import com.eugene.wc.protocol.api.keyexchange.ConnectionChooser;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeConnection;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeListener;
import com.eugene.wc.protocol.api.keyexchange.Payload;
import com.eugene.wc.protocol.api.keyexchange.TransportDescriptor;
import com.eugene.wc.protocol.api.keyexchange.exception.TransportException;
import com.eugene.wc.protocol.api.plugin.BluetoothConstants;
import com.eugene.wc.protocol.api.plugin.LanTcpConstants;
import com.eugene.wc.protocol.api.plugin.Plugin;
import com.eugene.wc.protocol.api.plugin.PluginManager;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexPlugin;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.annotation.Nullable;

public class KeyExchangeConnector {

    private static final Logger logger = Logger.getLogger(KeyExchangeConnector.class.getName());

    private static final List<TransportId> PREFERRED_TRANSPORTS =
            asList(BluetoothConstants.ID, LanTcpConstants.ID);

    private final PluginManager pluginManager;
    private final ConnectionChooser connectionChooser;
    private final List<KeyExchangeListener> listeners = new CopyOnWriteArrayList<>();
    private final KeyExchangeCrypto kec;

    private boolean isAlice;
    private final CountDownLatch aliceLatch = new CountDownLatch(1);
    private volatile boolean stopped;

    public KeyExchangeConnector(PluginManager pluginManager, ConnectionChooser chooser,
                                KeyExchangeCrypto kec) {
        this.pluginManager = pluginManager;
        connectionChooser = chooser;
        this.kec = kec;
    }

    public Payload listen(KeyPair localKeyPair) {
        byte[] commitment = kec.deriveCommitment(localKeyPair.getPublicKey().getBytes());
        logger.info("deriving commitment from local public key");

        List<TransportDescriptor> descriptors = new ArrayList<>();
        for (DuplexPlugin plugin : pluginManager.getKeyAgreementPlugins()) {

            logger.info("Creating KeyExchangeListener for " + plugin.getId());
            KeyExchangeListener listener = plugin.createKeyExchangeListener(commitment);
            if (listener != null) {
                WdfList properties = listener.getDescriptor();
                TransportId id = plugin.getId();

                descriptors.add(new TransportDescriptor(id, properties));
                listeners.add(listener);
                connectionChooser.submitTask(new ReadableTask(listener::accept));
            }
        }
        return new Payload(commitment, descriptors);
    }

    public void stopListening() {
        stopped = true;
        aliceLatch.countDown();
        for (KeyExchangeListener l : listeners) {
            l.close();
        }
        connectionChooser.close();
    }

    public KeyExchangeTransport connect(Payload remotePayload, boolean isAlice) {
        this.isAlice = isAlice;
        aliceLatch.countDown();

        // Start connecting over supported transports in order of preference
        if (logger.isLoggable(INFO)) {
            logger.info("Starting outgoing BQP connections as "
                    + (isAlice ? "Alice" : "Bob"));
        }
        Map<TransportId, TransportDescriptor> descriptors = new HashMap<>();
        for (TransportDescriptor d : remotePayload.getDescriptors()) {
            descriptors.put(d.getTransportId(), d);
        }
        List<Pair<DuplexPlugin, WdfList>> transports = new ArrayList<>();
        for (TransportId id : PREFERRED_TRANSPORTS) {

            TransportDescriptor d = descriptors.get(id);
            Plugin p = pluginManager.getPlugin(id);
            if (d != null && p instanceof DuplexPlugin) {
                if (logger.isLoggable(INFO))
                    logger.info("Connecting via " + id);
                transports.add(new Pair<>((DuplexPlugin) p, d.getProperties()));
            }
        }

        if (!transports.isEmpty()) {
            byte[] commitment = remotePayload.getCommitment();
            connectionChooser.submitTask(new ReadableTask(new ConnectorTask(transports, commitment)));
        }
        // Get chosen connection
        try {
            KeyExchangeConnection chosen = connectionChooser.pollConnection(CONNECTION_TIMEOUT);
            if (chosen == null) return null;
            return new KeyExchangeTransport(chosen);
        } finally {
            stopListening();
        }
    }

    private class ConnectorTask implements Callable<KeyExchangeConnection> {

        private final List<Pair<DuplexPlugin, WdfList>> transports;
        private final byte[] commitment;

        private ConnectorTask(List<Pair<DuplexPlugin, WdfList>> transports,
                              byte[] commitment) {
            this.transports = transports;
            this.commitment = commitment;
        }

        @Nullable
        @Override
        public KeyExchangeConnection call() throws Exception {
            // Repeat attempts until we connect, get stopped, or get interrupted
            while (!stopped) {
                for (Pair<DuplexPlugin, WdfList> pair : transports) {
                    if (stopped) return null;
                    DuplexPlugin plugin = pair.getFirst();
                    WdfList descriptor = pair.getSecond();
                    DuplexTransportConnection conn =
                            plugin.createKeyExchangeConnection(commitment, descriptor);
                    if (conn != null) {
                        if (logger.isLoggable(INFO))
                            logger.info(plugin.getId() + ": Outgoing connection");
                        return new KeyExchangeConnection(conn, plugin.getId());
                    }
                }
                Thread.sleep(2000);
            }
            return null;
        }
    }

    private class ReadableTask implements Callable<KeyExchangeConnection> {

        private final Callable<KeyExchangeConnection> connectionTask;

        private ReadableTask(Callable<KeyExchangeConnection> connectionTask) {
            this.connectionTask = connectionTask;
        }

        @Nullable
        @Override
        public KeyExchangeConnection call() throws Exception {
            KeyExchangeConnection c = connectionTask.call();
            if (c == null) return null;
            aliceLatch.await();
            if (isAlice || stopped) return c;
            // Bob waits here for Alice to scan his QR code, determine her
            // role, and send her key
            InputStream in = c.getConnection().getReader().getInputStream();
            while (!stopped && in.available() == 0) {
                if (logger.isLoggable(INFO))
                    logger.info(c.getTransportId() + ": Waiting for data");
//                waitingForAlice();
                Thread.sleep(500);
            }
            if (!stopped && logger.isLoggable(INFO))
                logger.info(c.getTransportId().toString() + ": Data available");
            return c;
        }
    }
}
