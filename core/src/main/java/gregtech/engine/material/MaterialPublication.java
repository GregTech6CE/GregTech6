package gregtech.engine.material;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Stable publication stamp for material definitions, effective compositions, and recipe rows.
 *
 * <p>Material definitions are process-static. The platform commits a new stamp only after all
 * composition bindings and recipe producers for one generation have finished successfully.</p>
 */
public final class MaterialPublication {
    public record Stamp(long revision, long materialRevision, long compositionRevision, long recipeRevision) {}

    private static final AtomicLong NEXT_REVISION = new AtomicLong();
    private static volatile Stamp current = new Stamp(0, 0, -1, -1);

    private MaterialPublication() {}

    /** Marks the one immutable material-definition publication for this process. */
    public static synchronized Stamp materialDefinitionsPublished(long materialRevision) {
        if (materialRevision <= 0) throw new IllegalArgumentException("Material revision must be positive");
        Stamp published = current;
        if (published.materialRevision() != 0)
            throw new IllegalStateException("Material definitions have already been published");
        published = new Stamp(
                NEXT_REVISION.incrementAndGet(),
                materialRevision,
                published.compositionRevision(),
                published.recipeRevision());
        current = published;
        return published;
    }

    /** Publishes one complete platform generation after compositions and recipe rows are final. */
    public static synchronized Stamp platformGenerationPublished(long compositionRevision, long recipeRevision) {
        Stamp previous = current;
        if (previous.materialRevision() == 0)
            throw new IllegalStateException("Material definitions must be published first");
        if (recipeRevision <= 0) throw new IllegalArgumentException("Recipe revision must be positive");
        Stamp published = new Stamp(
                NEXT_REVISION.incrementAndGet(), previous.materialRevision(), compositionRevision, recipeRevision);
        current = published;
        return published;
    }

    /** The last fully successful publication; an in-progress or failed generation leaves it unchanged. */
    public static Stamp current() {
        return current;
    }
}
