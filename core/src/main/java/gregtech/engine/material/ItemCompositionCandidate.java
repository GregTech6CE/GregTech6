package gregtech.engine.material;

import java.util.Objects;

/** One producer's proposed composition for an item. */
public record ItemCompositionCandidate(ItemComposition composition, String replacesSourceId) {
    public ItemCompositionCandidate {
        Objects.requireNonNull(composition, "composition");
        replacesSourceId = replacesSourceId == null || replacesSourceId.isBlank() ? null : replacesSourceId;
        if (composition.sources().size() != 1)
            throw new IllegalArgumentException("A submitted candidate must have exactly one source");
    }

    public String sourceId() {
        return composition.sources().get(0).declarationId();
    }
}
