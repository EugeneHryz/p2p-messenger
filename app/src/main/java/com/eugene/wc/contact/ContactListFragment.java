package com.eugene.wc.contact;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.fragment.BaseFragment;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.crypto.PrivateKey;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.data.StreamDataReader;
import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.event.EventListener;
import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeTask;
import com.eugene.wc.protocol.api.keyexchange.Payload;
import com.eugene.wc.protocol.api.keyexchange.PayloadDecoder;
import com.eugene.wc.protocol.api.keyexchange.exception.DecodeException;
import com.eugene.wc.protocol.api.lifecycle.EventExecutor;
import com.eugene.wc.protocol.api.util.StringUtils;
import com.eugene.wc.protocol.data.StreamDataReaderImpl;
import com.eugene.wc.protocol.keyexchange.PayloadDecoderImpl;
import com.eugene.wc.protocol.keyexchange.ReceivedLocalPayloadEvent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Provider;

public class ContactListFragment extends BaseFragment implements EventListener {

    private static final Logger logger = Logger.getLogger(ContactListFragment.class.getName());

    private static final String TAG = ContactListFragment.class.getName();

    @Inject
    @IoExecutor
    Executor ioExecutor;
    @Inject
    CryptoComponent crypto;
    @Inject
    Provider<KeyExchangeTask> ketProvider;
    @Inject
    EventBus eventBus;

    private KeyExchangeTask ket;

    @Override
    protected void injectFragment(ActivityComponent activityComponent) {
        activityComponent.inject(this);

        eventBus.addListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_list_fragment, container, false);

        FloatingActionButton fab = view.findViewById(R.id.add_contact_button);
        fab.setOnClickListener(v -> {
            startListening();
        });

        return view;
    }

    private void startListening() {
        ket = ketProvider.get();

        ioExecutor.execute(() -> {
//            KeyPair keyPair1 = crypto.generateAgreementKeyPair();
//
//            writeKeyToFile("pub1.key", keyPair1.getPublicKey().getBytes());
//            writeKeyToFile("pr1.key", keyPair1.getPrivateKey().getBytes());
//
//            logger.info("Generated public key: " +
//                    Arrays.toString(keyPair1.getPublicKey().getBytes()));

            byte[] pubK1 = readKeyFromFile("pub1.key");
            byte[] prK1 = readKeyFromFile("pr1.key");

            logger.info("pubK1 length: " + pubK1.length);
            logger.info("prK1 length: " + prK1.length);

            KeyPair kPair1 = new KeyPair(new PrivateKey(prK1), new PublicKey(pubK1));
            ket.listen(kPair1);

        });
    }

    @Override
    public void onEventOccurred(Event e) {

        if (e instanceof ReceivedLocalPayloadEvent) {
            ReceivedLocalPayloadEvent event = (ReceivedLocalPayloadEvent) e;
            logger.info("RECEIVED LOCAL PAYLOAD event");

            byte[] remotePayloadBytes = new byte[] {80, 0, 32, -127, 63, 123, -63, -117, -49, -56,
                    -2, 78, 72, 64, 52, -63, -114, -79, 34, 104, -83, -46, 45, 5, 42, 60, 79, -88,
                    -27, -7, -28, 46, -71, 95, -71, 0, 64, 0, 32, 99, 111, 109, 46, 101, 117, 103,
                    101, 110, 101, 46, 119, 99, 46, 112, 114, 111, 116, 111, 99, 111, 108, 46, 98,
                    108, 117, 101, 116, 111, 111, 116, 104, 0, 16, 0, 0, 0, 0, 80, 0, 6, 32, 71,
                    -38, -90, 57, -67, 96, 96};


            ByteArrayInputStream bais = new ByteArrayInputStream(remotePayloadBytes);
            StreamDataReader reader = new StreamDataReaderImpl(bais);
            PayloadDecoder decoder = new PayloadDecoderImpl(reader);

            Payload decodedPayload;
            try {
                decodedPayload = decoder.decode();
                logger.info("Payload decoded!!!");
                logger.info("First TransportId: " + decodedPayload.getDescriptors()
                        .get(0).getTransportId());

                ket.connectAndPerformKeyExchange(decodedPayload);

            } catch (DecodeException exc) {
                logger.warning("Unable to decode payload " + exc);
                throw new RuntimeException(exc);
            }
        }
    }

    private void writeKeyToFile(String filePath, byte[] keyBytes) {
        File dir = requireActivity().getDir(filePath, Context.MODE_PRIVATE);
        File file = new File(dir, filePath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(StringUtils.toHexString(keyBytes));

        } catch (IOException e) {
            logger.warning("IO error " + e);
            throw new RuntimeException(e);
        }
    }

    private byte[] readKeyFromFile(String filePath) {
        File dir = requireActivity().getDir(filePath, Context.MODE_PRIVATE);
        File file = new File(dir, filePath);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            StringBuilder strBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                strBuilder.append(line);
            }
            return StringUtils.fromHexString(strBuilder.toString());

        } catch (IOException e) {
            logger.warning("IO error " + e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getUniqueTag() {
        return TAG;
    }
}
