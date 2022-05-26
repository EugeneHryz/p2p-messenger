package com.eugene.wc.protocol.api.data;

import com.eugene.wc.protocol.api.ByteArray;
import com.eugene.wc.protocol.api.data.exception.FormatException;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class WdfDictionary2 extends TreeMap<String, Object> {

	public static final Object NULL_VALUE = new Object();

	/**
	 * Factory method for constructing dictionaries inline.
	 * <pre>
	 * BdfDictionary.of(
	 *     new BdfEntry("foo", foo),
	 *     new BdfEntry("bar", bar)
	 * );
	 * </pre>
	 */
	public static WdfDictionary2 of(Entry<String, ?>... entries) {
		WdfDictionary2 d = new WdfDictionary2();
		for (Entry<String, ?> e : entries) d.put(e.getKey(), e.getValue());
		return d;
	}

	public WdfDictionary2() {
		super();
	}

	public WdfDictionary2(Map<String, ?> m) {
		super(m);
	}

	public Boolean getBoolean(String key) throws FormatException {
		Object o = get(key);
		if (o instanceof Boolean) return (Boolean) o;
		throw new FormatException();
	}

	public Boolean getOptionalBoolean(String key) throws FormatException {
		Object o = get(key);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof Boolean) return (Boolean) o;
		throw new FormatException();
	}

	public Boolean getBoolean(String key, Boolean defaultValue) {
		Object o = get(key);
		if (o instanceof Boolean) return (Boolean) o;
		return defaultValue;
	}

	public Long getLong(String key) throws FormatException {
		Object o = get(key);
		if (o instanceof Long) return (Long) o;
		if (o instanceof Integer) return ((Integer) o).longValue();
		if (o instanceof Short) return ((Short) o).longValue();
		if (o instanceof Byte) return ((Byte) o).longValue();
		throw new FormatException();
	}

	public Long getOptionalLong(String key) throws FormatException {
		Object o = get(key);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof Long) return (Long) o;
		if (o instanceof Integer) return ((Integer) o).longValue();
		if (o instanceof Short) return ((Short) o).longValue();
		if (o instanceof Byte) return ((Byte) o).longValue();
		throw new FormatException();
	}

	public Long getLong(String key, Long defaultValue) {
		Object o = get(key);
		if (o instanceof Long) return (Long) o;
		if (o instanceof Integer) return ((Integer) o).longValue();
		if (o instanceof Short) return ((Short) o).longValue();
		if (o instanceof Byte) return ((Byte) o).longValue();
		return defaultValue;
	}

	public Double getDouble(String key) throws FormatException {
		Object o = get(key);
		if (o instanceof Double) return (Double) o;
		if (o instanceof Float) return ((Float) o).doubleValue();
		throw new FormatException();
	}

	public Double getOptionalDouble(String key) throws FormatException {
		Object o = get(key);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof Double) return (Double) o;
		if (o instanceof Float) return ((Float) o).doubleValue();
		throw new FormatException();
	}

	public Double getDouble(String key, Double defaultValue) {
		Object o = get(key);
		if (o instanceof Double) return (Double) o;
		if (o instanceof Float) return ((Float) o).doubleValue();
		return defaultValue;
	}

	public String getString(String key) throws FormatException {
		Object o = get(key);
		if (o instanceof String) return (String) o;
		throw new FormatException();
	}

	public String getOptionalString(String key) throws FormatException {
		Object o = get(key);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof String) return (String) o;
		throw new FormatException();
	}

	public String getString(String key, String defaultValue) {
		Object o = get(key);
		if (o instanceof String) return (String) o;
		return defaultValue;
	}

	public byte[] getRaw(String key) throws FormatException {
		Object o = get(key);
		if (o instanceof byte[]) return (byte[]) o;
		if (o instanceof ByteArray) return ((ByteArray) o).getBytes();
		throw new FormatException();
	}

	public byte[] getOptionalRaw(String key) throws FormatException {
		Object o = get(key);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof byte[]) return (byte[]) o;
		if (o instanceof ByteArray) return ((ByteArray) o).getBytes();
		throw new FormatException();
	}

	public byte[] getRaw(String key, byte[] defaultValue) {
		Object o = get(key);
		if (o instanceof byte[]) return (byte[]) o;
		if (o instanceof ByteArray) return ((ByteArray) o).getBytes();
		return defaultValue;
	}

	public WdfList getList(String key) throws FormatException {
		Object o = get(key);
		if (o instanceof WdfList) return (WdfList) o;
		throw new FormatException();
	}

	public WdfList getOptionalList(String key) throws FormatException {
		Object o = get(key);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof WdfList) return (WdfList) o;
		throw new FormatException();
	}

	public WdfList getList(String key, WdfList defaultValue) {
		Object o = get(key);
		if (o instanceof WdfList) return (WdfList) o;
		return defaultValue;
	}

	public WdfDictionary2 getDictionary(String key) throws FormatException {
		Object o = get(key);
		if (o instanceof WdfDictionary2) return (WdfDictionary2) o;
		throw new FormatException();
	}

	public WdfDictionary2 getOptionalDictionary(String key)
			throws FormatException {
		Object o = get(key);
		if (o == null || o == NULL_VALUE) return null;
		if (o instanceof WdfDictionary2) return (WdfDictionary2) o;
		throw new FormatException();
	}

	public WdfDictionary2 getDictionary(String key, WdfDictionary2 defaultValue) {
		Object o = get(key);
		if (o instanceof WdfDictionary2) return (WdfDictionary2) o;
		return defaultValue;
	}
}
