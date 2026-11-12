package gregtech.engine.material;

import java.util.Objects;

/** One submitted item-composition candidate and its outcome in a resolved snapshot. */
public record ItemCompositionDecision(ItemComposition candidate, Status status, String reason) {
    public enum Status {
        SELECTED,
        REPLACED,
        LOWER_PRIORITY
    }

    public ItemCompositionDecision {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(status, "status");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason must not be blank");
    }

    public ItemCompositionSource source() {
        return candidate.sources().get(0);
    }
}
