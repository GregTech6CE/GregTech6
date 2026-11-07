package gregtech.engine.recipe.chem;

/** The source's {@code UT.Code.units(..., aRoundUp)} direction at a material boundary. */
public enum Rounding {
    FLOOR,
    CEIL;

    public static Rounding ofSource(boolean roundUp) {
        return roundUp ? CEIL : FLOOR;
    }
}
