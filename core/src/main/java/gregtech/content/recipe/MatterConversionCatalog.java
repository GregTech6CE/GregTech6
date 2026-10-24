package gregtech.content.recipe;

import java.util.List;

/** Original Loader_Recipes_Other special matter conversions; native resolution is a separate step. */
public final class MatterConversionCatalog {
    private MatterConversionCatalog() {}

    /** Duration is in ticks; outputAmount is in millibuckets. All rows use 16 QU/t. */
    public record Conversion(String material, String form, int circuit, long duration,
                             String outputFluid, int outputAmount) {}

    public static final List<Conversion> SPECIAL_CONVERSIONS = List.of(
            new Conversion("Dilithium",     "dustDiv72",  1, 144L,    "Ender_TE", 250),
            new Conversion("Dilithium",     "dustTiny",   1, 1152L,   "Ender_TE", 20000),
            new Conversion("Dilithium",     "dustSmall",  1, 2592L,   "Ender_TE", 45000),
            new Conversion("Dilithium",     "dust",       1, 10368L,  "Ender_TE", 180000),
            new Conversion("Dilithium",     "gem",        1, 20736L,  "Ender_TE", 360000),
            new Conversion("Dilithium",     "blockDust",  1, 93312L,  "Ender_TE", 1620000),
            new Conversion("Dilithium",     "blockGem",   1, 186624L, "Ender_TE", 3240000),
            new Conversion("AncientDebris", "dustDiv72",  1, 144L,    "Ender_TE", 250),
            new Conversion("AncientDebris", "dustTiny",   1, 1152L,   "Ender_TE", 2000),
            new Conversion("AncientDebris", "dustSmall",  1, 2592L,   "Ender_TE", 4500),
            new Conversion("AncientDebris", "dust",       1, 10368L,  "Ender_TE", 18000),
            new Conversion("AncientDebris", "ingot",      1, 10368L,  "Ender_TE", 18000),
            new Conversion("AncientDebris", "blockDust",  1, 93312L,  "Ender_TE", 162000),
            new Conversion("AncientDebris", "blockIngot", 1, 93312L,  "Ender_TE", 162000),
            new Conversion("Dilithium",     "dustDiv72",  0, 144L,    "Ender",    144),
            new Conversion("Dilithium",     "dustTiny",   0, 1152L,   "Ender",    11520),
            new Conversion("Dilithium",     "dustSmall",  0, 2592L,   "Ender",    25920),
            new Conversion("Dilithium",     "dust",       0, 10368L,  "Ender",    103680),
            new Conversion("Dilithium",     "gem",        0, 20736L,  "Ender",    207360),
            new Conversion("Dilithium",     "blockDust",  0, 93312L,  "Ender",    933120),
            new Conversion("Dilithium",     "blockGem",   0, 186624L, "Ender",    1866240),
            new Conversion("AncientDebris", "dustDiv72",  0, 144L,    "Ender",    144),
            new Conversion("AncientDebris", "dustTiny",   0, 1152L,   "Ender",    1152),
            new Conversion("AncientDebris", "dustSmall",  0, 2592L,   "Ender",    2592),
            new Conversion("AncientDebris", "dust",       0, 10368L,  "Ender",    10368),
            new Conversion("AncientDebris", "ingot",      0, 10368L,  "Ender",    10368),
            new Conversion("AncientDebris", "blockDust",  0, 93312L,  "Ender",    93312),
            new Conversion("AncientDebris", "blockIngot", 0, 93312L,  "Ender",    93312)
    );
}
