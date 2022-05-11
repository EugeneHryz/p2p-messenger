package com.eugene.wc.protocol.api;

@FunctionalInterface
public interface Predicate<T> {

    boolean test(T t);
}
