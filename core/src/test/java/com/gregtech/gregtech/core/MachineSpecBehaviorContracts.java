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
        originalMachineMaterials();
        originalBlastTooltips();
        originalManualAndBoilerTooltips();
        originalLargeBoilerTooltipState();
        originalEnergyDeviceTooltips();
        System.out.println("Machine spec behavior contracts passed: " + assertions
                + " assertions; brick25percent/16HU, ceramic7U/2500K, mold5U, charged45HU/K, original machine CR.REV data; no game runtime");
    }

    private static void originalMachineMaterials() {
        long u = GTValues.U;
        composition(com.gregtech.gregtech.content.machine.OriginalMachineMaterialData.find("oven", 1).orElseThrow(),
                java.util.Map.of("Steel", 8*u, "Brick", 8*u, "Copper", 2*u));
        composition(com.gregtech.gregtech.content.machine.OriginalMachineMaterialData.find("centrifuge", 1).orElseThrow(),
                java.util.Map.of("Bronze", 23*u));
        composition(com.gregtech.gregtech.content.machine.OriginalMachineMaterialData.find("electrolyzer", 1).orElseThrow(),
                java.util.Map.of("SteelGalvanized", 8*u, "Platinum", u, "Tin", u, "Rubber", 2*u));
        composition(com.gregtech.gregtech.content.machine.OriginalMachineMaterialData.find("melter", 1).orElseThrow(),
                java.util.Map.of("Iron", 14*u, "Ceramic", 7*u, "Brick", 8*u, "Copper", 2*u));
        composition(com.gregtech.gregtech.content.machine.OriginalMachineMaterialData.find("massfab", 5).orElseThrow(),
                java.util.Map.of("Osmiridium", 8*u, "Osmium", 64*u, "Titanium", 16*u, "Platinum", 4*u,
                        "NetherStar", 4*u, "Ruby", 2*u, "Sapphire", 2*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("hopper_steel").orElseThrow(),
                java.util.Map.of("Steel", 5*u, "Wood", 4*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("queue_hopper_steel").orElseThrow(),
                java.util.Map.of("Steel", 5*u, "Wood", 8*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("burning_box_solid_bronze").orElseThrow(),
                java.util.Map.of("Bronze", 4*u, "Copper", 2*u, "Brick", 12*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("gearbox_iridium").orElseThrow(),
                java.util.Map.of("Iridium", 14*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("rotation_transformer_iridium").orElseThrow(),
                java.util.Map.of("Iridium", 26*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("gearbox_wood").orElseThrow(),
                java.util.Map.of("WoodTreated", 10*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("axle_wood_3").orElseThrow(),
                java.util.Map.of("WoodTreated", 4*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("engine_steam_strong_bronze").orElseThrow(),
                java.util.Map.of("Bronze", 38*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("electric_motor_lv").orElseThrow(),
                java.util.Map.of("SteelGalvanized", 18*u + u/3, "IronMagnetic", u, "Copper", u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("electric_dynamo_lv").orElseThrow(),
                java.util.Map.of("SteelGalvanized", 18*u + u/3, "IronMagnetic", u, "Copper", u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("battery_box_ulv").orElseThrow(),
                java.util.Map.of("TinAlloy", 8*u, "Lead", 3*u, "Rubber", 2*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("energy_storage_ulv").orElseThrow(),
                java.util.Map.of("TinAlloy", 8*u, "Lead", 12*u, "Rubber", 4*u, "Copper", 4*u, "Iron", 4*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("solar_panel_silicon").orElseThrow(),
                java.util.Map.of("TinAlloy", 8*u, "Silicon", 4*u, "Copper", u, "Rubber", 2*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("steam_turbine_brass").orElseThrow(),
                java.util.Map.of("Bronze", 23*u, "Brass", 17*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("implosion_compressor_wall").orElseThrow(),
                java.util.Map.of("TungstenSteel", 36*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("large_iridium_coil").orElseThrow(),
                java.util.Map.of("Iridium", 16*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("small_stainless_steel_tank_main_valve").orElseThrow(),
                java.util.Map.of("StainlessSteel", 9*u/2));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("stainless_steel_boiler_main_barometer").orElseThrow(),
                java.util.Map.of("StainlessSteel", 90*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("steam_boiler_bronze").orElseThrow(),
                java.util.Map.of("Bronze", 10*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("strong_steam_boiler_bronze").orElseThrow(),
                java.util.Map.of("Bronze", 45*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("rotational_pump_bronze").orElseThrow(),
                java.util.Map.of("Bronze", 22*u, "StainlessSteel", 23*u/2));
        for (String[] mortar : new String[][]{{"mortar_block", "Iron"}, {"mortar_netherite", "Netherite"},
                {"mortar_sapphire", "Sapphire"}, {"mortar_diamond", "Diamond"}, {"mortar_amethyst", "Amethyst"}})
            composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block(mortar[0]).orElseThrow(),
                    java.util.Map.of("Ceramic", 5*u, mortar[1], u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("grindstone_block").orElseThrow(),
                java.util.Map.of("Iron", 21*u/2));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("sifting_table").orElseThrow(),
                java.util.Map.of("Iron", 161*u/36));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("mixing_bowl").orElseThrow(),
                java.util.Map.of("Ceramic", 5*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("mixing_bowl_table").orElseThrow(),
                java.util.Map.of("Ceramic", 5*u, "Brick", 2*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("juicer").orElseThrow(),
                java.util.Map.of("Ceramic", 4*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("bathing_pot").orElseThrow(),
                java.util.Map.of("StainlessSteel", 5*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("bathing_pot_table").orElseThrow(),
                java.util.Map.of("StainlessSteel", 5*u, "Brick", 2*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("bathing_pot_wood").orElseThrow(),
                java.util.Map.of("Wood", 5*u, "Lead", u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("bathing_pot_table_wood").orElseThrow(),
                java.util.Map.of("Wood", 5*u, "Lead", u, "Brick", 2*u));
        check(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("axle_wood_4").isEmpty(),
                "Source beamWood/creosote has no automatic material data; do not invent8U");
        check(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("energy_storage_xv").isEmpty(),
                "Original null transformer10049 aborts CR.shaped before OM.data; partial Graphene must not become recovery data");
        check(com.gregtech.gregtech.content.machine.OriginalMachineMaterialData.find("scannermolecular", 1).isEmpty(),
                "Commented-out source molecular scanner cannot create a material registration");
        var spec = com.gregtech.gregtech.content.machine.BasicMachineCatalog.specifications().stream()
                .filter(p -> p.id().equals("centrifuge_bronze")).findFirst().orElseThrow();
        check(spec.constructionMaterials().size() == 1 && spec.constructionMaterials().get(0).amount() == 23*u,
                "Both native catalogs receive exact CS.U values, not raw23 or a fixed8U hull estimate");
    }

    private static void originalBlastTooltips() {
        var terrible = com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.BlastRating.TERRIBLE;
        var ghast = com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.BlastRating.GHAST;
        var creeper = com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.BlastRating.CREEPER;
        var tnt = com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.BlastRating.TNT;
        var dynamite = com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.BlastRating.DYNAMITE;
        check(com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.blastRating(3.99) == terrible, "Original below4 warning");
        for (double value : new double[]{4, 7, 11.99}) check(com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.blastRating(value) == ghast, "Original ghast tooltip at " + value);
        for (double value : new double[]{12, 15.99}) check(com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.blastRating(value) == creeper, "Original creeper tooltip at " + value);
        for (double value : new double[]{16, 40}) check(com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.blastRating(value) == tnt, "Original TNT tooltip at " + value);
        for (double value : new double[]{40.01, 3329, 3330, 3600000}) check(com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.blastRating(value) == dynamite, "Absent IC2 compat must not invent a nuclear warning at " + value);
        check(com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.blastRating(3330, true, false)
                == com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.BlastRating.IC2_NUKE_UNPROTECTED, "Original IC2 conditional warning");
        check(com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.blastRating(3330, true, true) == dynamite, "Original IC2 whitelist branch");
        check(com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.blastNumber(6.99).equals("6.9"), "Original decimal truncation");
        check(!com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.showMultitileBlast(3.99), "MTE item suppresses blast line below4");
        check(com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.showMultitileBlast(4), "MTE blast boundary4 is visible");
    }

    private static void originalManualAndBoilerTooltips() {
        var mortar = com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.manual("mortar_diamond");
        check(mortar.recipeKey().equals("gt.recipe.mortar") && mortar.faceKey().equals("gt.lang.face.top")
                && !mortar.magnifier() && !mortar.facingWrench(), "Source mortar top interaction without invented magnifier");
        var grind = com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.manual("GRINDSTONE");
        check(grind.preparationKey().equals("gt.lang.recipes.grindstone.init") && grind.facingWrench()
                && grind.faceKey().equals("gt.lang.face.any.but.sides"), "Source grindstone sandstone preparation and facing");
        var bath = com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.manual("bathing_pot_table_wood");
        check(bath.recipeKey().equals("gt.recipe.bath") && bath.magnifier(), "Wood table inherits source bath tooltip");
        var bronze = com.gregtech.gregtech.content.energy.BoilerCatalog.all().stream()
                .filter(s -> s.id().equals("steam_boiler_bronze")).findFirst().orElseThrow();
        var normal = com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.boiler(bronze, 10000);
        check(normal.heatInput() == 24 && normal.heatCapacity() == 480000 && normal.steamOutput() == 48
                && normal.steamCapacity() == 480000, "Original bronze boiler24HU/48Steam/480000 buffers");
        var calcified = com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.boiler(bronze, 5000);
        check(calcified.steamOutput() == 24 && calcified.heatInput() == 24 && calcified.steamCapacity() == 480000,
                "Calcification halves displayed Steam output without halving input or capacity");
        check(com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.boiler(bronze, 9999).steamOutput() == 47,
                "Original floor conversion for non-round efficiency");
        check(com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.boilerEfficiency(-1) == 0
                && com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.boilerEfficiency(10001) == 10000,
                "Original saved efficiency clamp0..10000");
        for (var value : java.util.Map.of(10000, "100.00", 5000, "50.00", 9999, "99.99", 1, "0.01").entrySet())
            check(com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.efficiencyPercent(value.getKey()).equals(value.getValue()),
                    "Original percent " + value.getKey());
        var juicer = com.gregtech.gregtech.content.tool.OpenVesselRules.profile("juicer");
        check(juicer.hardness() == 1 && juicer.resistance() == 5, "Original ceramic juicer physical properties");
    }

    private static void composition(com.gregtech.gregtech.api.material.ItemComposition data, java.util.Map<String, Long> expected) {
        var wanted = new java.util.HashMap<com.gregtech.gregtech.api.material.GTMaterial, Long>();
        expected.forEach((name, amount) -> wanted.put(GTMaterialRegistry.get(name), amount));
        var actual = new java.util.HashMap<com.gregtech.gregtech.api.material.GTMaterial, Long>();
        data.components().forEach(part -> actual.put(part.material(), part.amount()));
        check(actual.equals(wanted), "Original components for " + data.source() + ": " + actual + " expected " + wanted);
    }

    private static void originalLargeBoilerTooltipState() {
        // Loader_MultiTileEntities17201..17205 and MultiTileEntityLargeBoiler.addToolTips/readFromNBT2.
        int[] ids = {17201, 17202, 17203, 17204, 17205};
        long[] outputs = {8192, 16384, 32768, 262144, 8192};
        for (int i = 0; i < ids.length; i++) {
            var full = com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.largeBoiler(ids[i], 10000);
            check(full.heatInput() == outputs[i]/2 && full.steamOutput() == outputs[i]
                    && full.heatCapacity() == outputs[i]*10000 && full.steamCapacity() == outputs[i]*10000,
                    "Original large boiler rates and capacities " + ids[i]);
            var saved = com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.largeBoiler(ids[i], 9999);
            check(saved.steamOutput() == outputs[i]*9999/10000 && saved.heatInput() == full.heatInput()
                    && saved.steamCapacity() == full.steamCapacity(), "Saved large boiler efficiency floor " + ids[i]);
        }
        var zero = com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.largeBoiler(17204, -1);
        check(zero.efficiency() == 0 && zero.steamOutput() == 0 && zero.steamCapacity() == 2621440000L,
                "Original saved efficiency may be below natural calcification floor; capacity exceeds int range");
        var over = com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.largeBoiler(17201, 10001);
        check(over.efficiency() == 10000 && over.steamOutput() == 8192, "Source large boiler saved efficiency upper clamp");
        check(com.gregtech.gregtech.content.energy.GearboxRotationRules.gearsWork(0, 0), "Empty original gearbox has no warning");
        check(!com.gregtech.gregtech.content.energy.GearboxRotationRules.gearsWork(3, 0), "Two opposite gears without axle warn");
        check(com.gregtech.gregtech.content.energy.GearboxRotationRules.gearsWork(3, 2), "Matching Y axle interlocks opposite gears");
        check(!com.gregtech.gregtech.content.energy.GearboxRotationRules.gearsWork(21, 0), "Original three-axis triangle warns");
    }

    private static void originalEnergyDeviceTooltips() {
        // Original10080..10099 /10040..10048 /10050..10051, with Root/Converter/Bidirectional stats.
        var boxes = com.gregtech.gregtech.content.energy.BatteryBoxDefinitions.specifications();
        check(boxes.size() == 20, "Original four/sixteen-slot boxes cover ten voltage tiers");
        for (int tier = 0; tier < 10; tier++) {
            long voltage = 8L << (tier * 2);
            for (int size = 0; size < 2; size++) {
                var spec = boxes.get(tier * 2 + size);
                var profile = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(spec, false);
                check(spec.batterySlots() == (size == 0 ? 4 : 16)
                                && profile.input().minimum() == voltage / 2
                                && profile.input().recommended() == voltage && profile.input().maximum() == voltage * 2,
                        "Original battery box rated input " + tier + "/" + size);
                check(profile.output().minimum() == voltage && profile.output().recommended() == voltage
                                && profile.output().maximum() == voltage && profile.batteryModes()
                                && !profile.alwaysShowRange() && profile.efficiency() == -1 && !profile.monkeyWrench(),
                        "Original fixed battery output and source hint chain " + tier + "/" + size);
            }
        }
        check(com.gregtech.gregtech.api.energy.EnergyGate.gateInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,
                        true, 3, com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.batteryInputMinimum(8), 7,
                        () -> { throw new AssertionError("ULV packet below4 must never reach storage"); }) == 7,
                "Original ULV under-voltage packet is consumed without storing");
        check(com.gregtech.gregtech.api.energy.EnergyGate.gateInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,
                        true, 4, com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.batteryInputMinimum(8), 7,
                        () -> 5) == 5, "Original ULV4 packet reaches storage callback");
        var nodes = com.gregtech.gregtech.content.energy.EnergyNodeDefinitions.specifications();
        int transformers = 0, solar = 0;
        for (var spec : nodes) {
            if (spec.id().startsWith("transformer_")) {
                long high = 32L << (transformers * 2), low = high / 4;
                var normal = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(spec, false);
                var reverse = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(spec, true);
                check(normal.input().minimum() == high / 2 && normal.input().recommended() == high
                                && normal.input().maximum() == high * 2 && normal.output().minimum() == low / 2
                                && normal.output().recommended() == low && normal.output().maximum() == low * 2,
                        "Original electric transformer normal ratings " + transformers);
                check(reverse.input().minimum() == (low / 2 <= 8 ? 1 : low / 2)
                                && reverse.input().recommended() == high && reverse.input().maximum() == high * 2
                                && reverse.output().minimum() == high * 3 / 4 && reverse.output().recommended() == high
                                && reverse.output().maximum() == high * 2,
                        "Original reverse recommendation and ranges " + transformers);
                check(normal.alwaysShowRange() && normal.efficiency() == 10000 && reverse.efficiency() == 10000
                                && normal.monkeyWrench() && reverse.monkeyWrench()
                                && normal.inputFaceKey().equals(reverse.inputFaceKey())
                                && normal.outputFaceKey().equals("gt.lang.face.any.but.front"),
                        "Source converter range/efficiency/fixed face labels/Monkey Wrench " + transformers);
                transformers++;
            } else if (spec.kind() == com.gregtech.gregtech.api.energy.EnergyNodeSpec.Kind.SOLAR) {
                long output = solar == 0 ? 8 : 16;
                var profile = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(spec, false);
                check(profile.input() == null && profile.output().minimum() == output / 8
                                && profile.output().recommended() == output && profile.output().maximum() == output
                                && profile.outputFaceKey().equals("gt.lang.face.front") && profile.efficiency() == -1,
                        "Original solar output-only source tooltip " + solar);
                solar++;
            }
        }
        check(transformers == 9 && solar == 2, "Source electric transformer and solar families");
        var wood = com.gregtech.gregtech.content.energy.GearboxCatalog.transformer(
                com.gregtech.gregtech.content.energy.GearboxCatalog.tiers()[0]);
        var normal = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(wood, false);
        var reverse = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(wood, true);
        check(normal.input().minimum() == 1 && normal.input().recommended() == 8 && normal.input().maximum() == 16
                        && normal.output().minimum() == 1 && normal.output().recommended() == 2 && normal.output().maximum() == 4,
                "Original wood rotational transformer source8to2");
        check(reverse.input().minimum() == 1 && reverse.input().recommended() == 8 && reverse.input().maximum() == 16
                        && reverse.output().minimum() == 6 && reverse.output().recommended() == 8 && reverse.output().maximum() == 16
                        && reverse.outputFaceKey().equals("gt.lang.face.back"),
                "Original wood rotational reverse6to16 input recommendation8");
        throwsType(() -> com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(nodes.get(0), false),
                "Incomplete motor conversion is not falsely assigned a battery/transformer tooltip profile");
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
