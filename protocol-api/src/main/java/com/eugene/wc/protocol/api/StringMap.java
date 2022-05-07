package com.eugene.wc.protocol.api;

import java.util.HashMap;
import java.util.Map;

public abstract class StringMap {

    private final Map<String, String> stringMap;

    protected StringMap(Map<String, String> stringMap) {
        this.stringMap = stringMap;
    }

    protected StringMap() {
        stringMap = new HashMap<>();
    }

    public void putBoolean(String key, boolean value) {
        stringMap.put(key, String.valueOf(value));
    }

    public boolean getBoolean(String key) {
        String value = stringMap.get(key);
        return Boolean.parseBoolean(value);
    }

    public void putInteger(String key, int value) {
        stringMap.put(key, String.valueOf(value));
    }

    public int getInteger(String key) {
        String value = stringMap.get(key);
        return Integer.parseInt(value);
    }

    public void putLong(String key, long value) {
        stringMap.put(key, String.valueOf(value));
    }

    public long getLong(String key) {
        String value = stringMap.get(key);
        return Long.parseLong(value);
    }

    public void putString(String key, String value) {
        stringMap.put(key, value);
    }

    public String getString(String key) {
        return stringMap.get(key);
    }
}
