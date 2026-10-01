package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.api.machine.crucible.ThermalStep;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.CompoundMaterials;
import com.gregtech.gregtech.data.MaterialGroups;
import com.gregtech.gregtech.data.ModReferences;

/** Fixed original registry inputs; runs against both original Git blobs and relocated records. */
public final class MachineSpecBehaviorContracts {
    private static int assertions;
    private MachineSpecBehaviorContracts() {}

    public static void main(String[] args) {
        // Bind before any domain holder; the probe uses the same domain graph as both loaders.
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gregtech"));
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        MaterialGroups.Glowstone.getClass();
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();

        brickHeater();
        ceramicCrucibleAndMold();
        chargedCrucibleHeatRequirement();
        System.out.println("Machine spec behavior contracts passed: " + assertions
                + " assertions; brick25percent/16HU, ceramic7U/2500K, mold5U, charged45HU/K; no game runtime");
    }

    private static void brickHeater() {
        // import/saltnya-snapshot GTMachines.java:79-80; MachineRegistry.java:84-88.
        var brick = CompoundMaterials.ClayBrick;
        var heater = new MachineSpec("burning_box_solid_brick", brick.getLocalName(), brick.getColor(),
                2500, 16, "burning_solid", 6.0F, 6.0F);
        check(heater.id().equals("burning_box_solid_brick"), "Original heater registry path");
        check(heater.materialName().equals("Brick") && heater.tintRgb() == 0xB75A40,
                "Original hull identity and tint");
        check(heater.efficiency() == 2500 && heater.efficiencyPercent() == 25.0F, "Original25percent efficiency");
        check(heater.outputRate() == 16, "Original16HU/t rate");
        check(heater.textureSet().equals("burning_solid"), "Original solid-fuel texture family");
        check(heater.hardness() == 6.0F && heater.blastResistance() == 6.0F, "Original brick block stats");
        var legacyCtor = new MachineSpec("legacy", "Brick", 0, 2500, 16, "burning_solid");
        check(legacyCtor.hardness() == 4.0F && legacyCtor.blastResistance() == 4.0F, "Retained six-argument constructor");
        throwsType(() -> new MachineSpec("invalid", "Brick", 0, -1, 16, "burning_solid"), "Negative efficiency rejected");
        throwsType(() -> new MachineSpec("invalid", "Brick", 0, 10001, 16, "burning_solid"), "Efficiency above100percent rejected");
        throwsType(() -> new MachineSpec("invalid", "Brick", 0, 2500, -1, "burning_solid"), "Negative output rejected");
        check(new MachineSpec("zero", "Brick", 0, 0, 0, "burning_solid").outputRate() == 0,
                "Original zero-output boundary retained");
    }

    private static void ceramicCrucibleAndMold() {
        // Original GTMachines.java:334-335 specifies hull density, not Ceramic's default density.
        var ceramic = CrucibleSpec.of("smelting_crucible_ceramic", Materials.Ceramic,
                1005, 5.0F, 5.0F, false, 2000, 4000, 0.8181818181818182D);
        check(ceramic.id().equals("smelting_crucible_ceramic") && ceramic.gt6MetaId() == 1005,
                "Original ceramic registry path and meta");
        check(ceramic.material() == GTMaterialRegistry.get(8225), "Same full shared material identity");
        check(ceramic.meltingPointK() == 2000 && ceramic.boilingPointK() == 4000, "Explicit hull Kelvin bounds");
        check(Double.doubleToLongBits(ceramic.hullDensity()) == Double.doubleToLongBits(0.8181818181818182D),
                "Explicit density retained bit-for-bit");
        check(ceramic.hullDensity() != Materials.Ceramic.getDensity(), "Do not substitute material density");
        check(ceramic.hullMaterialUnits() == 4540536000L && ceramic.hullMaterialUnits() == 7 * GTValues.U,
                "Original7U shell, not a144-unit replacement");
        check(ceramic.meltDownTemperatureK() == 2500, "Original round(2000*1.25) hull limit");
        check(ceramic.hardness() == 5.0F && ceramic.blastResistance() == 5.0F && !ceramic.acidProof(),
                "Original ceramic block and acid parameters");
        check(!ceramic.isStoneTier(), "Ceramic shell is not63U stone tier");
        near(ceramic.thermalMassKg(), 636.3636357272728D, "Independent original7U shell mass");

        // Original MachineRegistry.java:149-168 uses offset50 and the5U companion constant.
        var mold = new CrucibleSpec("mold_ceramic", ceramic.material(), ceramic.gt6MetaId() + 50,
                ceramic.meltingPointK(), ceramic.boilingPointK(), ceramic.hullDensity(),
                ceramic.hardness(), ceramic.blastResistance(), ceramic.acidProof(), CrucibleSpec.MOLD_HULL_UNITS);
        check(mold.id().equals("mold_ceramic") && mold.gt6MetaId() == 1055, "Original mold path and meta offset");
        check(mold.material() == ceramic.material() && mold.hullDensity() == ceramic.hullDensity(),
                "Mold companion retains the same material and explicit hull density");
        check(mold.hullMaterialUnits() == 3243240000L && mold.hullMaterialUnits() == 5 * GTValues.U,
                "Original5U mold shell");
        check(mold.meltDownTemperatureK() == 2500, "Original mold Kelvin limit");
        near(mold.thermalMassKg(), 454.5454540909092D, "Independent original5U mold mass");
        check(CrucibleSpec.BASIN_HULL_UNITS == 5 * GTValues.U
                        && CrucibleSpec.CROSSING_HULL_UNITS == 5 * GTValues.U
                        && CrucibleSpec.FAUCET_HULL_UNITS == 3 * GTValues.U,
                "Other original companion weights remain unchanged");
        var defaults = CrucibleSpec.of("default_hull", Materials.Ceramic, 1005, 5, 5, false);
        check(defaults.hullDensity() == Materials.Ceramic.getDensity(), "Retained material-derived overload");
        var stone = CrucibleSpec.of("stone_hull", Materials.Ceramic, 1005, 5, 5, false,
                CrucibleSpec.STONE_CRUCIBLE_HULL_UNITS);
        check(stone.isStoneTier() && stone.hullMaterialUnits() == 63 * GTValues.U, "Original63U tier predicate");
    }

    private static void chargedCrucibleHeatRequirement() {
        var ceramic = CrucibleSpec.of("smelting_crucible_ceramic", Materials.Ceramic,
                1005, 5.0F, 5.0F, false, 2000, 4000, 0.8181818181818182D);
        // Decimal expected values were calculated independently from Java float32 source densities.
        double copperKg = com.gregtech.gregtech.api.material.MaterialMass.kilograms(Materials.Copper, 3 * GTValues.U);
        double tinKg = com.gregtech.gregtech.api.material.MaterialMass.kilograms(Materials.Tin, GTValues.U);
        near(copperKg, 2986.6666763956573D, "Original three copper units mass");
        near(tinKg, 809.6666857781968D, "Original one tin unit mass");
        double chargedKg = ceramic.thermalMassKg() + copperKg + tinKg;
        near(chargedKg, 4432.696997901126D, "Fixed3Cu+1Sn with original ceramic shell mass");
        check(ThermalStep.requiredEnergy(chargedKg) == 45, "Charged vessel needs45HU for one Kelvin");
        check(ThermalStep.requiredEnergy(ceramic.thermalMassKg()) == 7, "Empty original hull needs7HU/K");
    }

    private static void near(double actual, double expected, String message) {
        check(Double.isFinite(actual) && Math.abs(actual - expected) < 1.0e-9D, message + ": " + actual);
    }

    private static void throwsType(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            check(true, message);
            return;
        }
        throw new AssertionError(message);
    }

    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
}
