package gregtech.engine.material;

import java.util.List;
import java.util.Objects;

/** Structured provenance for one item-composition submission. */
public record ItemCompositionSource(
        Kind kind,
        String declarationId,
        String producer,
        String reference,
        String note,
        List<String> replacedSourceIds,
        MaterialProcessingAuthority materialProcessingAuthority) {
    public enum Kind {
        EXPLICIT(1),
        VANILLA_JSON(1),
        CORRECTION(1),
        BLOCK_WEIGHT(1),
        LEGACY(1),
        UNIFICATION(2),
        FORM_TAG(3),
        RECIPE_REVERSE(4);

        private final int priority;

        Kind(int priority) {
            this.priority = priority;
        }

        public int priority() {
            return priority;
        }
    }

    public enum MaterialProcessingAuthority {
        DEFAULT,
        ALLOW,
        DENY
    }

    public ItemCompositionSource {
        Objects.requireNonNull(kind, "kind");
        requireText(declarationId, "declarationId");
        requireText(producer, "producer");
        reference = reference == null ? "" : reference;
        note = note == null ? "" : note;
        replacedSourceIds = List.copyOf(Objects.requireNonNull(replacedSourceIds, "replacedSourceIds"));
        materialProcessingAuthority =
                Objects.requireNonNull(materialProcessingAuthority, "materialProcessingAuthority");
        if (replacedSourceIds.stream().anyMatch(value -> value == null || value.isBlank()))
            throw new IllegalArgumentException("replacedSourceIds must not contain blank values");
    }

    public ItemCompositionSource(Kind kind, String declarationId, String producer, String reference, String note) {
        this(kind, declarationId, producer, reference, note, List.of(), MaterialProcessingAuthority.DEFAULT);
    }

    public ItemCompositionSource(
            Kind kind,
            String declarationId,
            String producer,
            String reference,
            String note,
            List<String> replacedSourceIds) {
        this(kind, declarationId, producer, reference, note, replacedSourceIds, MaterialProcessingAuthority.DEFAULT);
    }

    public int priority() {
        return kind.priority();
    }

    /** Whether the selected source may replace a vanilla cooking rule. */
    public boolean allowsMaterialProcessingRule() {
        return switch (materialProcessingAuthority) {
            case ALLOW -> true;
            case DENY -> false;
            case DEFAULT ->
                switch (kind) {
                    case CORRECTION, BLOCK_WEIGHT, VANILLA_JSON -> false;
                    default -> true;
                };
        };
    }

    public String description() {
        String value = declarationId + " [" + producer + "]";
        if (!reference.isBlank() && !reference.equals(declarationId)) value += " @" + reference;
        if (!replacedSourceIds.isEmpty()) value += " replaces " + String.join(" -> ", replacedSourceIds);
        return note.isBlank() ? value : value + ": " + note;
    }

    /**
     * Preserves legacy free-form source text without guessing its category from wording.
     *
     * <p>The compatibility registration path replaces repeated legacy submissions. New producers
     * must submit a controlled {@link Kind} instead.</p>
     */
    @Deprecated(forRemoval = false)
    public static ItemCompositionSource fromLegacy(String source) {
        String value = source == null || source.isBlank() ? "unspecified" : source;
        return new ItemCompositionSource(
                Kind.LEGACY,
                value,
                "legacy item-data producer",
                value,
                "category not inferred from source text",
                List.of(),
                MaterialProcessingAuthority.DEFAULT);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }
}
