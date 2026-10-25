package gregtech;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.ModConfigSpec;
*///?} else {
import net.minecraftforge.common.ForgeConfigSpec;
//?}

/** Small user-facing switches; detailed recipe and survival policies belong to addons. */
public final class GregTechConfig {
    public static final String COMMON_FILE = "GT6CE/common.toml";
    public static final String CLIENT_FILE = "GT6CE/client.toml";

    /** Keep existing settings when upgrading from the old root-level config files. */
    public static void prepareDirectory(java.nio.file.Path configRoot) {
        try {
            java.nio.file.Files.createDirectories(configRoot.resolve("GT6CE"));
            migrateFile(configRoot, "gt6ce-common.toml", COMMON_FILE);
            migrateFile(configRoot, "gt6ce-client.toml", CLIENT_FILE);
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException("Cannot prepare GT6CE configuration directory", failure);
        }
    }

    private static void migrateFile(java.nio.file.Path root, String oldName, String newName)
            throws java.io.IOException {
        var source = root.resolve(oldName);
        var target = root.resolve(newName);
        if (java.nio.file.Files.isRegularFile(source) && !java.nio.file.Files.exists(target)) {
            java.nio.file.Files.copy(source, target);
        }
    }

//? if >=1.21.1 {
    /*private static final ModConfigSpec.Builder COMMON = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();
*///?} else {
    private static final ForgeConfigSpec.Builder COMMON = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.Builder CLIENT = new ForgeConfigSpec.Builder();
//?}

    public enum Worldgen {
        NEXUS("nexus", false, "Generate the Nexus complex, roads, central terrain, beacon and test area. Disabled: use vanilla spawn placement."),
        GEOLOGY("geology", true, "Generate geological layers, their ores, regular veins, small ores and ocean mineral pillars. Bedrock ores are separate."),
        BEDROCK_ORES("bedrock_ores", true, "Generate bedrock ores and indicators, including veins placed by dungeons and coltan fields."),
        DEPOSITS("deposits", true, "Generate deposits and pits, including clay, peat and magnetite sand."),
        WATER_BODIES("water_bodies", true, "Generate GT ocean, river and swamp water bodies."),
        FLUID_SPRINGS("fluid_springs", true, "Generate natural fluid springs."),
        VEGETATION("vegetation", true, "Generate natural trees, bushes, surface plants and twigs. Does not affect planted saplings."),
        HIVES("hives", true, "Generate natural bee hives."),
        SURFACE_ROCKS("surface_rocks", true, "Generate surface rocks and Nether litter. Ore indicators follow their ore generation settings."),
        DUNGEONS("dungeons", true, "Generate GT large dungeons and new dungeon locate targets.");

        private final String key;
        private final boolean defaultValue;
        private final String description;

        Worldgen(String key, boolean defaultValue, String description) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.description = description;
        }

        public boolean enabled() {
            return WORLDGEN.get(this).getAsBoolean();
        }
    }

    private static final Map<Worldgen, BooleanSupplier> WORLDGEN = worldgenSettings();
    // Retain the existing keys so upgrading does not reset a user's overload policy.
    private static final BooleanSupplier OVERLOAD_EXPLOSIONS = option(COMMON,
            "machines.explode_by_overload", false, "Explode and destroy machines on energy overload. Does not control boiler overpressure or other failures.");
    private static final BooleanSupplier OVERLOAD_BREAKING = option(COMMON,
            "machines.break_by_overload", false, "Destroy overloaded machines without an area explosion. Overload explosions always imply destruction.");
    private static final BooleanSupplier CHEMICAL_FORMULAS = option(CLIENT,
            "tooltips.chemical_formulas", true, "Show chemical formulas in item and fluid tooltips.");
    private static final BooleanSupplier MATERIAL_DETAILS = option(CLIENT,
            "tooltips.material_details", true, "Show material amounts and properties with F3+H. Disabling also hides the corresponding hint.");

//? if >=1.21.1 {
    /*public static final ModConfigSpec SPEC = COMMON.build();
    public static final ModConfigSpec CLIENT_SPEC = CLIENT.build();
*///?} else {
    public static final ForgeConfigSpec SPEC = COMMON.build();
    public static final ForgeConfigSpec CLIENT_SPEC = CLIENT.build();
//?}

    private static Map<Worldgen, BooleanSupplier> worldgenSettings() {
        var settings = new EnumMap<Worldgen, BooleanSupplier>(Worldgen.class);
        COMMON.comment("World generation: restart after changes. Affects new chunks only; existing blocks and structures remain.")
                .push("worldgen");
        for (Worldgen feature : Worldgen.values()) {
            COMMON.worldRestart();
            settings.put(feature, option(COMMON, feature.key, feature.defaultValue, feature.description));
        }
        COMMON.pop();
        return settings;
    }

//? if >=1.21.1 {
    /*private static BooleanSupplier option(ModConfigSpec.Builder builder, String key,
*///?} else {
    private static BooleanSupplier option(ForgeConfigSpec.Builder builder, String key,
//?}
                                          boolean defaultValue, String description) {
        if (builder == CLIENT) builder.translation("gt6ce.configuration." + key);
        var value = builder.comment(description).define(key, defaultValue);
        return () -> {
            // Configs are not yet loaded during registry construction and some tooling tasks.
            try {
                return value.get();
            } catch (IllegalStateException notLoaded) {
                return defaultValue;
            }
        };
    }

    public static boolean explodeByOverload() {
        return OVERLOAD_EXPLOSIONS.getAsBoolean();
    }

    public static boolean breakByOverload() {
        return explodeByOverload() || OVERLOAD_BREAKING.getAsBoolean();
    }

    public static boolean chemicalFormulas() {
        return CHEMICAL_FORMULAS.getAsBoolean();
    }

    public static boolean materialDetails() {
        return MATERIAL_DETAILS.getAsBoolean();
    }

    private GregTechConfig() {}
}
