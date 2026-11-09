package gregtech.engine.material;

import java.util.Locale;
import java.util.Objects;

/** Structured provenance for one item-composition submission. */
public record ItemCompositionSource(
        Kind kind, String declarationId, String producer, String reference, String note) {
    public enum Kind {
        EXPLICIT(1),
        VANILLA_JSON(1),
        CORRECTION(1),
        BLOCK_WEIGHT(1),
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

    public ItemCompositionSource {
        Objects.requireNonNull(kind, "kind");
        requireText(declarationId, "declarationId");
        requireText(producer, "producer");
        reference = reference == null ? "" : reference;
        note = note == null ? "" : note;
    }

    public int priority() {
        return kind.priority();
    }

    public String description() {
        String value = declarationId + " [" + producer + "]";
        return note.isBlank() ? value : value + ": " + note;
    }

    /** Preserves the legacy free-form source while recognizing the known derived producers. */
    public static ItemCompositionSource fromLegacy(String source) {
        String value = source == null || source.isBlank() ? "unspecified" : source;
        String lower = value.toLowerCase(Locale.ROOT);
        Kind kind = lower.contains("common form tags") || lower.contains("external ore-prefix tags")
                ? Kind.FORM_TAG
                : lower.contains("cr.rev") || lower.contains("reversible crafting")
                        ? Kind.RECIPE_REVERSE
                        : lower.contains("block composition") ? Kind.BLOCK_WEIGHT
                        : value.equals("GT6 unification") ? Kind.UNIFICATION : Kind.EXPLICIT;
        return new ItemCompositionSource(kind, value, "legacy item-data producer", "", value);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }
}
