package com.gregtech.gregtech.content.machine;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.energy.MachineFaceMasks;
import com.gregtech.gregtech.api.machine.BasicMachineParameters;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialChemistry.WeightedMaterial;
import com.gregtech.gregtech.data.ImportedMaterialData;

import static com.gregtech.gregtech.api.energy.MachineFaceMasks.*;

import java.util.*;
import java.util.function.Function;


/**
 * GT6 basic machine batch registration (GT6 {@code Loader_MultiTileEntities#machines1–4}).
 * Energy types and tier materials follow the original GT6 {@code MT.DATA.Heat_T / Kinetic_T / Electric_T}.
 */
public final class BasicMachineCatalog {
    private BasicMachineCatalog() {}

    // ── Tier material arrays ────────────────────────────────────────────────

    /** GT6 {@code Heat_T}: Steel, Invar, Ti, TungstenCarbide */
    private static final GTMaterial[] HU_TIERS = {Materials.Steel, Materials.Invar, Materials.Titanium, Materials.TungstenCarbide};
    /** GT6 {@code Kinetic_T}: Bronze, Steel, Ti, TungstenSteel */
    private static final GTMaterial[] RU_KU_TIERS = {Materials.Bronze, Materials.Steel, Materials.Titanium, Materials.Tungstensteel};
    /** GT6 {@code Electric_T}: SteelGalvanized, Al, StainlessSteel, Cr, Ti */
    private static final GTMaterial[] EU_MU_LU_CU_TIERS = {Materials.SteelGalvanized, Materials.Aluminium, Materials.StainlessSteel, Materials.Chromium, Materials.Titanium};

    // ── Hardness / blast resistance by tier index ───────────────────────────

    private static final float[][] TIER_STATS = {
        /* T1 */ {6.0F, 6.0F},   /* T2 */ {4.0F, 4.0F},   /* T3 */ {9.0F, 9.0F},
        /* T4 */ {12.5F, 12.5F}, /* T5 */ {9.0F, 9.0F}
    };

    /** Per-machine hardness/resistance overrides from GT6 Loader_MultiTileEntities. */
    private static final Map<String, float[]> STAT_OVERRIDES = Map.of(
            "largecentrifuge", new float[]{12.5F, 12.5F},
            "largecrusher",    new float[]{12.5F, 12.5F},
            "largeshredder",   new float[]{12.5F, 12.5F},
            "largesluice",     new float[]{9.0F, 9.0F},
            "implosioncompressor", new float[]{12.5F, 12.5F},
            "fusionreactor",   new float[]{12.5F, 12.5F},
            "cokeoven",        new float[]{5.0F, 5.0F});

    // ── Machine type definitions ────────────────────────────────────────────

    /**
     * @param tierBase the port tier the first element of {@code tiers} is registered as. Almost every
     *                 machine starts at tier 1; the ones GT6 numbers differently (see
     *                 {@link #EXTRA_TIERS} and the {@code tierBase} on the Molecular Scanner row) set
     *                 it so {@code BasicMachineOriginalParams.find(name, tier)} reads the very GT6
     *                 registration the machine is a port of.
     */
    private record MachineDef(String name, String energy, GTMaterial[] tiers,
                              Function<GTMaterial, List<WeightedMaterial>> materials, int tierBase) {
        MachineDef(String name, String energy, GTMaterial[] tiers,
                   Function<GTMaterial, List<WeightedMaterial>> materials) {
            this(name, energy, tiers, materials, 1);
        }
    }

    private static final List<MachineDef> MACHINE_DEFS = new ArrayList<>();

    /**
     * Port tiers whose GT6 registration has no row in {@link MachineDef#tiers()} because every tier
     * uses the same casing material ({@code MT.Osmiridium}), so the tier array cannot express them.
     * Mirrors {@code EXTRA_TIERS} in tools/gt6_machine_map.py; the ids carry the tier suffix
     * ({@code massfab_osmiridium_t2}) exactly like the ones already registered.
     */
    private static final java.util.Map<String, int[]> EXTRA_TIERS = java.util.Map.of(
            "massfab", new int[]{2, 3, 4, 5},
            "replicator", new int[]{2, 3, 4, 5});

    /**
     * Machines that exist in GT6 <em>only</em> as multiblocks and that this port registers as multiblock
     * controllers too ({@code registry/GTMultiblocks}). Their {@link MachineDef} would add a second,
     * single-block item for the same machine — the port used to show e.g. a "Distillation Tower" and a
     * "Coke Oven" item next to the multiblock controller of the same name.
     * <p>
     * The {@code large*} batch machines are deliberately <em>not</em> in this set: the port has no
     * multiblock controller for them yet (GT6 does), so their single-block definition is the only
     * implementation and removing it would delete the machine. See docs/PORTING_REMAINING_2026-09-14.md.
     * </p>
     */
    private static final Set<String> MULTIBLOCK_ONLY = Set.of(
            "distillationtower", "cryodistillationtower", "cokeoven",
            "fusionreactor", "implosioncompressor", "lightning");

    /** Casing (8 units) + fixed extras per machine type. */
    private static List<WeightedMaterial> buildMaterials(GTMaterial casing, Object... extras) {
        List<WeightedMaterial> list = new ArrayList<>();
        list.add(new WeightedMaterial(casing, 8));
        for (int i = 0; i < extras.length; i += 2) {
            list.add(new WeightedMaterial((GTMaterial) extras[i], (Long) extras[i + 1]));
        }
        return list;
    }

    static {
        // HU machines (4 tiers: Steel, Invar, Ti, TungstenCarbide)
        MACHINE_DEFS.add(new MachineDef("oven",           "HU", HU_TIERS, m -> buildMaterials(m, Materials.ClayBrick, 8L, Materials.Copper, 2L)));
        MACHINE_DEFS.add(new MachineDef("roaster",        "HU", HU_TIERS, m -> buildMaterials(m, Materials.ClayBrick, 4L, Materials.Copper, 2L)));
        MACHINE_DEFS.add(new MachineDef("distillery",     "HU", HU_TIERS, m -> buildMaterials(m, Materials.Bronze, 4L, Materials.Copper, 2L)));
        MACHINE_DEFS.add(new MachineDef("extruder",       "HU", HU_TIERS, m -> buildMaterials(m, Materials.Steel, 4L, Materials.Copper, 2L)));
        MACHINE_DEFS.add(new MachineDef("smelter",        "HU", HU_TIERS, m -> buildMaterials(m, Materials.ClayBrick, 6L, Materials.Copper, 1L)));
        MACHINE_DEFS.add(new MachineDef("crystallisationcrucible", "HU", HU_TIERS, m -> buildMaterials(m, Materials.ClayBrick, 4L, Materials.Steel, 2L)));
        MACHINE_DEFS.add(new MachineDef("dryer",          "HU", HU_TIERS, m -> buildMaterials(m, Materials.ClayBrick, 4L, Materials.Copper, 2L)));
        MACHINE_DEFS.add(new MachineDef("laminator",      "HU", HU_TIERS, m -> buildMaterials(m, Materials.Steel, 4L, Materials.Copper, 1L)));
        MACHINE_DEFS.add(new MachineDef("catalyticcracker", "HU", HU_TIERS, m -> buildMaterials(m, Materials.Steel, 6L, Materials.Copper, 3L)));
        MACHINE_DEFS.add(new MachineDef("steamcracker",   "HU", HU_TIERS, m -> buildMaterials(m, Materials.Steel, 6L, Materials.Copper, 2L)));

        // RU machines (4 tiers: Bronze, Steel, Ti, TungstenSteel)
        MACHINE_DEFS.add(new MachineDef("shredder",       "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 4L)));
        MACHINE_DEFS.add(new MachineDef("lathe",          "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 4L, Materials.Copper, 1L)));
        MACHINE_DEFS.add(new MachineDef("buzzsaw",        "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 4L)));
        MACHINE_DEFS.add(new MachineDef("centrifuge",     "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 4L, Materials.Copper, 2L)));
        MACHINE_DEFS.add(new MachineDef("rollingmill",    "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 4L)));
        MACHINE_DEFS.add(new MachineDef("rollbender",     "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 2L)));
        MACHINE_DEFS.add(new MachineDef("rollformer",     "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 2L)));
        MACHINE_DEFS.add(new MachineDef("clustermill",    "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 6L)));
        MACHINE_DEFS.add(new MachineDef("wiremill",       "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 4L)));
        MACHINE_DEFS.add(new MachineDef("mixer",          "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 2L)));
        MACHINE_DEFS.add(new MachineDef("loom",           "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 2L, Materials.Copper, 1L)));
        MACHINE_DEFS.add(new MachineDef("sluice",         "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 2L)));
        MACHINE_DEFS.add(new MachineDef("sander",         "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 2L)));
        MACHINE_DEFS.add(new MachineDef("burnmixer",      "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 2L)));
        MACHINE_DEFS.add(new MachineDef("debarker",       "RU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 2L)));

        // KU machines (4 tiers: Bronze, Steel, Ti, TungstenSteel)
        MACHINE_DEFS.add(new MachineDef("crusher",        "KU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 4L)));
        MACHINE_DEFS.add(new MachineDef("sifter",         "KU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 2L)));
        MACHINE_DEFS.add(new MachineDef("squeezer",       "KU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 4L, Materials.Copper, 1L)));
        MACHINE_DEFS.add(new MachineDef("compressor",     "KU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 4L)));
        MACHINE_DEFS.add(new MachineDef("press",          "KU", RU_KU_TIERS, m -> buildMaterials(m, Materials.Steel, 4L)));

        // EU machines (5 tiers: SteelGalvanized, Al, StainlessSteel, Cr, Ti)
        MACHINE_DEFS.add(new MachineDef("electrolyzer",   "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("canner",         "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 2L)));
        MACHINE_DEFS.add(new MachineDef("injector",       "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("printer",        "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 2L, Materials.Plastic, 2L)));
        MACHINE_DEFS.add(new MachineDef("scannervisuals", "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L, Materials.Glass, 2L)));
        MACHINE_DEFS.add(new MachineDef("autocrafter",    "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 2L)));
        MACHINE_DEFS.add(new MachineDef("electricmixer",  "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L, Materials.Steel, 2L)));
        MACHINE_DEFS.add(new MachineDef("electricloom",   "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("electricsifter", "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("slicer",         "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L, Materials.Steel, 2L)));
        MACHINE_DEFS.add(new MachineDef("nanofab",        "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 6L, Materials.Steel, 4L)));
        MACHINE_DEFS.add(new MachineDef("plantalyzer",    "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L, Materials.Glass, 2L)));
        MACHINE_DEFS.add(new MachineDef("bumblelyzer",    "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L, Materials.Glass, 2L)));
        MACHINE_DEFS.add(new MachineDef("boxinator",      "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 2L)));
        MACHINE_DEFS.add(new MachineDef("unboxinator",    "EU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 2L)));

        // MU machines (5 tiers)
        MACHINE_DEFS.add(new MachineDef("polarizer",      "MU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L, Materials.Steel, 2L)));
        MACHINE_DEFS.add(new MachineDef("magneticseparator", "MU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 6L, Materials.Steel, 4L)));

        // LU machines (5 tiers)
        MACHINE_DEFS.add(new MachineDef("laserengraver",  "LU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L, Materials.Glass, 2L)));
        MACHINE_DEFS.add(new MachineDef("laserwelder",    "LU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L, Materials.Steel, 2L)));

        // CU machines (5 tiers)
        MACHINE_DEFS.add(new MachineDef("freezer",        "CU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("cryomixer",      "CU", EU_MU_LU_CU_TIERS, m -> buildMaterials(m, Materials.Copper, 4L, Materials.Steel, 2L)));

        // QU machines — Osmium color only
        MACHINE_DEFS.add(new MachineDef("massfab",        "QU", new GTMaterial[]{Materials.OsmiumElemental}, m -> buildMaterials(m, Materials.Copper, 8L, Materials.Steel, 8L)));
        // GT6 registers the Molecular Scanner as T3 only (Loader_MultiTileEntities:1549-1553; T1/T2/
        // T4/T5 are commented out) and its NBT_INPUT of 512 is exactly the scanner recipe's power
        // (RecipeMapScannerMolecular), so the single port machine is tier 3.
        MACHINE_DEFS.add(new MachineDef("scannermolecular", "QU", new GTMaterial[]{Materials.OsmiumElemental}, m -> buildMaterials(m, Materials.Copper, 6L, Materials.Glass, 2L), 3));
        MACHINE_DEFS.add(new MachineDef("replicator",     "QU", new GTMaterial[]{Materials.OsmiumElemental}, m -> buildMaterials(m, Materials.Copper, 8L, Materials.Steel, 8L)));

        // Single-tier machines (StainlessSteel color)
        GTMaterial[] SS = {Materials.StainlessSteel};
        MACHINE_DEFS.add(new MachineDef("autoclave",      "TU", SS, m -> buildMaterials(m, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("bath",           "TU", SS, m -> buildMaterials(m, Materials.Copper, 2L)));
        MACHINE_DEFS.add(new MachineDef("generifier",     "TU", SS, m -> buildMaterials(m, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("coagulator",     "TU", SS, m -> buildMaterials(m, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("fermenter",      "HU", SS, m -> buildMaterials(m, Materials.ClayBrick, 4L, Materials.Copper, 2L)));
        MACHINE_DEFS.add(new MachineDef("melter",         "HU", SS, m -> buildMaterials(m, Materials.ClayBrick, 8L)));
        MACHINE_DEFS.add(new MachineDef("cokeoven",       "HU", SS, m -> buildMaterials(m, Materials.ClayBrick, 8L)));
        MACHINE_DEFS.add(new MachineDef("lightning",      "EU", SS, m -> buildMaterials(m, Materials.Copper, 8L)));
        MACHINE_DEFS.add(new MachineDef("implosioncompressor", "HU", SS, m -> buildMaterials(m, Materials.Steel, 8L)));
        MACHINE_DEFS.add(new MachineDef("fusionreactor",  "QU", SS, m -> buildMaterials(m, Materials.Copper, 12L, Materials.Steel, 12L)));
        MACHINE_DEFS.add(new MachineDef("cryodistillationtower", "CU", SS, m -> buildMaterials(m, Materials.Steel, 8L, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("distillationtower", "HU", SS, m -> buildMaterials(m, Materials.Steel, 8L, Materials.Copper, 4L)));

        // Large batch machines (GT6 single-tier, 512 GU/t; materials per original casing)
        MACHINE_DEFS.add(new MachineDef("largecentrifuge",   "RU", new GTMaterial[]{Materials.Tungstensteel}, m -> buildMaterials(m, Materials.Steel, 8L, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("largeelectrolyzer", "EU", SS, m -> buildMaterials(m, Materials.Copper, 8L)));
        MACHINE_DEFS.add(new MachineDef("largecoagulator",   "TU", SS, m -> buildMaterials(m, Materials.Copper, 8L)));
        MACHINE_DEFS.add(new MachineDef("largeautoclave",    "TU", SS, m -> buildMaterials(m, Materials.Copper, 8L)));
        MACHINE_DEFS.add(new MachineDef("largebath",         "TU", SS, m -> buildMaterials(m, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("largemixer",        "RU", SS, m -> buildMaterials(m, Materials.Steel, 8L)));
        MACHINE_DEFS.add(new MachineDef("largefermenter",    "HU", SS, m -> buildMaterials(m, Materials.ClayBrick, 8L, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("largeoven",         "EU", new GTMaterial[]{Materials.Invar}, m -> buildMaterials(m, Materials.Copper, 4L)));
        MACHINE_DEFS.add(new MachineDef("largesluice",       "RU", new GTMaterial[]{Materials.Titanium}, m -> buildMaterials(m, Materials.Steel, 8L)));
        MACHINE_DEFS.add(new MachineDef("largecrusher",      "RU", new GTMaterial[]{Materials.Tungstensteel}, m -> buildMaterials(m, Materials.Steel, 8L)));
        MACHINE_DEFS.add(new MachineDef("largeshredder",     "RU", new GTMaterial[]{Materials.Tungstensteel}, m -> buildMaterials(m, Materials.Steel, 8L)));
        MACHINE_DEFS.add(new MachineDef("largesqueezer",     "RU", new GTMaterial[]{Materials.Steel}, m -> buildMaterials(m, Materials.Steel, 8L, Materials.Copper, 2L)));
        MACHINE_DEFS.add(new MachineDef("largemassfab",      "QU", new GTMaterial[]{Materials.Lead}, m -> buildMaterials(m, Materials.Copper, 12L, Materials.Steel, 12L)));
    }

    // ── Material short names for ID generation ──────────────────────────────

    private static final Map<GTMaterial, String> MAT_SHORT = new HashMap<>();
    static {
        MAT_SHORT.put(Materials.Steel, "steel");
        MAT_SHORT.put(Materials.Invar, "invar");
        MAT_SHORT.put(Materials.Titanium, "titanium");
        MAT_SHORT.put(Materials.TungstenCarbide, "tungsten_carbide");
        MAT_SHORT.put(Materials.Bronze, "bronze");
        MAT_SHORT.put(Materials.Tungstensteel, "tungsten_steel");
        MAT_SHORT.put(Materials.SteelGalvanized, "galvanized_steel");
        MAT_SHORT.put(Materials.Aluminium, "aluminium");
        MAT_SHORT.put(Materials.StainlessSteel, "stainless_steel");
        MAT_SHORT.put(Materials.Chromium, "chromium");
        MAT_SHORT.put(Materials.OsmiumElemental, "osmium");
    }

    // ── Batch registration ──────────────────────────────────────────────────

    /** Base energy rate (grade-0 nominal) per machine type. Defaults to 32. */
    private static long energyBase(String machineName) {
        return switch (machineName) {
            // GT6 large batch machines run at 512 GU/t nominal
            case "largecentrifuge", "largeelectrolyzer", "largecoagulator", "largeautoclave",
                 "largebath", "largemixer", "largefermenter", "largeoven", "largesluice",
                 "largecrusher", "largeshredder", "largesqueezer" -> 512;
            case "largemassfab" -> 1;
            // Most machines: 32 base → 32/128/512/2048/8192
            default -> 32;
        };
    }

    /** Build a complete, validated batch before registering any blocks. */
    public static List<BasicMachineParameters> specifications() {
        List<BasicMachineParameters> result = new ArrayList<>();
        for (MachineDef def : MACHINE_DEFS) {
            if (MULTIBLOCK_ONLY.contains(def.name())) continue;
            long baseEnergy = energyBase(def.name());
            for (int t = 0; t < def.tiers().length; t++) {
                GTMaterial mat = def.tiers()[t];
                String matName = MAT_SHORT.getOrDefault(mat, mat.getName().toLowerCase(java.util.Locale.ROOT));
                String id = def.name() + "_" + matName;
                int tierNumber = t + def.tierBase();
                if(def.name().equals("massfab"))mat=com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Osmiridium");
                // Original GT6 per-machine/per-tier registration values; the synthetic
                // tables below remain only as the fallback for machines GT6 has no
                // "Basic Machines" row for (the large* batch machines).
                var original = com.gregtech.gregtech.data.BasicMachineOriginalParams.find(def.name(), tierNumber);
                float[] stats = original != null
                        ? new float[]{original.hardness(), original.resistance()}
                        : STAT_OVERRIDES.getOrDefault(def.name(), TIER_STATS[Math.min(t, TIER_STATS.length - 1)]);
                String energyName = original != null ? original.energyType() : def.energy();
                long energyIn = original != null ? original.energyInput()
                        : (def.energy().equals("TU") ? 1 : baseEnergy << (2 * t)); // x4 per tier
                int parallel = original != null ? original.parallel()
                        : BasicMachineParameters.legacyParallelLimit(def.name());
                List<WeightedMaterial> materials = def.materials() != null
                        ? def.materials().apply(mat) : List.of(new WeightedMaterial(mat, 8));
                var energyType = com.gregtech.gregtech.data.GregTechTags.Energy.ALL.stream()
                        .filter(type -> type.getShortName().equals(energyName)).findFirst().orElseThrow();
                BasicMachineParameters spec = BasicMachineParameters.builder(id, mat)
                        .machineType(def.name())

                        .energy(energyType, energyIn).tier(tierNumber)
                        // GT6 NBT_INPUT_MIN/MAX; without an explicit range GT6 itself uses
                        // input/2..input*2 (MultiTileEntityBasicMachine.readFromNBT2).
                        .energyRange(original != null ? original.energyInputMin()
                                        : BasicMachineParameters.defaultEnergyInMin(energyName, energyIn),
                                original != null ? original.energyInputMax()
                                        : BasicMachineParameters.defaultEnergyInMax(energyName, energyIn))
                        .parallel(parallel)
                        .strength(def.name().equals("massfab")?16:stats[0], def.name().equals("massfab")?16:stats[1]).faces(defaultMachineFaceMasks(def.name()))
                        .constructionMaterials(materials).build();
                result.add(spec);
            }
        }
        // Tiers GT6 registers with the same Osmiridium casing as tier 1, so the per-machine tier
        // array above cannot express them (massfab T2..T5, replicator T2..T5).
        for (var extra : EXTRA_TIERS.entrySet()) {
            String name = extra.getKey();
            for (int tier : extra.getValue()) {
                var original = com.gregtech.gregtech.data.BasicMachineOriginalParams.find(name, tier);
                String energyName = original != null ? original.energyType() : "QU";
                long energyIn = original != null ? original.energyInput() : 32L << (2 * (tier - 1));
                var energyType = com.gregtech.gregtech.data.GregTechTags.Energy.ALL.stream()
                        .filter(type -> type.getShortName().equals(energyName)).findFirst().orElseThrow();
                result.add(BasicMachineParameters.builder(name + "_osmiridium_t" + tier,
                        com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Osmiridium"))
                        .machineType(name)

                        .energy(energyType, energyIn).tier(tier)
                        .parallel(original != null ? original.parallel() : 1)
                        .energyRange(original != null ? original.energyInputMin() : energyIn,
                                original != null ? original.energyInputMax() : energyIn)
                        .strength(original != null ? original.hardness() : 16,
                                original != null ? original.resistance() : 16)
                        .faces(defaultMachineFaceMasks(name)).build());
            }
        }
        return com.gregtech.gregtech.api.definition.DefinitionCatalog.validated(result, BasicMachineParameters::id);
    }

    /** Per-machine MachineFaceMasks matching original GT6 {@code Loader_MultiTileEntities} NBT side bitmasks.
     *  Uses machine-relative directions: TOP/BOTTOM/LEFT/RIGHT/FRONT/BACK.
     *  Auto I/O pulls from / pushes to the adjacent block on the specified relative face
     *  every server tick when enabled. */
    private static MachineFaceMasks defaultMachineFaceMasks(String machineName) {
        // ── HU heat machines ──────────────────────────────────────────
        return switch (machineName) {
            case "oven" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            case "roaster" -> MachineFaceMasks.builder()
                    .itemIn(BACK, LEFT).itemOut(RIGHT)
                    .fluidIn(BACK, LEFT).fluidOut(TOP)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(TOP).build();

            case "distillery" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(RIGHT)
                    .fluidIn(TOP, LEFT).fluidOut(BACK)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(BACK).build();

            case "extruder" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            case "smelter" -> MachineFaceMasks.builder()
                    .itemIn(TOP).itemOut(LEFT)
                    .fluidIn(TOP).fluidOut(RIGHT)
                    .energyIn(BOTTOM)
                    .itemAutoIn(TOP).itemAutoOut(LEFT)
                    .fluidAutoIn(TOP).fluidAutoOut(RIGHT).build();

            case "crystallisationcrucible" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, BACK, TOP).itemOut(RIGHT)
                    .fluidIn(LEFT, BACK, TOP)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).build();

            case "dryer" -> MachineFaceMasks.builder()
                    .itemIn(BACK, LEFT).itemOut(RIGHT)
                    .fluidIn(BACK, LEFT).fluidOut(TOP)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(TOP).build();

            case "laminator" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, TOP).itemOut(RIGHT)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            case "catalyticcracker", "steamcracker" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(RIGHT, BACK)
                    .fluidIn(TOP, LEFT).fluidOut(RIGHT, BACK)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            // ── RU rotation machines ────────────────────────────────────
            case "shredder" -> MachineFaceMasks.builder()
                    .itemIn(TOP).itemOut(BOTTOM)
                    .energyIn(LEFT, RIGHT)
                    .itemAutoIn(TOP).itemAutoOut(BOTTOM).build();

            case "lathe" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            case "buzzsaw" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT)
                    .fluidIn(TOP, BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(TOP).build();

            case "centrifuge" -> MachineFaceMasks.builder()
                    .itemIn(TOP).itemOut(RIGHT)
                    .fluidIn(TOP).fluidOut(LEFT)
                    .energyIn(BOTTOM)
                    .itemAutoIn(TOP).itemAutoOut(RIGHT)
                    .fluidAutoIn(TOP).fluidAutoOut(LEFT).build();

            case "rollingmill", "rollbender", "rollformer", "clustermill", "wiremill" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            case "mixer", "burnmixer" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, TOP).itemOut(RIGHT, BACK)
                    .fluidIn(LEFT, TOP).fluidOut(RIGHT, BACK)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            case "loom" -> MachineFaceMasks.builder()
                    .itemIn(TOP).itemOut(BOTTOM)
                    .energyIn(LEFT, RIGHT)
                    .itemAutoIn(TOP).itemAutoOut(BOTTOM).build();

            case "sluice" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, TOP).itemOut(RIGHT, BOTTOM)
                    .fluidIn(LEFT, TOP).fluidOut(RIGHT, BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            case "sander" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT)
                    .energyIn(TOP)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            case "debarker" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT)
                    .fluidIn(TOP, BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(TOP).build();

            // ── KU kinetic machines ─────────────────────────────────────
            case "crusher", "sifter" -> MachineFaceMasks.builder()
                    .itemIn(TOP).itemOut(BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(TOP).itemAutoOut(BOTTOM).build();

            case "squeezer" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT)
                    .fluidOut(BOTTOM)
                    .energyIn(TOP)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoOut(BOTTOM).build();

            case "compressor" -> MachineFaceMasks.builder()
                    .itemIn(TOP).itemOut(BOTTOM)
                    .energyIn(LEFT)
                    .itemAutoIn(TOP).itemAutoOut(BOTTOM).build();

            case "press" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT)
                    .energyIn(TOP)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            // ── EU electric machines ────────────────────────────────────
            case "electrolyzer" -> MachineFaceMasks.builder()
                    .itemIn(TOP, FRONT, BACK).itemOut(RIGHT, LEFT)
                    .fluidIn(TOP, FRONT, BACK).fluidOut(RIGHT, LEFT)
                    .energyIn(BOTTOM)
                    .itemAutoIn(TOP).itemAutoOut(RIGHT)
                    .fluidAutoIn(TOP).fluidAutoOut(RIGHT).build();

            case "canner" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(RIGHT, BOTTOM)
                    .fluidIn(TOP, LEFT).fluidOut(RIGHT, BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            case "injector" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(RIGHT, BOTTOM)
                    .fluidIn(TOP, LEFT).fluidOut(RIGHT, BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            case "printer" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(RIGHT, BOTTOM)
                    .fluidIn(TOP, LEFT)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).build();

            case "scannervisuals" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(RIGHT, BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            case "autocrafter" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(RIGHT, BOTTOM)
                    .energyIn(TOP, BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            case "electricmixer" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, TOP).itemOut(RIGHT, BACK)
                    .fluidIn(LEFT, TOP).fluidOut(RIGHT, BACK)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            case "electricloom" -> MachineFaceMasks.builder()
                    .itemIn(TOP).itemOut(BOTTOM)
                    .energyIn(LEFT, RIGHT)
                    .itemAutoIn(TOP).itemAutoOut(BOTTOM).build();

            case "electricsifter" -> MachineFaceMasks.builder()
                    .itemIn(TOP).itemOut(BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(TOP).itemAutoOut(BOTTOM).build();

            case "slicer" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, TOP).itemOut(RIGHT, BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            case "nanofab" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, TOP).itemOut(RIGHT, BOTTOM)
                    .fluidIn(LEFT, TOP).fluidOut(RIGHT, BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            case "plantalyzer", "bumblelyzer" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(RIGHT, BOTTOM)
                    .fluidIn(TOP, BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(TOP).build();

            case "boxinator", "unboxinator" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, TOP).itemOut(RIGHT)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            // ── MU magnetic machines ────────────────────────────────────
            case "polarizer" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT)
                    .energyIn(TOP, BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            case "magneticseparator" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT, BOTTOM)
                    .fluidIn(LEFT).fluidOut(RIGHT, BOTTOM)
                    .energyIn(TOP)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            // ── LU laser machines ───────────────────────────────────────
            case "laserengraver" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT)
                    .energyIn(TOP)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            case "laserwelder" -> MachineFaceMasks.builder()
                    .itemIn(LEFT).itemOut(RIGHT)
                    .fluidIn(BOTTOM, LEFT)
                    .energyIn(TOP)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).build();

            // ── CU cryo machines ────────────────────────────────────────
            case "freezer" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(RIGHT, BOTTOM)
                    .fluidIn(TOP, LEFT).fluidOut(RIGHT, BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            case "cryomixer" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, TOP).itemOut(RIGHT, BACK)
                    .fluidIn(LEFT, TOP).fluidOut(RIGHT, BACK)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            // ── QU quantum machines ─────────────────────────────────────
            case "massfab" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, TOP).itemOut(BOTTOM, RIGHT)
                    .fluidIn(LEFT, TOP).fluidOut(BOTTOM, RIGHT)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(TOP).fluidAutoOut(BOTTOM).build();

            case "replicator" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, TOP).itemOut(BOTTOM, RIGHT)
                    .fluidIn(LEFT, TOP).fluidOut(BOTTOM, RIGHT)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            case "scannermolecular" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, TOP).itemOut(BOTTOM, RIGHT)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            // ── TU single-tier machines ─────────────────────────────────
            case "autoclave" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(BACK, RIGHT)
                    .fluidIn(BOTTOM, LEFT).fluidOut(BACK, RIGHT)
                    .energyIn(BOTTOM, TOP, LEFT,
                              FRONT, RIGHT, BACK)
                    .itemAutoIn(LEFT).itemAutoOut(BACK)
                    .fluidAutoIn(LEFT).fluidAutoOut(BACK).build();

            case "bath", "generifier" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(BOTTOM, RIGHT)
                    .fluidIn(TOP, LEFT).fluidOut(BOTTOM, RIGHT)
                    .energyIn(BOTTOM, TOP, LEFT,
                              FRONT, RIGHT, BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            case "coagulator" -> MachineFaceMasks.builder()
                    .itemOut(BOTTOM, RIGHT)
                    .fluidIn(TOP, LEFT)
                    .energyIn(BOTTOM, TOP, LEFT,
                              FRONT, RIGHT, BACK)
                    .itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).build();

            case "fermenter" -> MachineFaceMasks.builder()
                    .itemIn(BACK, LEFT).itemOut(RIGHT)
                    .fluidIn(BACK, LEFT).fluidOut(TOP)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(TOP).build();

            case "melter" -> MachineFaceMasks.builder()
                    .itemIn(TOP).itemOut(LEFT)
                    .fluidIn(TOP).fluidOut(RIGHT)
                    .energyIn(BOTTOM)
                    .itemAutoIn(TOP).itemAutoOut(LEFT)
                    .fluidAutoIn(TOP).fluidAutoOut(RIGHT).build();

            // ── special / multiblock machines ───────────────────────────
            case "cokeoven" -> MachineFaceMasks.builder()
                    .itemIn(TOP).itemOut(BOTTOM)
                    .fluidOut(BOTTOM, TOP, LEFT,
                              FRONT, RIGHT, BACK)
                    .energyIn(BOTTOM, TOP, LEFT,
                              FRONT, RIGHT, BACK)
                    .itemAutoIn(TOP).itemAutoOut(BOTTOM)
                    .fluidAutoOut(BOTTOM).build();

            case "lightning" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(RIGHT, BOTTOM)
                    .fluidIn(TOP, LEFT).fluidOut(RIGHT, BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT)
                    .fluidAutoIn(LEFT).fluidAutoOut(RIGHT).build();

            case "implosioncompressor" -> MachineFaceMasks.builder()
                    .itemIn(TOP).itemOut(BOTTOM)
                    .energyIn(BOTTOM, TOP, LEFT,
                              FRONT, RIGHT, BACK)
                    .itemAutoIn(TOP).itemAutoOut(BOTTOM).build();

            case "fusionreactor" -> MachineFaceMasks.builder()
                    .itemIn(LEFT, TOP).itemOut(RIGHT, BOTTOM)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(RIGHT).build();

            case "cryodistillationtower" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(BACK)
                    .fluidOut(BACK)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(BACK)
                    .fluidAutoOut(BACK).build();

            case "distillationtower" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT).itemOut(BACK)
                    .fluidOut(BACK)
                    .energyIn(BOTTOM)
                    .itemAutoIn(LEFT).itemAutoOut(BACK)
                    .fluidAutoOut(BACK).build();

            case "largemassfab" -> MachineFaceMasks.builder()
                    .itemIn(TOP,LEFT,FRONT,BACK,RIGHT).itemOut(BOTTOM)
                    .fluidIn(TOP,LEFT,FRONT,BACK,RIGHT).fluidOut(BOTTOM)
                    .energyIn(TOP,LEFT,FRONT,BACK,RIGHT,BOTTOM)
                    .itemAutoOut(BOTTOM).fluidAutoOut(BOTTOM).build();

            // ── large batch machines: GT6 auto-output bottom, inputs around ──
            case "largecentrifuge", "largemixer", "largesluice", "largecrusher",
                 "largeshredder", "largesqueezer" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT, FRONT).itemOut(BOTTOM, RIGHT)
                    .fluidIn(TOP, LEFT, FRONT).fluidOut(BOTTOM, RIGHT)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(BOTTOM)
                    .fluidAutoIn(LEFT).fluidAutoOut(BOTTOM).build();

            case "largeelectrolyzer", "largeoven", "largefermenter" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT, FRONT).itemOut(BOTTOM, RIGHT)
                    .fluidIn(TOP, LEFT, FRONT).fluidOut(BOTTOM, RIGHT)
                    .energyIn(BACK)
                    .itemAutoIn(LEFT).itemAutoOut(BOTTOM)
                    .fluidAutoIn(LEFT).fluidAutoOut(BOTTOM).build();

            case "largecoagulator", "largeautoclave", "largebath" -> MachineFaceMasks.builder()
                    .itemIn(TOP, LEFT, FRONT).itemOut(BOTTOM, RIGHT)
                    .fluidIn(TOP, LEFT, FRONT).fluidOut(BOTTOM, RIGHT)
                    .energyIn(BOTTOM, TOP, LEFT, FRONT, RIGHT, BACK)
                    .itemAutoIn(LEFT).itemAutoOut(BOTTOM)
                    .fluidAutoIn(LEFT).fluidAutoOut(BOTTOM).build();

            default -> MachineFaceMasks.ALL_SIDES;
        };
    }
}
