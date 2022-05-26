package com.eugene.wc.protocol.data;

import static com.eugene.wc.protocol.api.data.WdfDictionary2.NULL_VALUE;
import static com.eugene.wc.protocol.api.sync.Metadata.REMOVE;

import com.eugene.wc.protocol.api.ByteArray;
import com.eugene.wc.protocol.api.data.MetadataEncoder;
import com.eugene.wc.protocol.api.data.WdfDictionary2;
import com.eugene.wc.protocol.api.data.WdfWriter;
import com.eugene.wc.protocol.api.data.exception.FormatException;
import com.eugene.wc.protocol.api.sync.Metadata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
public class MetadataEncoderImpl implements MetadataEncoder {

	@Inject
	public MetadataEncoderImpl() {
	}

	@Override
	public Metadata encode(WdfDictionary2 d) throws FormatException {
		Metadata m = new Metadata();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		WdfWriter writer = new WdfWriterImpl(out);
		try {
			for (Entry<String, Object> e : d.entrySet()) {
				if (e.getValue() == NULL_VALUE) {
					// Special case: if value is null, key is being removed
					m.put(e.getKey(), REMOVE);
				} else {
					encodeObject(writer, e.getValue());
					m.put(e.getKey(), out.toByteArray());
					out.reset();
				}
			}
		} catch (FormatException e) {
			throw e;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return m;
	}

	private void encodeObject(WdfWriter writer, Object o)
			throws IOException {
		if (o instanceof Boolean) writer.writeBoolean((Boolean) o);
		else if (o instanceof Byte) writer.writeLong((Byte) o);
		else if (o instanceof Short) writer.writeLong((Short) o);
		else if (o instanceof Integer) writer.writeLong((Integer) o);
		else if (o instanceof Long) writer.writeLong((Long) o);
		else if (o instanceof Float) writer.writeDouble((Float) o);
		else if (o instanceof Double) writer.writeDouble((Double) o);
		else if (o instanceof String) writer.writeString((String) o);
		else if (o instanceof byte[]) writer.writeRaw((byte[]) o);
		else if (o instanceof ByteArray) writer.writeRaw(((ByteArray) o).getBytes());
		else if (o instanceof List) writer.writeList((List) o);
		else if (o instanceof Map) writer.writeDictionary((Map) o);
		else throw new FormatException();
	}
}
