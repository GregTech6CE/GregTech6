package com.gregtech.gregtech.api.multiblock;

import java.util.*;
import java.util.function.*;

/** Atomic claim bookkeeping: validate every candidate before changing ownership. */
public final class PartBindings<P,R> {
    private final Set<P> bound=new HashSet<>();
    public boolean contains(P position) { return bound.contains(position); }
    public boolean update(Map<P,R> candidates, Predicate<P> canClaim, BiConsumer<P,R> claim, Consumer<P> release) {
        if (!candidates.keySet().stream().allMatch(canClaim)) { clear(release); return false; }
        for (P old:bound) if (!candidates.containsKey(old)) release.accept(old);
        candidates.forEach(claim);
        bound.clear(); bound.addAll(candidates.keySet());
        return true;
    }
    public void clear(Consumer<P> release) { bound.forEach(release); bound.clear(); }
}
