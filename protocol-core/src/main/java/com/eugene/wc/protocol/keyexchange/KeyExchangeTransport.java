package com.eugene.wc.protocol.keyexchange;

import com.eugene.wc.protocol.api.Predicate;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.keyexchange.exception.AbortException;
import com.eugene.wc.protocol.api.keyexchange.exception.TransportException;
import com.eugene.wc.protocol.api.keyexchange.record.Record;
import com.eugene.wc.protocol.api.keyexchange.record.RecordReader;
import com.eugene.wc.protocol.api.keyexchange.record.RecordWriter;
import com.eugene.wc.protocol.keyexchange.record.RecordReaderImpl;
import com.eugene.wc.protocol.keyexchange.record.RecordWriterImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class KeyExchangeTransport {

    private static final Predicate<Record> ACCEPT_KEY = (r) -> r.getType() == Record.Type.KEY
                                                || r.getType() == Record.Type.ABORT;

    private RecordWriter recordWriter;
    private RecordReader recordReader;

    // todo: probably need to get them from DuplexConnection class or smth like that
    public KeyExchangeTransport(OutputStream out, InputStream in) {
        recordWriter = new RecordWriterImpl(out);
        recordReader = new RecordReaderImpl(in);
    }

    public void sendKey(PublicKey publicKey) throws TransportException {
        try {
            Record record = new Record(Record.Type.KEY, publicKey.getBytes());
            recordWriter.writeRecord(record);
        } catch (IOException e) {
            throw new TransportException("Unable to send key", e);
        }
    }

    public PublicKey receiveKey() throws TransportException, AbortException {
        try {
            Record record = recordReader.readRecord(ACCEPT_KEY);
            PublicKey publicKey = null;
            if (record != null) {
                if (record.getType() == Record.Type.ABORT) {
                    throw new AbortException();
                }
                publicKey = new PublicKey(record.getContent());
            }
            return publicKey;

        } catch (IOException e) {
            throw new TransportException("Unable to receive a key", e);
        }
    }

    public void sendAbort() throws TransportException {
        try {
            Record record = new Record(Record.Type.ABORT);
            recordWriter.writeRecord(record);
        } catch (IOException e) {
            throw new TransportException("Unable to send abort record");
        }
    }
}
