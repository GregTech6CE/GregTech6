/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original Loader_MultiTileEntities machine registrations. */
package gregtech.content.machine;

import static gregtech.api.energy.EnergyType.*;
import static gregtech.content.data.MachineRecipeMapDefinitions.*;
import static gregtech.content.machine.BasicMachineFaces.*;

import gregtech.api.energy.EnergyType;
import gregtech.api.machine.BasicMachineParameters;
import gregtech.api.machine.MachineFaceMasks;
import gregtech.api.material.GTMaterial;
import gregtech.content.data.BasicMachineCraftingRecipes;
import gregtech.content.data.BasicMachineCraftingRecipes.Recipe;
import gregtech.content.data.SourceBlockProperties;
import gregtech.content.material.Materials;
import gregtech.content.multiblock.OriginalAdvancedControllerData;
import gregtech.content.multiblock.OriginalLargeRecipeMachineData;
import gregtech.content.recipe.RecipeMapSpec;
import gregtech.engine.definition.DefinitionCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

/** Built-in machine families and their source registration metadata. */
public final class BasicMachineCatalog {
    private BasicMachineCatalog() {}

    private record Casing(GTMaterial material, String suffix, float strength) {}

    private record Input(long nominal, long min, long max) {}

    private record Processing(int baseParallel, boolean doublesPerTier, boolean scalesDuration) {
        int parallel(int tier) {
            return doublesPerTier ? baseParallel << (tier - 1) : baseParallel;
        }
    }

    enum Behavior {
        STANDARD,
        CHEAP_OVERCLOCKING,
        IGNITION,
        HALF_EFFICIENCY,
        MATTER,
        NO_CONSTANT_POWER
    }

    private enum Kind {
        BASIC,
        LARGE,
        CONTROLLER
    }

    private record Family(
            String name,
            RecipeMapSpec recipeMap,
            int sourceIdBase,
            EnergyType energy,
            int firstTier,
            List<Casing> casings,
            Input fixedInput,
            Processing processing,
            Behavior behavior,
            MachineFaceMasks faces,
            IntFunction<Recipe> crafting,
            Kind kind,
            String controllerId,
            String tool) {
        Casing casing(int tier) {
            return casings.get(tier - firstTier);
        }

        boolean hasTier(int tier) {
            return tier >= firstTier && tier < firstTier + casings.size();
        }

        Input input(int tier) {
            if (fixedInput != null) return fixedInput;
            long nominal = 32L << (2 * (tier - 1));
            return new Input(nominal, nominal / 2, nominal * 2);
        }

        SourceBlockProperties.Params harvest(int tier) {
            return new SourceBlockProperties.Params(
                    sourceIdBase + tier, tool, casing(tier).material(), -1, false);
        }

        String id(int tier) {
            if (controllerId != null) return controllerId;
            // These paths shipped before the original casing material was restored.
            return name + "_"
                    + switch (name) {
                        case "melter" -> "stainless_steel";
                        case "scannermolecular" -> "osmium";
                        case "massfab", "replicator" -> tier == 1 ? "osmium" : "osmiridium_t" + tier;
                        default -> casing(tier).suffix();
                    };
        }
    }

    private static final List<Casing> HEAT = List.of(
            new Casing(Materials.Steel, "steel", 6), new Casing(Materials.Invar, "invar", 4),
            new Casing(Materials.Titanium, "titanium", 9),
                    new Casing(Materials.TungstenCarbide, "tungsten_carbide", 12.5F));
    private static final List<Casing> KINETIC = List.of(
            new Casing(Materials.Bronze, "bronze", 7), new Casing(Materials.Steel, "steel", 6),
            new Casing(Materials.Titanium, "titanium", 9),
                    new Casing(Materials.Tungstensteel, "tungsten_steel", 12.5F));
    private static final List<Casing> ELECTRIC = List.of(
            new Casing(Materials.SteelGalvanized, "galvanized_steel", 4),
            new Casing(Materials.Aluminium, "aluminium", 4),
            new Casing(Materials.StainlessSteel, "stainless_steel", 4),
            new Casing(Materials.Chromium, "chromium", 4),
            new Casing(Materials.Titanium, "titanium", 4));
    private static final Casing OSMIRIDIUM = new Casing(Materials.Osmiridium, "osmiridium", 16);
    private static final List<Casing> QUANTUM = Collections.nCopies(5, OSMIRIDIUM);
    private static final Casing STAINLESS = new Casing(Materials.StainlessSteel, "stainless_steel", 6);
    private static final Casing TUNGSTEN_STEEL = KINETIC.get(3);
    private static final Casing LEAD = new Casing(Materials.Lead, "lead", 6);

    private static final Input TIME_INPUT = new Input(1, 1, 16);
    private static final Input LARGE_INPUT = new Input(512, 512, 4096);
    private static final Input TOWER_INPUT = new Input(512, 1, 1024);
    private static final Processing SINGLE = fixed(1, false);
    private static final Processing PARALLEL_1 = doubling(1, true);
    private static final Processing PARALLEL_4 = doubling(4, true);
    private static final Processing PARALLEL_8 = doubling(8, true);
    private static final Processing BULK_SMELTING = fixed(1000, true);

    private static List<Family> families() {
        return List.of(
                tiered("oven", Furnace, 20000, HU, HEAT, LEFT_TO_RIGHT_BOTTOM_POWER, BasicMachineCraftingRecipes::oven),
                tiered(
                        "roaster",
                        Roasting,
                        20170,
                        HU,
                        HEAT,
                        ROASTER,
                        BasicMachineCraftingRecipes::roaster,
                        doubling(1, false),
                        Behavior.CHEAP_OVERCLOCKING),
                tiered(
                        "distillery",
                        Distillery,
                        20190,
                        HU,
                        HEAT,
                        DISTILLERY,
                        BasicMachineCraftingRecipes::distillery,
                        PARALLEL_8,
                        Behavior.CHEAP_OVERCLOCKING),
                tiered(
                        "extruder",
                        Extruder,
                        20200,
                        HU,
                        HEAT,
                        LEFT_TO_RIGHT_BOTTOM_POWER,
                        BasicMachineCraftingRecipes::extruder),
                tiered(
                        "smelter",
                        Smelter,
                        20240,
                        HU,
                        HEAT,
                        SMELTER,
                        BasicMachineCraftingRecipes::smelter,
                        BULK_SMELTING,
                        Behavior.CHEAP_OVERCLOCKING),
                tiered(
                        "crystallisationcrucible",
                        CrystallisationCrucible,
                        20250,
                        HU,
                        HEAT,
                        CRYSTALLISATION_CRUCIBLE,
                        BasicMachineCraftingRecipes::crystallisationCrucible),
                tiered(
                        "dryer",
                        Drying,
                        20310,
                        HU,
                        HEAT,
                        ROASTER,
                        BasicMachineCraftingRecipes::dryer,
                        PARALLEL_8,
                        Behavior.CHEAP_OVERCLOCKING),
                tiered(
                        "laminator",
                        Laminator,
                        20390,
                        HU,
                        HEAT,
                        TOP_LEFT_TO_RIGHT_BOTTOM_POWER,
                        BasicMachineCraftingRecipes::laminator),
                tiered(
                        "catalyticcracker",
                        CatalyticCracking,
                        20480,
                        HU,
                        HEAT,
                        CRACKER,
                        BasicMachineCraftingRecipes::catalyticCracker),
                tiered(
                        "steamcracker",
                        SteamCracking,
                        20490,
                        HU,
                        HEAT,
                        CRACKER,
                        BasicMachineCraftingRecipes::steamCracker),
                tiered(
                        "shredder",
                        Shredder,
                        20010,
                        RU,
                        KINETIC,
                        TOP_TO_BOTTOM_SIDE_POWER,
                        BasicMachineCraftingRecipes::shredder),
                tiered(
                        "lathe",
                        Lathe,
                        20040,
                        RU,
                        KINETIC,
                        LEFT_TO_RIGHT_BOTTOM_POWER,
                        BasicMachineCraftingRecipes::lathe),
                tiered("buzzsaw", Cutter, 20060, RU, KINETIC, BUZZSAW, BasicMachineCraftingRecipes::buzzsaw),
                tiered(
                        "centrifuge",
                        Centrifuge,
                        20080,
                        RU,
                        KINETIC,
                        CENTRIFUGE,
                        BasicMachineCraftingRecipes::centrifuge,
                        PARALLEL_1,
                        Behavior.STANDARD),
                tiered(
                        "rollingmill",
                        RollingMill,
                        20110,
                        RU,
                        KINETIC,
                        LEFT_TO_RIGHT_BACK_POWER,
                        BasicMachineCraftingRecipes::rollingMill),
                tiered(
                        "rollbender",
                        RollBender,
                        20120,
                        RU,
                        KINETIC,
                        LEFT_TO_RIGHT_BACK_POWER,
                        BasicMachineCraftingRecipes::rollBender),
                tiered(
                        "rollformer",
                        RollFormer,
                        20130,
                        RU,
                        KINETIC,
                        LEFT_TO_RIGHT_BACK_POWER,
                        BasicMachineCraftingRecipes::rollFormer),
                tiered(
                        "clustermill",
                        ClusterMill,
                        20140,
                        RU,
                        KINETIC,
                        LEFT_TO_RIGHT_BACK_POWER,
                        BasicMachineCraftingRecipes::clusterMill),
                tiered(
                        "wiremill",
                        Wiremill,
                        20150,
                        RU,
                        KINETIC,
                        LEFT_TO_RIGHT_BACK_POWER,
                        BasicMachineCraftingRecipes::wireMill),
                tiered(
                        "mixer",
                        Mixer,
                        20180,
                        RU,
                        KINETIC,
                        MIXER,
                        BasicMachineCraftingRecipes::mixer,
                        PARALLEL_4,
                        Behavior.STANDARD),
                tiered("loom", Loom, 20210, RU, KINETIC, TOP_TO_BOTTOM_SIDE_POWER, BasicMachineCraftingRecipes::loom),
                tiered("sluice", Sluice, 20290, RU, KINETIC, CANNER, BasicMachineCraftingRecipes::sluice),
                tiered(
                        "sander",
                        Sharpening,
                        20510,
                        RU,
                        KINETIC,
                        LEFT_TO_RIGHT_TOP_POWER,
                        BasicMachineCraftingRecipes::sander),
                tiered(
                        "burnmixer",
                        BurnMixer,
                        20520,
                        RU,
                        KINETIC,
                        MIXER,
                        BasicMachineCraftingRecipes::burnMixer,
                        PARALLEL_4,
                        Behavior.IGNITION),
                tiered("debarker", PressureWasher, 20550, RU, KINETIC, DEBARKER, BasicMachineCraftingRecipes::debarker),
                tiered(
                        "crusher",
                        Crusher,
                        20020,
                        KU,
                        KINETIC,
                        TOP_TO_BOTTOM_BACK_POWER,
                        BasicMachineCraftingRecipes::crusher,
                        PARALLEL_4,
                        Behavior.STANDARD),
                tiered(
                        "sifter",
                        Sifting,
                        20050,
                        KU,
                        KINETIC,
                        TOP_TO_BOTTOM_BACK_POWER,
                        BasicMachineCraftingRecipes::sifter,
                        PARALLEL_4,
                        Behavior.STANDARD),
                tiered(
                        "squeezer",
                        Squeezer,
                        20070,
                        KU,
                        KINETIC,
                        SQUEEZER,
                        BasicMachineCraftingRecipes::squeezer,
                        PARALLEL_4,
                        Behavior.STANDARD),
                tiered(
                        "compressor",
                        Compressor,
                        20100,
                        KU,
                        KINETIC,
                        TOP_TO_BOTTOM_LEFT_POWER,
                        BasicMachineCraftingRecipes::compressor,
                        PARALLEL_4,
                        Behavior.STANDARD),
                tiered(
                        "press",
                        Press,
                        20230,
                        KU,
                        KINETIC,
                        LEFT_TO_RIGHT_TOP_POWER,
                        BasicMachineCraftingRecipes::press,
                        PARALLEL_4,
                        Behavior.STANDARD),
                tiered(
                        "electrolyzer",
                        Electrolyzer,
                        20090,
                        EU,
                        ELECTRIC,
                        ELECTROLYZER,
                        BasicMachineCraftingRecipes::electrolyzer,
                        PARALLEL_1,
                        Behavior.STANDARD),
                tiered("canner", Canner, 20160, EU, ELECTRIC, CANNER, BasicMachineCraftingRecipes::canner),
                tiered("injector", Injector, 20260, EU, ELECTRIC, CANNER, BasicMachineCraftingRecipes::injector),
                tiered("printer", Printer, 20270, EU, ELECTRIC, PRINTER, BasicMachineCraftingRecipes::printer),
                tiered(
                        "scannervisuals",
                        ScannerVisuals,
                        20280,
                        EU,
                        ELECTRIC,
                        SCANNER,
                        BasicMachineCraftingRecipes::scannerVisuals),
                tiered(
                        "autocrafter",
                        Autocrafter,
                        20340,
                        EU,
                        ELECTRIC,
                        AUTOCRAFTER,
                        BasicMachineCraftingRecipes::autocrafter),
                tiered(
                        "electricmixer",
                        Mixer,
                        20350,
                        EU,
                        ELECTRIC,
                        MIXER,
                        BasicMachineCraftingRecipes::electricMixer,
                        PARALLEL_4,
                        Behavior.HALF_EFFICIENCY),
                tiered(
                        "electricloom",
                        Loom,
                        20360,
                        EU,
                        ELECTRIC,
                        TOP_TO_BOTTOM_SIDE_POWER,
                        BasicMachineCraftingRecipes::electricLoom,
                        SINGLE,
                        Behavior.HALF_EFFICIENCY),
                tiered(
                        "electricsifter",
                        Sifting,
                        20370,
                        EU,
                        ELECTRIC,
                        TOP_TO_BOTTOM_BACK_POWER,
                        BasicMachineCraftingRecipes::electricSifter,
                        SINGLE,
                        Behavior.HALF_EFFICIENCY),
                tiered("slicer", Slicer, 20380, EU, ELECTRIC, SCANNER, BasicMachineCraftingRecipes::slicer),
                tiered("nanofab", Nanofab, 20440, EU, ELECTRIC, CANNER, BasicMachineCraftingRecipes::nanofab),
                tiered(
                        "plantalyzer",
                        Plantalyzer,
                        20530,
                        EU,
                        ELECTRIC,
                        ANALYZER,
                        BasicMachineCraftingRecipes::plantalyzer,
                        fixed(64, false),
                        Behavior.STANDARD),
                tiered(
                        "bumblelyzer",
                        Bumblelyzer,
                        20540,
                        EU,
                        ELECTRIC,
                        ANALYZER,
                        BasicMachineCraftingRecipes::bumblelyzer,
                        fixed(64, false),
                        Behavior.STANDARD),
                tiered(
                        "boxinator",
                        Boxinator,
                        20580,
                        EU,
                        ELECTRIC,
                        TOP_LEFT_TO_RIGHT_BOTTOM_POWER,
                        BasicMachineCraftingRecipes::boxinator),
                tiered(
                        "unboxinator",
                        Unboxinator,
                        20590,
                        EU,
                        ELECTRIC,
                        TOP_LEFT_TO_RIGHT_BOTTOM_POWER,
                        BasicMachineCraftingRecipes::unboxinator),
                tiered("polarizer", Polarizer, 20220, MU, ELECTRIC, POLARIZER, BasicMachineCraftingRecipes::polarizer),
                tiered(
                        "magneticseparator",
                        MagneticSeparator,
                        20300,
                        MU,
                        ELECTRIC,
                        MAGNETIC_SEPARATOR,
                        BasicMachineCraftingRecipes::magneticSeparator),
                tiered(
                        "laserengraver",
                        LaserEngraver,
                        20320,
                        LU,
                        ELECTRIC,
                        LEFT_TO_RIGHT_TOP_POWER,
                        BasicMachineCraftingRecipes::laserEngraver),
                tiered(
                        "laserwelder",
                        Welder,
                        20330,
                        LU,
                        ELECTRIC,
                        LASER_WELDER,
                        BasicMachineCraftingRecipes::laserWelder),
                tiered("freezer", Freezer, 20560, CU, ELECTRIC, CANNER, BasicMachineCraftingRecipes::freezer),
                tiered(
                        "cryomixer",
                        CryoMixer,
                        20570,
                        CU,
                        ELECTRIC,
                        MIXER,
                        BasicMachineCraftingRecipes::cryoMixer,
                        PARALLEL_4,
                        Behavior.STANDARD),
                tiered(
                        "massfab",
                        Massfab,
                        20410,
                        QU,
                        QUANTUM,
                        CANNER,
                        BasicMachineCraftingRecipes::massFab,
                        SINGLE,
                        Behavior.MATTER),
                single(
                        "scannermolecular",
                        ScannerMolecular,
                        20423,
                        QU,
                        OSMIRIDIUM,
                        3,
                        SCANNER,
                        BasicMachineCraftingRecipes::scannerMolecular,
                        SINGLE,
                        Behavior.STANDARD),
                tiered(
                        "replicator",
                        Replicator,
                        20430,
                        QU,
                        QUANTUM,
                        CANNER,
                        BasicMachineCraftingRecipes::replicator,
                        SINGLE,
                        Behavior.MATTER),
                single(
                        "autoclave",
                        Autoclave,
                        22004,
                        TU,
                        STAINLESS,
                        1,
                        AUTOCLAVE,
                        BasicMachineCraftingRecipes::autoclave,
                        SINGLE,
                        Behavior.NO_CONSTANT_POWER),
                single(
                        "bath",
                        Bath,
                        22002,
                        TU,
                        STAINLESS,
                        1,
                        BATH,
                        BasicMachineCraftingRecipes::bath,
                        SINGLE,
                        Behavior.NO_CONSTANT_POWER),
                single(
                        "generifier",
                        Generifier,
                        22001,
                        TU,
                        STAINLESS,
                        1,
                        BATH,
                        BasicMachineCraftingRecipes::generifier,
                        fixed(100, false),
                        Behavior.NO_CONSTANT_POWER),
                single(
                        "coagulator",
                        Coagulator,
                        22000,
                        TU,
                        STAINLESS,
                        1,
                        COAGULATOR,
                        BasicMachineCraftingRecipes::coagulator,
                        SINGLE,
                        Behavior.NO_CONSTANT_POWER),
                single(
                        "fermenter",
                        Fermenter,
                        22003,
                        HU,
                        STAINLESS,
                        1,
                        ROASTER,
                        BasicMachineCraftingRecipes::fermenter,
                        SINGLE,
                        Behavior.STANDARD),
                single(
                        "melter",
                        Melter,
                        22010,
                        HU,
                        new Casing(Materials.Iron, "iron", 6),
                        1,
                        SMELTER,
                        BasicMachineCraftingRecipes::melter,
                        BULK_SMELTING,
                        Behavior.CHEAP_OVERCLOCKING),
                tiered("lightning", Lightning, 20500, EU, ELECTRIC, CANNER, BasicMachineCraftingRecipes::lightning),
                controller(
                        "cokeoven",
                        CokeOven,
                        17000,
                        TU,
                        new Casing(Materials.Ceramic, "ceramic", 5),
                        TIME_INPUT,
                        16,
                        COKE_OVEN,
                        "coke_oven_main",
                        "pickaxe"),
                controller(
                        "implosioncompressor",
                        ImplosionCompressor,
                        17110,
                        TU,
                        TUNGSTEN_STEEL,
                        TIME_INPUT,
                        64,
                        OriginalAdvancedControllerData.faces("implosioncompressor"),
                        "implosion_compressor_main",
                        "wrench"),
                controller(
                        "fusionreactor",
                        Fusion,
                        17198,
                        TU,
                        new Casing(Materials.SteelGalvanized, "galvanized_steel", 12.5F),
                        new Input(8192, 1, 16384),
                        1,
                        OriginalAdvancedControllerData.faces("fusionreactor"),
                        "fusion_reactor_main",
                        "wrench"),
                controller(
                        "distillationtower",
                        DistillationTower,
                        17101,
                        HU,
                        STAINLESS,
                        TOWER_INPUT,
                        1,
                        DISTILLATION_TOWER,
                        "distillation_tower_main",
                        "wrench"),
                controller(
                        "cryodistillationtower",
                        CryoDistillationTower,
                        17111,
                        CU,
                        STAINLESS,
                        TOWER_INPUT,
                        1,
                        DISTILLATION_TOWER,
                        "cryo_distillation_main",
                        "wrench"),
                large("largecentrifuge", Centrifuge, 17100, RU, TUNGSTEN_STEEL, 16),
                large("largeelectrolyzer", Electrolyzer, 17103, EU, STAINLESS, 16),
                large("largecoagulator", Coagulator, 17105, TU, STAINLESS, 64),
                large("largeautoclave", Autoclave, 17112, TU, STAINLESS, 16),
                large("largebath", Bath, 17104, TU, STAINLESS, 64),
                large("largemixer", Mixer, 17102, RU, STAINLESS, 256),
                large("largefermenter", Fermenter, 17113, HU, STAINLESS, 256),
                large("largeoven", Furnace, 17106, EU, new Casing(Materials.Invar, "invar", 6), 64),
                large("largesluice", Sluice, 17107, RU, KINETIC.get(2), 64),
                large("largecrusher", Crusher, 17108, RU, TUNGSTEN_STEEL, 64),
                large("largeshredder", Shredder, 17109, RU, TUNGSTEN_STEEL, 64),
                large("largesqueezer", Squeezer, 17114, RU, KINETIC.get(1), 64),
                large("largemassfab", Massfab, 17199, QU, LEAD, 64));
    }

    private static Family tiered(
            String name,
            RecipeMapSpec map,
            int sourceIdBase,
            EnergyType energy,
            List<Casing> casings,
            MachineFaceMasks faces,
            IntFunction<Recipe> crafting) {
        return tiered(name, map, sourceIdBase, energy, casings, faces, crafting, SINGLE, Behavior.STANDARD);
    }

    private static Family tiered(
            String name,
            RecipeMapSpec map,
            int sourceIdBase,
            EnergyType energy,
            List<Casing> casings,
            MachineFaceMasks faces,
            IntFunction<Recipe> crafting,
            Processing processing,
            Behavior behavior) {
        return new Family(
                name,
                map,
                sourceIdBase,
                energy,
                1,
                casings,
                null,
                processing,
                behavior,
                faces,
                crafting,
                Kind.BASIC,
                null,
                "wrench");
    }

    private static Family single(
            String name,
            RecipeMapSpec map,
            int sourceId,
            EnergyType energy,
            Casing casing,
            int tier,
            MachineFaceMasks faces,
            IntFunction<Recipe> crafting,
            Processing processing,
            Behavior behavior) {
        return new Family(
                name,
                map,
                sourceId - tier,
                energy,
                tier,
                List.of(casing),
                energy.equals(TU) ? TIME_INPUT : null,
                processing,
                behavior,
                faces,
                crafting,
                Kind.BASIC,
                null,
                "wrench");
    }

    private static Family controller(
            String name,
            RecipeMapSpec map,
            int sourceId,
            EnergyType energy,
            Casing casing,
            Input input,
            int parallel,
            MachineFaceMasks faces,
            String id,
            String tool) {
        return new Family(
                name,
                map,
                sourceId - 1,
                energy,
                1,
                List.of(casing),
                input,
                fixed(parallel, false),
                Behavior.STANDARD,
                faces,
                null,
                Kind.CONTROLLER,
                id,
                tool);
    }

    private static Family large(
            String name, RecipeMapSpec map, int sourceId, EnergyType energy, Casing casing, int parallel) {
        Input input = switch (energy.name()) {
            case "TU" -> TIME_INPUT;
            case "HU" -> new Input(512, 1, 4096);
            case "QU" -> new Input(1, 1, 2097152);
            default -> LARGE_INPUT;
        };
        var faces = energy.equals(QU)
                ? OriginalAdvancedControllerData.faces(name)
                : OriginalLargeRecipeMachineData.faces(name);
        return new Family(
                name,
                map,
                sourceId - 1,
                energy,
                1,
                List.of(casing),
                input,
                fixed(parallel, !energy.equals(TU)),
                Behavior.STANDARD,
                faces,
                null,
                Kind.LARGE,
                null,
                "wrench");
    }

    private static Processing fixed(int parallel, boolean scalesDuration) {
        return new Processing(parallel, false, scalesDuration);
    }

    private static Processing doubling(int parallel, boolean scalesDuration) {
        return new Processing(parallel, true, scalesDuration);
    }

    private static final List<Family> FAMILIES = families();
    private static final Map<String, Family> BY_NAME =
            FAMILIES.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Family::name, family -> family));

    /** Includes dedicated controllers, whose family names are also reserved for built-ins. */
    public static Set<String> machineTypeNames() {
        return BY_NAME.keySet();
    }

    public static RecipeMapSpec recipeMap(String name) {
        return switch (name) {
            case "pressurewasher" -> PressureWasher;
            case "heatmixer" -> Mixer;
            case "default" -> DidYouKnow;
            default -> requireFamily(name).recipeMap();
        };
    }

    public static GTMaterial casing(String name, int tier) {
        return requireVariant(name, tier).casing(tier).material();
    }

    static GTMaterial electricTierMaterial(int tier) {
        return ELECTRIC.get(tier - 1).material();
    }

    public static int sourceId(String name, int tier) {
        return requireVariant(name, tier).sourceIdBase() + tier;
    }

    public static Recipe crafting(String name, int tier) {
        var family = findVariant(name, tier);
        return family == null || family.crafting() == null
                ? null
                : family.crafting().apply(tier);
    }

    public static SourceBlockProperties.Params harvest(String name, int tier) {
        var family = findVariant(name, tier);
        return family == null ? null : family.harvest(tier);
    }

    public static Map<String, SourceBlockProperties.Params> harvestProperties() {
        var result = new LinkedHashMap<String, SourceBlockProperties.Params>();
        for (var family : FAMILIES) {
            for (int tier = family.firstTier(); family.hasTier(tier); tier++) {
                result.put(family.name() + "/" + tier, family.harvest(tier));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, SourceBlockProperties.Params> controllerHarvestProperties() {
        var result = new LinkedHashMap<String, SourceBlockProperties.Params>();
        for (var family : FAMILIES) {
            if (family.kind() == Kind.CONTROLLER) result.put(family.controllerId(), family.harvest(1));
        }
        return Collections.unmodifiableMap(result);
    }

    static boolean isOriginalBasic(String name, int tier) {
        var family = findVariant(name, tier);
        return family != null && family.kind() == Kind.BASIC;
    }

    static Behavior behavior(String name, int tier) {
        var family = findVariant(name, tier);
        return family == null ? Behavior.STANDARD : family.behavior();
    }

    public static boolean parallelScalesDuration(String name, int tier) {
        var family = findVariant(name, tier);
        return family == null || family.processing().scalesDuration();
    }

    public static List<BasicMachineParameters> specifications() {
        var result = new ArrayList<BasicMachineParameters>();
        for (var family : FAMILIES) {
            if (family.kind() == Kind.CONTROLLER) continue;
            for (int tier = family.firstTier(); family.hasTier(tier); tier++) result.add(parameters(family, tier));
        }
        return DefinitionCatalog.validated(result, BasicMachineParameters::id);
    }

    public static BasicMachineParameters controller(String name) {
        var family = requireFamily(name);
        if (family.kind() != Kind.CONTROLLER) throw new IllegalArgumentException("Not a dedicated controller: " + name);
        return parameters(family, 1);
    }

    private static BasicMachineParameters parameters(Family family, int tier) {
        Casing casing = family.casing(tier);
        Input input = family.input(tier);
        return BasicMachineParameters.builder(family.id(tier), casing.material())
                .machineType(family.name())
                .tier(tier)
                .energy(family.energy(), input.nominal())
                .energyRange(input.min(), input.max())
                .parallel(family.processing().parallel(tier))
                .strength(casing.strength(), casing.strength())
                .faces(family.faces())
                .constructionMaterials(MachineConstructionMaterials.machineWeights(family.name(), tier))
                .build();
    }

    private static Family requireFamily(String name) {
        var family = BY_NAME.get(name);
        if (family == null) throw new IllegalArgumentException("Unknown machine family: " + name);
        return family;
    }

    private static Family requireVariant(String name, int tier) {
        var family = requireFamily(name);
        if (!family.hasTier(tier)) throw new IllegalArgumentException("Unknown machine tier: " + name + "/" + tier);
        return family;
    }

    private static Family findVariant(String name, int tier) {
        var family = BY_NAME.get(name);
        return family != null && family.hasTier(tier) ? family : null;
    }
}
