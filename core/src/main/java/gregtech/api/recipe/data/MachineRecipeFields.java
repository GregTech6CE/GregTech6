package gregtech.api.recipe.data;

/** JSON names shared by the Forge and NeoForge machine-recipe datapack readers. */
public final class MachineRecipeFields {
    public static final String TYPE_ADDITION = "gt6ce:machine";
    public static final String TYPE_REMOVAL = "gt6ce:machine_removal";
    public static final String MAP = "map";
    public static final String ITEM_INPUTS = "item_inputs";
    public static final String FLUID_INPUTS = "fluid_inputs";
    public static final String ITEM_OUTPUTS = "item_outputs";
    public static final String FLUID_OUTPUTS = "fluid_outputs";
    public static final String EU_PER_TICK = "eu_per_tick";
    public static final String DURATION = "duration";
    public static final String SPECIAL = "special";
    public static final String CLEAR = "clear";
    public static final String ITEM = "item";
    public static final String TAG = "tag";
    public static final String FLUID = "fluid";
    public static final String COUNT = "count";
    public static final String AMOUNT = "amount";
    public static final String CHANCE = "chance";
    /** GT6 output chances are parts per 10_000. 10_000 is certain. */
    public static final int CHANCE_CERTAIN = 10_000;
    /** Tag inputs expand to one concrete row per item, and stop at this product. */
    public static final int EXPANSION_LIMIT = 4096;
    public static final long MAX_AMOUNT = Integer.MAX_VALUE;

    private MachineRecipeFields() {}
}
