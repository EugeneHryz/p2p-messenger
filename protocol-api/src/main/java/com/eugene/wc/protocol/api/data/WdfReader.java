package com.eugene.wc.protocol.api.data;

import java.io.IOException;

public interface WdfReader {

	int DEFAULT_NESTED_LIMIT = 5;
	int DEFAULT_MAX_BUFFER_SIZE = 64 * 1024;

	int available() throws IOException;

	boolean eof() throws IOException;

	void close() throws IOException;

	boolean hasNull() throws IOException;

	void readNull() throws IOException;

	void skipNull() throws IOException;

	boolean hasBoolean() throws IOException;

	boolean readBoolean() throws IOException;

	void skipBoolean() throws IOException;

	boolean hasLong() throws IOException;

	long readLong() throws IOException;

	void skipLong() throws IOException;

	boolean hasDouble() throws IOException;

	double readDouble() throws IOException;

	void skipDouble() throws IOException;

	boolean hasString() throws IOException;

	String readString() throws IOException;

	void skipString() throws IOException;

	boolean hasRaw() throws IOException;

	byte[] readRaw() throws IOException;

	void skipRaw() throws IOException;

	boolean hasList() throws IOException;

	WdfList2 readList() throws IOException;

	void readListStart() throws IOException;

	boolean hasListEnd() throws IOException;

	void readListEnd() throws IOException;

	void skipList() throws IOException;

	boolean hasDictionary() throws IOException;

	WdfDictionary2 readDictionary() throws IOException;

	void readDictionaryStart() throws IOException;

	boolean hasDictionaryEnd() throws IOException;

	void readDictionaryEnd() throws IOException;

	void skipDictionary() throws IOException;
}
