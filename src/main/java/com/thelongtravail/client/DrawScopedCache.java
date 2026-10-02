package com.thelongtravail.client;

import java.util.IdentityHashMap;

/** 缓存仅在单次绘制中有效；常见的单图集路径不分配映射表或工厂 lambda。 */
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
