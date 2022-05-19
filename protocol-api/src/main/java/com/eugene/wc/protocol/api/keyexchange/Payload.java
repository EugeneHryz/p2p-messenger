package com.eugene.wc.protocol.api.keyexchange;

import com.eugene.wc.protocol.api.util.ArrayUtil;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Payload implements Comparable<Payload> {

    public static final int COMMITMENT_LENGTH = 32;

    // part of public key hash
    private final byte[] commitment;
    private final List<TransportDescriptor> descriptors;

    public Payload(byte[] commitment, List<TransportDescriptor> descriptors) {
        if (commitment.length != COMMITMENT_LENGTH) {
            throw new IllegalArgumentException("Illegal commitment length");
        }
        this.commitment = commitment;
        this.descriptors = descriptors;
    }

    public byte[] getCommitment() {
        return commitment;
    }

    public List<TransportDescriptor> getDescriptors() {
        return descriptors;
    }

    @Override
    public String toString() {
        return "Payload{" +
                "commitment=" + Arrays.toString(commitment) +
                ", descriptors=" + descriptors +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payload payload = (Payload) o;
        return Arrays.equals(commitment, payload.commitment) && Objects.equals(descriptors, payload.descriptors);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(descriptors);
        result = 31 * result + Arrays.hashCode(commitment);
        return result;
    }

    @Override
    public int compareTo(Payload o) {
        return ArrayUtil.compare(commitment, 0, o.commitment, 0, commitment.length);
    }
}
