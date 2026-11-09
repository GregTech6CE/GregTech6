package gregtech.engine.material;

import java.util.Objects;

/**
 * One producer's proposed composition for an item. {@code itemKey} is a stable diagnostic key;
 * the caller may resolve its own runtime key separately.
 */
public record ItemCompositionCandidate(
        String itemKey, ItemComposition composition, String replacesSourceId) {
    public ItemCompositionCandidate {
        if (itemKey == null || itemKey.isBlank())
            throw new IllegalArgumentException("itemKey must not be blank");
        Objects.requireNonNull(composition, "composition");
        replacesSourceId = replacesSourceId == null || replacesSourceId.isBlank() ? null : replacesSourceId;
        if (composition.sources().size() != 1)
            throw new IllegalArgumentException("A submitted candidate must have exactly one source");
    }

    public String sourceId() {
        return composition.sources().get(0).declarationId();
    }
}
