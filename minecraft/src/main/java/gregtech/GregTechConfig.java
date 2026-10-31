package gregtech;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.ModConfigSpec;
*///?} else {
import net.minecraftforge.common.ForgeConfigSpec;
//?}

/** Loader configuration for the selected original GT6 policies. */
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
        NEXUS("nexus", false, "Generate the Nexus."),
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
//? if >=1.21.1 {
    /*private static final BooleanSupplier EASY_U235_EXTRACTION = option(COMMON.gameRestart(),
*///?} else {
    private static final BooleanSupplier EASY_U235_EXTRACTION = option(COMMON.worldRestart(),
//?}
            "general.EasyU235Extraction", false,
            "Original GT6: enable only the two refined Uranium-238 centrifuge recipes. Disabled by default. Requires a full game/server process restart; /reload or reopening a world in the same process does not apply changes.");
    private static final BooleanSupplier CHEMICAL_FORMULAS = option(CLIENT,
            "tooltips.chemical_formulas", true, "Show chemical formulas in item and fluid tooltips.");
    private static final BooleanSupplier MATERIAL_DETAILS = option(CLIENT,
            "tooltips.material_details", true, "Show material amounts and properties with F3+H. Disabling also hides the corresponding hint.");
    private static final BooleanSupplier FLUID_MILLIBUCKETS = option(CLIENT,
            "display.fluid_in_mB", true,
            "True: display fluids in mB/B/kB/MB/GB/TB/PB/EB. False: use L/KL/ML/GL/TL/PL/EL. Display only; stored amounts do not change.");

    public enum Rule {
        STREETS("worldgen.center.streets", false),
        CENTER_BIOMES("worldgen.center.biomes", false),
        BEACON("worldgen.center.beacon", false),
        TESTING("worldgen.center.testing", false),
        STICKS("worldgen.surface.sticks.Enabled", true),
        LOG_DRY("worldgen.surface.log_dry.Enabled", true),
        LOG_ROTTEN("worldgen.surface.log_rotten.Enabled", true),
        LOG_MOSSY("worldgen.surface.log_mossy.Enabled", true),
        LOG_FROZEN("worldgen.surface.log_frozen.Enabled", true),
        BUSHES("worldgen.surface.bushes.Enabled", true),
        TREES("worldgen.surface.trees.Enabled", true),
        GLOWTUS("worldgen.surface.glowtus.Enabled", true),
        WORKSHOP("worldgen.dungeon.Room.Workshop", true),
        MINING_BEDROCK("worldgen.dungeon.Room.Mining_Bedrock", true),
        LIBRARY("worldgen.dungeon.Room.Library_Normal", true),
        FARM_MOBS("worldgen.dungeon.Room.Farming_Mobs", true),
        FARM_CROP("worldgen.dungeon.Room.Farming_Crop", true),
        FARM_FISH("worldgen.dungeon.Room.Farming_Fish", true),
        PORTAL_NETHER("worldgen.dungeon.PortalNether", true),
        PORTAL_END("worldgen.dungeon.PortalEnd", false),
        ZPMS("worldgen.dungeon.ZPMs", true),
        FIRE_EXPLOSIONS("machines.explode_by_fire", true),
        FIRE_BREAKING("machines.break_by_fire", true),
        RAIN_EXPLOSIONS("machines.explode_by_rain", true),
        RAIN_BREAKING("machines.break_by_rain", true),
        WATER_EXPLOSIONS("machines.explode_by_water", true),
        WATER_BREAKING("machines.break_by_water", true),
        THUNDER_EXPLOSIONS("machines.explode_by_thunder", true),
        THUNDER_BREAKING("machines.break_by_thunder", true),
        CONSTANT_ENERGY("general.UninterruptedEnergyRequirement", true),
        OBSTRUCTION_CHECKS("general.ObstructionChecks", true),
        EU_AS_RF("general.Emit_EU_as_RF_from_Blocks", false),
        TANKS_REQUIRE_FLUID_TOOLS("general.TanksRequireFluidTools", true),
        AFK_HUNGER("general.AFK_Hunger", true),
        TOOL_BREAK_FATIGUE("general.ToolBreakFatique", true),
        SMALLER_VANILLA_TOOL_DURABILITY("general.SmallerVanillaToolDurability", true),
        RANDOMIZE_VANILLA_FLINT_AND_STEEL("general.RandomizeVanillaFlintAndSteel", true),
        NUTRITION("general.NutritionSystem", true),
        OVERDOSE_DEATH("general.DeathByOverdosingCertainFoods", true),
        ALWAYS_DRINK("general.drinks_always_drinkable", false),
        DISABLE_VANILLA_ORES("general.DisableVanillaOres", true),
        DISABLE_VANILLA_DECORATIVE_STONE("worldgen.DisableVanillaDecorativeStone", true),
        TOOL_MERGING("recipes.allow_tool_merging", false),
        GENERATE_NETHER_GOLD_ORE("worldgen.GenerateNetherGoldOre", true),
        GENERATE_ANCIENT_DEBRIS("worldgen.GenerateAncientDebris", true),
        INCREASE_DUNGEON_LOOT("general.IncreaseDungeonLoot", true),
        REPLACE_VANILLA_RECIPES("recipes.recipereplacements.minecraft", true),
        VANILLA_STORAGE_BLOCK_RECIPES("recipes.enable_vanilla_storage_block_crafting", false),
        REPLACE_MOD_RECIPES("recipes.recipereplacements.other_mods", true),
        DISABLE_CHARCOAL("recipes.disabledrecipes.wood2charcoalsmelting", true),
        DISABLE_COAL_TORCHES("recipes.disabledrecipes.torchesFromCoal", false),
        DISABLE_ENCHANTMENT_TABLE("recipes.disabledrecipes.enchantmenttable", false),
        DISABLE_ENDER_CHEST("recipes.disabledrecipes.enderchest", false),
        INTERNAL_NAMES("visibility.InternalNames", false, true),
        HIDDEN_ITEMS("visibility.HiddenGTItems", false, true),
        HIDDEN_MATERIALS("visibility.HiddenGTMaterials", false, true),
        HIDDEN_PREFIXES("visibility.HiddenGTPrefixes", false, true),
        ORE_BLOCKS("visibility.OreBlocks", false, true),
        MICROBLOCKS("visibility.MicroBlocks", false, true),
        BUMBLEBEES("visibility.Bumblebees", false, true),
        FLUID_DISPLAY("visibility.HiddenGTFluidDisplay", false, true),
        TOOL_SOUNDS("general.sound_tools", true, true),
        GT_ALPHA("general.useGTAlphaBlending", true, true),
        MC_ALPHA("general.useMCAlphaBlending", true, true),
        COINS_WORLD_3D("general.use3DCoins", true, true),
        COINS_ITEM_3D("general.use3DCoinsInv", true, true),
        CRATE_OVERLAY("blocks.crates.UseOverlayIcon", false, true),
        COVER_SNEAK("covers.require_sneaking", true);

        private final String key;
        private final boolean initial, client;

        Rule(String key, boolean initial) { this(key, initial, false); }

        Rule(String key, boolean initial, boolean client) {
            this.key = key;
            this.initial = initial;
            this.client = client;
        }

        public boolean enabled() { return RULES.get(this).getAsBoolean(); }
    }

    public enum Number {
        CENTER_HEIGHT("worldgen.center.Height", 66, -64, 300),
        ROCK_AMOUNT("worldgen.surface.rocks.Amount", 2, 0, 64),
        ROCK_PROBABILITY("worldgen.surface.rocks.Probability", 3, 1, 1000000),
        STICK_AMOUNT("worldgen.surface.sticks.Amount", 2, 0, 64),
        STICK_PROBABILITY("worldgen.surface.sticks.Probability", 2, 1, 1000000),
        BUSH_AMOUNT("worldgen.surface.bushes.Amount", 1, 0, 64),
        BUSH_PROBABILITY("worldgen.surface.bushes.Probability", 4, 1, 1000000),
        LOG_AMOUNT("worldgen.surface.logs.Amount", 1, 0, 16),
        LOG_PROBABILITY("worldgen.surface.logs.Probability", 8, 1, 1000000),
        TREE_AMOUNT("worldgen.surface.trees.Amount", 1, 0, 64),
        TREE_PROBABILITY("worldgen.surface.trees.ProbabilityMultiplier", 1, 1, 1000),
        GLOWTUS_AMOUNT("worldgen.surface.glowtus.Amount", 16, 0, 64),
        GLOWTUS_PROBABILITY("worldgen.surface.glowtus.Probability", 2, 1, 1000000),
        SPRING_PROBABILITY("worldgen.fluid_springs_probability", 100, 1, 1000000),
        OCEAN_HEIGHT("worldgen.water.ocean.Height", 63, -64, 319),
        RIVER_HEIGHT("worldgen.water.river.Height", 63, -64, 319),
        SWAMP_HEIGHT("worldgen.water.swamp.Height", 63, -64, 319),
        DUNGEON_PROBABILITY("worldgen.dungeon.Probability", 100, 1, 1000000),
        DUNGEON_MIN_SIZE("worldgen.dungeon.MinSize", 3, 3, 16),
        DUNGEON_MAX_SIZE("worldgen.dungeon.MaxSize", 7, 3, 16),
        DUNGEON_MIN_Y("worldgen.dungeon.MinY", 20, -59, 280),
        DUNGEON_MAX_Y("worldgen.dungeon.MaxY", 20, -59, 280),
        DUNGEON_ROOM_CHANCE("worldgen.dungeon.RoomChance", 6, 1, 1000);

        private final String key;
        private final int initial, min, max;

        Number(String key, int initial, int min, int max) {
            this.key = key;
            this.initial = initial;
            this.min = min;
            this.max = max;
        }

        public int value() { return NUMBERS.get(this).getAsInt(); }
    }

    private static final Map<Rule, BooleanSupplier> RULES = ruleSettings();
    private static final Map<Number, java.util.function.IntSupplier> NUMBERS = numberSettings();

    private static Map<Rule, BooleanSupplier> ruleSettings() {
        var settings = new EnumMap<Rule, BooleanSupplier>(Rule.class);
        for (Rule rule : Rule.values()) {
            var builder = rule.client ? CLIENT : COMMON;
            if (rule.client || rule.key.startsWith("worldgen.") || rule.key.startsWith("recipes.")) builder.worldRestart();
            String description = switch (rule) {
                case SMALLER_VANILLA_TOOL_DURABILITY -> "Original GT6: wood/stone/gold/iron/diamond swords, pickaxes, shovels, axes and hoes use 8/16/32/128/512 durability. Netherite and custom stack durability are unchanged. Does not rewrite existing damage.";
                case RANDOMIZE_VANILLA_FLINT_AND_STEEL -> "Use original GT6's 30 percent success chance for vanilla flint and steel on blocks. Failed attempts cost one durability. Creative players are exempt. Disable to restore vanilla ignition behavior.";
                case DISABLE_VANILLA_ORES -> "Disable vanilla ore generation. Nether gold ore and ancient debris have independent settings.";
                case DISABLE_VANILLA_DECORATIVE_STONE -> "Disable vanilla granite, diorite and andesite ore features in newly generated chunks.";
                case TOOL_MERGING -> "Allow two identical GT tools with matching head and handle materials to be crafted together to repair durability, with the vanilla 5 percent bonus.";
                case GENERATE_NETHER_GOLD_ORE -> "Generate vanilla Nether gold ore, independently of DisableVanillaOres.";
                case GENERATE_ANCIENT_DEBRIS -> "Generate vanilla ancient debris, independently of DisableVanillaOres.";
                case VANILLA_STORAGE_BLOCK_RECIPES -> "Enable vanilla crafting recipes for compressing materials into storage blocks.";
                case COVER_SNEAK -> "Require sneaking to place or preview covers. Disable to attach and preview covers with a plain right-click, as in original GT6.";
                case TANKS_REQUIRE_FLUID_TOOLS -> "Require taps, funnels or other fluid-transfer tools to fill or empty GT tanks by hand. Disable to allow direct right-click transfer with buckets and other fluid containers. Does not affect pipes or automation.";
                default -> rule.key.substring(rule.key.lastIndexOf('.') + 1);
            };
            settings.put(rule, option(builder, rule.key, rule.initial, description));
        }
        return settings;
    }

    private static Map<Number, java.util.function.IntSupplier> numberSettings() {
        var settings = new EnumMap<Number, java.util.function.IntSupplier>(Number.class);
        for (Number number : Number.values()) {
            var value = COMMON.worldRestart().comment(number.key.substring(number.key.lastIndexOf('.') + 1),
                    number.name().contains("PROBABILITY") ? "One attempt in N. Spring/log/tree values scale the original relative probabilities."
                    : number == Number.DUNGEON_MIN_Y || number == Number.DUNGEON_MAX_Y ? "GT6 Y coordinate; remapped to modern world depth."
                    : "Restart required; affects newly generated chunks.")
                    .defineInRange(number.key, number.initial, number.min, number.max);
            settings.put(number, () -> {
                try { return value.get(); } catch (IllegalStateException notLoaded) { return number.initial; }
            });
        }
        return settings;
    }

    private static final java.util.function.Supplier<java.util.List<? extends String>> DISABLED_LOOT =
            strings("worldgen.loot.disabled", "Disabled loot: category|item spec, or category|*.");
    private static final java.util.function.Supplier<java.util.List<? extends String>> DISABLED_RECIPES =
            strings("recipes.disabled", "Disabled recipes: namespace:path.");
    private static final java.util.function.Supplier<java.util.List<? extends String>> DISABLED_TOOLS =
            strings("recipes.toolrecipes.disabled_materials", "Disabled tool recipe materials.");
    private static final java.util.function.Supplier<java.util.List<? extends String>> DISABLED_HEADS =
            strings("recipes.toolheadrecipe.disabled_materials", "Disabled tool head recipe materials.");

    private static java.util.function.Supplier<java.util.List<? extends String>> strings(String key, String comment) {
        var value = COMMON.worldRestart().comment(comment, "Restart required.").defineListAllowEmpty(java.util.List.of(key.split("\\.")), java.util.List.<String>of(), entry -> entry instanceof String);
        return () -> { try { return value.get(); } catch (IllegalStateException notLoaded) { return java.util.List.of(); } };
    }

    public static boolean fallenLogEnabled(String id) {
        if (id.contains("dry")) return Rule.LOG_DRY.enabled();
        if (id.contains("rotten")) return Rule.LOG_ROTTEN.enabled();
        if (id.contains("mossy")) return Rule.LOG_MOSSY.enabled();
        if (id.contains("frozen")) return Rule.LOG_FROZEN.enabled();
        return true;
    }

    public static boolean lootEnabled(String category, String spec) {
        var disabled = DISABLED_LOOT.get();
        return !disabled.contains(category + "|*") && !disabled.contains(category + "|" + spec);
    }

    public static boolean recipeEnabled(String id) { return !DISABLED_RECIPES.get().contains(id); }

    public static boolean toolRecipeEnabled(String material, boolean head) {
        return (head ? DISABLED_HEADS : DISABLED_TOOLS).get().stream().noneMatch(name -> name.trim().equalsIgnoreCase(material));
    }

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

    /** Loader-exposed common-config value, read by the one-time machine-recipe bootstrap.
     * This accessor is not a live recipe toggle; existing startup rows survive datapack reloads.
     */
    public static boolean easyU235Extraction() {
        return EASY_U235_EXTRACTION.getAsBoolean();
    }

    public static boolean chemicalFormulas() {
        return CHEMICAL_FORMULAS.getAsBoolean();
    }

    public static boolean materialDetails() {
        return MATERIAL_DETAILS.getAsBoolean();
    }

    public static boolean fluidInMillibuckets() {
        return FLUID_MILLIBUCKETS.getAsBoolean();
    }

    private GregTechConfig() {}
}
