package com.eugene.wc.protocol.data;

import static com.eugene.wc.protocol.api.data.WdfDictionary2.NULL_VALUE;
import static com.eugene.wc.protocol.api.sync.Metadata.REMOVE;

import com.eugene.wc.protocol.api.data.MetadataParser;
import com.eugene.wc.protocol.api.data.WdfDictionary2;
import com.eugene.wc.protocol.api.data.WdfReader;
import com.eugene.wc.protocol.api.data.exception.FormatException;
import com.eugene.wc.protocol.api.sync.Metadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map.Entry;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
public class MetadataParserImpl implements MetadataParser {

	@Inject
	public MetadataParserImpl() {
	}

	@Override
	public WdfDictionary2 parse(Metadata m) throws FormatException {
		WdfDictionary2 d = new WdfDictionary2();
		try {
			for (Entry<String, byte[]> e : m.entrySet()) {
				// Special case: if key is being removed, value is null
				if (e.getValue() == REMOVE) d.put(e.getKey(), NULL_VALUE);
				else d.put(e.getKey(), parseValue(e.getValue()));
			}
		} catch (FormatException e) {
			throw e;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return d;
	}

	private Object parseValue(byte[] b) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		WdfReader reader = new WdfReaderImpl(in);
		Object o = parseObject(reader);
		if (!reader.eof()) throw new FormatException();
		return o;
	}

	private Object parseObject(WdfReader reader) throws IOException {
		if (reader.hasNull()) return NULL_VALUE;
		if (reader.hasBoolean()) return reader.readBoolean();
		if (reader.hasLong()) return reader.readLong();
		if (reader.hasDouble()) return reader.readDouble();
		if (reader.hasString()) return reader.readString();
		if (reader.hasRaw()) return reader.readRaw();
		if (reader.hasList()) return reader.readList();
		if (reader.hasDictionary()) return reader.readDictionary();
		throw new FormatException();
	}
}
