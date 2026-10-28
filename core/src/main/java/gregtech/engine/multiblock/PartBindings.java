package gregtech.engine.multiblock;

import java.util.*;
import java.util.function.*;

/**
 * Atomic claim bookkeeping: validate every candidate before changing ownership, and only touch the
 * cells whose role is new, changed, or whose derived binding no longer holds. A repeated evaluation
 * with the same cells therefore claims and releases nothing.
 */
public final class PartBindings<P, R> {
    public enum Outcome {
        APPLIED,
        UNCHANGED,
        FAILED
    }

    private final Map<P, R> bound = new HashMap<>();

    public boolean contains(P position) {
        return bound.containsKey(position);
    }

    public Outcome update(
            Map<P, R> candidates,
            Predicate<P> canClaim,
            BiPredicate<P, R> retained,
            BiConsumer<P, R> claim,
            Consumer<P> release) {
        Objects.requireNonNull(candidates);
        Objects.requireNonNull(canClaim);
        Objects.requireNonNull(retained);
        Objects.requireNonNull(claim);
        Objects.requireNonNull(release);
        if (!candidates.keySet().stream().allMatch(canClaim)) {
            clear(release);
            return Outcome.FAILED;
        }
        boolean changed = false;
        for (P old : new ArrayList<>(bound.keySet())) {
            if (!candidates.containsKey(old)) {
                release.accept(old);
                changed = true;
            }
        }
        for (var entry : candidates.entrySet()) {
            P position = entry.getKey();
            R role = entry.getValue();
            R previous = bound.get(position);
            if (previous != null && Objects.equals(previous, role) && retained.test(position, role)) continue;
            claim.accept(position, role);
            changed = true;
        }
        bound.clear();
        bound.putAll(candidates);
        return changed ? Outcome.APPLIED : Outcome.UNCHANGED;
    }

    public void clear(Consumer<P> release) {
        for (P position : new ArrayList<>(bound.keySet())) release.accept(position);
        bound.clear();
    }

    public Set<P> positions() {
        return Set.copyOf(bound.keySet());
    }
}
