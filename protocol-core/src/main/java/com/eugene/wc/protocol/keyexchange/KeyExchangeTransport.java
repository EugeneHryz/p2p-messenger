package com.eugene.wc.protocol.keyexchange;

import com.eugene.wc.protocol.api.Predicate;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeConnection;
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
import java.util.logging.Logger;

public class KeyExchangeTransport {

    private static final Logger logger = Logger.getLogger(KeyExchangeTransport.class.getName());

    private static final Predicate<Record> ACCEPT_KEY = (r) -> r.getType() == Record.Type.KEY
                                                || r.getType() == Record.Type.ABORT;

    private RecordWriter recordWriter;
    private RecordReader recordReader;

    public KeyExchangeTransport(KeyExchangeConnection kec) {
        try {
            InputStream inputStream = kec.getConnection().getReader().getInputStream();
            OutputStream outputStream = kec.getConnection().getWriter().getOutputStream();

            recordWriter = new RecordWriterImpl(outputStream);
            recordReader = new RecordReaderImpl(inputStream);
        } catch (IOException e) {
            logger.warning("Unable to get InputStream or OutputStream from KeyExchangeConnection");
            throw new AssertionError();
        }
    }

    public void sendKey(byte[] keyBytes) throws TransportException {
        try {
            Record record = new Record(Record.Type.KEY, keyBytes);
            recordWriter.writeRecord(record);
        } catch (IOException e) {
            logger.warning(e.toString());
            throw new TransportException("Unable to send key", e);
        }
    }

    public byte[] receiveKey() throws TransportException, AbortException {
        try {
            Record record = recordReader.readRecord(ACCEPT_KEY);
            if (record != null) {
                if (record.getType() == Record.Type.ABORT) {
                    throw new AbortException();
                }
                return record.getContent();
            }
        } catch (IOException e) {
            logger.warning(e.toString());
            throw new TransportException("Unable to receive a key", e);
        }
        return null;
    }

    public void sendAbort() throws TransportException {
        try {
            Record record = new Record(Record.Type.ABORT);
            recordWriter.writeRecord(record);
        } catch (IOException e) {
            logger.warning(e.toString());
            throw new TransportException("Unable to send abort record");
        }
    }
}
