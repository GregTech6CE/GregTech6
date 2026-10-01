package com.gregtech.gregtech.api.inventory;

import java.util.function.BiPredicate;
import java.util.function.UnaryOperator;

/** Bulk count is never encoded in an ItemStack count. Simulation never selects a filter. */
public final class BulkStorageState<K> {
    private final long capacity;
    private final BiPredicate<K,K> same;
    private final UnaryOperator<K> copy;
    private K filter;
    private long count;
    private boolean resetWhenEmpty;
    public BulkStorageState(long capacity, BiPredicate<K,K> same, UnaryOperator<K> copy) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity");
        this.capacity = capacity; this.same = same; this.copy = copy;
    }
    public K filter() { return filter == null ? null : copy.apply(filter); }
    public long count() { return count; }
    public long capacity() { return capacity; }
    public boolean accepts(K key) { return key != null && (filter == null || same.test(filter, key)); }
    public long insert(K key, long amount, boolean simulate) {
        if (amount <= 0 || !accepts(key)) return 0;
        long accepted = Math.min(amount, Math.max(0, capacity - count));
        if (!simulate && accepted > 0) { if (filter == null) filter = copy.apply(key); count += accepted; }
        return accepted;
    }
    public long extract(long amount, boolean simulate) {
        long taken = Math.min(count, Math.max(0, amount));
        if (!simulate && taken > 0) { count -= taken; clearIfEmpty(); }
        return taken;
    }
    public void setResetWhenEmpty(boolean reset) { resetWhenEmpty = reset; clearIfEmpty(); }
    private void clearIfEmpty() { if (count == 0 && resetWhenEmpty) filter = null; }
    public void restore(K key, long amount) {
        filter = key == null ? null : copy.apply(key);
        // Preserve legacy over-capacity contents; reject inserts until enough has been extracted.
        count = filter == null ? 0 : Math.max(0, amount);
        clearIfEmpty();
    }
}
