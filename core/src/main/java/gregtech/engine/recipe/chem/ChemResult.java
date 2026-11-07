package gregtech.engine.recipe.chem;

/** One source row's disposition, with the reason needed to audit it later. */
public record ChemResult(ChemRecipe recipe, String source, ChemOutcome outcome, String detail, String existingSource) {
    public ChemResult {
        if (outcome == null) throw new IllegalArgumentException("Chem result outcome");
        if (source == null || source.isBlank()) source = recipe == null ? "" : recipe.source();
        if (source == null || source.isBlank()) throw new IllegalArgumentException("Chem result source");
        detail = detail == null ? "" : detail;
        existingSource = existingSource == null ? "" : existingSource;
    }

    public static ChemResult published(ChemRecipe recipe) {
        return new ChemResult(recipe, recipe.source(), ChemOutcome.PUBLISHED, "", "");
    }

    public static ChemResult failed(ChemRecipe recipe, ChemOutcome outcome, String detail) {
        return new ChemResult(recipe, recipe.source(), outcome, detail, "");
    }

    public static ChemResult collision(ChemRecipe recipe, String existingSource) {
        return new ChemResult(recipe, recipe.source(), ChemOutcome.COLLISION, "input collision", existingSource);
    }

    public static ChemResult excluded(String source, String detail) {
        return new ChemResult(null, source, ChemOutcome.EXCLUDED, detail, "");
    }

    public static ChemResult optionalAbsent(String source, String detail) {
        return new ChemResult(null, source, ChemOutcome.OPTIONAL_ABSENT, detail, "");
    }
}
