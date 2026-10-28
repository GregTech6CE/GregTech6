package gregtech.engine.logistics;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Predicate;

/** Per-route, bounded readiness polling; positions must be immutable and predicates non-mutating. */
public final class PendingRouteFrontier<P> {
    public static final int POLL_BUDGET = 16;

    public record Ready<P>(P position, boolean rescan) {}

    private final LinkedHashMap<P, Boolean> pending = new LinkedHashMap<>();

    /** A missing search boundary needs a fresh search once readable. */
    public void addScanBoundary(P position) {
        // Replacing a value does not reorder this insertion-ordered map.
        pending.put(Objects.requireNonNull(position), true);
    }

    /** An authenticated route needs state comparison, not unconditional rescanning. */
    public void addWatchedChunk(P position) {
        pending.putIfAbsent(Objects.requireNonNull(position), false);
    }

    /** Reset/new scan/successful resolution forgets pending work and its cursor. */
    public void clear() {
        pending.clear();
    }

    /**
     * Return and remove at most one ready position, or null. At most 16 distinct
     * positions are checked per call; unready entries rotate to the tail. Repeated
     * adds do not move entries or starve the rest. No topology epoch is required.
     */
    public Ready<P> pollReady(Predicate<? super P> readable) {
        Objects.requireNonNull(readable);
        int checks = Math.min(POLL_BUDGET, pending.size());
        for (int i = 0; i < checks; i++) {
            var iterator = pending.entrySet().iterator();
            var first = iterator.next();
            P position = first.getKey();
            boolean rescan = first.getValue();
            boolean ready = readable.test(position);
            iterator.remove();
            if (ready) return new Ready<>(position, rescan);
            pending.put(position, rescan);
        }
        return null;
    }
}
