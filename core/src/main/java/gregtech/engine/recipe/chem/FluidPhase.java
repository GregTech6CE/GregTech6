package gregtech.engine.recipe.chem;

/** The material phase a source expression names explicitly. Never inferred from a fluid id. */
public enum FluidPhase {
    LIQUID("liquid"),
    GAS("gas"),
    PLASMA("plasma");

    private final String sourceName;

    FluidPhase(String sourceName) {
        this.sourceName = sourceName;
    }

    public String sourceName() {
        return sourceName;
    }

    public static FluidPhase ofSource(String name) {
        for (FluidPhase phase : values()) if (phase.sourceName.equals(name)) return phase;
        throw new IllegalArgumentException("Unknown material phase " + name);
    }
}
