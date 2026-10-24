package gregtech.internal.fluid;

import java.util.Map;

/** Saltnya FluidTankGT long storage rules; the platform owns fluid identity and serialization. */
public final class LongFluidStorage {
    private long capacity, amount;
    private boolean preventDraining, keepFilterOnEmpty, voidExcess;
    private Map<String, Long> adjustableCapacity;
    private long adjustableMultiplier = 1;

    public LongFluidStorage(long capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public long amount() {
        return amount;
    }

    public long baseCapacity() {
        return capacity;
    }

    public boolean preventsDraining() {
        return preventDraining;
    }

    public boolean keepsFilterOnEmpty() {
        return keepFilterOnEmpty;
    }

    /** Unlike GT6's similarly named flag, this forbids extraction rather than retaining identity. */
    public void preventDraining(boolean value) {
        preventDraining = value;
    }

    public void keepFilterOnEmpty(boolean value) {
        keepFilterOnEmpty = value;
    }

    public void voidExcess(boolean value) {
        voidExcess = value;
    }

    public void adjustableCapacity(Map<String, Long> capacities, long multiplier) {
        adjustableCapacity = capacities;
        adjustableMultiplier = multiplier;
    }

    public long capacity(String key) {
        if (key == null || adjustableCapacity == null) return Math.max(amount, capacity);
        Long override = adjustableCapacity.get(key);
        return override == null ? Math.max(amount, capacity)
                : Math.max(override * adjustableMultiplier, Math.max(amount, capacity));
    }

    public long add(long offered, String key) {
        if (offered <= 0) return 0;
        long space = capacity(key) - amount;
        if (space <= 0) return voidExcess ? offered : 0;
        long accepted = Math.min(offered, space);
        amount += accepted;
        return accepted;
    }

    public long remove(long requested) {
        if (preventDraining || requested <= 0) return 0;
        long removed = Math.min(requested, amount);
        amount -= removed;
        if (amount <= 0) amount = 0;
        return removed;
    }

    /** Exact-amount removal under the port's drain lock; callers supply nonnegative input. */
    public boolean drainAll(long requested) {
        if (preventDraining || requested > amount) return false;
        amount -= requested;
        if (amount <= 0) amount = 0;
        return true;
    }

    public void setAmount(long value) {
        amount = value;
    }

    /** Current resize policy truncates contents; GT6 changed only the nominal capacity. */
    public void setCapacity(long value) {
        capacity = Math.max(0, value);
        if (amount > capacity) amount = capacity;
    }

    /** Persisted long values retain the original load semantics; do not silently truncate saves. */
    public void restore(long storedAmount, Long storedCapacity) {
        amount = storedAmount;
        if (storedCapacity != null) capacity = storedCapacity;
    }

    public static int bindInt(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }
}
