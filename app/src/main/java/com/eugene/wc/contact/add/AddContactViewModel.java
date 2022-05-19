package com.eugene.wc.contact.add;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.data.StreamDataReader;
import com.eugene.wc.protocol.api.data.StreamDataWriter;
import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.event.EventListener;
import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeTask;
import com.eugene.wc.protocol.api.keyexchange.Payload;
import com.eugene.wc.protocol.api.keyexchange.PayloadDecoder;
import com.eugene.wc.protocol.api.keyexchange.PayloadEncoder;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeListeningEvent;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeStartedEvent;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeWaitingEvent;
import com.eugene.wc.protocol.api.keyexchange.exception.DecodeException;
import com.eugene.wc.protocol.api.keyexchange.exception.EncodeException;
import com.eugene.wc.protocol.data.StreamDataReaderImpl;
import com.eugene.wc.protocol.data.StreamDataWriterImpl;
import com.eugene.wc.protocol.keyexchange.PayloadDecoderImpl;
import com.eugene.wc.protocol.keyexchange.PayloadEncoderImpl;
import com.eugene.wc.qrcode.QrCodeDecoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

public class AddContactViewModel extends ViewModel implements EventListener, QrCodeDecoder.Callback {

    private static final String TAG = AddContactViewModel.class.getName();

    private final MutableLiveData<State> state = new MutableLiveData<>();

    enum State {
        LISTENING,
        CONNECTING,
        WAITING,
        STARTED,
        FINISHED,
        ABORTED
    }

    private final CryptoComponent crypto;
    private final EventBus eventBus;
    private final Executor ioExecutor;

    private final KeyExchangeTask ket;

    private final MutableLiveData<String> encodedPayload = new MutableLiveData<>();
    private Payload localPayload;

    private final AtomicBoolean payloadReceived = new AtomicBoolean();

    @Inject
    public AddContactViewModel(CryptoComponent crypto, EventBus eventBus,
                               @IoExecutor Executor ioExecutor, KeyExchangeTask ket) {
        this.crypto = crypto;
        this.eventBus = eventBus;
        this.ioExecutor = ioExecutor;
        this.ket = ket;

        eventBus.addListener(this);
    }

    @Override
    public void onEventOccurred(Event e) {
        if (e instanceof KeyExchangeListeningEvent) {
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

    public LiveData<State> getState() {
        return state;
    }

    public LiveData<String> getEncodedLocalPayload() {
        return encodedPayload;
    }
}
