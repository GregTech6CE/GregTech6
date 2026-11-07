package gregtech.engine.recipe.chem;

/** The distinct dispositions of a chemistry source row. Missing optional content is not a failure. */
public enum ChemOutcome {
    /** The row was resolved and accepted by the recipe map. */
    PUBLISHED,
    /** The row is intentionally superseded by a more specific producer. */
    SUBSTITUTED,
    /** A source condition excludes the row for this content set. */
    EXCLUDED,
    /** A required material, fluid, item or prefix is absent. */
    MISSING_CONTENT,
    /** An optional-mod fluid is absent; the row is skipped without failing startup. */
    OPTIONAL_ABSENT,
    /** Another row already owns these inputs and conditions. */
    COLLISION,
    /** The row itself is malformed or its resolved amounts exceed a native limit. */
    VALIDATION_FAILED
}
