package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.api.machine.DieselEngineSpec;
import com.gregtech.gregtech.api.machine.ElectricEngineSpec;
import com.gregtech.gregtech.api.machine.FluxEngineSpec;
import com.gregtech.gregtech.api.machine.SteamEngineSpec;
import com.gregtech.gregtech.api.machine.StrongSteamEngineSpec;
import com.gregtech.gregtech.api.machine.RotationEngineSpec;
import com.gregtech.gregtech.api.material.GTMaterial;

import java.util.HashMap;
import java.util.Map;

/**
 * KU generator engine batch registration.
 * <p>
 * All engines are 6-way-rotatable blocks with no GUI.
 * They convert input energy (EU / RF / Steam / RU) → KU output.
 */
public final class GTEngines {
    private GTEngines() {}

    private static final Map<GTMaterial, String> MAT = new HashMap<>();
    static {
        MAT.put(Materials.SteelGalvanized, "galvanized_steel");
        MAT.put(Materials.Aluminium, "aluminium");
        MAT.put(Materials.StainlessSteel, "stainless_steel");
        MAT.put(Materials.Chromium, "chromium");
        MAT.put(Materials.Titanium, "titanium");
        MAT.put(Materials.Lead, "lead");
        MAT.put(Materials.Invar, "invar");
        MAT.put(Materials.Electrum, "electrum");
        MAT.put(Materials.EnderiumBase, "enderium_base");
        MAT.put(Materials.Enderium, "enderium");
        MAT.put(Materials.Bronze, "bronze");
        MAT.put(Materials.Steel, "steel");
        MAT.put(Materials.Tungstensteel, "tungsten_steel");
        MAT.put(Materials.TinAlloy, "tin_alloy");
        MAT.put(Materials.ArsenicCopper, "arsenic_copper");
        MAT.put(Materials.ArsenicBronze, "arsenic_bronze");
        MAT.put(Materials.Brass, "brass");
        MAT.put(Materials.Ironwood, "ironwood");
        MAT.put(Materials.FierySteel, "fiery_steel");
        MAT.put(Materials.Tungsten, "tungsten");
        MAT.put(Materials.Iridium, "iridium");
    }

    // ── Tier arrays ────────────────────────────────────────────────────────

    /** Electric engine tiers (EU → KU): SteelGalvanized, Al, StainlessSteel, Cr, Ti */
    private static final GTMaterial[] ELECTRIC_TIERS = {Materials.SteelGalvanized, Materials.Aluminium, Materials.StainlessSteel, Materials.Chromium, Materials.Titanium};
    private static final long[] ELECTRIC_INPUTS   = {32, 128, 512, 2048, 8192};
    private static final long[] ELECTRIC_OUTPUTS  = {16, 64, 256, 1024, 4096};
    private static final int[]  ELECTRIC_TIER_NUM = {1, 2, 3, 4, 5};

    /** Flux engine tiers (RF → KU): Pb, Invar, Electrum, EnderiumBase, Enderium */
    private static final GTMaterial[] FLUX_TIERS = {Materials.Lead, Materials.Invar, Materials.Electrum, Materials.EnderiumBase, Materials.Enderium};
    private static final long[] FLUX_INPUTS   = {128, 512, 2048, 8192, 32768};
    private static final long[] FLUX_OUTPUTS  = {16, 64, 256, 1024, 4096};
    private static final int[]  FLUX_TIER_NUM = {1, 2, 3, 4, 5};

    /** GT6 kinetic() 24777–24897: one RU → KU engine for each original axle material. */
    private record RotationTier(String id, GTMaterial material, int voltageTier, long input) {}
    private static final RotationTier[] ROTATION_TIERS = {
            new RotationTier("wood", WoodMaterials.WoodTreated, 0, 8),                   // 24807
            new RotationTier("bronze", Materials.Bronze, 1, 32),                         // 24817
            new RotationTier("brass", Materials.Brass, 1, 32),                           // 24777
            new RotationTier("arsenic_copper", Materials.ArsenicCopper, 1, 32),          // 24787
            new RotationTier("arsenic_bronze", Materials.ArsenicBronze, 1, 32),          // 24797
            new RotationTier("steel", Materials.Steel, 2, 128),                         // 24827
            new RotationTier("titanium", Materials.Titanium, 3, 512),                    // 24837
            new RotationTier("tungsten_steel", Materials.Tungstensteel, 4, 2048),        // 24847 (existing ID)
            new RotationTier("iridium", Materials.Iridium, 5, 8192),                    // 24857
            new RotationTier("iritanium", Materials.TitaniumIridium, 6, 32768),         // 24867
            new RotationTier("trinitanium", Materials.Trinitanium, 7, 131072),          // 24877
            new RotationTier("trinaquadalloy", Materials.Trinaquadalloy, 8, 524288),    // 24887
            new RotationTier("adamantium", Materials.Adamantium, 9, 2097152),           // 24897
    };

    // ── Diesel engine data (from original GT6 Loader_MultiTileEntities) ──────

    /** Diesel engines (liquid fuel → KU): Bronze, ArsenicCopper, ArsenicBronze, Steel, Invar, Ti, TungstenSteel, Ir */
    private static final GTMaterial[] DIESEL_MATS = {
            Materials.Bronze, Materials.ArsenicCopper, Materials.ArsenicBronze, Materials.Steel,
            Materials.Invar, Materials.Titanium, Materials.Tungstensteel, Materials.Iridium
    };
    private static final long[] DIESEL_OUTPUTS = {16, 16, 24, 32, 64, 128, 256, 512};
    private static final int[]  DIESEL_TIERS = {0, 0, 0, 1, 1, 2, 3, 4};

    // ── Steam engine data (from original GT6 Loader_MultiTileEntities) ──────

    /** Regular steam engines: springSmall + plateDouble */
    private static final GTMaterial[] STEAM_MATS = {
            Materials.Lead, Materials.TinAlloy, Materials.Bronze, Materials.ArsenicCopper, Materials.ArsenicBronze,
            Materials.Brass, Materials.Invar, Materials.Ironwood, Materials.Steel, Materials.FierySteel,
            Materials.Chromium, Materials.Titanium, Materials.Tungsten, Materials.Tungstensteel
    };
    private static final int[] STEAM_EFFICIENCIES = {
            3000, 4000, 5000, 5000, 5000,
            5000, 6400, 6450, 5000, 6200,
            6300, 5800, 5800, 6000
    };
    private static final int[] STEAM_CAPACITIES = {
            16000, 20000, 24000, 24000, 28000,
            24000, 16000, 16000, 32000, 64000,
            96000, 112000, 128000, 128000
    };
    private static final long[] STEAM_OUTPUTS = {
            16, 20, 24, 24, 28,
            24, 16, 16, 32, 64,
            96, 112, 128, 128
    };
    private static final float[] STEAM_HARDNESS = {
            4.0F, 4.0F, 7.0F, 7.0F, 7.0F,
            7.0F, 4.0F, 4.0F, 6.0F, 7.0F,
            4.0F, 9.0F, 10.0F, 12.5F
    };
    private static final float[] STEAM_BLAST_RESISTANCE = {
            4.0F, 4.0F, 7.0F, 7.0F, 7.0F,
            7.0F, 4.0F, 4.0F, 6.0F, 7.0F,
            4.0F, 9.0F, 10.0F, 12.5F
    };

    /** Strong steam engines (4× capacity/output of regular): spring + plateDense */
    private static final long[] STRONG_STEAM_OUTPUTS = {
            64, 80, 96, 96, 112,
            96, 64, 64, 128, 256,
            384, 448, 512, 512
    };
    private static final int[] STRONG_STEAM_CAPACITIES = {
            64000, 80000, 96000, 96000, 112000,
            96000, 64000, 64000, 128000, 256000,
            384000, 448000, 512000, 512000
    };

    // ── Hardness / blast resistance per tier (lightweight engines) ─────────

    private static final float[] HARDNESS = {4.0F, 4.5F, 5.0F, 6.0F, 7.0F};
    private static final float[] BLAST_RESISTANCE = {4.0F, 4.5F, 5.0F, 6.0F, 7.0F};

    // ── Registration ───────────────────────────────────────────────────────

    public static void registerAll() {
        registerElectricEngines();
        registerFluxEngines();
        registerSteamEngines();
        registerStrongSteamEngines();
        registerRotationEngines();
        registerDieselEngines();
    }

    private static void registerElectricEngines() {
        for (int i = 0; i < ELECTRIC_TIERS.length; i++) {
            GTMaterial mat = ELECTRIC_TIERS[i];
            String matName = MAT.getOrDefault(mat, mat.getName().toLowerCase());
            String id = "engine_electric_" + matName;
            ElectricEngineSpec spec = new ElectricEngineSpec(
                    id, mat, ELECTRIC_TIER_NUM[i],
                    ELECTRIC_INPUTS[i], ELECTRIC_OUTPUTS[i],
                    HARDNESS[i], BLAST_RESISTANCE[i]);
            com.gregtech.gregtech.api.machine.MachineRegistry.registerElectricEngine(spec);
        }
    }

    private static void registerFluxEngines() {
        for (int i = 0; i < FLUX_TIERS.length; i++) {
            GTMaterial mat = FLUX_TIERS[i];
            String matName = MAT.getOrDefault(mat, mat.getName().toLowerCase());
            String id = "engine_flux_" + matName;
            FluxEngineSpec spec = new FluxEngineSpec(
                    id, mat, FLUX_TIER_NUM[i],
                    FLUX_INPUTS[i], FLUX_OUTPUTS[i],
                    HARDNESS[i], BLAST_RESISTANCE[i]);
            com.gregtech.gregtech.api.machine.MachineRegistry.registerFluxEngine(spec);
        }
    }

    private static void registerSteamEngines() {
        for (int i = 0; i < STEAM_MATS.length; i++) {
            GTMaterial mat = STEAM_MATS[i];
            String matName = MAT.getOrDefault(mat, mat.getName().toLowerCase());
            String id = "engine_steam_" + matName;
            SteamEngineSpec spec = new SteamEngineSpec(
                    id, mat, STEAM_EFFICIENCIES[i],
                    (int)(STEAM_OUTPUTS[i]*200), STEAM_OUTPUTS[i]/2,
                    STEAM_HARDNESS[i], STEAM_BLAST_RESISTANCE[i]);
            com.gregtech.gregtech.api.machine.MachineRegistry.registerSteamEngine(spec);
        }
    }

    private static void registerStrongSteamEngines() {
        for (int i = 0; i < STEAM_MATS.length; i++) {
            GTMaterial mat = STEAM_MATS[i];
            String matName = MAT.getOrDefault(mat, mat.getName().toLowerCase());
            String id = "engine_steam_strong_" + matName;
            StrongSteamEngineSpec spec = new StrongSteamEngineSpec(
                    id, mat, STEAM_EFFICIENCIES[i],
                    (int)(STRONG_STEAM_OUTPUTS[i]*200), STRONG_STEAM_OUTPUTS[i]/2,
                    STEAM_HARDNESS[i], STEAM_BLAST_RESISTANCE[i]);
            com.gregtech.gregtech.api.machine.MachineRegistry.registerStrongSteamEngine(spec);
        }
    }

    private static void registerRotationEngines() {
        for (RotationTier tier : ROTATION_TIERS) {
            String id = "engine_rotation_" + tier.id();
            RotationEngineSpec spec = new RotationEngineSpec(
                    id, tier.material(), tier.voltageTier(),
                    tier.input(), tier.input() / 2,
                    6.0F, 6.0F);
            com.gregtech.gregtech.api.machine.MachineRegistry.registerRotationEngine(spec);
        }
    }

    private static void registerDieselEngines() {
        for (int i = 0; i < DIESEL_MATS.length; i++) {
            GTMaterial mat = DIESEL_MATS[i];
            String matName = MAT.getOrDefault(mat, mat.getName().toLowerCase());
            String id = "engine_diesel_" + matName;
            DieselEngineSpec spec = new DieselEngineSpec(
                    id, mat, DIESEL_TIERS[i],
                    DIESEL_OUTPUTS[i], DIESEL_OUTPUTS[i],
                    6.0F, 6.0F);
            com.gregtech.gregtech.api.machine.MachineRegistry.registerDieselEngine(spec);
        }
    }
}
