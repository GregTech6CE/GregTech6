package com.gregtech.gregtech.data;


import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialProperty;



import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Fluid name registry from GT6 FL.java. */
public class FluidCatalog {
    protected FluidCatalog() {}

    // =======================================================================
    // Fluid category flags — matching original GT6 CS.FluidsGT sets
    // =======================================================================

    public static final class FluidFlags {
        private FluidFlags() {}

        public static final long LIQUID            = 1L <<  0;
        public static final long GAS_FLAG          = 1L <<  1;
        public static final long PLASMA            = 1L <<  2;
        public static final long SIMPLE            = 1L <<  3;
        public static final long HIDDEN            = 1L <<  4;
        public static final long ENCHANTED_EFFECT  = 1L <<  5;
        public static final long MAGIC             = 1L <<  6;
        public static final long VOID_OVERFLOW     = 1L <<  7;
        public static final long AIR               = 1L <<  8;
        public static final long OXYGEN            = 1L <<  9;
        public static final long LIQUID_OXYGEN     = 1L << 10;
        public static final long STEAM             = 1L << 11;
        public static final long POWER_CONDUCTING  = 1L << 12;
        public static final long LUBRICANT         = 1L << 13;
        public static final long THERMOS           = 1L << 14;
        public static final long POTION            = 1L << 15;
        public static final long FOOD              = 1L << 16;
        public static final long JUICE             = 1L << 17;
        public static final long FRUIT_JUICE       = 1L << 18;
        public static final long CITRUS_JUICE      = 1L << 19;
        public static final long WATER             = 1L << 20;
        public static final long BATH              = 1L << 21;
        public static final long HONEY             = 1L << 22;
        public static final long MILK              = 1L << 23;
        public static final long TEA               = 1L << 24;
        public static final long COOKING_OIL       = 1L << 25;
        public static final long SLIME             = 1L << 26;
        public static final long ALCOHOLIC         = 1L << 27;
        public static final long VINEGAR           = 1L << 28;
        public static final long RUM               = 1L << 29;
        public static final long WINE              = 1L << 30;
        public static final long BEER              = 1L << 31;
        public static final long CIDER             = 1L << 32;
        public static final long SPIRIT            = 1L << 33;
        public static final long BRANDY            = 1L << 34;
        public static final long LIQUOR            = 1L << 35;
        public static final long LIQUEUR           = 1L << 36;
        public static final long WHISKEY           = 1L << 37;
        public static final long DYE               = 1L << 38;
        public static final long BROKEN            = 1L << 39;
        public static final long INFINITE          = 1L << 40;
        public static final long NONSTANDARD       = 1L << 41;
    }

    /** How a fluid is drawn in-world (crucibles, tanks, cells). */
    public enum FluidTextureMode {
        DEDICATED,
        GENERIC_MOLTEN,
        VANILLA_WATER,
        VANILLA_LAVA
    }

    /** GT6 default environment temperature in Kelvin. */
    public static final int DEF_ENV_TEMP = 300;

    public record FluidEntry(
            String registryName,
            FluidTextureMode textureMode,
            String materialKey,
            int tintArgb,
            boolean tintTexture,
            boolean glint,
            boolean gas,
            int density,
            int viscosity,
            int luminosity,
            int temperature,
            long flags
    ) {
        public FluidEntry(String registryName) {
            this(registryName, FluidTextureMode.DEDICATED, null, 0, true, false, false, 0, 0, 0, DEF_ENV_TEMP, FluidFlags.LIQUID | FluidFlags.SIMPLE);
        }

        public FluidEntry withGlint() {
            return new FluidEntry(registryName, textureMode, materialKey, tintArgb, tintTexture, true, gas, density, viscosity, luminosity, temperature, flags | FluidFlags.ENCHANTED_EFFECT);
        }

        public FluidEntry withGas() {
            return new FluidEntry(registryName, textureMode, materialKey, tintArgb, tintTexture, glint, true, density, viscosity, luminosity, temperature, (flags | FluidFlags.GAS_FLAG) & ~FluidFlags.LIQUID);
        }

        public FluidEntry withDensity(int v) {
            return new FluidEntry(registryName, textureMode, materialKey, tintArgb, tintTexture, glint, gas, v, viscosity, luminosity, temperature, flags);
        }

        public FluidEntry withViscosity(int v) {
            return new FluidEntry(registryName, textureMode, materialKey, tintArgb, tintTexture, glint, gas, density, v, luminosity, temperature, flags);
        }

        public FluidEntry withLuminosity(int v) {
            return new FluidEntry(registryName, textureMode, materialKey, tintArgb, tintTexture, glint, gas, density, viscosity, v, temperature, flags);
        }

        public FluidEntry withTemperature(int v) {
            return new FluidEntry(registryName, textureMode, materialKey, tintArgb, tintTexture, glint, gas, density, viscosity, luminosity, v, flags);
        }

        public FluidEntry withFlags(long v) {
            return new FluidEntry(registryName, textureMode, materialKey, tintArgb, tintTexture, glint, gas, density, viscosity, luminosity, temperature, flags | v);
        }

        public boolean hasFlag(long f) { return (flags & f) != 0; }
        public boolean isHidden() { return hasFlag(FluidFlags.HIDDEN); }
    }

    private static final Map<String, FluidEntry> REGISTRY = new LinkedHashMap<>();
    /**
     * Generated key aliases: GT6 asks for "the material's gas/liquid" regardless of which of the two
     * states registered the fluid first, so {@code GenGas_X}/{@code GenLiquid_X} may both exist while
     * only one canonical field owns the fluid.
     */
    private static final Map<String, String> GENERATED_ALIASES = new LinkedHashMap<>();
    /** Registry name -> material name for fluids whose declaration carries no {@code materialKey}. */
    private static final Map<String, String> BOUND_MATERIALS = new LinkedHashMap<>();
    private static boolean definitionsClosed;
    private static boolean importedFieldsReconciled;

    /** Canonical field for a generated alias key ({@code GenGas_Hydrogen} -> {@code Hydrogen}). */
    public static String canonicalField(String field) {
        String target = GENERATED_ALIASES.get(field);
        return target == null ? field : target;
    }

    /** New content path. Legacy aliases remain accepted by the private import helper. */
    public static FluidEntry registerDefinition(String key, FluidEntry entry) {
        java.util.Objects.requireNonNull(entry, "Fluid definition");
        if (definitionsClosed) throw new IllegalStateException("Fluid definitions are already submitted to Forge");
        if (key == null || key.isBlank() || REGISTRY.containsKey(key))
            throw new IllegalArgumentException("Duplicate or empty fluid key: " + key);
        if (entry.registryName() == null || !entry.registryName().matches("[a-z0-9/._-]+")
                || entry.textureMode() == null || entry.temperature() < 0 || entry.viscosity() < 0
                || entry.luminosity() < 0 || entry.luminosity() > 15)
            throw new IllegalArgumentException("Invalid fluid definition: " + key);
        if (entry.gas() && entry.hasFlag(FluidFlags.LIQUID))
            throw new IllegalArgumentException("Fluid cannot be marked both liquid and gas: " + key);
        String path = sanitizePath(entry.registryName());
        for (FluidEntry existing : REGISTRY.values()) {
            String other = sanitizePath(existing.registryName());
            if (path.equals(other) || path.equals(other + "_flowing") || other.equals(path + "_flowing"))
                throw new IllegalArgumentException("Fluid source/flowing ID collision: " + path + " / " + other);
        }
        return register(key, entry);
    }

    public static void closeDefinitions() { definitionsClosed = true; }

    public static String sanitizeTextureId(String registryName) {
        return registryName.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    /** Converts a fluid registry name to a valid 1.20.1 ResourceLocation path segment. */
    public static String sanitizePath(String registryName) {
        return sanitizeTextureId(registryName).replace('-', '_').replace('.', '_');
    }



    private static FluidEntry register(String field, FluidEntry entry) {
        REGISTRY.put(field, entry);
        return entry;
    }

    private static FluidEntry fluid(String field, String registryName) {
        return register(field, new FluidEntry(registryName));
    }

    private static FluidEntry fluid(String field, String registryName, FluidTextureMode mode) {
        long fl = (mode == FluidTextureMode.VANILLA_LAVA ? FluidFlags.LIQUID | FluidFlags.SIMPLE : FluidFlags.LIQUID | FluidFlags.SIMPLE);
        return register(field, new FluidEntry(registryName, mode, null, 0,
                mode != FluidTextureMode.VANILLA_WATER && mode != FluidTextureMode.VANILLA_LAVA,
                false, false, 0, 0, 0, DEF_ENV_TEMP, fl));
    }

    private static FluidEntry fluidMolten(String field, String registryName, String materialKey) {
        return register(field, new FluidEntry(registryName, FluidTextureMode.GENERIC_MOLTEN, materialKey, 0, true, false, false, 0, 0, 0, DEF_ENV_TEMP, FluidFlags.LIQUID));
    }

    private static FluidEntry fluidTinted(String field, String registryName, int tintArgb) {
        return register(field, new FluidEntry(registryName, FluidTextureMode.DEDICATED, null, tintArgb, true, false, false, 0, 0, 0, DEF_ENV_TEMP, FluidFlags.LIQUID | FluidFlags.SIMPLE));
    }

    /** Original GT6 {@code FL.createMolten(Material, IIcon, FluidType...)} */
    @SuppressWarnings("unused")
    private static FluidEntry createMolten(GTMaterial material) {
        String name = material.getName();
        FluidEntry entry = register("GenMolten_" + name,
                new FluidEntry("molten." + name.toLowerCase(Locale.ROOT), FluidTextureMode.GENERIC_MOLTEN,
                        name, 0, true, false, false, 3000, 6000, 0,
                        (int) material.getMeltingPoint(), FluidFlags.LIQUID));
        // GT6's molten fluids know their material too (FL.createMolten stores it); without the binding a
        // material's tag would miss its own fluid container items.
        bindMaterial("GenMolten_" + name, material);
        return entry;
    }

    // =======================================================================
    // Dye colour name tables
    // =======================================================================

    public static final String[] DYE_OREDICTS_POST = {
        "White", "Orange", "Magenta", "LightBlue", "Yellow", "Lime", "Pink", "Gray",
        "LightGray", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black"
    };

    public static final String[] DYE_NAMES = {
        "White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink", "Gray",
        "Light Gray", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black"
    };

    // =======================================================================
    // Error / Special / Magical
    // =======================================================================

    public static final FluidEntry
            Error = fluid("Error", "error").withFlags(FluidFlags.HIDDEN),
            UUM = fluid("UUM", "ic2uumatter").withGlint(),
            MatterNeutral = fluid("MatterNeutral", "neutralmatter").withGlint(),
            MatterCharged = fluid("MatterCharged", "chargedmatter").withGlint(),
            UUAmplifier = fluid("UUAmplifier", "uuamplifier"),
            NitroFuel = fluid("NitroFuel", "nitrofuel"),
            XP = fluid("XP", "xpjuice").withGlint().withFlags(FluidFlags.MAGIC | FluidFlags.VOID_OVERFLOW),
            XP_Molten = fluid("XP_Molten", "xp").withGlint().withFlags(FluidFlags.MAGIC | FluidFlags.VOID_OVERFLOW),
            Mob = fluid("Mob", "mobessence").withFlags(FluidFlags.MAGIC | FluidFlags.VOID_OVERFLOW),
            Freezing_Ooze = fluid("Freezing_Ooze", "ooze").withFlags(FluidFlags.BROKEN),
            Mcguffium = fluid("Mcguffium", "mcguffium"),

    // =======================================================================
    // Plasmas
    // =======================================================================

            HeliumPlasma = fluid("HeliumPlasma", "heliumplasma").withLuminosity(15).withTemperature(10000)
                    .withFlags(FluidFlags.PLASMA | FluidFlags.HIDDEN),
            NitrogenPlasma = fluid("NitrogenPlasma", "nitrogenplasma").withLuminosity(15).withTemperature(10000)
                    .withFlags(FluidFlags.PLASMA | FluidFlags.HIDDEN),

    // =======================================================================
    // Elemental gases
    // =======================================================================

            Oxygen = fluid("Oxygen", "oxygen").withGas().withFlags(FluidFlags.OXYGEN),
            Reikygen = fluid("Reikygen", "rc oxygen").withGas().withFlags(FluidFlags.OXYGEN),
            Nitrogen = fluid("Nitrogen", "nitrogen").withGas(),
            Hydrogen = fluid("Hydrogen", "hydrogen").withGas(),
            Deuterium = fluid("Deuterium", "deuterium").withGas(),
            Tritium = fluid("Tritium", "tritium").withGas(),
            Helium = fluid("Helium", "helium").withGas(),
            Helium_3 = fluid("Helium_3", "helium-3").withGas(),
            Neon = fluid("Neon", "neon").withGas(),
            Argon = fluid("Argon", "argon").withGas(),
            CarbonDioxide = fluid("CarbonDioxide", "carbondioxide").withGas(),
            Methane = fluid("Methane", "methane").withGas(),
            Propane = fluid("Propane", "propane").withGas().withDensity(-1000),
            Butane = fluid("Butane", "butane").withGas().withDensity(-1000),
            Ethylene = fluid("Ethylene", "ethylene").withGas(),
            Propylene = fluid("Propylene", "propylene").withGas(),
            Gas_Natural = fluid("Gas_Natural", "gas_natural_gas").withGas().withDensity(-500),
            LPG = fluid("LPG", "lpg").withGas(),

    // =======================================================================
    // Air variants
    // =======================================================================

            Air = fluid("Air", "air").withGas().withFlags(FluidFlags.AIR),
            Air_End = fluid("Air_End", "enderair").withGas().withTemperature(280).withFlags(FluidFlags.AIR),
            Air_Nether = fluid("Air_Nether", "netherair").withGas().withTemperature(370).withFlags(FluidFlags.AIR),

    // =======================================================================
    // Steam / hot gas variants
    // =======================================================================

            Steam = fluid("Steam", "steam").withGas().withDensity(-1000).withTemperature(373)
                    .withFlags(FluidFlags.STEAM | FluidFlags.POWER_CONDUCTING),
            Steam_IC2 = fluid("Steam_IC2", "ic2steam").withGas()
                    .withFlags(FluidFlags.STEAM | FluidFlags.POWER_CONDUCTING),
            Steam_IC2_Superheated = fluid("Steam_IC2_Superheated", "ic2superheatedsteam").withGas()
                    .withFlags(FluidFlags.STEAM | FluidFlags.POWER_CONDUCTING),
            Hot_Helium = fluid("Hot_Helium", "hothelium").withGas().withTemperature(1150).withFlags(FluidFlags.POWER_CONDUCTING),
            Hot_Carbon_Dioxide = fluid("Hot_Carbon_Dioxide", "hotcarbondioxide").withGas().withTemperature(950).withFlags(FluidFlags.POWER_CONDUCTING),

    // =======================================================================
    // Liquefied gases
    // =======================================================================

            Liquid_Oxygen = fluid("Liquid_Oxygen", "liquidoxygen").withTemperature(85).withFlags(FluidFlags.LIQUID_OXYGEN),
            Liquid_Reikygen = fluid("Liquid_Reikygen", "rc liquid oxygen").withFlags(FluidFlags.LIQUID_OXYGEN),
            Liquid_Nitrogen = fluid("Liquid_Nitrogen", "liquidnitrogen"),
            Liquid_Methane = fluid("Liquid_Methane", "liquidmethane").withFlags(FluidFlags.THERMOS),

    // =======================================================================
    // Water
    // =======================================================================

            Water = fluid("Water", "water", FluidTextureMode.VANILLA_WATER).withFlags(FluidFlags.FOOD | FluidFlags.WATER | FluidFlags.BATH),
            DistW = fluid("DistW", "ic2distilledwater").withFlags(FluidFlags.FOOD | FluidFlags.WATER | FluidFlags.BATH),
            River_Water = fluid("River_Water", "riverwater").withFlags(FluidFlags.FOOD | FluidFlags.WATER | FluidFlags.BATH),
            SpDew = fluid("SpDew", "spectral_dew").withFlags(FluidFlags.FOOD | FluidFlags.WATER | FluidFlags.BATH | FluidFlags.INFINITE),
            Cold_Water = fluid("Cold_Water", "cold_water").withFlags(FluidFlags.FOOD | FluidFlags.WATER | FluidFlags.BATH | FluidFlags.THERMOS),
            Hot_Water = fluid("Hot_Water", "hot_water").withFlags(FluidFlags.FOOD | FluidFlags.WATER | FluidFlags.BATH | FluidFlags.THERMOS),
            Water_Hot = fluid("Water_Hot", "ic2hotwater").withFlags(FluidFlags.FOOD | FluidFlags.WATER | FluidFlags.BATH | FluidFlags.THERMOS),
            Water_Boiling = fluid("Water_Boiling", "boilingwater").withFlags(FluidFlags.FOOD | FluidFlags.WATER | FluidFlags.BATH | FluidFlags.THERMOS),
            Water_Geothermal = fluid("Water_Geothermal", "watergeothermal").withFlags(FluidFlags.FOOD | FluidFlags.WATER | FluidFlags.BATH | FluidFlags.THERMOS),
            MnWtr = fluid("MnWtr", "potion.mineralwater").withFlags(FluidFlags.FOOD | FluidFlags.WATER),
            Ice = fluid("Ice", "ice").withFlags(FluidFlags.FOOD | FluidFlags.WATER | FluidFlags.BATH | FluidFlags.THERMOS),
            Heavy_Reiker = fluid("Heavy_Reiker", "rc_heavy_water"),
            Mineralsoda = fluid("Mineralsoda", "mineralsoda").withFlags(FluidFlags.FOOD),
            Soda = fluid("Soda", "soda").withFlags(FluidFlags.FOOD),
            Tropics_Water = fluid("Tropics_Water", "tropicswater"),

    // =======================================================================
    // Ocean / salt / waste
    // =======================================================================

            Ocean = fluid("Ocean", "seawater"),
            OceanGrC = fluid("OceanGrC", "grccore.saltwater"),
            Dirty_Water = fluid("Dirty_Water", "waterdirty"),
            Stagnant_Water = fluid("Stagnant_Water", "stagnantwater"),
            Swampwater = fluid("Swampwater", "swampwater"),
            Saltwater = fluid("Saltwater", "saltwater"),
            Brine = fluid("Brine", "brine"),
            Holywater = fluid("Holywater", "holywater").withGlint().withFlags(FluidFlags.MAGIC),

    // =======================================================================
    // Milk / Honey / Sap / Cream
    // =======================================================================

            Milk = fluid("Milk", "milk").withFlags(FluidFlags.FOOD | FluidFlags.MILK),
            MilkSoy = fluid("MilkSoy", "soymilk").withFlags(FluidFlags.FOOD | FluidFlags.MILK),
            MilkGrC = fluid("MilkGrC", "grcmilk.milk").withFlags(FluidFlags.FOOD | FluidFlags.MILK | FluidFlags.NONSTANDARD),
            Milk_Spoiled = fluid("Milk_Spoiled", "spoiledmilk").withFlags(FluidFlags.FOOD | FluidFlags.MILK),
            Honey = fluid("Honey", "for.honey").withFlags(FluidFlags.FOOD | FluidFlags.HONEY),
            HoneyGrC = fluid("HoneyGrC", "grc.honey").withFlags(FluidFlags.FOOD | FluidFlags.HONEY | FluidFlags.NONSTANDARD),
            HoneyBoP = fluid("HoneyBoP", "honey").withFlags(FluidFlags.FOOD | FluidFlags.HONEY | FluidFlags.NONSTANDARD),
            Honeydew = fluid("Honeydew", "honeydew").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC),
            Ambrosia = fluid("Ambrosia", "potion.ambrosia").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC),
            RoyalJelly = fluid("RoyalJelly", "royaljelly").withFlags(FluidFlags.FOOD),
            Sap = fluid("Sap", "sap").withFlags(FluidFlags.FOOD),
            Sap_Rainbow = fluid("Sap_Rainbow", "rainbowsap").withFlags(FluidFlags.FOOD | FluidFlags.MAGIC),
            Sap_Maple = fluid("Sap_Maple", "maplesap").withFlags(FluidFlags.FOOD),
            Syrup_Maple = fluid("Syrup_Maple", "maplesyrup").withFlags(FluidFlags.FOOD),
            Cream = fluid("Cream", "grcmilk.cream").withFlags(FluidFlags.FOOD),
            // GT6 Loader_Recipes_Food's coffee chain (FL.make_("potion.coffee", …) and friends):
            // the recipes themselves are already transpiled, this registers the fluids they name.
            Coffee = fluid("Coffee", "potion.coffee").withFlags(FluidFlags.FOOD),
            Coffee_Dark = fluid("Coffee_Dark", "potion.darkcoffee").withFlags(FluidFlags.FOOD),
            CafeAuLait = fluid("CafeAuLait", "potion.cafeaulait").withFlags(FluidFlags.FOOD),
            CafeAuLait_Dark = fluid("CafeAuLait_Dark", "potion.darkcafeaulait").withFlags(FluidFlags.FOOD),
            LaitAuCafe = fluid("LaitAuCafe", "potion.laitaucafe").withFlags(FluidFlags.FOOD),
            ChocolateMilk = fluid("ChocolateMilk", "potion.chocolatemilk").withFlags(FluidFlags.FOOD),
            ChocolateMilk_Dark = fluid("ChocolateMilk_Dark", "potion.darkchocolatemilk").withFlags(FluidFlags.FOOD),
            Cream_Chocolate = fluid("Cream_Chocolate", "chocolatecream").withFlags(FluidFlags.FOOD),
            Cream_Coconut = fluid("Cream_Coconut", "coconutcream").withFlags(FluidFlags.FOOD),
            Cream_Nutella = fluid("Cream_Nutella", "nutella").withFlags(FluidFlags.FOOD),
            Nutbutter_Peanut = fluid("Nutbutter_Peanut", "peanutbutter").withFlags(FluidFlags.FOOD),

    // =======================================================================
    // Juices
    // =======================================================================

            Juice = fluid("Juice", "juice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE),
            Juice_Kiwi = fluid("Juice_Kiwi", "kiwijuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE | FluidFlags.CITRUS_JUICE),
            Juice_Lime = fluid("Juice_Lime", "binnie.juicelime").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE | FluidFlags.CITRUS_JUICE),
            Juice_Lemon = fluid("Juice_Lemon", "binnie.juicelemon").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE | FluidFlags.CITRUS_JUICE),
            Juice_Orange = fluid("Juice_Orange", "binnie.juiceorange").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE | FluidFlags.CITRUS_JUICE),
            Juice_Persimmon = fluid("Juice_Persimmon", "persimmonjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE | FluidFlags.CITRUS_JUICE),
            Juice_Melon = fluid("Juice_Melon", "melonjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Currant = fluid("Juice_Currant", "currantjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Raspberry = fluid("Juice_Raspberry", "raspberryjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Blackberry = fluid("Juice_Blackberry", "blackberryjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Blueberry = fluid("Juice_Blueberry", "blueberryjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Gooseberry = fluid("Juice_Gooseberry", "gooseberryjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Strawberry = fluid("Juice_Strawberry", "strawberryjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Plum = fluid("Juice_Plum", "binnie.juiceplum").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Peach = fluid("Juice_Peach", "binnie.juicepeach").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Elderberry = fluid("Juice_Elderberry", "binnie.juiceelderberry").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Hellderberry = fluid("Juice_Hellderberry", "hellderberryjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Grapefruit = fluid("Juice_Grapefruit", "binnie.juicegrapefruit").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Apricot = fluid("Juice_Apricot", "binnie.juiceapricot").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Pear = fluid("Juice_Pear", "binnie.juicepear").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Grape_Green = fluid("Juice_Grape_Green", "grapejuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Grape_Purple = fluid("Juice_Grape_Purple", "grc.grapewine0").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Grape_Red = fluid("Juice_Grape_Red", "binnie.juiceredgrape").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Grape_White = fluid("Juice_Grape_White", "binnie.juicewhitegrape").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Apple = fluid("Juice_Apple", "binnie.juiceapple").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_AppleGrC = fluid("Juice_AppleGrC", "grc.applecider0").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE | FluidFlags.NONSTANDARD),
            Juice_Ananas = fluid("Juice_Ananas", "binnie.juicepineapple").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Banana = fluid("Juice_Banana", "binnie.juicebanana").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Cherry = fluid("Juice_Cherry", "binnie.juicecherry").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Cranberry = fluid("Juice_Cranberry", "binnie.juicecranberry").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_CactusFruit = fluid("Juice_CactusFruit", "cactusfruitjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Mango = fluid("Juice_Mango", "mangojuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Pomegranate = fluid("Juice_Pomegranate", "pomegranatejuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Starfruit = fluid("Juice_Starfruit", "starfruitjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Papaya = fluid("Juice_Papaya", "papayajuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Fig = fluid("Juice_Fig", "figjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Coconut = fluid("Juice_Coconut", "coconutmilk").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE | FluidFlags.COOKING_OIL),
            Juice_Date = fluid("Juice_Date", "datejuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE | FluidFlags.FRUIT_JUICE),
            Juice_Carrot = fluid("Juice_Carrot", "binnie.juicecarrot").withFlags(FluidFlags.FOOD | FluidFlags.JUICE),
            Juice_Tomato = fluid("Juice_Tomato", "binnie.juicetomato").withFlags(FluidFlags.FOOD | FluidFlags.JUICE),
            Juice_Beet = fluid("Juice_Beet", "beetjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE),
            Juice_Pumpkin = fluid("Juice_Pumpkin", "pumpkinjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE),
            Juice_Cucumber = fluid("Juice_Cucumber", "cucumberjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE),
            Juice_Onion = fluid("Juice_Onion", "onionjuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE),
            Juice_Potato = fluid("Juice_Potato", "potatojuice").withFlags(FluidFlags.FOOD | FluidFlags.JUICE),
            Juice_Reed = fluid("Juice_Reed", "reedwater").withFlags(FluidFlags.FOOD | FluidFlags.JUICE),
            Juice_Cactus = fluid("Juice_Cactus", "cactuswater").withFlags(FluidFlags.FOOD | FluidFlags.JUICE),

    // =======================================================================
    // Smoothies
    // =======================================================================

            Smoothie_Fruit = fluid("Smoothie_Fruit", "fruitsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Melon = fluid("Smoothie_Melon", "melonsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Kiwi = fluid("Smoothie_Kiwi", "kiwismoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Currant = fluid("Smoothie_Currant", "currantsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Raspberry = fluid("Smoothie_Raspberry", "raspberrysmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Blackberry = fluid("Smoothie_Blackberry", "blackberrysmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Blueberry = fluid("Smoothie_Blueberry", "blueberrysmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Gooseberry = fluid("Smoothie_Gooseberry", "gooseberrysmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Strawberry = fluid("Smoothie_Strawberry", "strawberrysmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Plum = fluid("Smoothie_Plum", "plumsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Peach = fluid("Smoothie_Peach", "peachsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Elderberry = fluid("Smoothie_Elderberry", "elderberrysmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Grapefruit = fluid("Smoothie_Grapefruit", "grapefruitsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Lime = fluid("Smoothie_Lime", "limesmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Orange = fluid("Smoothie_Orange", "orangesmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Persimmon = fluid("Smoothie_Persimmon", "persimmonsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Apricot = fluid("Smoothie_Apricot", "apricotsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Pear = fluid("Smoothie_Pear", "pearsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Grape_Red = fluid("Smoothie_Grape_Red", "redgrapesmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Grape_White = fluid("Smoothie_Grape_White", "whitegrapesmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Grape_Green = fluid("Smoothie_Grape_Green", "grapesmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Grape_Purple = fluid("Smoothie_Grape_Purple", "purplegrapesmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Apple = fluid("Smoothie_Apple", "applesmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Ananas = fluid("Smoothie_Ananas", "pineapplesmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Banana = fluid("Smoothie_Banana", "bananasmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Cherry = fluid("Smoothie_Cherry", "cherrysmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Cranberry = fluid("Smoothie_Cranberry", "cranberrysmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Lemon = fluid("Smoothie_Lemon", "lemonsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Mango = fluid("Smoothie_Mango", "mangosmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Pomegranate = fluid("Smoothie_Pomegranate", "pomegranatesmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Starfruit = fluid("Smoothie_Starfruit", "starfruitsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Papaya = fluid("Smoothie_Papaya", "papayasmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Fig = fluid("Smoothie_Fig", "figsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),
            Smoothie_Coconut = fluid("Smoothie_Coconut", "coconutsmoothie").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS),

    // =======================================================================
    // Mashes
    // =======================================================================

            Mash_Rice = fluid("Mash_Rice", "ricewater").withFlags(FluidFlags.FOOD),
            Mash_Hops = fluid("Mash_Hops", "hopsmash").withFlags(FluidFlags.FOOD),
            Mash_WheatHops = fluid("Mash_WheatHops", "wheathopsmash").withFlags(FluidFlags.FOOD),
            Mash_Wheat = fluid("Mash_Wheat", "binnie.mashwheat").withFlags(FluidFlags.FOOD),
            Mash_Corn = fluid("Mash_Corn", "binnie.mashcorn").withFlags(FluidFlags.FOOD),
            Mash_Rye = fluid("Mash_Rye", "binnie.mashrye").withFlags(FluidFlags.FOOD),
            Mash_Grain = fluid("Mash_Grain", "binnie.mashgrain").withFlags(FluidFlags.FOOD),

    // =======================================================================
    // Teas / Soft drinks / Sauces / Condiments
    // =======================================================================

            Tea = fluid("Tea", "tea").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS | FluidFlags.TEA),
            Tea_Sweet = fluid("Tea_Sweet", "sweettea").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS | FluidFlags.TEA),
            Tea_Ice = fluid("Tea_Ice", "icetea").withFlags(FluidFlags.FOOD | FluidFlags.THERMOS | FluidFlags.TEA),
            Purple_Drink = fluid("Purple_Drink", "purpledrink").withFlags(FluidFlags.FOOD),
            Lemonade = fluid("Lemonade", "potion.lemonade").withFlags(FluidFlags.FOOD),
            Grenade_Juice = fluid("Grenade_Juice", "potion.cavejohnsonsgrenadejuice").withFlags(FluidFlags.FOOD),
            BAWLS = fluid("BAWLS", "bawls").withFlags(FluidFlags.FOOD),
            Ketchup = fluid("Ketchup", "ketchup").withFlags(FluidFlags.FOOD),
            Mayo = fluid("Mayo", "mayo").withFlags(FluidFlags.FOOD),
            Dressing = fluid("Dressing", "potion.dressing").withFlags(FluidFlags.FOOD),
            Soup_Mushroom = fluid("Soup_Mushroom", "mushroomsoup").withFlags(FluidFlags.FOOD),
            Sauce_Chili = fluid("Sauce_Chili", "chillysauce").withFlags(FluidFlags.FOOD),
            Sauce_Hot = fluid("Sauce_Hot", "potion.hotsauce").withFlags(FluidFlags.FOOD),
            Sauce_Diabolo = fluid("Sauce_Diabolo", "potion.diabolosauce").withFlags(FluidFlags.FOOD),
            Sauce_Diablo = fluid("Sauce_Diablo", "potion.diablosauce").withFlags(FluidFlags.FOOD),
            Sauce_Cow_Level = fluid("Sauce_Cow_Level", "potion.diablosauce.strong").withFlags(FluidFlags.FOOD),
            Sauce_BBQ = fluid("Sauce_BBQ", "bbqsauce").withFlags(FluidFlags.FOOD),
            Vinegar_Grape = fluid("Vinegar_Grape", "vinegar").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.VINEGAR),
            Vinegar_Apple = fluid("Vinegar_Apple", "applevinegar").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.VINEGAR),
            Vinegar_Cane = fluid("Vinegar_Cane", "canevinegar").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.VINEGAR),
            Vinegar_Rice = fluid("Vinegar_Rice", "ricevinegar").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.VINEGAR),

    // =======================================================================
    // Wines
    // =======================================================================

            Wine_Fruit = fluid("Wine_Fruit", "binnie.juice").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Lemon = fluid("Wine_Lemon", "limoncello").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Agave = fluid("Wine_Agave", "binnie.wineagave").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Apricot = fluid("Wine_Apricot", "binnie.wineapricot").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Banana = fluid("Wine_Banana", "binnie.winebanana").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Carrot = fluid("Wine_Carrot", "binnie.winecarrot").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Cherry = fluid("Wine_Cherry", "binnie.winecherry").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Citrus = fluid("Wine_Citrus", "binnie.winecitrus").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Cranberry = fluid("Wine_Cranberry", "binnie.winecranberry").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Elderberry = fluid("Wine_Elderberry", "binnie.wineelderberry").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Plum = fluid("Wine_Plum", "binnie.wineplum").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Sparkling = fluid("Wine_Sparkling", "binnie.winesparkling").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Tomato = fluid("Wine_Tomato", "binnie.winetomato").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Grape_Green = fluid("Wine_Grape_Green", "wine").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Grape_Purple = fluid("Wine_Grape_Purple", "ricardosanchez").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Grape_Red = fluid("Wine_Grape_Red", "binnie.winered").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Grape_White = fluid("Wine_Grape_White", "binnie.winewhite").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),
            Wine_Fortified = fluid("Wine_Fortified", "binnie.winefortified").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WINE),

    // =======================================================================
    // Ciders
    // =======================================================================

            Cider_Apple = fluid("Cider_Apple", "binnie.ciderapple").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.CIDER),
            Cider_Pear = fluid("Cider_Pear", "binnie.ciderpear").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.CIDER),
            Cider_Peach = fluid("Cider_Peach", "binnie.ciderpeach").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.CIDER),
            Cider_Ananas = fluid("Cider_Ananas", "binnie.winepineapple").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.CIDER),

    // =======================================================================
    // Beers / Ales / Stouts
    // =======================================================================

            Beer = fluid("Beer", "beer").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BEER),
            Beer_Dark = fluid("Beer_Dark", "darkbeer").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BEER),
            Beer_Dragonblood = fluid("Beer_Dragonblood", "potion.dragonblood").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BEER),
            Beer_Ale = fluid("Beer_Ale", "binnie.beerale").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BEER),
            Beer_Corn = fluid("Beer_Corn", "binnie.beercorn").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BEER),
            Beer_Lager = fluid("Beer_Lager", "binnie.beerlager").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BEER),
            Beer_Rye = fluid("Beer_Rye", "binnie.beerrye").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BEER),
            Beer_Stout = fluid("Beer_Stout", "binnie.beerstout").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BEER),
            Beer_Wheat = fluid("Beer_Wheat", "binnie.beerwheat").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BEER),

    // =======================================================================
    // Liquors / Liqueurs / Spirits / Brandies / Whiskeys
    // =======================================================================

            Liqueur_Chocolate = fluid("Liqueur_Chocolate", "binnie.liqueurchocolate").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Almond = fluid("Liqueur_Almond", "binnie.liqueuralmond").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Anise = fluid("Liqueur_Anise", "binnie.liqueuranise").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Banana = fluid("Liqueur_Banana", "binnie.liqueurbanana").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Blackberry = fluid("Liqueur_Blackberry", "binnie.liqueurblackberry").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Blackcurrant = fluid("Liqueur_Blackcurrant", "binnie.liqueurblackcurrant").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Cherry = fluid("Liqueur_Cherry", "binnie.liqueurcherry").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Cinnamon = fluid("Liqueur_Cinnamon", "binnie.liqueurcinnamon").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Coffee = fluid("Liqueur_Coffee", "binnie.liqueurcoffee").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Hazelnut = fluid("Liqueur_Hazelnut", "binnie.liqueurhazelnut").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Herbal = fluid("Liqueur_Herbal", "binnie.liqueurherbal").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Lemon = fluid("Liqueur_Lemon", "binnie.liqueurlemon").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Melon = fluid("Liqueur_Melon", "binnie.liqueurmelon").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Mint = fluid("Liqueur_Mint", "binnie.liqueurmint").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Orange = fluid("Liqueur_Orange", "binnie.liqueurorange").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Peach = fluid("Liqueur_Peach", "binnie.liqueurpeach").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liqueur_Raspberry = fluid("Liqueur_Raspberry", "binnie.liqueurraspberry").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUEUR),
            Liquor = fluid("Liquor", "binnie.liquorfruit").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUOR),
            Liquor_Apple = fluid("Liquor_Apple", "binnie.liquorapple").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUOR),
            Liquor_Apricot = fluid("Liquor_Apricot", "binnie.liquorapricot").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUOR),
            Liquor_Cherry = fluid("Liquor_Cherry", "binnie.liquorcherry").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUOR),
            Liquor_Elderberry = fluid("Liquor_Elderberry", "binnie.liquorelderberry").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUOR),
            Liquor_Pear = fluid("Liquor_Pear", "binnie.liquorpear").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.LIQUOR),
            Spirit_Gin = fluid("Spirit_Gin", "binnie.spiritgin").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.SPIRIT),
            Spirit_Cane = fluid("Spirit_Cane", "binnie.spiritneutral").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.SPIRIT),
            Spirit_Neutral = fluid("Spirit_Neutral", "binnie.spiritsugarcane").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.SPIRIT),
            Brandy = fluid("Brandy", "binnie.brandyfruit").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BRANDY),
            Brandy_Apple = fluid("Brandy_Apple", "binnie.brandyapple").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BRANDY),
            Brandy_Apricot = fluid("Brandy_Apricot", "binnie.brandyapricot").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BRANDY),
            Brandy_Cherry = fluid("Brandy_Cherry", "binnie.brandycherry").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BRANDY),
            Brandy_Citrus = fluid("Brandy_Citrus", "binnie.brandycitrus").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BRANDY),
            Brandy_Elderberry = fluid("Brandy_Elderberry", "binnie.brandyelderberry").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BRANDY),
            Brandy_Grape = fluid("Brandy_Grape", "binnie.brandygrape").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BRANDY),
            Brandy_Pear = fluid("Brandy_Pear", "binnie.brandypear").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BRANDY),
            Brandy_Plum = fluid("Brandy_Plum", "binnie.brandyplum").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.BRANDY),
            Whiskey = fluid("Whiskey", "binnie.whiskey").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WHISKEY),
            Whiskey_Rye = fluid("Whiskey_Rye", "binnie.whiskeyrye").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WHISKEY),
            Whiskey_Corn = fluid("Whiskey_Corn", "binnie.whiskeycorn").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WHISKEY),
            Whiskey_Scotch = fluid("Whiskey_Scotch", "binnie.whiskeywheat").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WHISKEY),
            Whiskey_GlenMcKenner = fluid("Whiskey_GlenMcKenner", "glenmckenner").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.WHISKEY),
            Rum_White = fluid("Rum_White", "binnie.rumwhite").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.RUM),
            Rum_Dark = fluid("Rum_Dark", "binnie.rumdark").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.RUM),
            Pina_Colada = fluid("Pina_Colada", "pina.colada").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC | FluidFlags.RUM),
            Vodka = fluid("Vodka", "binnie.vodka").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC),
            Leninade = fluid("Leninade", "potion.leninade").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC),
            Mead = fluid("Mead", "mead").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC),
            ShortMead = fluid("ShortMead", "short.mead").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC),
            Sake = fluid("Sake", "potion.sake").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC),
            Tequila = fluid("Tequila", "binnie.tequila").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC),
            Alcopops = fluid("Alcopops", "potion.alcopops").withFlags(FluidFlags.FOOD | FluidFlags.ALCOHOLIC),

    // =======================================================================
    // Oils
    // =======================================================================

            Oil_Frying = fluid("Oil_Frying", "hotfryingoil").withFlags(FluidFlags.FOOD | FluidFlags.COOKING_OIL | FluidFlags.BATH),
            Oil_Seed = fluid("Oil_Seed", "seedoil").withFlags(FluidFlags.FOOD | FluidFlags.COOKING_OIL | FluidFlags.BATH),
            Oil_Plant = fluid("Oil_Plant", "plantoil").withFlags(FluidFlags.FOOD | FluidFlags.COOKING_OIL | FluidFlags.BATH),
            Oil_Sunflower = fluid("Oil_Sunflower", "sunfloweroil").withFlags(FluidFlags.FOOD | FluidFlags.COOKING_OIL | FluidFlags.BATH),
            Oil_Olive = fluid("Oil_Olive", "binnie.juiceolive").withFlags(FluidFlags.FOOD | FluidFlags.COOKING_OIL | FluidFlags.BATH),
            Oil_Nut = fluid("Oil_Nut", "nutoil").withFlags(FluidFlags.FOOD | FluidFlags.COOKING_OIL | FluidFlags.BATH),
            Oil_Lin = fluid("Oil_Lin", "linoil").withFlags(FluidFlags.FOOD | FluidFlags.COOKING_OIL | FluidFlags.BATH),
            Oil_Hemp = fluid("Oil_Hemp", "hempoil").withFlags(FluidFlags.FOOD | FluidFlags.COOKING_OIL | FluidFlags.BATH),
            Oil_Fish = fluid("Oil_Fish", "fishoil").withFlags(FluidFlags.FOOD | FluidFlags.COOKING_OIL | FluidFlags.BATH),
            Oil_Whale = fluid("Oil_Whale", "whaleoil").withFlags(FluidFlags.FOOD | FluidFlags.COOKING_OIL | FluidFlags.BATH),
            Oil_Canola = fluid("Oil_Canola", "canolaoil"),
            Oil_Creosote = fluid("Oil_Creosote", "creosote").withFlags(FluidFlags.BATH),
            Oil_Soulsand = fluid("Oil_Soulsand", "soulsandoil"),
            Oil_Light2 = fluid("Oil_Light2", "lightoil").withDensity(600),
            Oil_Light = fluid("Oil_Light", "liquid_light_oil").withDensity(600),
            Oil_Normal = fluid("Oil_Normal", "oil"),
            Oil_Medium = fluid("Oil_Medium", "liquid_medium_oil").withDensity(700),
            Oil_HotCrude = fluid("Oil_HotCrude", "hotcrude"),
            Oil_Heavy = fluid("Oil_Heavy", "liquid_heavy_oil").withDensity(800),
            Oil_Heavy2 = fluid("Oil_Heavy2", "heavyoil").withDensity(800),
            Oil_ExtraHeavy = fluid("Oil_ExtraHeavy", "liquid_extra_heavy_oil").withDensity(900),

    // =======================================================================
    // Fuels / Petrochemicals
    // =======================================================================

            Petrol = fluid("Petrol", "petrol"),
            Fuel = fluid("Fuel", "fuel"),
            Diesel = fluid("Diesel", "diesel"),
            Kerosine = fluid("Kerosine", "kerosine"),
            JetFuel = fluid("JetFuel", "rc jet fuel"),
            BioFuel = fluid("BioFuel", "biofuel"),
            BioDiesel = fluid("BioDiesel", "biodiesel"),
            BioEthanol = fluid("BioEthanol", "bioethanol"),
            Reikanol = fluid("Reikanol", "rc ethanol"),
            Biomass = fluid("Biomass", "biomass"),
            BiomassIC2 = fluid("BiomassIC2", "ic2biomass"),

    // =======================================================================
    // Industrial chemicals / Materials
    // =======================================================================

            Lubricant = fluid("Lubricant", "lubricant").withFlags(FluidFlags.LUBRICANT),
            LubRoCant = fluid("LubRoCant", "rc lubricant").withFlags(FluidFlags.LUBRICANT),
            Coolant_IC2 = fluid("Coolant_IC2", "ic2coolant"),
            Coolant_IC2_Hot = fluid("Coolant_IC2_Hot", "ic2hotcoolant").withFlags(FluidFlags.POWER_CONDUCTING),
            Glue = fluid("Glue", "glue"),
            Latex = fluid("Latex", "latex").withTemperature(DEF_ENV_TEMP),
            Resin = fluid("Resin", "resin"),
            Resin_Spruce = fluid("Resin_Spruce", "spruceresin"),
            Resin_Rubber = fluid("Resin_Rubber", "fluidrubbertreesap"),
            Turpentine = fluid("Turpentine", "turpentine"),
            Concrete = fluid("Concrete", "concrete"),
            CFoam = fluid("CFoam", "ic2constructionfoam"),
            Sewage = fluid("Sewage", "sewage"),
            Sludge = fluid("Sludge", "sludge"),
            Tar = fluid("Tar", "tar"),
            Glass = fluid("Glass", "glass"),
            Sluice = fluid("Sluice", "sluicejuice"),
            Indigo = fluid("Indigo", "indigo"),

    // =======================================================================
    // Molten salts / reactor fluids
    // =======================================================================

            Thorium_Salt = fluid("Thorium_Salt", "thoriumsalt"),
            Hot_Molten_Sodium = fluid("Hot_Molten_Sodium", "hotmoltensodium").withTemperature(1100).withFlags(FluidFlags.POWER_CONDUCTING),
            Hot_Molten_Tin = fluid("Hot_Molten_Tin", "hotmoltentin").withTemperature(2800).withFlags(FluidFlags.POWER_CONDUCTING),
            Hot_Heavy_Water = fluid("Hot_Heavy_Water", "hotheavywater").withTemperature(600).withFlags(FluidFlags.POWER_CONDUCTING),
            Hot_Semi_Heavy_Water = fluid("Hot_Semi_Heavy_Water", "hotsemiheavywater").withTemperature(550).withFlags(FluidFlags.POWER_CONDUCTING),
            Hot_Tritiated_Water = fluid("Hot_Tritiated_Water", "hottritiatedwater").withTemperature(650).withFlags(FluidFlags.POWER_CONDUCTING),
            Hot_Molten_LiCl = fluid("Hot_Molten_LiCl", "hotmoltenlicl").withTemperature(1600).withFlags(FluidFlags.POWER_CONDUCTING),

    // =======================================================================
    // Mob / organic drops
    // =======================================================================

            Blood = fluid("Blood", "blood").withFlags(FluidFlags.FOOD),
            Slime_Blue = fluid("Slime_Blue", "slime.blue").withFlags(FluidFlags.FOOD | FluidFlags.SLIME),
            Slime_Pink = fluid("Slime_Pink", "pinkslime").withFlags(FluidFlags.FOOD | FluidFlags.SLIME),
            Slime_Green = fluid("Slime_Green", "slime").withFlags(FluidFlags.FOOD | FluidFlags.SLIME),
            InkSquid = fluid("InkSquid", "squidink"),
            InkMyst = fluid("InkMyst", "myst.ink.black"),

    // =======================================================================
    // Magical / elemental fluids
    // =======================================================================

            Blaze = fluid("Blaze", "blaze").withGlint().withLuminosity(15).withTemperature(4000),
            FieryBlood = fluid("FieryBlood", "fieryblood").withGlint().withLuminosity(10).withTemperature(1500),
            FieryTears = fluid("FieryTears", "fierytears").withGlint().withLuminosity(10).withTemperature(1500),
            Pyrotheum = fluid("Pyrotheum", "pyrotheum").withGlint().withDensity(2000).withViscosity(1200).withLuminosity(15).withTemperature(4000),
            Cryotheum = fluid("Cryotheum", "cryotheum").withGlint().withDensity(4000).withViscosity(3000).withTemperature(50),
            Petrotheum = fluid("Petrotheum", "petrotheum").withGlint().withDensity(4000).withViscosity(1500).withTemperature(400),
            Aerotheum = fluid("Aerotheum", "aerotheum").withGlint().withDensity(-800).withViscosity(100).withTemperature(300),
            Mana_TE = fluid("Mana_TE", "mana").withGlint(),
            Ender = fluidMolten("Ender", "molten.enderpearl", "EnderPearl").withGlint().withTemperature(2723).withLuminosity(5),
            Ender_TE = fluid("Ender_TE", "ender"),
            Dragon_Breath = fluid("Dragon_Breath", "dragonbreath").withGlint().withGas().withDensity(100).withLuminosity(5),

    // =======================================================================
    // Lava variants
    // =======================================================================

            Lava = fluid("Lava", "lava", FluidTextureMode.VANILLA_LAVA),
            Lava_Volcanic = fluid("Lava_Volcanic", "volcanic_lava_fluid", FluidTextureMode.VANILLA_LAVA),
            Lava_Pahoehoe = fluid("Lava_Pahoehoe", "ic2pahoehoelava", FluidTextureMode.VANILLA_LAVA).withLuminosity(10).withDensity(50000).withViscosity(250000),
            Lava_Pure = fluid("Lava_Pure", "purelava", FluidTextureMode.VANILLA_LAVA).withFlags(FluidFlags.BROKEN | FluidFlags.INFINITE),
            Ender_Goo = fluid("Ender_Goo", "endergoo").withFlags(FluidFlags.MAGIC),

    // =======================================================================
    // Molten materials (explicit)
    // =======================================================================

            Redstone = fluidMolten("Redstone", "molten.redstone", "Redstone").withLuminosity(5).withTemperature(500),
            Redstone_TE = fluid("Redstone_TE", "redstone"),
            Glowstone_TE = fluid("Glowstone_TE", "glowstone"),
            Calcite = fluidMolten("Calcite", "molten.calcite", "Calcite"),
            Brass = fluidMolten("Brass", "molten.brass", "Brass"),
            Zinc = fluidMolten("Zinc", "molten.zinc", "Zinc"),
            Plastic = fluidMolten("Plastic", "molten.plastic", "Plastic").withTemperature(423),
            // EnderPearl molten already covered by Ender above — skip duplicate registry name
            HSLA_Molten = fluidMolten("HSLA_Molten", "molten.hsla", "HSLA").withLuminosity(5).withTemperature(1873),

    // =======================================================================
    // Medicines
    // =======================================================================

            Med_Heal = fluid("Med_Heal", "medicine.heal"),
            Med_Laxative = fluid("Med_Laxative", "medicine.laxative"),
            Poison = fluid("Poison", "poison"),
            Rotten_Drink = fluid("Rotten_Drink", "rottendrink"),

    // =======================================================================
    // Potions
    // =======================================================================

            Potion_Awkward = fluid("Potion_Awkward", "potion.awkward").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Thick = fluid("Potion_Thick", "potion.thick").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Mundane = fluid("Potion_Mundane", "potion.mundane").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Tainted = fluid("Potion_Tainted", "potion.tainted").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_Harm_1 = fluid("Potion_Harm_1", "potion.damage").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Harm_2 = fluid("Potion_Harm_2", "potion.damage.strong").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Harm_1S = fluid("Potion_Harm_1S", "potion.damage.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Harm_2S = fluid("Potion_Harm_2S", "potion.damage.strong.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Harm_1D = fluid("Potion_Harm_1D", "potion.damage.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Harm_2D = fluid("Potion_Harm_2D", "potion.damage.strong.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_Heal_1 = fluid("Potion_Heal_1", "potion.health").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Heal_2 = fluid("Potion_Heal_2", "potion.health.strong").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Heal_1S = fluid("Potion_Heal_1S", "potion.health.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Heal_2S = fluid("Potion_Heal_2S", "potion.health.strong.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Heal_1D = fluid("Potion_Heal_1D", "potion.health.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Heal_2D = fluid("Potion_Heal_2D", "potion.health.strong.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_Jump_1 = fluid("Potion_Jump_1", "potion.jump").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Jump_2 = fluid("Potion_Jump_2", "potion.jump.strong").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Jump_1S = fluid("Potion_Jump_1S", "potion.jump.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Jump_2S = fluid("Potion_Jump_2S", "potion.jump.strong.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Jump_1D = fluid("Potion_Jump_1D", "potion.jump.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Jump_2D = fluid("Potion_Jump_2D", "potion.jump.strong.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_Speed_1 = fluid("Potion_Speed_1", "potion.speed").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Speed_2 = fluid("Potion_Speed_2", "potion.speed.strong").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Speed_1L = fluid("Potion_Speed_1L", "potion.speed.long").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Speed_1S = fluid("Potion_Speed_1S", "potion.speed.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Speed_2S = fluid("Potion_Speed_2S", "potion.speed.strong.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Speed_1LS = fluid("Potion_Speed_1LS", "potion.speed.long.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Speed_1D = fluid("Potion_Speed_1D", "potion.speed.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Speed_2D = fluid("Potion_Speed_2D", "potion.speed.strong.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Speed_1LD = fluid("Potion_Speed_1LD", "potion.speed.long.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_Strength_1 = fluid("Potion_Strength_1", "potion.strength").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Strength_2 = fluid("Potion_Strength_2", "potion.strength.strong").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Strength_1L = fluid("Potion_Strength_1L", "potion.strength.long").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Strength_1S = fluid("Potion_Strength_1S", "potion.strength.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Strength_2S = fluid("Potion_Strength_2S", "potion.strength.strong.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Strength_1LS = fluid("Potion_Strength_1LS", "potion.strength.long.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Strength_1D = fluid("Potion_Strength_1D", "potion.strength.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Strength_2D = fluid("Potion_Strength_2D", "potion.strength.strong.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Strength_1LD = fluid("Potion_Strength_1LD", "potion.strength.long.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_Regen_1 = fluid("Potion_Regen_1", "potion.regen").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Regen_2 = fluid("Potion_Regen_2", "potion.regen.strong").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Regen_1L = fluid("Potion_Regen_1L", "potion.regen.long").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Regen_1S = fluid("Potion_Regen_1S", "potion.regen.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Regen_2S = fluid("Potion_Regen_2S", "potion.regen.strong.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Regen_1LS = fluid("Potion_Regen_1LS", "potion.regen.long.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Regen_1D = fluid("Potion_Regen_1D", "potion.regen.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Regen_2D = fluid("Potion_Regen_2D", "potion.regen.strong.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Regen_1LD = fluid("Potion_Regen_1LD", "potion.regen.long.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_Poison_1 = fluid("Potion_Poison_1", "potion.poison").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Poison_2 = fluid("Potion_Poison_2", "potion.poison.strong").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Poison_1L = fluid("Potion_Poison_1L", "potion.poison.long").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Poison_1S = fluid("Potion_Poison_1S", "potion.poison.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Poison_2S = fluid("Potion_Poison_2S", "potion.poison.strong.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Poison_1LS = fluid("Potion_Poison_1LS", "potion.poison.long.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Poison_1D = fluid("Potion_Poison_1D", "potion.poison.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Poison_2D = fluid("Potion_Poison_2D", "potion.poison.strong.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Poison_1LD = fluid("Potion_Poison_1LD", "potion.poison.long.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_FireResistance_1 = fluid("Potion_FireResistance_1", "potion.fireresistance").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_FireResistance_1L = fluid("Potion_FireResistance_1L", "potion.fireresistance.long").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_FireResistance_1S = fluid("Potion_FireResistance_1S", "potion.fireresistance.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_FireResistance_1LS = fluid("Potion_FireResistance_1LS", "potion.fireresistance.long.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_FireResistance_1D = fluid("Potion_FireResistance_1D", "potion.fireresistance.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_FireResistance_1LD = fluid("Potion_FireResistance_1LD", "potion.fireresistance.long.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_NightVision_1 = fluid("Potion_NightVision_1", "potion.nightvision").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_NightVision_1L = fluid("Potion_NightVision_1L", "potion.nightvision.long").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_NightVision_1S = fluid("Potion_NightVision_1S", "potion.nightvision.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_NightVision_1LS = fluid("Potion_NightVision_1LS", "potion.nightvision.long.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_NightVision_1D = fluid("Potion_NightVision_1D", "potion.nightvision.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_NightVision_1LD = fluid("Potion_NightVision_1LD", "potion.nightvision.long.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_Weakness_1 = fluid("Potion_Weakness_1", "potion.weakness").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Weakness_1L = fluid("Potion_Weakness_1L", "potion.weakness.long").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Weakness_1S = fluid("Potion_Weakness_1S", "potion.weakness.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Weakness_1LS = fluid("Potion_Weakness_1LS", "potion.weakness.long.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Weakness_1D = fluid("Potion_Weakness_1D", "potion.weakness.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Weakness_1LD = fluid("Potion_Weakness_1LD", "potion.weakness.long.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_Slowness_1 = fluid("Potion_Slowness_1", "potion.slowness").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Slowness_1L = fluid("Potion_Slowness_1L", "potion.slowness.long").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Slowness_1S = fluid("Potion_Slowness_1S", "potion.slowness.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Slowness_1LS = fluid("Potion_Slowness_1LS", "potion.slowness.long.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Slowness_1D = fluid("Potion_Slowness_1D", "potion.slowness.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Slowness_1LD = fluid("Potion_Slowness_1LD", "potion.slowness.long.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_WaterBreathing_1 = fluid("Potion_WaterBreathing_1", "potion.waterbreathing").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_WaterBreathing_1L = fluid("Potion_WaterBreathing_1L", "potion.waterbreathing.long").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_WaterBreathing_1S = fluid("Potion_WaterBreathing_1S", "potion.waterbreathing.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_WaterBreathing_1LS = fluid("Potion_WaterBreathing_1LS", "potion.waterbreathing.long.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_WaterBreathing_1D = fluid("Potion_WaterBreathing_1D", "potion.waterbreathing.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_WaterBreathing_1LD = fluid("Potion_WaterBreathing_1LD", "potion.waterbreathing.long.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),

            Potion_Invisibility_1 = fluid("Potion_Invisibility_1", "potion.invisibility").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Invisibility_1L = fluid("Potion_Invisibility_1L", "potion.invisibility.long").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT | FluidFlags.BATH),
            Potion_Invisibility_1S = fluid("Potion_Invisibility_1S", "potion.invisibility.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Invisibility_1LS = fluid("Potion_Invisibility_1LS", "potion.invisibility.long.splash").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Invisibility_1D = fluid("Potion_Invisibility_1D", "potion.invisibility.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT),
            Potion_Invisibility_1LD = fluid("Potion_Invisibility_1LD", "potion.invisibility.long.lingering").withFlags(FluidFlags.POTION | FluidFlags.MAGIC | FluidFlags.ENCHANTED_EFFECT)

    // end
    ;

    // =======================================================================
    // Dye fluids
    // =======================================================================

    public static final FluidEntry[] DYE_FLUIDS_WATER = new FluidEntry[16];
    public static final FluidEntry[] DYE_FLUIDS_FLOWER = new FluidEntry[16];
    public static final FluidEntry[] DYE_FLUIDS_CHEMICAL = new FluidEntry[16];
    public static final FluidEntry[] DYED_C_FOAMS = new FluidEntry[16];
    public static final FluidEntry[] DYED_C_FOAMS_OWNED = new FluidEntry[16];

    static {
        for (byte i = 0; i < 16; i++) {
            String post = DYE_OREDICTS_POST[i].toLowerCase(Locale.ROOT);
            DYE_FLUIDS_WATER[i] = register("Dye_Water_" + DYE_OREDICTS_POST[i],
                    new FluidEntry("dye.watermixed." + post, FluidTextureMode.DEDICATED, null, 0, true, false, false, 0, 0, 0, DEF_ENV_TEMP, FluidFlags.LIQUID | FluidFlags.SIMPLE | FluidFlags.DYE));
            DYE_FLUIDS_FLOWER[i] = register("Dye_Flower_" + DYE_OREDICTS_POST[i],
                    new FluidEntry("dye.flower." + post, FluidTextureMode.DEDICATED, null, 0, true, false, false, 0, 0, 0, DEF_ENV_TEMP, FluidFlags.LIQUID | FluidFlags.SIMPLE | FluidFlags.DYE));
            DYE_FLUIDS_CHEMICAL[i] = register("Dye_Chemical_" + DYE_OREDICTS_POST[i],
                    new FluidEntry("dye.chemical." + post, FluidTextureMode.DEDICATED, null, 0, true, false, false, 0, 0, 0, DEF_ENV_TEMP, FluidFlags.LIQUID | FluidFlags.SIMPLE | FluidFlags.DYE));
            DYED_C_FOAMS[i] = register("CFoam_Dyed_" + DYE_OREDICTS_POST[i],
                    new FluidEntry("cfoam." + post, FluidTextureMode.DEDICATED, null, 0, true, false, false, 0, 0, 0, DEF_ENV_TEMP, FluidFlags.LIQUID | FluidFlags.SIMPLE));
            DYED_C_FOAMS_OWNED[i] = register("CFoam_DyedOwned_" + DYE_OREDICTS_POST[i],
                    new FluidEntry("cfoam.owned." + post, FluidTextureMode.DEDICATED, null, 0, true, false, false, 0, 0, 0, DEF_ENV_TEMP, FluidFlags.LIQUID | FluidFlags.SIMPLE));
        }
    }

    // =======================================================================
    // createMolten entries
    // =======================================================================

    public static final FluidEntry
            Molten_Chocolate = createMolten("Chocolate"),
            Molten_Cheese = createMolten("Cheese"),
            Molten_Sugar = createMolten("Sugar"),
            Molten_Rubber = createMolten("Rubber"),
            Molten_Wax = createMolten("Wax"),
            Molten_WaxBee = createMolten("WaxBee"),
            Molten_WaxParaffin = createMolten("WaxParaffin"),
            Molten_WaxPlant = createMolten("WaxPlant"),
            Molten_WaxRefractory = createMolten("WaxRefractory"),
            Molten_WaxMagic = createMolten("WaxMagic"),
            Molten_WaxAmnesic = createMolten("WaxAmnesic"),
            Molten_WaxSoulful = createMolten("WaxSoulful"),
            Molten_Al2O3 = createMolten("Al2O3")
    ;

    private static FluidEntry createMolten(String materialName) {
        return register("GenMolten_" + materialName,
                new FluidEntry("molten." + materialName.toLowerCase(Locale.ROOT), FluidTextureMode.GENERIC_MOLTEN,
                        materialName, 0, true, false, false, 3000, 6000, 0, DEF_ENV_TEMP, FluidFlags.LIQUID));
    }

    // =======================================================================
    // Registry
    // =======================================================================

    public static Map<String, FluidEntry> all() {
        return Map.copyOf(REGISTRY);
    }

    /** GT6 Loader_Fluids: auto-register {@code molten.{material}} for metals/alloys. */
    public static void registerGeneratedMoltenFluids(Collection<GTMaterial> materials) {
        for (GTMaterial raw : materials) {
            GTMaterial material = raw.resolve();
            if (!material.isValid() || material.getId() <= 0 || material.has(MaterialProperty.HIDDEN)) {
                continue;
            }
            if (!material.has(MaterialProperty.MOLTEN)
                    && !material.has(MaterialProperty.METAL)
                    && !material.has(MaterialProperty.ALLOY)) {
                continue;
            }
            String registryName = "molten." + material.getName().toLowerCase(Locale.ROOT);
            if (hasRegistryName(registryName)) {
                continue;
            }
            int tint = moltenTint(material);
            int temp = (int) material.getMeltingPoint();
            register("GenMolten_" + material.getName(),
                    new FluidEntry(registryName, FluidTextureMode.GENERIC_MOLTEN,
                            material.getName(), tint, true, false, false, 3000, 6000, 0, temp, FluidFlags.LIQUID));
        }
    }

    /**
     * GT6 generated a gas/liquid fluid for every material in those states; the chemistry chains
     * (SO2, H2SO4, Cl, ...) consume these.
     * <p>
     * GT6 names them after the material and <em>reuses</em> whatever fluid already carries that name:
     * {@code FL.createGas}/{@code FL.createLiquid} use {@code aMaterial.mNameInternal.toLowerCase()}
     * (FL.java:1072,1080) and {@code FL.create} falls back to {@code FluidRegistry.getFluid(aName)}
     * when the name is taken (FL.java:1110), after which the very same fluid is stored as both the
     * material's gas and its liquid. The port used to invent {@code gas.<material>} / {@code liquid.<material>}
     * names, which produced three fluids for one material (e.g. {@code hydrogen} from the dedicated
     * {@code FL.Hydrogen} entry plus an orphaned {@code gas_hydrogen} and {@code liquid_hydrogen}).
     * <p>
     * So: a material that already has a dedicated fluid entry keeps it (and now gets its material bound
     * for tinting), and only materials without one get a single generated fluid named after the material.
     */
    public static void registerGeneratedGasLiquidFluids(Collection<GTMaterial> materials) {
        for (GTMaterial raw : materials) {
            GTMaterial material = raw.resolve();
            if (!material.isValid() || material.getId() <= 0 || material.has(MaterialProperty.HIDDEN)) {
                continue;
            }
            boolean gas = material.has(MaterialProperty.GAS);
            boolean liquid = material.has(MaterialProperty.LIQUID);
            if (!gas && !liquid) continue;
            String lower = material.getName().toLowerCase(Locale.ROOT);
            String dedicated = fieldOfRegistryName(lower);
            if (dedicated != null) {
                bindMaterial(dedicated, material);
                GENERATED_ALIASES.put("GenGas_" + material.getName(), dedicated);
                GENERATED_ALIASES.put("GenLiquid_" + material.getName(), dedicated);
                continue;
            }
            String field = gas ? "GenGas_" + material.getName() : "GenLiquid_" + material.getName();
            if (REGISTRY.containsKey(field)) continue;
            register(field, new FluidEntry(lower, FluidTextureMode.DEDICATED, material.getName(), 0, true, false,
                    gas, gas ? -100 : 1000, gas ? 200 : 1000, 0, DEF_ENV_TEMP,
                    gas ? FluidFlags.GAS_FLAG : FluidFlags.LIQUID));
            // GT6 keeps one fluid object for both states, so the other generated key aliases it.
            GENERATED_ALIASES.put((gas ? "GenLiquid_" : "GenGas_") + material.getName(), field);
        }
    }

    /** Field key of the entry carrying this registry name, or null. */
    private static String fieldOfRegistryName(String registryName) {
        for (Map.Entry<String, FluidEntry> entry : REGISTRY.entrySet()) {
            if (entry.getValue().registryName().equalsIgnoreCase(registryName)) return entry.getKey();
        }
        return null;
    }

    /** GT6 {@code FL.create(material)} stores the material on the fluid; the port binds it for tinting. */
    private static void bindMaterial(String field, GTMaterial material) {
        // The declaration objects are immutable and shadowed by public static fields which the test
        // suite compares against the registry, so the binding is kept beside the entry instead of
        // replacing it (see FluidVisualPolicy#material).
        FluidEntry entry = REGISTRY.get(field);
        if (entry == null || entry.materialKey() != null) return;
        BOUND_MATERIALS.put(entry.registryName().toLowerCase(Locale.ROOT), material.getName());
    }

    /** Material bound to a fluid registry name without a {@code materialKey} on the declaration. */
    
    public static String boundMaterial(String registryName) {
        return registryName == null ? null : BOUND_MATERIALS.get(registryName.toLowerCase(Locale.ROOT));
    }

    private static int moltenTint(GTMaterial material) {
        return com.gregtech.gregtech.api.fluid.FluidTintRules.moltenTint(material);
    }

    private static boolean hasRegistryName(String registryName) {
        for (FluidEntry entry : REGISTRY.values()) {
            if (entry.registryName().equals(registryName)) {
                return true;
            }
        }
        return false;
    }

    public static FluidEntry get(String field) {
        FluidEntry direct = REGISTRY.get(field);
        if (direct != null) return direct;
        String canonical = canonicalField(field);
        return canonical.equals(field) ? null : REGISTRY.get(canonical);
    }

    public static void bootstrap() {
        if (REGISTRY.isEmpty()) {
            throw new IllegalStateException("FL failed to initialize");
        }
        if(importedFieldsReconciled)return;
        // The imported declarations register first, then chain immutable withGas/withTemperature
        // copies into their public fields. Publish those FINAL definitions once, before Forge
        // takes its snapshot. Generated definitions already register their final value directly.
        if(definitionsClosed)throw new IllegalStateException("Fluid import reconciliation after registration");
        try {
            for(var field:FluidCatalog.class.getFields()){
                if(field.getType()!=FluidEntry.class||!java.lang.reflect.Modifier.isStatic(field.getModifiers()))continue;
                if(REGISTRY.containsKey(field.getName())){
                    var finalized=(FluidEntry)field.get(null);
                    if(finalized==null)throw new IllegalStateException("Missing fluid declaration: "+field.getName());
                    REGISTRY.put(field.getName(),finalized);
                }
            }
        }catch(IllegalAccessException e){throw new IllegalStateException("Cannot reconcile imported fluid definitions",e);}
        importedFieldsReconciled=true;
    }
}
