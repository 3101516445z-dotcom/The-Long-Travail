package com.thelongtravail.client;

import java.util.IdentityHashMap;

/** One draw owns its values. The common single-atlas path needs no map or factory lambda. */
public abstract class DrawScopedCache<K, V> {
    private K first;
    private V firstValue;
    private IdentityHashMap<K, V> others;

    public final V value(K key) {
        java.util.Objects.requireNonNull(key);
        if (first == null) { first = key; firstValue = create(key); }
        if (key == first) return firstValue;
        if (others == null) others = new IdentityHashMap<>();
        V value = others.get(key);
        if (value == null) { value = create(key); others.put(key, value); }
        return value;
    }

    protected abstract V create(K key);
}
