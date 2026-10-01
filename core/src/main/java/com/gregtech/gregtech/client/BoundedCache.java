package com.gregtech.gregtech.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Small access-ordered cache; no world, ItemStack or GPU resources belong in its keys. */
public final class BoundedCache<K, V> {
    private final int limit;
    private final Map<K, V> values = new LinkedHashMap<>(4, 0.75f, true);
    public BoundedCache(int limit) {
        if (limit < 1) throw new IllegalArgumentException("Cache limit must be positive");
        this.limit = limit;
    }
    public synchronized V computeIfAbsent(K key, Function<? super K, ? extends V> factory) {
        V present = values.get(key);
        if (present != null) return present;
        V created = Objects.requireNonNull(factory.apply(key));
        if (values.size() >= limit) values.remove(values.keySet().iterator().next());
        values.put(key, created);
        return created;
    }
    public synchronized int size() { return values.size(); }
    public synchronized void clear() { values.clear(); }
}
