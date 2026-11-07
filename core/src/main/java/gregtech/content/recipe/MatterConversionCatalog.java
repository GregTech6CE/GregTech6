package gregtech.content.recipe;

import java.util.List;

/**
 * Original {@code Loader_Recipes_Other} special matter conversions that have no generated table row.
 * The circuit-zero rows live in {@code GTOtherRecipes}; only the circuit-one variants remain here.
 */
public final class MatterConversionCatalog {
    private MatterConversionCatalog() {}

    /** Duration is in ticks; outputAmount is in millibuckets. All rows use 16 QU/t. */
    public record Conversion(
            String material, String form, int circuit, long duration, String outputFluid, int outputAmount) {}

    public static final List<Conversion> SPECIAL_CONVERSIONS = List.of(
            new Conversion("AncientDebris", "dustDiv72", 1, 144L, "Ender", 250),
            new Conversion("AncientDebris", "dustTiny", 1, 1152L, "Ender", 2000),
            new Conversion("AncientDebris", "dustSmall", 1, 2592L, "Ender", 4500),
            new Conversion("AncientDebris", "dust", 1, 10368L, "Ender", 18000),
            new Conversion("AncientDebris", "ingot", 1, 10368L, "Ender", 18000),
            new Conversion("AncientDebris", "blockDust", 1, 93312L, "Ender", 162000),
            new Conversion("AncientDebris", "blockIngot", 1, 93312L, "Ender", 162000));
}
