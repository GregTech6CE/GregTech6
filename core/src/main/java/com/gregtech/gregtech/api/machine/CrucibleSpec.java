package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialMass;

/**
 * GT6 {@code MultiTileEntitySmeltery} hull parameters ({@code Loader_MultiTileEntities#crucible}).
 */
public record CrucibleSpec(
        String id,
        GTMaterial material,
        int gt6MetaId,
        int meltingPointK,
        int boilingPointK,
        double hullDensity,
        float hardness,
        float blastResistance,
        boolean acidProof,
        long hullMaterialUnits
) {
    /** Default crucible hull weight in U. Stone crucible uses 63. */
    public static final long DEFAULT_CRUCIBLE_HULL_UNITS = 7L * GTValues.U;
    public static final long STONE_CRUCIBLE_HULL_UNITS = 63L * GTValues.U;

    /** Companion block hull weights in U. */
    public static final long MOLD_HULL_UNITS = 5L * GTValues.U;
    public static final long BASIN_HULL_UNITS = 5L * GTValues.U;
    public static final long CROSSING_HULL_UNITS = 5L * GTValues.U;
    public static final long FAUCET_HULL_UNITS = 3L * GTValues.U;

    public static final double HEAT_RESISTANCE_BONUS = 1.25D;
    public static final long KG_PER_ENERGY = 100L;
    public static final long MIN_HU_PER_TICK = 1L;

    /** True if this spec represents a stone-tier crucible (9x capacity). */
    public boolean isStoneTier() {
        return hullMaterialUnits >= STONE_CRUCIBLE_HULL_UNITS;
    }

    public int tintRgb() {
        return material.getColor();
    }

    public String materialName() {
        return material.getLocalName();
    }

    public long meltDownTemperatureK() {
        return Math.round(meltingPointK * HEAT_RESISTANCE_BONUS);
    }

    public double thermalMassKg() {
        return MaterialMass.kilograms(hullDensity, hullMaterialUnits);
    }

    /** GT6 {@code RM.CrucibleAlloying} — registered for future recipe logic. */
    public static CrucibleSpec of(String id, GTMaterial material, int gt6MetaId,
                                   float hardness, float blastResistance, boolean acidProof) {
        return of(id, material, gt6MetaId, hardness, blastResistance, acidProof, DEFAULT_CRUCIBLE_HULL_UNITS);
    }

    public static CrucibleSpec of(String id, GTMaterial material, int gt6MetaId,
                                   float hardness, float blastResistance, boolean acidProof,
                                   int meltingPointK, int boilingPointK, double hullDensity) {
        return of(id, material, gt6MetaId, hardness, blastResistance, acidProof,
                meltingPointK, boilingPointK, hullDensity, DEFAULT_CRUCIBLE_HULL_UNITS);
    }

    public static CrucibleSpec of(String id, GTMaterial material, int gt6MetaId,
                                   float hardness, float blastResistance, boolean acidProof,
                                   long hullMaterialUnits) {
        return new CrucibleSpec(id, material, gt6MetaId,
                material.getMeltingPoint(), material.getBoilingPoint(), material.getDensity(),
                hardness, blastResistance, acidProof, hullMaterialUnits);
    }

    public static CrucibleSpec of(String id, GTMaterial material, int gt6MetaId,
                                   float hardness, float blastResistance, boolean acidProof,
                                   int meltingPointK, int boilingPointK, double hullDensity,
                                   long hullMaterialUnits) {
        return new CrucibleSpec(id, material, gt6MetaId, meltingPointK, boilingPointK, hullDensity,
                hardness, blastResistance, acidProof, hullMaterialUnits);
    }
}
