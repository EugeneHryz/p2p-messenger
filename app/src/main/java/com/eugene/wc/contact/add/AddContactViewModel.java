package com.eugene.wc.contact.add;

import static com.eugene.wc.work.TransportKeyRotationWork.CONTACT_ID_KEY;
import static com.eugene.wc.work.TransportKeyRotationWork.WORK_TAG;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.eugene.wc.protocol.api.client.ClientHelper;
import com.eugene.wc.protocol.api.connection.ConnectionManager;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.contact.exception.ContactExchangeException;
import com.eugene.wc.protocol.api.contact.exchange.ContactExchangeManager;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.data.StreamDataReader;
import com.eugene.wc.protocol.api.data.StreamDataWriter;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.event.EventListener;
import com.eugene.wc.protocol.api.identity.IdentityManager;
import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeResult;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeTask;
import com.eugene.wc.protocol.api.keyexchange.Payload;
import com.eugene.wc.protocol.api.keyexchange.PayloadDecoder;
import com.eugene.wc.protocol.api.keyexchange.PayloadEncoder;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeAbortedEvent;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeFinishedEvent;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeListeningEvent;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeStartedEvent;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeWaitingEvent;
import com.eugene.wc.protocol.api.keyexchange.exception.DecodeException;
import com.eugene.wc.protocol.api.keyexchange.exception.EncodeException;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;
import com.eugene.wc.protocol.api.properties.TransportPropertyManager;
import com.eugene.wc.protocol.contact.exchange.ContactExchangeManagerImpl;
import com.eugene.wc.protocol.data.StreamDataReaderImpl;
import com.eugene.wc.protocol.data.StreamDataWriterImpl;
import com.eugene.wc.protocol.keyexchange.PayloadDecoderImpl;
import com.eugene.wc.protocol.keyexchange.PayloadEncoderImpl;
import com.eugene.wc.qrcode.QrCodeDecoder;
import com.eugene.wc.work.TransportKeyRotationWork;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

public class AddContactViewModel extends AndroidViewModel implements EventListener, QrCodeDecoder.Callback {

    private static final String TAG = AddContactViewModel.class.getName();

    private final MutableLiveData<State> state = new MutableLiveData<>();

    enum State {
        LISTENING,
        CONNECTING,
        WAITING,
        STARTED,
        FINISHED,
        FAILED
    }

    private final IdentityManager identityManager;
    private final CryptoComponent crypto;
    private final EventBus eventBus;
    private final Executor ioExecutor;
    private final ContactManager contactManager;
    private final ConnectionManager connectionManager;
    private final ClientHelper clientHelper;
    private final TransportPropertyManager tpm;
    private final DatabaseComponent db;

    private final KeyExchangeTask ket;

    private final MutableLiveData<String> encodedPayload = new MutableLiveData<>();
    private Payload localPayload;

    private final AtomicBoolean payloadReceived = new AtomicBoolean();
    private boolean startedListening;

    @Inject
    public AddContactViewModel(Application app, CryptoComponent crypto,
                               EventBus eventBus, @IoExecutor Executor ioExecutor,
                               KeyExchangeTask ket, IdentityManager identityManager,
                               ContactManager contactManager, ClientHelper clientHelper,
                               TransportPropertyManager tpm, DatabaseComponent db,
                               ConnectionManager connectionManager) {
        super(app);
        this.identityManager = identityManager;
        this.crypto = crypto;
        this.contactManager = contactManager;
        this.clientHelper = clientHelper;
        this.connectionManager = connectionManager;
        this.tpm = tpm;
        this.db = db;
        this.eventBus = eventBus;
        this.ioExecutor = ioExecutor;
        this.ket = ket;

        eventBus.addListener(this);
    }

    @Override
    protected void onCleared() {
        eventBus.removeListener(this);

        if (startedListening) {
            ioExecutor.execute(ket::stopListening);
        }
    }

    @Override
    public void onEventOccurred(Event e) {
        if (e instanceof KeyExchangeListeningEvent) {
            startedListening = true;
            KeyExchangeListeningEvent event = (KeyExchangeListeningEvent) e;
            localPayload = event.getPayload();

            ioExecutor.execute(() -> {
                try {
                    String encoded = encodePayload(localPayload);

                    encodedPayload.postValue(encoded);
                } catch (EncodeException | UnsupportedEncodingException exc) {
                    // show an error msg to user?
                    Log.w(TAG, "Unable to encode payload", exc);
                }
            });
        } else if (e instanceof KeyExchangeStartedEvent) {
            state.postValue(State.STARTED);
        } else if (e instanceof KeyExchangeWaitingEvent) {
            state.postValue(State.WAITING);
        } else if (e instanceof KeyExchangeFinishedEvent) {
            KeyExchangeFinishedEvent event = (KeyExchangeFinishedEvent) e;

            Log.d(TAG, "Starting contact exchange...");
            startContactExchange(event.getResult());
        } else if (e instanceof KeyExchangeAbortedEvent) {
            state.postValue(State.FAILED);
        }
    }

    @Override
    public void onQrCodeDecoded(String decodedQr) {
        if (!payloadReceived.get()) {
            ioExecutor.execute(() -> {
                try {
                    Payload remotePayload = decodePayload(decodedQr);

                    payloadReceived.set(true);
                    Log.d(TAG, "QR-code decoded in viewModel");
                    ket.connectAndPerformKeyExchange(remotePayload);
                    state.postValue(State.CONNECTING);

                } catch (DecodeException e) {
                    // ??
                    Log.w(TAG, "Unable to decode payload", e);
                }
            });
        }
    }

    public void startAddingContact() {
        ioExecutor.execute(() -> {
            KeyPair ephemeralKeyPair = crypto.generateAgreementKeyPair();

            ket.listen(ephemeralKeyPair);
            state.postValue(State.LISTENING);
        });
    }

    public LiveData<State> getState() {
        return state;
    }

    public LiveData<String> getEncodedLocalPayload() {
        return encodedPayload;
    }

    private void startContactExchange(KeyExchangeResult result) {
        ContactExchangeManager cem = new ContactExchangeManagerImpl(result, identityManager,
                crypto, contactManager, clientHelper, tpm, db);

        DuplexTransportConnection conn = result.getTransport().getConnection();
        TransportId transportId = result.getTransport().getTransportId();

        ioExecutor.execute(() -> {
            try {
                ContactId addedContactId = cem.startContactExchange();
                scheduleContactKeysRotation(addedContactId);

                if (result.isAlice()) {
                    connectionManager.manageOutgoingConnection(conn, transportId, addedContactId);
                } else {
                    connectionManager.manageIncomingConnection(conn, transportId);
                }
                state.postValue(State.FINISHED);
            } catch (ContactExchangeException e) {
                Log.w(TAG, "Contact exchange failed", e);
            }
        });
    }

    private void scheduleContactKeysRotation(ContactId contactId) {
        Data inputData = new Data.Builder()
                .putInt(CONTACT_ID_KEY, contactId.getInt())
                .build();

        WorkRequest scheduledWorkRequest = new PeriodicWorkRequest.Builder(
                TransportKeyRotationWork.class, 1, TimeUnit.DAYS)
                .addTag(WORK_TAG)
                .setInputData(inputData)
                .setInitialDelay(1, TimeUnit.DAYS)
                .build();

        WorkManager.getInstance(getApplication()).enqueue(scheduledWorkRequest);
    }

    private String encodePayload(Payload payload) throws EncodeException,
            UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamDataWriter dataWriter = new StreamDataWriterImpl(baos);
        try {
            PayloadEncoder payloadEncoder = new PayloadEncoderImpl(dataWriter);
            payloadEncoder.encode(payload);

            String encodedP = new String(baos.toByteArray(), StandardCharsets.ISO_8859_1);
            Log.d(TAG, "Encoded payload: " + encodedP);
            return encodedP;
        } finally {
            try {
                dataWriter.close();
            } catch (IOException e) {
                Log.w(TAG, "Unable to close StreamDataWriter while encoding payload", e);
            }
        }
    }

    private Payload decodePayload(String encoded) throws DecodeException {
        ByteArrayInputStream bais = new ByteArrayInputStream(encoded
                .getBytes(StandardCharsets.ISO_8859_1));
        StreamDataReader dataReader = new StreamDataReaderImpl(bais);
        try {
            PayloadDecoder decoder = new PayloadDecoderImpl(dataReader);

            return decoder.decode();
        } finally {
            try {
                dataReader.close();
            } catch (IOException e) {
                Log.w(TAG, "Unable to close StreamDataReader while decoding payload", e);
            }
        }
    }
}
