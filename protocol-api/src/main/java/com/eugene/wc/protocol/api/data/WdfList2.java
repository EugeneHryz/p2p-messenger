package com.eugene.wc.protocol.api.data;

import static com.eugene.wc.protocol.api.data.WdfDictionary2.NULL_VALUE;

import com.eugene.wc.protocol.api.ByteArray;
import com.eugene.wc.protocol.api.data.exception.FormatException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WdfList2 extends ArrayList<Object> {

	/**
	 * Factory method for constructing lists inline.
	 * <pre>
	 * BdfList.of(1, 2, 3);
	 * </pre>
	 */
	public static WdfList2 of(Object... items) {
		return new WdfList2(Arrays.asList(items));
	}

	public WdfList2() {
		super();
	}

	public WdfList2(List<Object> items) {
		super(items);
	}

	private boolean isInRange(int index) {
		return index >= 0 && index < size();
	}

	public Boolean getBoolean(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o instanceof Boolean) return (Boolean) o;
		throw new FormatException();
	}

	public Boolean getOptionalBoolean(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof Boolean) return (Boolean) o;
		throw new FormatException();
	}

	public Boolean getBoolean(int index, Boolean defaultValue) {
		if (!isInRange(index)) return defaultValue;
		Object o = get(index);
		if (o instanceof Boolean) return (Boolean) o;
		return defaultValue;
	}

	public Long getLong(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o instanceof Long) return (Long) o;
		if (o instanceof Integer) return ((Integer) o).longValue();
		if (o instanceof Short) return ((Short) o).longValue();
		if (o instanceof Byte) return ((Byte) o).longValue();
		throw new FormatException();
	}

	public Long getOptionalLong(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof Long) return (Long) o;
		if (o instanceof Integer) return ((Integer) o).longValue();
		if (o instanceof Short) return ((Short) o).longValue();
		if (o instanceof Byte) return ((Byte) o).longValue();
		throw new FormatException();
	}

	public Long getLong(int index, Long defaultValue) {
		if (!isInRange(index)) return defaultValue;
		Object o = get(index);
		if (o instanceof Long) return (Long) o;
		if (o instanceof Integer) return ((Integer) o).longValue();
		if (o instanceof Short) return ((Short) o).longValue();
		if (o instanceof Byte) return ((Byte) o).longValue();
		return defaultValue;
	}

	public Double getDouble(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o instanceof Double) return (Double) o;
		if (o instanceof Float) return ((Float) o).doubleValue();
		throw new FormatException();
	}

	public Double getOptionalDouble(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof Double) return (Double) o;
		if (o instanceof Float) return ((Float) o).doubleValue();
		throw new FormatException();
	}

	public Double getDouble(int index, Double defaultValue) {
		if (!isInRange(index)) return defaultValue;
		Object o = get(index);
		if (o instanceof Double) return (Double) o;
		if (o instanceof Float) return ((Float) o).doubleValue();
		return defaultValue;
	}

	public String getString(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o instanceof String) return (String) o;
		throw new FormatException();
	}

	public String getOptionalString(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof String) return (String) o;
		throw new FormatException();
	}

	public String getString(int index, String defaultValue) {
		if (!isInRange(index)) return defaultValue;
		Object o = get(index);
		if (o instanceof String) return (String) o;
		return defaultValue;
	}

	public byte[] getRaw(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o instanceof byte[]) return (byte[]) o;
		if (o instanceof ByteArray) return ((ByteArray) o).getBytes();
		throw new FormatException();
	}

	public byte[] getOptionalRaw(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof byte[]) return (byte[]) o;
		if (o instanceof ByteArray) return ((ByteArray) o).getBytes();
		throw new FormatException();
	}

	public byte[] getRaw(int index, byte[] defaultValue) {
		if (!isInRange(index)) return defaultValue;
		Object o = get(index);
		if (o instanceof byte[]) return (byte[]) o;
		if (o instanceof ByteArray) return ((ByteArray) o).getBytes();
		return defaultValue;
	}

	public WdfList2 getList(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o instanceof WdfList2) return (WdfList2) o;
		throw new FormatException();
	}

	public WdfList2 getOptionalList(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof WdfList2) return (WdfList2) o;
		throw new FormatException();
	}

	public WdfList2 getList(int index, WdfList2 defaultValue) {
		if (!isInRange(index)) return defaultValue;
		Object o = get(index);
		if (o instanceof WdfList2) return (WdfList2) o;
		return defaultValue;
	}

	public WdfDictionary2 getDictionary(int index) throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o instanceof WdfDictionary2) return (WdfDictionary2) o;
		throw new FormatException();
	}

	public WdfDictionary2 getOptionalDictionary(int index)
			throws FormatException {
		if (!isInRange(index)) throw new FormatException();
		Object o = get(index);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof WdfDictionary2) return (WdfDictionary2) o;
		throw new FormatException();
	}

	public WdfDictionary2 getDictionary(int index, WdfDictionary2 defaultValue) {
		if (!isInRange(index)) return defaultValue;
		Object o = get(index);
		if (o instanceof WdfDictionary2) return (WdfDictionary2) o;
		return defaultValue;
	}
}
