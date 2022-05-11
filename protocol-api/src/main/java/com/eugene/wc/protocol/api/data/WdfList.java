package com.eugene.wc.protocol.api.data;

import java.util.ArrayList;
import java.util.Collection;

public class WdfList extends ArrayList<Object> {

    public WdfList(int initialCapacity) {
        super(initialCapacity);
    }

    public WdfList() {
        super();
    }

    public WdfList(Collection<?> c) {
        super(c);
    }

    public Boolean getBoolean(int index) {
        Object o = get(index);
        if (!(o instanceof Boolean)) {
            throw new IllegalArgumentException();
        }
        return (Boolean) o;
    }

    public Integer getInteger(int index) {
        Object o = get(index);
        if (!(o instanceof Integer)) {
            throw new IllegalArgumentException();
        }
        return (Integer) o;
    }

    public Double getDouble(int index) {
        Object o = get(index);
        if (!(o instanceof Double)) {
            throw new IllegalArgumentException();
        }
        return (Double) o;
    }

    public String getString(int index) {
        Object o = get(index);
        if (!(o instanceof String)) {
            throw new IllegalArgumentException();
        }
        return (String) o;
    }

    public byte[] getRaw(int index) {
        Object o = get(index);
        if (!(o instanceof byte[])) {
            throw new IllegalArgumentException();
        }
        return (byte[]) o;
    }

    public WdfList getWdfList(int index) {
        Object o = get(index);
        if (!(o instanceof WdfList)) {
            throw new IllegalArgumentException();
        }
        return (WdfList) o;
    }
}
