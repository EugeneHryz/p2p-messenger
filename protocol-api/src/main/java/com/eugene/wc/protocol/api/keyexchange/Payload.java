package com.eugene.wc.protocol.api.keyexchange;

import com.eugene.wc.protocol.api.util.ArrayUtil;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

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
    public int compareTo(Payload o) {
        return ArrayUtil.compare(commitment, 0, o.commitment, 0, commitment.length);
    }
}
