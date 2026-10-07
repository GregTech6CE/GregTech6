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
        originalSmelteryParametersAndShapes();
        assertions += CrucibleHazardContracts.verify();
        originalTankTooltipsAndControllerMaterials();
        originalProcessControllerParameters();
        originalCryoDistillationParametersAndOutlets();
        originalAdjacentEnergySources();
        assertions += LargeRecipeControllerContracts.verify();
        assertions += BasicMachineSourceContracts.verify();
        originalAdvancedControllerParameters();
        assertions += GeneratorTooltipContracts.verify();
        assertions += UtilityControllerContracts.verify();
        assertions += SensorSourceContracts.verify();
        originalMachineMaterials();
        originalBlastTooltips();
        originalHarvestProperties();
        originalStorageMaterialsAndTooltips();
        originalRemainingStorageMaterials();
        originalManualAndBoilerTooltips();
        originalLargeBoilerTooltipState();
        originalEnergyDeviceTooltips();
        originalMotorDynamoConversion();
        originalThermalDevices();
        originalBipolarMagnets();
        originalSteamTurbines();
        System.out.println("Machine spec behavior contracts passed: " + assertions
                + " assertions; brick25percent/16HU, ceramic7U/2500K, mold5U, charged45HU/K, original machine CR.REV data; no game runtime");
    }

    private static void originalTankTooltipsAndControllerMaterials() {
        var capacities=java.util.Map.ofEntries(
                java.util.Map.entry(17001,432000L),java.util.Map.entry(17002,1728000L),java.util.Map.entry(17003,6912000L),
                java.util.Map.entry(17004,6912000L),java.util.Map.entry(17005,110592000L),java.util.Map.entry(17006,3456000L),java.util.Map.entry(17007,1728000L),
                java.util.Map.entry(17022,6912000L),java.util.Map.entry(17023,27648000L),java.util.Map.entry(17024,27648000L),
                java.util.Map.entry(17025,442368000L),java.util.Map.entry(17026,13824000L),java.util.Map.entry(17027,6912000L),
                java.util.Map.entry(17042,8000000L),java.util.Map.entry(17043,32000000L),java.util.Map.entry(17044,32000000L),
                java.util.Map.entry(17045,512000000L),java.util.Map.entry(17046,16000000L),java.util.Map.entry(17047,8000000L),
                java.util.Map.entry(17062,32000000L),java.util.Map.entry(17063,128000000L),java.util.Map.entry(17064,128000000L),
                java.util.Map.entry(17065,2048000000L),java.util.Map.entry(17066,64000000L),java.util.Map.entry(17067,32000000L));
        check(com.gregtech.gregtech.content.multiblock.TankValveParameters.all().size()==25,"all25 original tank registrations");
        for(var spec:com.gregtech.gregtech.content.multiblock.TankValveParameters.all()) {
            int id=spec.originalId();
            check(spec.capacity()==capacities.get(id),"source tank capacity registration "+id);
            int size=id>=17040?5:3;
            check(spec.size()==size && com.gregtech.gregtech.content.multiblock.OriginalTankTooltipData.structureKeys(size)
                    .equals(java.util.List.of("gt.tooltip.multiblock.tank"+size+"x"+size+"x"+size+".1",
                            "gt.tooltip.multiblock.tank"+size+"x"+size+"x"+size+".2","gt.tooltip.multiblock.tank"+size+"x"+size+"x"+size+".3")),"original shape keys "+id);
            var path=com.gregtech.gregtech.content.multiblock.SharedLargeMachineParts.DEFINITIONS.stream()
                    .filter(p->p.originalId()==id).findFirst().orElseThrow().name();
            var data=com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block(path).orElseThrow();
            long u=GTValues.U;
            if(id==17001) composition(data,java.util.Map.of("WoodTreated",4*u,"Pb",3*u/2));
            else {
                long amount=switch(id/20) {case 850->9*u/2;case 851->73*u/2;case 852->21*u/2;case 853->181*u/2;
                    default->throw new IllegalStateException("source fixture ID "+id);};
                check(data.components().size()==1 && data.components().get(0).material().resolve()==GTMaterialRegistry.get(spec.material()).resolve()
                        && data.components().get(0).amount()==amount,"original controller REV ring/wall/plate material "+id);
            }
        }
        var numbers=java.util.Map.of(0L,"0",9999L,"9999",10000L,"10_000",432000L,"432_000",2048000000L,"2_048_000_000",
                -9999L,"-9999",-10000L,"-10_000",3210000L,"3_210_000",Long.MAX_VALUE,"9_223_372_036_854_775_807");
        for(var entry:numbers.entrySet()) check(com.gregtech.gregtech.content.multiblock.OriginalTankTooltipData.formatNumber(entry.getKey())
                .equals(entry.getValue()),"original UT decimal grouping "+entry.getKey());
    }

    private static void originalProcessControllerParameters() {
        var distillation=com.gregtech.gregtech.content.multiblock.OriginalMultiblockMachineParameters.distillationTower();
        var faces=distillation.faceConfig();
        check(faces.itemInputs()==63 && faces.itemOutputs()==63 && faces.fluidInputs()==63 && faces.fluidOutputs()==63
                && faces.energyInputs()==63 && faces.energyOutputs()==0,
                "17101 inherits original any-side IO/energy defaults; formation still gates ports");
        check(faces.itemAutoInput()==-1 && faces.fluidAutoInput()==-1
                && faces.itemAutoOutput()==com.gregtech.gregtech.api.energy.MachineFaceMasks.BACK
                && faces.fluidAutoOutput()==com.gregtech.gregtech.api.energy.MachineFaceMasks.BACK,
                "17101 only declares back automatic outputs; no fabricated left automatic input");
        check(distillation.energyIn()==512 && distillation.energyInMin()==1 && distillation.energyInMax()==1024
                && distillation.energyType().equals("HU"),"17101 original explicit heat input range");
        var coke=com.gregtech.gregtech.content.multiblock.OriginalMultiblockMachineParameters.cokeOven();
        check(coke.parallelLimit()==16 && coke.energyType().equals("TU") && coke.faceConfig().fluidOutputs()==61
                && coke.faceConfig().fluidAutoOutput()==0,"17000 time energy, parallel16, original non-top fluid output and bottom auto");
        long u=GTValues.U;
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("logistics_core").orElseThrow(),
                java.util.Map.of("SteelGalvanized",8*u,"Pt",2*u,"Emerald",2*u));
        var harvest=com.gregtech.gregtech.data.SourceBlockProperties.block("logistics_core").orElseThrow();
        check(harvest.sourceId()==17997 && harvest.tool().equals("wrench") && !harvest.handHarvestable()
                && harvest.material().resolve()==GTMaterialRegistry.get("SteelGalvanized").resolve()
                && harvest.explicitLevel()==-1,"17997 original aMachine tool/material-quality metadata");
        var logistics=com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData.standaloneEnergy(
                com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData.Family.LOGISTICS_CORE);
        check(logistics.minimum()==256 && logistics.maximum()==1024 && logistics.totalPerTick()==0
                && logistics.unitKey().equals("gt.td.short.energy.electricity"),"17997 literal source tooltip packet range");
        var drill=com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData.standaloneEnergy(
                com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData.Family.BEDROCK_DRILL);
        check(drill.minimum()==1024 && drill.maximum()==4096 && drill.totalPerTick()==32768
                && drill.unitKey().equals("gt.td.short.energy.kinetic_rotation"),"17999 source tooltip packet and aggregate limits");
        check(com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData.basicFamily("oven")==null,
                "ordinary basic machine does not acquire specialized multiblock rows");
    }

    private static void originalAdjacentEnergySources() {
        var positions = com.gregtech.gregtech.content.energy.OriginalAdjacentEnergyRules.TOWER_SOURCES;
        check(positions.size() == 9 && new java.util.HashSet<>(positions).size() == 9, "source nine unique providers below transmitter base");
        for (int x=-1;x<=1;x++) for(int z=0;z<=2;z++) check(positions.contains(
                new com.gregtech.gregtech.content.energy.OriginalAdjacentEnergyRules.Position(x,-2,z)),
                "canonical source control coordinate relative to front-bottom main "+x+"/"+z);
        for (var spec : com.gregtech.gregtech.content.energy.OriginalThermalConverter.specifications())
            check(com.gregtech.gregtech.content.energy.OriginalAdjacentEnergyRules.respondsToAdjacent(spec),
                    "source twenty electric/flux thermal registrations all WASTE_ENERGY=T "+spec.id());
        check(!com.gregtech.gregtech.content.energy.OriginalAdjacentEnergyRules.respondsToAdjacent(null), "no adjacent source contract without an imported spec");
        var unsupported = com.gregtech.gregtech.api.energy.EnergyNodeSpec.builder("fixture_generic_hu_source",
                com.gregtech.gregtech.content.material.Materials.StainlessSteel)
                .texture("fixture").names("Fixture", "Fixture").capacity(64)
                .input(com.gregtech.gregtech.data.GregTechTags.Energy.EU,32)
                .output(com.gregtech.gregtech.data.GregTechTags.Energy.HU,16).build();
        check(!com.gregtech.gregtech.content.energy.OriginalAdjacentEnergyRules.respondsToAdjacent(unsupported),
                "generic switchable energy nodes do not acquire an unregistered source WASTE contract");
    }
    private static void originalCryoDistillationParametersAndOutlets() {
        var p=com.gregtech.gregtech.content.multiblock.OriginalMultiblockMachineParameters.cryoDistillationTower();
        check(p.id().equals("cryo_distillation_main") && p.machineName().equals("cryodistillationtower")
                && p.energyType().equals("CU") && p.energyIn()==512 && p.energyInMin()==1 && p.energyInMax()==1024,
                "17111 original CU512 explicit1..1024 range, never old HU splitter");
        check(p.faceConfig().itemInputs()==63 && p.faceConfig().itemOutputs()==63 && p.faceConfig().fluidInputs()==63
                && p.faceConfig().fluidOutputs()==63 && p.faceConfig().energyInputs()==63,
                "17111 original inherited any-face defaults");
        check(p.faceConfig().itemAutoInput()==-1 && p.faceConfig().fluidAutoInput()==-1
                && p.faceConfig().itemAutoOutput()==5 && p.faceConfig().fluidAutoOutput()==5,
                "17111 source declares back auto outputs only");
        long u=GTValues.U;
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("cryo_distillation_main").orElseThrow(),
                java.util.Map.of("Cu",72*u,"StainlessSteel",31*u/9));
        var cells=com.gregtech.gregtech.content.multiblock.SharedDistillationTowerStructure.CELLS;
        check(cells.size()==80 && cells.stream().filter(c->c.up()<0 && c.part()==18101).count()==9
                && cells.stream().filter(c->c.up()==0 && c.part()==18102).count()==8
                && cells.stream().filter(c->c.up()>0 && c.part()==18102).count()==63,
                "17111 full solid tower: nine transmitters, eight bottom parts, seven full9-part upper layers");
        var cryo=java.util.Map.of("helium",7,"neon",6,"nitrogen",5,"oxygen",4,"argon",3,
                "carbondioxide",2,"sulfurdioxide",2,"krypton",1,"xenon",1,"radon",1);
        for(var entry:cryo.entrySet()) check(com.gregtech.gregtech.content.multiblock.OriginalDistillationOutputRules.fluidHeight(true,entry.getKey())==entry.getValue(),
                "17111 original species rear output height "+entry.getKey());
        var normal=java.util.Map.ofEntries(java.util.Map.entry("propane",7),java.util.Map.entry("methane",7),java.util.Map.entry("butane",6),
                java.util.Map.entry("petrol",5),java.util.Map.entry("gasoline",5),java.util.Map.entry("bioethanol",5),
                java.util.Map.entry("kerosene",4),java.util.Map.entry("kerosine",4),java.util.Map.entry("glycerol",4),
                java.util.Map.entry("diesel",3),java.util.Map.entry("biodiesel",3),java.util.Map.entry("fuel",2),
                java.util.Map.entry("fueloil",2),java.util.Map.entry("biofuel",2),java.util.Map.entry("water",1));
        for(var entry:normal.entrySet()) check(com.gregtech.gregtech.content.multiblock.OriginalDistillationOutputRules.fluidHeight(false,entry.getKey())==entry.getValue(),
                "17101 original rear output height and source aliases "+entry.getKey());
        check(com.gregtech.gregtech.content.multiblock.OriginalDistillationOutputRules.fluidHeight(true,"HELIUM")==7
                && com.gregtech.gregtech.content.multiblock.OriginalDistillationOutputRules.fluidHeight(true,null)==1,
                "original FL.is is case insensitive; unknown species uses bottom fluid outlet");
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

    private static void originalHarvestProperties() {
        var properties = com.gregtech.gregtech.data.SourceBlockProperties.blocks();
        check(properties.size() == 1542, "689 fixed source identities plus120 hoppers/720 storage/12 generator aliases/1 utility alias");
        check(com.gregtech.gregtech.data.SourceBlockProperties.basics().size() == 265, "All265 adopted original machine metadata keys");
        var wood = properties.get("gearbox_wood");
        check(wood.sourceId() == 24809 && wood.tool().equals("axe") && wood.handHarvestable(), "Wood gearbox original aWooden exemption");
        var metal = properties.get("gearbox_iridium");
        check(metal.sourceId() == 24859 && metal.tool().equals("wrench") && !metal.handHarvestable()
                && metal.material().resolve() == GTMaterialRegistry.get("Iridium"), "Iridium gearbox original machine group and material identity");
        var mortar = properties.get("mortar_diamond");
        check(mortar.sourceId() == 32076 && mortar.tool().equals("pickaxe") && mortar.handHarvestable() && mortar.level() == 0,
                "Source aUtilMetal mortar ignores its diamond head tier and remains hand harvestable");
        var ironwood = properties.get("engine_steam_ironwood");
        check(ironwood.sourceId() == 1310 && ironwood.tool().equals("axe") && ironwood.handHarvestable(), "Ironwood source utility group exemption");
        check(properties.get("electric_motor_lv").material().resolve() == GTMaterialRegistry.get("SteelGalvanized"), "Motor harvest follows original casing, not magnet inside");
        check(com.gregtech.gregtech.data.SourceBlockProperties.basic("melter", 1).orElseThrow().material().resolve()
                == GTMaterialRegistry.get("Iron"), "Source melter harvest follows registration metadata before ceramic lining override");
        check(com.gregtech.gregtech.data.SourceBlockProperties.basic("cokeoven", 1).orElseThrow().tool().equals("pickaxe"), "Coke oven source aStone, not heuristic controller wrench");
        for (String name : new String[]{"hopper_steel", "queue_hopper_steel"}) {
            var hopper = properties.get(name);
            check(hopper != null && !hopper.handHarvestable() && hopper.tool().equals("wrench")
                    && hopper.material().resolve() == GTMaterialRegistry.get("Steel"), "Source metalset hopper metadata " + name);
        }
        for (var entry : com.gregtech.gregtech.content.transport.HopperCatalog.ALL) {
            check(properties.get(entry.spec().id()).explicitLevel() == 0 && properties.get(entry.spec().id()).level() == 0,
                    "Original hopper metadata is explicit0 even for iridium and infinity: " + entry.spec().id());
            String suffix=entry.spec().id().substring(entry.queue()?"queue_hopper_".length():"hopper_".length());
            check(properties.get(entry.spec().id()).sourceId() == properties.get("chest_"+suffix).sourceId()+(entry.queue()?8200:8000),
                    "Original hopper has its real metalset registration identity: "+entry.spec().id());
        }
        check(properties.get("axle_wood_4").handHarvestable(), "Empty CR.REV source axle retains original harvest metadata");
        check(properties.get("energy_storage_xv").sourceId() == 10099, "Broken source recipe does not discard legal machine metadata");
        check(!com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.showHarvestLevel(1)
                && com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.showHarvestLevel(2), "Original LH harvest level visibility boundary");
        for (String[] tier : new String[][]{{"2", "iron"}, {"3", "diamond"}, {"4", "netherite"}, {"5", "adamantium"}, {"15", "infinity"}})
            check(com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.harvestTierMaterial(Integer.parseInt(tier[0])).equals(tier[1]), "Source harvest tier name " + tier[0]);
        check(com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.harvestTierMaterial(14) == null, "Source tiers6..14 have no invented reference material");
    }

    private static void originalStorageMaterialsAndTooltips() {
        var storage = com.gregtech.gregtech.content.machine.OriginalStorageMaterialData.blocks();
        var harvest = com.gregtech.gregtech.content.machine.OriginalStorageMaterialData.harvest();
        check(storage.size() == 720 && harvest.size() == 720, "Twelve existing original storage families across60 metalsets");
        check(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.blocks().size() == 1500,
                "647 fixed plus120 hoppers plus720 storage plus12 source generator aliases plus1 utility alias");
        long u = GTValues.U;
        for (var spec : com.gregtech.gregtech.registry.GTStorageMetals.ALL) {
            var metal = spec.material().resolve();
            String suffix = spec.suffix();
            for (var row : java.util.Map.of("chest_"+suffix,5*u,
                    "reinforced_wood_chest_"+suffix,5*u/2,
                    suffix.equals("steel")?"safe":"safe_"+suffix,55*u/2,
                    "key_safe_"+suffix,55*u/2,
                    suffix.equals("stainless_steel")?"drawer_quad":"drawer_quad_"+suffix,20*u+4*u/9,
                    "mass_storage_"+suffix,18*u+4*u/9,
                    "bottle_crate_"+suffix,3*u/2+2*u/9).entrySet()) {
                var record = storage.get(row.getKey());
                var expected = new java.util.HashMap<String,Long>(); expected.put(metal.getName(),row.getValue());
                if (row.getKey().startsWith("reinforced_wood_chest_")) expected.put("Wood",4*u);
                composition(record,expected);
                var metadata = harvest.get(row.getKey());
                check(metadata.material().resolve() == metal, "Native metalset suffix/source material identity "+row.getKey());
                check(metadata.sourceId() >= 0 && com.gregtech.gregtech.data.SourceBlockProperties.block(row.getKey()).orElseThrow().equals(metadata),
                        "Native storage has real original registration identity "+row.getKey());
            }
        }
        check(harvest.get("safe").sourceId() == 2010 && harvest.get("drawer_quad").sourceId() == 4011, "Source legacy steel safe and stainless drawer aliases");
        check(harvest.get("chest_iridium").level() == 0 && !harvest.get("chest_iridium").handHarvestable(), "Original metal chest explicit0 does not inherit metal tier");
        check(harvest.get("reinforced_wood_chest_iridium").handHarvestable() && harvest.get("bottle_crate_iridium").handHarvestable(), "Original wood and utility exemptions");
        check(harvest.get("key_safe_iridium").level() == GTMaterialRegistry.get("Iridium").getToolQuality(), "Machine source metadata uses actual metal quality");
        var normal = com.gregtech.gregtech.content.storage.OriginalStorageTooltipData.hopper(1,false,null,false);
        var queue = com.gregtech.gregtech.content.storage.OriginalStorageTooltipData.hopper(1,true,null,true);
        check(normal.slots() == 1 && !normal.showStackSize() && !normal.exact(), "Default original hopper automatic insertion has no size/exact row");
        check(queue.slots() == 2 && queue.stackSize() == 64 && queue.showStackSize() && !queue.exact(), "Original queue minimum2 slots/default64; ordinary exact flag does not apply");
        var configured = com.gregtech.gregtech.content.storage.OriginalStorageTooltipData.hopper(36,false,16,true);
        check(configured.stackSize() == 16 && configured.showStackSize() && configured.exact(), "Configured native hopper shows actual16/exact");
        check(!queue.tools().contains("gt.lang.use.monkey.wrench.to.toggle") && normal.tools().contains("gt.lang.use.monkey.wrench.to.toggle"), "Original queue omits ordinary hopper monkey wrench mode");
        for (int mode=0;mode<16;mode++) {
            var tools = com.gregtech.gregtech.content.storage.OriginalStorageTooltipData.massTools(mode);
            boolean packed = (mode&8)!=0;
            check(tools.contains("gt.lang.use.untape") == packed && tools.contains("gt.lang.use.tape") != packed
                    && tools.contains("gt.lang.use.soft.hammer.to.reset") != packed, "Original taped container controls at mode"+mode);
        }
        check(com.gregtech.gregtech.content.storage.OriginalStorageTooltipData.lootKey("minecraft:chests/simple_dungeon").equals("loot.dungeonChest")
                && com.gregtech.gregtech.content.storage.OriginalStorageTooltipData.lootKey("other:custom") == null, "Known source loot label maps explicitly; custom table identity retained");
    }

    private static void originalRemainingStorageMaterials() {
        var storage = com.gregtech.gregtech.content.machine.OriginalStorageMaterialData.blocks();
        var harvest = com.gregtech.gregtech.content.machine.OriginalStorageMaterialData.harvest();
        long u = GTValues.U;
        for (var spec : com.gregtech.gregtech.registry.GTStorageMetals.ALL) {
            String suffix = spec.suffix(), tableSuffix = suffix.equals("steel") ? "" : "_"+suffix;
            var metal = spec.material().resolve();
            var amounts = java.util.Map.of("advanced_crafting_table"+tableSuffix,4*u+2*u/9,
                    "charging_crafting_table"+tableSuffix,4*u+4*u/9,
                    "logistics_mass_storage_"+suffix,18*u+8*u/9,
                    suffix.equals("steel")?"scaffold":"scaffold_"+suffix,2*u+2*u/9);
            for (var row : amounts.entrySet()) {
                var expected = new java.util.HashMap<String,Long>();expected.put(metal.getName(),row.getValue());
                String path = row.getKey(); int offset;
                if (path.startsWith("charging_crafting_table")) {
                    offset=5500;expected.merge(GTMaterialRegistry.get("Gold").getName(),8*u,Long::sum);
                    expected.put("Rubber",8*u);expected.put("Wood",4*u);
                } else if (path.startsWith("advanced_crafting_table")) { offset=5000;expected.put("Wood",4*u); }
                else if (path.startsWith("logistics_mass_storage_")) {
                    offset=6200;
                    expected.merge(GTMaterialRegistry.get("Aluminium").getName(),10*u/9,Long::sum);
                    expected.merge(GTMaterialRegistry.get("Platinum").getName(),u,Long::sum);
                    expected.merge(GTMaterialRegistry.get("Emerald").getName(),u,Long::sum);
                    expected.merge(GTMaterialRegistry.get("Osmium").getName(),u/4,Long::sum);
                } else offset=8400;
                composition(storage.get(path),expected);
                var metadata = harvest.get(path);
                check(metadata.sourceId()==harvest.get("chest_"+suffix).sourceId()+offset
                        && metadata.material().resolve()==metal,"Remaining metalset source identity "+path);
                check(metadata.tool().equals("wrench") && !metadata.handHarvestable()
                        && metadata.level()==(offset==8400?0:metal.getToolQuality()),"Actual source metadata, including explicit0 scaffold "+path);
            }
        }
        int shelves=0;
        for (var row : harvest.entrySet()) if (row.getKey().startsWith("bookshelf_metal_")) {
            var metadata=row.getValue();var metal=metadata.material().resolve();
            composition(storage.get(row.getKey()),java.util.Map.of(metal.getName(),4*u+2*u/9));
            var base=harvest.entrySet().stream().filter(e->e.getKey().startsWith("chest_")
                    && e.getValue().sourceId()==metadata.sourceId()-7100).findFirst().orElseThrow().getValue();
            check(base.material().resolve()==metal && metadata.tool().equals("wrench") && !metadata.handHarvestable()
                    && metadata.level()==metal.getToolQuality(),"Metal shelf original identity and actual construction material "+row.getKey());
            shelves++;
        }
        check(shelves==60 && harvest.get("bookshelf_metal_stainless_steel").sourceId()==7111,"All original metal shelves including native split-word paths");
        check(!storage.containsKey("locker") && !storage.containsKey("charging_locker"),"Unported original locker variants cannot give a false identity to the legacy mixed recipe");
        check(com.gregtech.gregtech.content.storage.OriginalStorageTooltipData.bookShelfTools().equals(java.util.List.of(
                "gt.lang.use.pincers.to.take","gt.lang.use.magnifyingglass.to.detail")),"Book shelf original take/detail tools without fabricated loot rows");
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
        var pendingTurbine = nodes.stream().filter(s -> s.id().startsWith("steam_turbine_")).findFirst().orElseThrow();
        check(com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(pendingTurbine, false).efficiency() == 6666,
                "Source steam turbine has its own LH Steam-to-RU efficiency profile");
    }

    private static void originalMotorDynamoConversion() {
        var nodes = com.gregtech.gregtech.content.energy.EnergyNodeDefinitions.specifications();
        int seen = 0;
        for (var spec : nodes) {
            if (!com.gregtech.gregtech.content.energy.OriginalRotaryConverter.handles(spec)) continue;
            boolean motor = com.gregtech.gregtech.content.energy.OriginalRotaryConverter.motor(spec);
            var profile = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(spec, false);
            check(spec.capacity() == spec.inputRate() * 2 && profile.input().minimum() == spec.inputRate() / 2
                            && profile.input().maximum() == spec.inputRate() * 2,
                    "Original motor/dynamo capacitor and input limits " + spec.id());
            check(profile.output().minimum() == spec.outputRate() / 2 && profile.output().maximum() == spec.outputRate() * 2
                            && profile.efficiency() == (motor ? 5000 : 6875) && profile.monkeyWrench() == motor
                            && profile.inputFaceKey().equals(motor ? "gt.lang.face.any.but.front" : "gt.lang.face.back"),
                    "Original motor/dynamo efficiency, faces and tool hints " + spec.id());
            var state = new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.State(spec);
            var simulated = state.inject(0, spec.inputRate(), Long.MAX_VALUE, false);
            check(simulated.consumed() == 2 && simulated.energy() == 0 && !simulated.overloaded(),
                    "Source packet injection simulation with huge amount is pure " + spec.id());
            var wholeLast = state.inject(spec.inputRate() / 2, spec.inputRate(), 2, true);
            check(wholeLast.consumed() == 2 && wholeLast.energy() == spec.inputRate() * 5 / 2,
                    "Original final packet overshoots room " + spec.id());
            seen++;
        }
        check(seen == 14, "Source registered motor/dynamo family coverage");
        var lv = nodes.stream().filter(s -> s.id().equals("electric_motor_lv")).findFirst().orElseThrow();
        var motor = new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.State(lv);
        var calls = new long[2];
        java.util.function.LongBinaryOperator receiver = (size, amount) -> { calls[0] = size; calls[1] = amount; return amount; };
        var step = motor.tick(16, false, receiver);
        check(calls[0] == 8 && calls[1] == 1 && step.energy() == 0 && step.possible() && step.emitted()
                        && !step.fast() && step.visual() == 2, "LV half input emits8RU as one packet and starts trinary activity");
        step = motor.tick(32, false, (size, amount) -> 0);
        check(step.energy() == 0 && step.possible() && !step.emitted(), "Original motor wastes energy without receiver");
        step = motor.tick(32, true, receiver);
        check(calls[0] == 16 && step.energy() == 0 && step.possible() && step.emitted() && step.visual() == 0,
                "Stopping source acceptance/visual does not prevent final buffered conversion");
        step = motor.tick(100, false, receiver);
        check(calls[0] == 32 && step.energy() == 36 && step.fast() && !step.overloaded(), "EU input limits oversized output rather than overloading");
        motor.reverse(); motor.tick(32, false, receiver);
        check(calls[0] == -16, "Monkey Wrench reverses motor RU sign");
        motor.inject(0, -32, 1, true); motor.tick(32, false, receiver);
        check(calls[0] == 16, "Source negative EU input and counterclockwise factor cancel");
        motor.restore(new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.Snapshot(1, false, false, false, false, false));
        step = motor.tick(64, false, receiver);
        check(calls[0] == 30 && step.energy() == 4 && step.fast(), "Mode1 floors output to30 and wastes60EU");
        motor.mode(15); calls[0] = 0;
        step = motor.tick(64, false, receiver);
        check(calls[0] == 0 && !step.possible() && step.energy() == 60, "Mode15 falls below packet minimum but still wastes4EU");
        motor.mode(0);
        for (int i = 0; i < 64; i++) step = motor.tick(32, false, receiver);
        check(step.visual() == 1, "Original64 consecutive active samples select stable activity");
        step = motor.tick(0, false, receiver);
        check(step.visual() == 2 && !step.possible(), "Original brief idle still selects transient active texture");
        for (int i = 0; i < 63; i++) step = motor.tick(0, false, receiver);
        check(step.visual() == 0, "Original64 idle samples clear activity history");
        var dyn = nodes.stream().filter(s -> s.id().equals("electric_dynamo_lv")).findFirst().orElseThrow();
        var dynamo = new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.State(dyn);
        step = dynamo.tick(16, false, receiver);
        check(calls[0] == 11 && calls[1] == 1 && step.energy() == 0, "LV half-speed dynamo emits11EU rather than fixed22EU");
        dynamo = new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.State(dyn);
        check(!dynamo.tick(66, false, receiver).overloaded() && !dynamo.tick(66, false, receiver).overloaded()
                        && dynamo.tick(66, false, receiver).overloaded(), "Original first2 chunk-load ticks clear excess; third overloads");
        var rfMotor = nodes.stream().filter(s -> s.id().equals("flux_motor_lv")).findFirst().orElseThrow();
        var flux = new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.State(rfMotor);
        step = flux.tick(64, false, receiver);
        check(calls[0] == 8 && step.energy() == 0, "Original64RF yields8RU at minimum speed");
        var rfDynamo = nodes.stream().filter(s -> s.id().equals("flux_dynamo_lv")).findFirst().orElseThrow();
        step = new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.State(rfDynamo).tick(16, false, receiver);
        check(calls[0] == 1 && calls[1] == 44 && step.energy() == 0, "Original RF output is44 unit packets at half speed");
        check(com.gregtech.gregtech.content.energy.OriginalRotaryConverter.overloadPower(32, com.gregtech.gregtech.data.GregTechTags.Energy.EU) == 1
                        && com.gregtech.gregtech.content.energy.OriginalRotaryConverter.overloadPower(33, com.gregtech.gregtech.data.GregTechTags.Energy.EU) == 2
                        && com.gregtech.gregtech.content.energy.OriginalRotaryConverter.overloadPower(65, com.gregtech.gregtech.data.GregTechTags.Energy.RU) == 2
                        && com.gregtech.gregtech.content.energy.OriginalRotaryConverter.overloadPower(1000, com.gregtech.gregtech.data.GregTechTags.Energy.RF) == 0.1F,
                "Source tierMax overcharge power and non-exploding RF break strength");
    }

    private static void originalThermalDevices() {
        // Original registrations10001..5/11001..5/10161..5/11161..5, OP amounts and CR.REV.
        var nodes = com.gregtech.gregtech.content.energy.EnergyNodeDefinitions.specifications();
        var recipes = com.gregtech.gregtech.content.energy.OriginalThermalCrafting.rows();
        String[] tiers = {"lv", "mv", "hv", "ev", "iv"};
        String[] electricMaterials = {"SteelGalvanized", "Aluminium", "StainlessSteel", "Chromium", "Titanium"};
        String[] fluxMaterials = {"Lead", "Invar", "Electrum", "EnderiumBase", "Enderium"};
        String[] fluxNames = {"Lead", "Invar", "Electrum", "Enderium Base", "Enderium"};
        String[] resistorMaterials = {"Copper", "Constantan", "Kanthal", "Nichrome", "Carborundum"};
        String[] cableMaterials = {"Tin", "Copper", "Gold", "Aluminium", "Platinum"};
        long u = GTValues.U;
        int seen = 0;
        for (boolean cooler : new boolean[]{false, true}) for (boolean rf : new boolean[]{false, true}) for (int i = 0; i < 5; i++) {
            String id = (rf ? "flux_" : "electric_") + (cooler ? "cooler_" : "heater_") + tiers[i];
            var spec = nodes.stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
            String sourceName = (cooler ? (rf ? "Thermofluxic Cooler" : "Thermoelectric Cooler") : (rf ? "Flux Heater" : "Electric Heater"))
                    + " (" + (rf ? fluxNames[i] : tiers[i].toUpperCase(java.util.Locale.ROOT)) + ")";
            check(spec.displayEn().equals(sourceName), "Original thermal English display name, not chemical symbol " + id);
            long input = (32L << (2*i)) * (rf ? 4 : 1), output = (cooler ? 8L : 16L) << (2*i);
            var profile = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(spec, false);
            check(spec.inputRate() == input && spec.outputRate() == output && spec.capacity() == 2*input
                            && spec.material() == GTMaterialRegistry.get((rf ? fluxMaterials : electricMaterials)[i]),
                    "Original thermal source hull/rates/storage " + id);
            check(spec.inType() == (rf ? com.gregtech.gregtech.data.GregTechTags.Energy.RF : com.gregtech.gregtech.data.GregTechTags.Energy.EU)
                            && spec.outType() == (cooler ? com.gregtech.gregtech.data.GregTechTags.Energy.CU : com.gregtech.gregtech.data.GregTechTags.Energy.HU)
                            && com.gregtech.gregtech.data.GregTechTags.Energy.isSizeIrrelevant(spec.outType()),
                    "Original thermal types and unit packet emission " + id);
            check(profile.input().minimum() == input/2 && profile.input().maximum() == input*2
                            && profile.output().minimum() == output/2 && profile.output().maximum() == output*2
                            && profile.efficiency() == (cooler && !rf ? 2500 : 5000) && !profile.monkeyWrench()
                            && profile.inputFaceKey().equals(cooler ? "gt.lang.face.any.but.front.back" : "gt.lang.face.any.but.front"),
                    "Source LH efficiency per channel, range and faces " + id);
            check(com.gregtech.gregtech.content.energy.OriginalThermalConverter.modeSelectable(spec) == !(cooler && rf),
                    "Source RF cooler has saved mode but no selector interface " + id);
            var expected = new java.util.HashMap<String, Long>();
            if (cooler) {
                expected.put(electricMaterials[i], 8*u);
                expected.put("Silicon", 2*(i+1)*u);
                expected.put("Copper", 2*(i+1)*u);
                expected.merge(cableMaterials[i], u, Long::sum);
                expected.put("Rubber", 2*u);
            } else {
                expected.put(electricMaterials[i], 14*u + u/3);
                expected.put(resistorMaterials[i], (2L << i)*u);
            }
            if (rf) expected.merge(fluxMaterials[i], 8*u, Long::sum);
            composition(com.gregtech.gregtech.content.machine.OriginalMachineMaterialData.block(id).orElseThrow(), expected);
            var recipe = recipes.stream().filter(r -> r.output().equals("gregtech:" + id)).findFirst().orElseThrow();
            check(recipe.pattern().equals(rf ? (cooler ? java.util.List.of("PSP", "PMP", "PSP") : java.util.List.of("SSS", "SMS", "SSS"))
                            : (cooler ? java.util.List.of("WPw", "CMC", "xPW") : java.util.List.of("TCT", "CMC", "TCd")))
                            && recipe.count() == 1 && recipe.empty() && !recipe.unpack(), "Original thermal shaped pattern and stored input protection " + id);
            if (rf) check(recipe.key().get('M').name().equals("gregtech:" + id.replace("flux_", "electric_")), "Source RF upgrade exact electric tier " + id);
            else check(recipe.key().get(cooler ? 'W' : 'C').kind().equals(cooler ? "cable" : "wire")
                            && recipe.key().get(cooler ? 'W' : 'C').name().equals(Integer.toString(cooler ? 1 : 1 << i)),
                    "Source thermal cable/wire widths " + id);
            var state = new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.State(spec);
            var simulation = state.inject(0, input, Long.MAX_VALUE, false);
            check(simulation.consumed() == 2 && simulation.energy() == 0 && !simulation.overloaded(), "Pure source bounded thermal injection " + id);
            var calls = new long[2];
            var step = state.tick(input, false, (size, amount) -> { calls[0] = size; calls[1] = amount; return amount; });
            check(calls[0] == 1 && calls[1] == output && step.energy() == 0 && step.possible() && step.emitted(),
                    "Thermal output is HU/CU unit packets, one waste tick " + id);
            state.mode(1);
            step = state.tick(2*input, true, (size, amount) -> { calls[0] = size; calls[1] = amount; return amount; });
            check(calls[0] == 1 && calls[1] == 2*output*15/16 && step.energy() == input/8 && step.visual() == 0,
                    "Stopped thermal buffer still converts; mode floors output and wastes15/16 once " + id);
            state.inject(0, -input, 1, true); state.mode(0);
            state.tick(input, false, (size, amount) -> { calls[0] = size; return amount; });
            check(calls[0] == 1, "Source thermal output remains positive on negative input " + id);
            check(com.gregtech.gregtech.content.energy.OriginalThermalConverter.contactDamage(spec) == (cooler ? 0 : Math.min(10F, output/10F)),
                    "Source heater rated output contact damage " + id);
            var creative = com.gregtech.gregtech.content.creative.SourceCreativeCatalog.entry(id);
            check(creative != null && creative.family().equals(cooler ? "coolers" : "heaters"), "Source explicit thermal creative family " + id);
            seen++;
        }
        check(seen == 20 && recipes.size() == 20, "All twenty source thermal registrations and recipes");
    }

    private static void originalBipolarMagnets() {
        // Fixed source registrations10031..35 /11031..35: six uninsulated wires,
        // 8U casing, and an RF upgrade adding eight1U long rods; not nominal hull-only data.
        String[] tiers = {"lv", "mv", "hv", "ev", "iv"};
        String[] hulls = {"SteelGalvanized", "Aluminium", "StainlessSteel", "Chromium", "Titanium"};
        String[] fluxHulls = {"Lead", "Invar", "Electrum", "EnderiumBase", "Enderium"};
        var specs = com.gregtech.gregtech.content.energy.MagnetMachineDefinitions.specifications();
        var rows = com.gregtech.gregtech.content.energy.OriginalMagnetCrafting.rows();
        for (boolean rf : new boolean[]{false, true}) for (int tier = 0; tier < 5; tier++) {
            String id = (rf ? "flux_magnet_" : "electromagnet_") + tiers[tier];
            var spec = specs.stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
            long input = (32L << (2*tier)) * (rf ? 4 : 1), output = 16L << (2*tier), u = GTValues.U;
            var profile = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(spec, false);
            check(spec.inputRate() == input && spec.outputRate() == output && spec.capacity() == 2*input
                            && spec.material() == GTMaterialRegistry.get((rf ? fluxHulls : hulls)[tier]), "Source magnet ratings and identity " + id);
            check(spec.outType() == com.gregtech.gregtech.data.GregTechTags.Energy.MU
                            && !com.gregtech.gregtech.data.GregTechTags.Energy.isSizeIrrelevant(spec.outType()), "Source signed MU packets " + id);
            check(profile.input().minimum() == input/2 && profile.input().maximum() == 2*input
                            && profile.output().minimum() == output/2 && profile.output().maximum() == 2*output
                            && profile.inputFaceKey().equals("gt.lang.face.any.but.front.back")
                            && profile.outputFaceKey().equals("gt.lang.face.front.back")
                            && profile.efficiency() == 10000 && !profile.monkeyWrench(), "Source bipolar total efficiency, faces and tool hints " + id);
            var expected = new java.util.HashMap<String,Long>();
            expected.put(hulls[tier], 8*u); expected.put(tier < 2 ? "Copper" : "AnnealedCopper", (3L << tier)*u);
            if (rf) expected.merge(fluxHulls[tier], 8*u, Long::sum);
            composition(com.gregtech.gregtech.content.machine.OriginalMachineMaterialData.block(id).orElseThrow(), expected);
            var row = rows.stream().filter(r -> r.output().equals("gregtech:" + id)).findFirst().orElseThrow();
            check(row.path().equals("magnets/" + id) && row.empty() && row.count() == 1 && !row.unpack()
                    && row.pattern().equals(rf ? java.util.List.of("SSS", "SMS", "SSS") : java.util.List.of("CxC", "CMC", "CwC")), "Source crafting pattern and retained recipe identity " + id);
            if (rf) check(row.key().get('M').name().equals("gregtech:electromagnet_" + tiers[tier])
                    && row.key().get('S').name().equals("stickLong"), "Source eight long rods and exact base tier " + id);
            else check(row.key().get('C').kind().equals("wire") && row.key().get('C').name().equals(Integer.toString(1 << tier))
                            && row.key().get('C').material() == (tier < 2 ? MaterialGroups.Cu : Materials.AnnealedCopper), "Source bare wire width and ANY.Cu group " + id);
            var state = new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.State(spec);
            var simulated = state.inject(0, input, Long.MAX_VALUE, false);
            check(simulated.energy() == 0 && simulated.consumed() == 2 && !simulated.overloaded(), "Source pure bounded magnet injection " + id);
            var calls = new long[2];
            state.inject(0, -input, 1, true);
            var tick = state.tick(input, false, (size, amount) -> { calls[0] = size; calls[1] = amount; return 0; });
            check(calls[0] == output && calls[1] == 1 && tick.energy() == 0 && tick.possible() && !tick.emitted() && tick.visual() == 2,
                    "Source input sign never flips bipolar poles; possible and emitted differ without receiver " + id);
            tick = state.tick(0, false, (size, amount) -> { throw new AssertionError("empty magnet emitted"); });
            check(tick.visual() == 2 && !tick.possible(), "Source activity overlay lingers in64-bit history " + id);
            state.mode(1);
            tick = state.tick(2*input, true, (size, amount) -> { calls[0] = size; return 2; });
            check(calls[0] == 2*output*15/16 && tick.energy() == input/8 && tick.visual() == 0 && tick.possible() && tick.emitted(),
                    "Source stopped residual buffer still reaches both poles with one fixed waste tick " + id);
            state.restore(new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.Snapshot(15, false, true, true, false, false));
            check(state.mode() == 15 && state.possible() && !state.emitted() && state.visual(false) == 0, "Source saved mode/emission flags reset unsaved visual history " + id);
            var entry = com.gregtech.gregtech.content.creative.SourceCreativeCatalog.entry(id);
            check(entry != null && entry.family().equals("magnets"), "Source magnet creative family " + id);
        }
        check(specs.size() == 10 && rows.size() == 10, "All ten original bipolar magnet variants");
    }

    private static void originalSteamTurbines() {
        // Independent original1512..1548 rows: four4.25U rotors +14U double casing
        // + two4U gears +1U long rod. Source displayed rotor material differs from Kinetic_T hull.
        String[] suffixes = {"bronze","brass","invar","steel","chromium","ironwood","steeleaf","thaumium",
                "titanium","fiery_steel","aluminium","magnalium","void_metal","trinitanium","graphene"};
        int[] sourceIds = {1512,1515,1518,1522,1525,1527,1528,1529,1530,1531,1535,1538,1540,1545,1548};
        long[] inputs = {48,72,96,192,288,384,384,384,768,768,1152,1536,2304,3072,6144};
        long[] outputs = {16,24,32,64,96,128,128,128,256,256,384,512,768,1024,2048};
        var variants = com.gregtech.gregtech.content.energy.OriginalSteamTurbines.VARIANTS;
        var rows = com.gregtech.gregtech.content.energy.OriginalSteamTurbines.rows();
        var specs = com.gregtech.gregtech.content.energy.OriginalSteamTurbines.specifications();
        for (int i=0;i<15;i++) {
            var v = variants.get(i); var spec = specs.get(i); long input = inputs[i], output = outputs[i], u = GTValues.U;
            var hull = i<3 ? Materials.Bronze : i<8 ? Materials.Steel : i<12 ? Materials.Titanium : Materials.Tungstensteel;
            check(v.id().equals("steam_turbine_"+suffixes[i]) && v.sourceId()==sourceIds[i] && spec.inputRate()==input
                    && spec.outputRate()==output && spec.capacity()==2*input && spec.material().resolve()==hull.resolve(), "Source turbine IDs, hull and rates " + v.id());
            check(spec.material().getColor()==hull.getColor(), "Source Kinetic_T hull appearance " + v.id());
            var expected = new java.util.HashMap<String,Long>();
            var rotor = v.rotor()==com.gregtech.gregtech.data.MaterialGroups.Steel ? Materials.Steel : v.rotor();
            expected.put(hull.resolve().getName(),23*u); expected.merge(rotor.resolve().getName(),17*u,Long::sum);
            composition(com.gregtech.gregtech.content.machine.OriginalMachineMaterialData.block(v.id()).orElseThrow(),expected);
            var row = rows.get(i);
            check(row.pattern().equals(java.util.List.of("TwT","GSG","TMT")) && row.count()==1 && row.empty()
                    && row.key().get('T').name().equals("rotor") && row.key().get('G').name().equals("gearGt")
                    && row.key().get('M').name().equals("casingMachineDouble") && row.key().get('S').name().equals("stickLong"), "Source four rotors, double hull and stored-input protection " + v.id());
            var profile = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(spec,false);
            check(profile.input().minimum()==input/2 && profile.input().maximum()==2*input && profile.output().minimum()==output/2
                    && profile.output().maximum()==2*output && profile.efficiency()==6666 && profile.monkeyWrench()
                    && profile.inputFaceKey().equals("gt.lang.face.back"), "Source Steam LH profile and motor hint " + v.id());
            var split = com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(0,0,2*input,199,input);
            check(split.energy()==input && split.pending()==input && split.consumed()==2*input
                    && split.condensate()==(199+2*input)/200 && split.remainder()==(199+2*input)%200, "Source whole batch split and local200L counter " + v.id());
            var state = new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.State(spec);
            var packet = new long[2];
            var first = state.tick(split.energy(),true,(size,amount)->{packet[0]=size;packet[1]=amount;return amount;});
            check(packet[0]==output && packet[1]==1 && first.energy()==0 && first.possible() && first.emitted() && first.visual()==0,
                    "Source stopped first half still converts " + v.id());
            state.reverse();
            var next = com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(first.energy(),split.pending(),2*input,split.remainder(),input);
            var second = state.tick(next.energy(),false,(size,amount)->{packet[0]=size;return amount;});
            check(packet[0]==-output && next.pending()==0 && next.consumed()==0 && second.energy()==0 && second.visual()==2,
                    "Source reverse preserves pending half without consuming another batch " + v.id());
            state.mode(1);
            var mode = state.tick(2*input,false,(size,amount)->{packet[0]=size;return amount;});
            check(packet[0]==-(2*output*15/16) && mode.energy()==2*input-(2*input*15+15)/16,
                    "Source turbine signed mode-limited packet and ceil waste " + v.id());
            var excess = new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.State(spec);
            var unloaded = excess.tick(4*input,false,(size,amount)->{throw new AssertionError("source excess Steam emitted");});
            check(!unloaded.overloaded() && unloaded.energy()==0,"Source first load tick clears excessive turbine batch " + v.id());
            excess.tick(0,false,(size,amount)->0);
            check(excess.tick(4*input,false,(size,amount)->0).overloaded(), "Source later steam excess overloads instead of EU/RF clipping " + v.id());
            var creative = com.gregtech.gregtech.content.creative.SourceCreativeCatalog.entry(v.id());
            check(creative!=null && creative.family().equals("turbines"),"Source original turbine creative family " + v.id());
        }
        check(variants.size()==15 && rows.size()==15 && specs.size()==15,"All fifteen source turbine variants");
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
        // Original Loader_MultiTileEntities uses the shared ceramic material statistics.
        var ceramic = com.gregtech.gregtech.api.machine.InitialSmelteryDefinitions.ceramicCrucible();
        check(ceramic.id().equals("smelting_crucible_ceramic") && ceramic.gt6MetaId() == 1005,
                "Original ceramic registry path and meta");
        check(ceramic.material() == GTMaterialRegistry.get(8225), "Same full shared material identity");
        check(ceramic.meltingPointK() == 2000 && ceramic.boilingPointK() == 4000, "Explicit hull Kelvin bounds");
        check(Double.doubleToLongBits(ceramic.hullDensity()) == Double.doubleToLongBits((double)0.866478F),
                "Original canonical ceramic density retained");
        check(ceramic.hullDensity() == Materials.Ceramic.getDensity(), "Source hull uses live material density");
        check(ceramic.hullMaterialUnits() == 4540536000L && ceramic.hullMaterialUnits() == 7 * GTValues.U,
                "Original7U shell, not a144-unit replacement");
        check(ceramic.meltDownTemperatureK() == 2500, "Original truncated(2000*1.25) hull limit");
        check(ceramic.hardness() == 5.0F && ceramic.blastResistance() == 5.0F && !ceramic.acidProof(),
                "Original ceramic block and acid parameters");
        check(!ceramic.isStoneTier(), "Ceramic shell is not63U stone tier");
        near(ceramic.thermalMassKg(), 673.9273528141471D, "Independent original7U shell mass");

        // Original Loader_MultiTileEntities registers the ceramic mold with5U and its own hardness.
        var mold = com.gregtech.gregtech.api.machine.InitialSmelteryDefinitions.ceramicMold();
        check(mold.id().equals("mold_ceramic") && mold.gt6MetaId() == 1055, "Original mold path and meta offset");
        check(mold.material() == ceramic.material() && mold.hullDensity() == ceramic.hullDensity(),
                "Mold companion uses the same canonical material density");
        check(mold.hullMaterialUnits() == 3243240000L && mold.hullMaterialUnits() == 5 * GTValues.U,
                "Original5U mold shell");
        check(mold.meltDownTemperatureK() == 2500, "Original mold Kelvin limit");
        near(mold.thermalMassKg(), 481.3766805815337D, "Independent original5U mold mass");
        check(CrucibleSpec.BASIN_HULL_UNITS == 5 * GTValues.U
                        && CrucibleSpec.CROSSING_HULL_UNITS == 5 * GTValues.U
                        && CrucibleSpec.FAUCET_HULL_UNITS == 3 * GTValues.U,
                "Other original companion weights remain unchanged");
        var defaults = CrucibleSpec.of("default_hull", Materials.Ceramic, 1005, 5, 5, false);
        check(defaults.hullDensity() == Materials.Ceramic.getDensity(), "Retained material-derived overload");
        var stone = CrucibleSpec.of("stone_hull", Materials.Ceramic, 1005, 5, 5, false,
                CrucibleSpec.STONE_CRUCIBLE_HULL_UNITS);
        check(stone.isStoneTier() && stone.hullMaterialUnits() == 63 * GTValues.U, "Legacy63U construction predicate never determines heat capacity");
    }

    private static void chargedCrucibleHeatRequirement() {
        var ceramic = com.gregtech.gregtech.api.machine.InitialSmelteryDefinitions.ceramicCrucible();
        // Decimal expected values were calculated independently from Java float32 source densities.
        double copperKg = com.gregtech.gregtech.api.material.MaterialMass.kilograms(Materials.Copper, 3 * GTValues.U);
        double tinKg = com.gregtech.gregtech.api.material.MaterialMass.kilograms(Materials.Tin, GTValues.U);
        near(copperKg, 2986.6666763956573D, "Original three copper units mass");
        near(tinKg, 809.6666857781968D, "Original one tin unit mass");
        double chargedKg = ceramic.thermalMassKg() + copperKg + tinKg;
        near(chargedKg, 4470.260714988001D, "Fixed3Cu+1Sn with original ceramic shell mass");
        check(ThermalStep.requiredEnergy(chargedKg) == 45, "Charged vessel needs45HU for one Kelvin");
        check(ThermalStep.requiredEnergy(ceramic.thermalMassKg()) == 7, "Empty original hull needs7HU/K");
    }

    private static void originalSmelteryParametersAndShapes() {
        var definitions = com.gregtech.gregtech.api.machine.OriginalSmelteryDefinitions.all();
        check(definitions.size() == 195, "39 original vessels with four source companions each");
        long u = GTValues.U;
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("smelting_crucible_stone").orElseThrow(), java.util.Map.of("Stone",63*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("mold_stone").orElseThrow(), java.util.Map.of("Stone",45*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("crucible_faucet_stone").orElseThrow(), java.util.Map.of("Stone",27*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("smelting_crucible_quartz").orElseThrow(), java.util.Map.of("SiliconDioxide",7*u));
        composition(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("smelting_crucible_carbon").orElseThrow(), java.util.Map.of("Graphene",7*u));
        check(com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block("smelting_crucible_basalt").isEmpty(), "OP.stone.mAmount=-1 supplies no positive source REV quantity");
        var stone = com.gregtech.gregtech.api.machine.OriginalSmelteryDefinitions.get(1000);
        near(stone.smeltingThermalMassKg(),777.7777769999999D,"Stone vessel heats7U while construction contains63U");
        var steel = com.gregtech.gregtech.api.machine.OriginalSmelteryDefinitions.get(1022);
        check(steel.meltDownTemperatureK()==2557,"Source long cast truncates2046*1.25 instead of rounding to2558K");
        check(com.gregtech.gregtech.api.machine.OriginalSmelteryDefinitions.get(1051).hardness()==1F
                && com.gregtech.gregtech.api.machine.OriginalSmelteryDefinitions.get(1701).blastResistance()==5F,
                "Basalt mold/faucet own source physical NBT does not inherit15/15 vessel parameters");
        check(com.gregtech.gregtech.api.machine.SmelteryCompanionDefinitions.copy(
                com.gregtech.gregtech.api.machine.OriginalSmelteryDefinitions.get(1001),"mold_basalt",50,5*u)
                ==com.gregtech.gregtech.api.machine.OriginalSmelteryDefinitions.get(1051),
                "Forge compatibility registry entry point uses the exact original companion record");
        check(com.gregtech.gregtech.api.machine.OriginalSmelteryDefinitions.get(1041).meltingPointK()==1873,"HSLA canonical source melting point");
        int positive=0;
        for(var spec:definitions) {
            var policy=com.gregtech.gregtech.data.SourceBlockProperties.block(spec.id()).orElseThrow();
            check(policy.sourceId()==spec.gt6MetaId() && policy.tool().equals("pickaxe"),"Smeltery source ID and tool identity "+spec.id());
            check(policy.handHarvestable()==(spec.id().startsWith("crucible_faucet_")
                    || spec.id().startsWith("mold_") && !spec.id().startsWith("mold_basin_")),
                    "Original utility mold/faucet hand exemption "+spec.id());
            if(spec.hullMaterialUnits()>0)positive++;
        }
        check(positive==155,"155 positive original quantities;40 nonpositive stone-prefix results remain undefined");
        var large=CrucibleSpec.of("large_crucible_fixture",Materials.Ceramic,0,5,5,false,100*u);
        near(large.smeltingThermalMassKg(),9627.533611630674D,"Large vessel keeps100U heat capacity");
        var shapes=com.gregtech.gregtech.api.machine.crucible.MoldShapes.recipes();
        check(com.gregtech.gregtech.api.machine.crucible.MoldShapes.recipe(0)==null,"Unselected mold has no cast recipe");
        check(com.gregtech.gregtech.api.machine.crucible.MoldShapes.recipe(1<<30).itemPrefix()==com.gregtech.gregtech.data.MaterialPrefix.nugget
                && com.gregtech.gregtech.api.machine.crucible.MoldShapes.requiredMaterialUnits(1<<30)==0,"Legacy high bits retain source nugget fallback but never consume material");
        check(com.gregtech.gregtech.api.machine.crucible.MoldShapes.requiredMaterialUnits((1<<30)|1)==GTValues.U9,
                "Unknown one-cell cavity consumes one nugget regardless of legacy high bits");
        check(com.gregtech.gregtech.api.machine.crucible.MoldShapes.requiredMaterialUnits(0x1ffffff)==u,"Full25-cell plate uses1U");
        int ingot=0b00000_11111_11111_11111_00000;
        check(com.gregtech.gregtech.api.machine.crucible.MoldShapes.recipe(ingot).itemPrefix()==com.gregtech.gregtech.data.MaterialPrefix.ingot,
                "Pinned original three-row ingot shape");
        check(com.gregtech.gregtech.api.machine.crucible.MoldShapes.requiredMaterialUnits(ingot|(1<<30))==u,"Known ingot shape ignores extra legacy bits");
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

    private static void originalAdvancedControllerParameters() {
        var data=com.gregtech.gregtech.content.multiblock.OriginalAdvancedControllerData.parameters("fusionreactor","fusion_reactor_main");
        check(data.energyType().equals("TU")&&data.energyIn()==8192&&data.energyInMin()==1&&data.energyInMax()==16384&&data.parallelLimit()==1,"Original17198 accepted TU / charged LU input8192, range1..16384");
        check(data.hardness()==12.5f&&data.blastResistance()==12.5f&&data.faceConfig().itemInputs()==63&&data.faceConfig().fluidOutputs()==63&&data.faceConfig().itemAutoOutput()==-1,"Original17198 strength/ANY/manual outputs");
        var implosion=com.gregtech.gregtech.content.multiblock.OriginalMultiblockMachineParameters.implosionCompressor();
        check(implosion.energyType().equals("TU")&&implosion.energyIn()==1&&implosion.energyInMax()==16&&implosion.parallelLimit()==64&&implosion.hardness()==12.5f,"Original17110 TU/64parallel/12.5 strength");
        check(implosion.faceConfig().itemInputs()==63&&implosion.faceConfig().fluidInputs()==63&&implosion.faceConfig().itemAutoInput()==-1&&implosion.faceConfig().itemAutoOutput()==0&&implosion.faceConfig().fluidAutoOutput()==0,"Original17110 ANY and bottom item/fluid auto output");
        var matter=com.gregtech.gregtech.content.machine.BasicMachineCatalog.specifications().stream().filter(s->s.machineName().equals("largemassfab")).findFirst().orElseThrow();
        check(matter.energyType().equals("QU")&&matter.energyIn()==1&&matter.energyInMin()==1&&matter.energyInMax()==2097152&&matter.parallelLimit()==64&&matter.hardness()==6&&matter.blastResistance()==6,"Original17199 QU/64parallel/range1..2097152/6 strength");
        check(matter.faceConfig().itemInputs()==63&&matter.faceConfig().itemOutputs()==63&&matter.faceConfig().fluidInputs()==63&&matter.faceConfig().fluidOutputs()==63&&matter.faceConfig().itemAutoInput()==-1&&matter.faceConfig().fluidAutoInput()==-1&&matter.faceConfig().itemAutoOutput()==0&&matter.faceConfig().fluidAutoOutput()==0,"Original17199 ANY and bottom auto outputs without fabricated input automation");
        var rules=com.gregtech.gregtech.content.multiblock.OriginalAdvancedControllerData.class;
        check(com.gregtech.gregtech.content.multiblock.OriginalAdvancedControllerData.cheapOverclocking("largemassfab")&&!com.gregtech.gregtech.content.multiblock.OriginalAdvancedControllerData.cheapOverclocking("fusionreactor")&&!com.gregtech.gregtech.content.multiblock.OriginalAdvancedControllerData.cheapOverclocking("implosioncompressor"),"Original three-controller cheap flags");
        check(com.gregtech.gregtech.content.multiblock.OriginalAdvancedControllerData.chargedEnergy("fusionreactor").equals("LU")&&com.gregtech.gregtech.content.multiblock.OriginalAdvancedControllerData.chargedEnergy("largemassfab").equals("TU"),"Original only fusion has charged LU row");
        int rows=0;
        for(var name:java.util.List.of("implosioncompressor","fusionreactor","largemassfab"))rows+=com.gregtech.gregtech.content.multiblock.OriginalAdvancedControllerData.structureKeys(name).size();
        check(rows==17,"Original three specialized structures2+7+8");
        var original=com.gregtech.gregtech.data.BasicMachineOriginalParams.find("largemassfab",1);
        check(original.originalName().equals("Large Matter Fabricator")&&original.parallelDuration(),"Original17199 omitted registration now present with parallel-duration flag");
        long u=com.gregtech.gregtech.api.material.GTValues.U;
        composition(com.gregtech.gregtech.content.machine.OriginalMachineMaterialData.find("largemassfab",1).orElseThrow(),java.util.Map.of("Osmium",128*u,"Titanium",32*u,"NetherStar",8*u,"Lead",36*u));
        var harvest=com.gregtech.gregtech.data.SourceBlockProperties.basics().get("largemassfab/1");
        check(harvest.sourceId()==17199&&harvest.tool().equals("wrench")&&!harvest.handHarvestable()&&harvest.material().resolve()==com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Pb"),"Original17199 source harvest identity and lead hull");
    }
}
