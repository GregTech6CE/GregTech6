package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.definition.DefinitionCatalog;
import com.gregtech.gregtech.api.fluid.FluidPipeChannels;
import com.gregtech.gregtech.api.fluid.FluidPipeSafety;
import com.gregtech.gregtech.api.energy.EnergyPackets;
import com.gregtech.gregtech.api.energy.GTVoltageTiers;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMath;
import com.gregtech.gregtech.api.material.AtomicProperties;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.multiblock.PartBindings;
import com.gregtech.gregtech.api.recipe.MachineWorkCost;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Fixed behavior samples: independent expected numbers, never a second copy of the algorithms. */
public final class CoreBehaviorContracts {
    private static int assertions;

    public static void main(String[] args) {
        workCostGoldens();
        machineEnergyGoldens();
        sourceSteamConversion();
        itemPipeRoutingAndDelivery();
        fluidPipeChannels();
        fluidPipeSafety();
        fluidPipeCatalog();
        voltageGoldens();
        materialAndCrucibleUnits();
        signedPacketsAndFiniteBuffer();
        atomicMassBounds();
        definitionIdentityAndSnapshot();
        multiblockOwnershipLifecycle();
        addonLifecycle();
        miniaturePortalSignals();
        bedrockAndBoilerSourceSamples();
        scannerEnergySourceSamples();
        autocraftingSourceSamples();
        conversionPermissionSourceSamples();
        formConversionSourcePositions();
        explosiveStorageSourceSamples();
        externalOreSourceSamples();
        externalGrindingSourceSamples();
        externalAdditionalGrindingSourceSamples();
        externalThermalSourceSamples();
        dustListenerSourceSamples();
        assertions += SetupOnceContracts.verify();
        assertions += SolarPanelEnergyContracts.verify();
        assertions += OriginWorldgenSamples.verify();
        assertions += LongDistanceSourceContracts.verify();
        assertions += com.gregtech.gregtech.content.compat.CompatSpecs.check();
        assertions += CanvasSourceContracts.verify();
        System.out.println("Core behavior contracts passed: " + assertions + " assertions in 32 groups (Java 17; no game dependencies)");
    }

    private static void externalGrindingSourceSamples() {
        // Handler:117-119,129-131,158-160; costs round after multiplying, not before.
        var rows = com.gregtech.gregtech.content.recipe.ExternalOreProcessingRules.GRINDING;
        equal(25, rows.size(), "eleven adaptive Shredder, eight Anvil and six Mortar routes");
        long[] soft0 = {34, 34, 50}, hard0 = {541, 541, 797};
        long[] soft2 = {102, 102, 150}, hard2 = {1622, 1622, 2390};
        long[] anvil0 = {34, 34, 48}, anvil2 = {102, 102, 144};
        for (int i = 0; i < 3; i++) {
            var shredder = rows.get(i);
            equal(soft0[i], shredder.duration(0, true), "source mortar Shredder cost including fines " + i);
            equal(hard0[i], shredder.duration(0, false), "source hard Shredder cost including fines " + i);
            equal(soft2[i], shredder.duration(2, true), "source quality multiplication before rounding " + i);
            equal(hard2[i], shredder.duration(2, false), "source hard quality multiplication before rounding " + i);
            var anvil = rows.get(i + 3);
            equal(anvil0[i], anvil.duration(0, true), "source Anvil uses max of input and output weights " + i);
            equal(anvil2[i], anvil.duration(2, true), "source quality-scaled Anvil cost " + i);
            check(anvil.requiresEmptySlot(), "source Anvil preserves the empty workpiece " + i);
            check(!shredder.requiresEmptySlot(), "Shredder has no Anvil empty workpiece " + i);
        }
        equal(3, rows.get(2).dustCount(), "pebbles yield three dust in the Shredder");
        equal(2, rows.get(5).dustCount(), "pebbles yield only two dust in the source Anvil row");
    }

    private static void externalAdditionalGrindingSourceSamples() {
        // OP:143-144,149-151; Handler:84,86-87,89,120-124,132-136,161-165.
        var rows = com.gregtech.gregtech.content.recipe.ExternalOreProcessingRules.GRINDING;
        var forms = new com.gregtech.gregtech.data.MaterialPrefix[]{
                com.gregtech.gregtech.data.MaterialPrefix.clump, com.gregtech.gregtech.data.MaterialPrefix.reduced,
                com.gregtech.gregtech.data.MaterialPrefix.crystalline, com.gregtech.gregtech.data.MaterialPrefix.cleanGravel,
                com.gregtech.gregtech.data.MaterialPrefix.cluster};
        long[] weights = {648_648_000L, 648_648_000L, 648_648_000L, 648_648_000L, 1_945_944_000L};
        long[] soft0 = {16, 16, 16, 16, 48}, hard0 = {256, 256, 256, 256, 768};
        long[] soft2 = {48, 48, 48, 48, 144}, hard2 = {768, 768, 768, 768, 2304};
        int[] dustCounts = {1, 1, 1, 1, 3};
        for (int i = 0; i < forms.length; i++) {
            equal(weights[i], forms[i].getMaterialWeight(), "source external form weight " + forms[i].getName());
            var shredder = rows.get(i + 6);
            var anvil = rows.get(i + 11);
            check(shredder.map().equals("Shredder") && shredder.input() == forms[i], "source additional Shredder input " + i);
            check(anvil.map().equals("Anvil") && anvil.input() == forms[i], "source additional Anvil input " + i);
            equal(dustCounts[i], shredder.dustCount(), "source fixed Shredder dust count " + i);
            equal(dustCounts[i], anvil.dustCount(), "source fixed Anvil dust count " + i);
            check(!shredder.fines() && !anvil.fines(), "source rows produce no invented fines " + i);
            equal(soft0[i], shredder.duration(0, true), "source additional mortar Shredder duration " + i);
            equal(hard0[i], shredder.duration(0, false), "source additional hard Shredder duration " + i);
            equal(soft2[i], shredder.duration(2, true), "source additional quality-scaled mortar Shredder " + i);
            equal(hard2[i], shredder.duration(2, false), "source additional quality-scaled hard Shredder " + i);
            equal(soft0[i], anvil.duration(0, true), "source additional Anvil duration " + i);
            equal(soft2[i], anvil.duration(2, true), "source additional quality-scaled Anvil " + i);
            check(anvil.requiresEmptySlot() && !shredder.requiresEmptySlot(), "source additional Anvil empty workpiece " + i);
            check(!anvil.pulverizedRemains() && !shredder.pulverizedRemains(), "explicit source dust counts stay explicit " + i);
        }
        var mortarForms = new com.gregtech.gregtech.data.MaterialPrefix[]{
                com.gregtech.gregtech.data.MaterialPrefix.cleanGravel, com.gregtech.gregtech.data.MaterialPrefix.crystalline,
                com.gregtech.gregtech.data.MaterialPrefix.reduced, com.gregtech.gregtech.data.MaterialPrefix.clump};
        for (int i = 0; i < mortarForms.length; i++) {
            var row = rows.get(i + 16);
            check(row.map().equals("Mortar") && row.input() == mortarForms[i], "source Mortar input; no cluster Mortar route " + i);
            check(row.pulverizedRemains() && !row.fines() && !row.requiresEmptySlot(), "source Mortar uses OM remains without Anvil workpiece " + i);
            equal(16, row.duration(0, true), "source one-unit Mortar duration " + i);
            equal(48, row.duration(2, true), "source quality-scaled Mortar duration " + i);
        }
        var external = com.gregtech.gregtech.content.recipe.ExternalOreProcessingRules.EXTERNAL_FORMS;
        equal(13, external.size(), "thirteen typed external forms");
        for (var form : external) check(!com.gregtech.gregtech.api.material.MaterialItemDefinitions.candidatePrefixes().contains(form),
                "external metadata creates no GT-owned items " + form.getName());
        // OM:370-371 / UT.Code.units:1677-1687, target amount multiplies the input then floors.
        equal(972_972_000L, com.gregtech.gregtech.content.recipe.PulverizationRules.amount(1_945_944_000L, 324_324_000L), "three input units at a half-unit pulver target");
        equal(648_648_000L, com.gregtech.gregtech.content.recipe.PulverizationRules.amount(324_324_000L, 1_297_296_000L), "half input unit at a double-unit pulver target");
        equal(0, com.gregtech.gregtech.content.recipe.PulverizationRules.amount(1, 324_324_000L), "source pulver target rounds down fractional atoms");
        equal(41_513_472_000L, com.gregtech.gregtech.content.recipe.PulverizationRules.amount(41_513_472_000L, 648_648_000L), "64-unit identity pulverization avoids overflowing an intermediate product");
        equal(20_756_736_000L, com.gregtech.gregtech.content.recipe.PulverizationRules.amount(41_513_472_000L, 324_324_000L), "64-unit half target avoids overflowing an intermediate product");
        equal(0, com.gregtech.gregtech.content.recipe.PulverizationRules.amount(648_648_000L, 0), "source zero target yields zero");
    }

    private static void externalThermalSourceSamples() {
        // OP:152,185; Handler:85,88; Furnace:151-160/190-201 and RecipeMapCrucible:68-76.
        var grinding = com.gregtech.gregtech.content.recipe.ExternalOreProcessingRules.GRINDING;
        String[] inputs = {"dirtyGravel", "crystal"};
        for (int i = 0; i < inputs.length; i++) {
            var row = grinding.get(i + 20);
            check(row.map().equals("Mortar") && row.input().getName().equals(inputs[i]), "source last external Mortar input " + i);
            equal(648_648_000L, row.input().getMaterialWeight(), "source dirty gravel and crystal each weigh U " + i);
            check(row.pulverizedRemains() && !row.fines() && !row.requiresEmptySlot(), "source Mortar has only pulverized remains " + i);
            equal(16, row.duration(0, true), "source last Mortar duration " + i);
            equal(48, row.duration(2, true), "source last Mortar quality duration " + i);
        }
        var furnace = com.gregtech.gregtech.content.recipe.FurnaceSmeltingRules.EXTERNAL;
        String[] furnaceInputs = {"rawOreChunk", "chunk", "rubble", "pebbles", "cluster", "cleanGravel", "dirtyGravel", "crystalline", "reduced"};
        long[] amounts = {243_243_000L, 1_297_296_000L, 1_297_296_000L, 1_945_944_000L, 1_945_944_000L, 648_648_000L, 648_648_000L, 648_648_000L, 648_648_000L};
        equal(12, furnace.size(), "source nine ore and three dirty-dust furnace listeners; no clump or crystal listener");
        for (int i = 0; i < furnaceInputs.length; i++) {
            check(furnace.get(i).input().getName().equals(furnaceInputs[i]), "source external furnace listener " + i);
            equal(amounts[i], furnace.get(i).amount(), "source furnace fixed/prefix amount " + i);
        }
        equal(486_486_000L, com.gregtech.gregtech.content.recipe.FurnaceSmeltingRules.amount(648_648_000L, 486_486_000L), "Cassiterite dust source 3/4U smelting ratio is applied only once");
        equal(108_108_000L, com.gregtech.gregtech.content.recipe.FurnaceSmeltingRules.amount(648_648_000L, 108_108_000L), "Malachite dust source 1/6U smelting ratio is applied only once");
        equal(1_945_944_000L, com.gregtech.gregtech.content.recipe.FurnaceSmeltingRules.amount(1_945_944_000L, 648_648_000L), "three-unit cluster keeps three-unit furnace output");
        equal(1, com.gregtech.gregtech.content.recipe.FurnaceSmeltingRules.experience(243_243_000L, 0), "fractional raw chunk furnace experience rounds up");
        equal(3, com.gregtech.gregtech.content.recipe.FurnaceSmeltingRules.experience(1_945_944_000L, 0), "three-unit cluster furnace experience");
        equal(9, com.gregtech.gregtech.content.recipe.FurnaceSmeltingRules.experience(1_945_944_000L, 2), "cluster furnace experience scales by input tool quality");
        equal(0, com.gregtech.gregtech.content.recipe.FurnaceSmeltingRules.experience(0, 2), "empty furnace output earns no experience");
        var crucible = com.gregtech.gregtech.content.recipe.ExternalOreProcessingRules.CRUCIBLE_FORMS;
        String[] crucibleInputs = {"chunk", "rubble", "pebbles", "cluster", "cleanGravel", "dirtyGravel", "crystalline", "reduced"};
        equal(8, crucible.size(), "source eight external crucible display forms");
        for (int i = 0; i < crucibleInputs.length; i++) check(crucible.get(i).getName().equals(crucibleInputs[i]), "source crucible form " + i);
    }

    private static void dustListenerSourceSamples() {
        // OP:158-160,570-573; Handler:114-116/126-128; Furnace:141-143; Washing:64-89.
        var grinding = com.gregtech.gregtech.content.recipe.ExternalOreProcessingRules.GRINDING;
        String[] names = {"dustImpure", "dustPure", "dustRefined"};
        long[] weights = {720_720_000L, 792_792_000L, 864_864_000L};
        long[] soft0 = {18, 20, 22}, hard0 = {285, 313, 342};
        long[] soft2 = {54, 59, 64}, hard2 = {854, 939, 1024};
        for (int i = 0; i < 3; i++) {
            var row = grinding.get(i + 22);
            check(row.map().equals("Shredder") && row.input().getName().equals(names[i]), "source dirty dust Shredder input " + i);
            equal(weights[i], row.input().getMaterialWeight(), "source dirty dust weight includes impurities " + i);
            equal(1, row.dustCount(), "source dirty dust main output " + i);
            equal(i + 1, row.fineCount(), "source dirty dust explicit fine count " + i);
            check(row.excludesBedrock() && !row.requiresEmptySlot() && !row.pulverizedRemains(), "source dirty dust flags " + i);
            check(!row.allows(com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bedrock), "source dirty dust rejects Bedrock " + i);
            equal(soft0[i], row.duration(0, true), "source dirty dust mortar speed " + i);
            equal(hard0[i], row.duration(0, false), "source dirty dust hard speed " + i);
            equal(soft2[i], row.duration(2, true), "source dirty dust quality and rounding " + i);
            equal(hard2[i], row.duration(2, false), "source dirty dust hard quality and rounding " + i);
        }
        var furnace = com.gregtech.gregtech.content.recipe.FurnaceSmeltingRules.EXTERNAL;
        check(furnace.get(9).input() == com.gregtech.gregtech.data.MaterialPrefix.dustPure && !furnace.get(9).experience(), "source pure dust smelting grants no XP");
        check(furnace.get(10).input() == com.gregtech.gregtech.data.MaterialPrefix.dustRefined && !furnace.get(10).experience(), "source refined dust smelting grants no XP");
        check(furnace.get(11).input() == com.gregtech.gregtech.data.MaterialPrefix.dustImpure && !furnace.get(11).experience(), "source foreign impure dust smelting grants no XP");
        equal(792_792_000L, furnace.get(9).amount(), "source pure dust furnace uses 11/9 U");
        equal(864_864_000L, furnace.get(10).amount(), "source refined dust furnace uses 12/9 U");
        equal(720_720_000L, furnace.get(11).amount(), "source impure dust furnace uses 10/9 U");
        var washing = com.gregtech.gregtech.content.recipe.MaterialWashingRules.ROWS;
        equal(4, washing.size(), "four source cauldron listeners");
        check(washing.get(0).input() == com.gregtech.gregtech.data.MaterialPrefix.crushed
                && washing.get(0).output() == com.gregtech.gregtech.data.MaterialPrefix.crushedPurified
                && washing.get(0).byproduct() == com.gregtech.gregtech.data.MaterialPrefix.crushedPurifiedTiny,
                "source crushed washing output and optional tiny byproduct");
        check(!washing.get(0).givesByproduct(0) && washing.get(0).givesByproduct(1), "source crushed washing accepts one of two random outcomes");
        for (int i = 1; i < 4; i++) {
            check(washing.get(i).input().getName().equals(names[i - 1]) && washing.get(i).output() == com.gregtech.gregtech.data.MaterialPrefix.dust,
                    "source dust washing returns clean dust " + i);
            check(washing.get(i).byproduct() == null && !washing.get(i).givesByproduct(0), "source dust washing discards impurities " + i);
        }
        check(com.gregtech.gregtech.content.recipe.MaterialWashingRules.row(com.gregtech.gregtech.data.MaterialPrefix.dust) == null,
                "clean dust has no repeated washing listener");
        var step = com.gregtech.gregtech.content.recipe.MaterialWashingRules.step(64, 3, true);
        equal(63, step.remainingCount(), "one washing tick consumes one item from a full stack");
        equal(2, step.remainingWater(), "one washing tick consumes one water level");
        var last = com.gregtech.gregtech.content.recipe.MaterialWashingRules.step(1, 1, true);
        equal(0, last.remainingCount(), "last source item is removed");
        equal(0, last.remainingWater(), "last water level empties the cauldron");
        check(com.gregtech.gregtech.content.recipe.MaterialWashingRules.step(64, 0, true) == null, "dry cauldron does not consume input");
        check(com.gregtech.gregtech.content.recipe.MaterialWashingRules.step(64, 3, false) == null, "missing main output preserves water and input");
        check(com.gregtech.gregtech.content.recipe.MaterialWashingRules.step(0, 3, true) == null, "empty input does not consume water");
        var negative = com.gregtech.gregtech.content.recipe.MaterialWashingRules.cell(-.01, 64.2, -1.01);
        equal(-1, negative.x(), "washing floors negative X");
        equal(63, negative.y(), "washing checks one quarter block below the item");
        equal(-2, negative.z(), "washing floors negative Z");
        equal(-1, com.gregtech.gregtech.content.recipe.MaterialWashingRules.cell(0, -.1, 0).y(), "washing floors negative Y after the quarter offset");
        equal(0, com.gregtech.gregtech.content.recipe.MaterialWashingRules.cell(0, .25, 0).y(), "washing includes the exact quarter-block boundary");
        var iron = com.gregtech.gregtech.content.material.generated.ElementMaterials.Iron;
        var noByproducts = com.gregtech.gregtech.api.material.GTMaterialRegistry.createMaterial(-1, "WashingEmptyFixture", "Washing Empty Fixture", 0);
        check(com.gregtech.gregtech.content.recipe.MaterialWashingRules.byproductMaterial(noByproducts, 0) == noByproducts,
                "source byproduct selection falls back to the input when none exist");
        var weighted = com.gregtech.gregtech.api.material.GTMaterialRegistry.createMaterial(-1, "WashingFixture", "Washing Fixture", 0)
                .ores(iron, iron, com.gregtech.gregtech.content.material.generated.ElementMaterials.Copper);
        check(com.gregtech.gregtech.content.recipe.MaterialWashingRules.byproductMaterial(weighted, 1) == iron, "source byproduct duplicates preserve selection weight");
        check(com.gregtech.gregtech.content.recipe.MaterialWashingRules.byproductMaterial(weighted, 2)
                == com.gregtech.gregtech.content.material.generated.ElementMaterials.Copper, "source byproduct selection retains the final candidate");
        equal(15, com.gregtech.gregtech.content.recipe.ExternalOreProcessingRules.COMPOSITION_FORMS.size(), "source world listeners bind foreign crushed and impure dust alongside external forms");
        var taggedForms = com.gregtech.gregtech.content.recipe.ExternalOreProcessingRules.TAGGED_MATERIAL_FORMS;
        String[] taggedNames = {"ingot", "nugget", "gem", "dust", "dustSmall", "dustTiny", "plate", "stick"};
        equal(taggedNames.length, taggedForms.size(), "common form tags bind eight material forms");
        for (int i = 0; i < taggedNames.length; i++) {
            check(taggedForms.get(i).getName().equals(taggedNames[i]), "common form tag " + taggedNames[i]);
            check(!taggedForms.get(i).getName().toLowerCase(java.util.Locale.ROOT).contains("block"),
                    "common form tags exclude storage blocks " + taggedNames[i]);
        }
        var aliases = com.gregtech.gregtech.api.material.MaterialTagAliases.ALTERNATE_SPELLINGS;
        equal(4, aliases.size(), "four established common-tag spellings");
        check("aluminum".equals(aliases.get("aluminium")), "aluminium common-tag alias");
        check("nether_quartz".equals(aliases.get("quartz")), "quartz common-tag alias");
        check("sulphur".equals(aliases.get("sulfur")), "sulfur common-tag alias");
        check("tungstensteel".equals(aliases.get("tungsten_steel")), "tungsten steel common-tag alias");
    }

    private static void externalOreSourceSamples() {
        // OP:142,146-148; Handler:62,65-67. Independent source weights and getCosts goldens.
        var rows = com.gregtech.gregtech.content.recipe.ExternalOreProcessingRules.ROUTES;
        equal(4, rows.size(), "four original optional ore-prefix routes");
        equal(243_243_000L, com.gregtech.gregtech.data.MaterialPrefix.rawOreChunk.getMaterialWeight(), "source raw chunks weigh 27/72 U");
        equal(1_297_296_000L, com.gregtech.gregtech.data.MaterialPrefix.chunk.getMaterialWeight(), "source external chunks weigh 2 U");
        equal(1_297_296_000L, com.gregtech.gregtech.data.MaterialPrefix.rubble.getMaterialWeight(), "source rubble weighs 2 U");
        equal(1_945_944_000L, com.gregtech.gregtech.data.MaterialPrefix.pebbles.getMaterialWeight(), "source pebbles weigh 3 U");
        long[] baseTicks = {24, 256, 384, 1536};
        for (int i = 0; i < rows.size(); i++) {
            equal(baseTicks[i], rows.get(i).duration(0), "source larger input/output weight cost row " + i);
            equal(baseTicks[i] * 3, rows.get(i).duration(2), "source material quality scales cost row " + i);
        }
        check(rows.get(0).output() == com.gregtech.gregtech.data.MaterialPrefix.crushedTiny && rows.get(0).count() == 3,
                "raw chunks crush to exactly three tiny crushed ores");
        check(rows.get(3).map().equals("Sifting") && rows.get(3).output() == com.gregtech.gregtech.data.MaterialPrefix.dust && rows.get(3).count() == 3,
                "pebbles finish in the sifter with three dust, not the crusher");
        for (var row : rows) check(!com.gregtech.gregtech.api.material.MaterialItemDefinitions.candidatePrefixes().contains(row.input()),
                "optional source prefix never creates a GT-owned item " + row.input().getName());
        check(com.gregtech.gregtech.content.recipe.ExternalOreProcessingRules.allows(
                com.gregtech.gregtech.content.material.generated.ElementMaterials.Iron), "ordinary source material accepts prefix processing");
        check(!com.gregtech.gregtech.content.recipe.ExternalOreProcessingRules.allows(
                com.gregtech.gregtech.api.material.MaterialSentinels.Invalid), "invalid material cannot create an external ore route");
    }

    private static void explosiveStorageSourceSamples() {
        // Original PrefixBlock :399 and :540, OP weights 9/16/64 units, MT :1309-1310 flags.
        com.gregtech.gregtech.data.MaterialPrefixes.bootstrap();
        var material = com.gregtech.gregtech.content.material.generated.CompoundMaterials.Dynamite;
        check(material.has(com.gregtech.gregtech.api.material.MaterialProperty.EXPLOSIVE)
                && material.has(com.gregtech.gregtech.api.material.MaterialProperty.FLAMMABLE), "source dynamite has both reaction flags");
        var prefixes = new com.gregtech.gregtech.api.prefix.BlockMaterialPrefix[]{
                com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.blockDust,
                com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.crateGtDust,
                com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.crateGt64Dust};
        float[] ignition = {4.5F, 8F, 32F}, chain = {6.3F, 11.2F, 44.8F};
        for (int i = 0; i < prefixes.length; i++) {
            check(Math.abs(com.gregtech.gregtech.content.hazard.MaterialBlockHazards.ignitionPower(prefixes[i], material) - ignition[i]) < .0001F,
                    "source ignition strength follows contents " + prefixes[i].getName());
            check(Math.abs(com.gregtech.gregtech.content.hazard.MaterialBlockHazards.chainPower(prefixes[i], material) - chain[i]) < .0001F,
                    "source chain strength follows contents " + prefixes[i].getName());
        }
        check(com.gregtech.gregtech.content.hazard.MaterialBlockHazards.chainPower(prefixes[0], com.gregtech.gregtech.content.material.generated.ElementMaterials.Iron) == 0,
                "ordinary metal dust storage does not explode");
        check(com.gregtech.gregtech.content.hazard.MaterialBlockHazards.chainPower(com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.ore, material) == 0,
                "ore registration does not inherit storage reactions");
    }

    private static void formConversionSourcePositions() {
        // Loader_Recipes_Handlers:560-564: row-major positions select nuggets/tiny vs chunks/small.
        var first = new com.gregtech.gregtech.content.recipe.FormConversionSelector(2, 0);
        var second = new com.gregtech.gregtech.content.recipe.FormConversionSelector(2, 1);
        for (int slot : new int[]{0, 2, 4, 6, 8}) {
            check(first.matches(9, slot, 1), "source first variant selects even nine-grid slot " + slot);
            check(!second.matches(9, slot, 1), "second variant cannot compete at even slot " + slot);
        }
        for (int slot : new int[]{1, 3, 5, 7}) {
            check(second.matches(9, slot, 1), "source alternate selects odd nine-grid slot " + slot);
            check(!first.matches(9, slot, 1), "first variant cannot compete at odd slot " + slot);
        }
        check(second.matches(4, 3, 1), "two-by-two bottom-right selects the alternate");
        check(first.matches(4, 2, 1), "two-by-two bottom-left selects the first variant");
        check(!second.matches(1, 0, 1), "one-cell grid cannot select a reserved second offset");
        check(!first.matches(9, 0, 2), "a stack count does not replace the one occupied-cell requirement");
        check(!first.matches(9, -1, 0), "empty input cannot select a variant");
        check(!first.matches(9, 9, 1), "out-of-grid source slot is invalid");
        // Source wireGt16 registers divisors 1,2,4,8 in this order, even if a form is absent.
        var wireTo4 = new com.gregtech.gregtech.content.recipe.FormConversionSelector(4, 2);
        check(wireTo4.matches(9, 2, 1), "wire16 to four wire4 first source position");
        check(wireTo4.matches(9, 6, 1), "wire16 to four wire4 repeats after four empty cells");
        check(!wireTo4.matches(9, 1, 1), "absent earlier wire form does not shift source offsets");
        check(!wireTo4.matches(2, 1, 1), "small custom grid lacks the third source offset");
        equal(3, wireTo4.minimumGridSize(), "recipe dimension guard preserves source offset");
        check(com.gregtech.gregtech.content.recipe.FormConversionSelector.NONE.matches(9, 8, 4),
                "ordinary shapeless rows have no positional selector");
    }

    private static void conversionPermissionSourceSamples() {
        // AdvancedCrafting1ToY/XToY constructor removal rules, independent fixed source cases.
        var ingot = new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form("ingot", "Iron");
        var nugget = new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form("nugget", "Copper");
        var block = new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form("blockIngot", "Iron");
        check(com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(List.of(ingot), 1, 1, true, false, nugget),
                "source single conversion checks output prefix even with another material");
        check(!com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(List.of(ingot), 1, 1, true, true, nugget),
                "source GT recipe interface is exempt from plain replacement");
        var nine = java.util.Collections.nCopies(9, ingot);
        check(com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(nine, 3, 3, false, false, block),
                "source nine-ingot shaped block conversion is replaced");
        check(!com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(nine, 3, 3, false, false,
                new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form("blockIngot", "Copper")),
                "source many-input conversion checks output material");
        var mixed = new ArrayList<>(nine); mixed.set(8, new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form("ingot", "Copper"));
        check(!com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(mixed, 3, 3, true, false, block),
                "source many-input conversion requires one material");
        var small = new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form("dustSmall", "Iron");
        var dust = new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form("dust", "Iron");
        check(!com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(java.util.Collections.nCopies(4, small), 2, 2, false, false, dust),
                "source shaped four-input removal requires the full nine-slot array");
        var full = new ArrayList<com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form>(java.util.Collections.nCopies(9, null));
        for (int i : new int[]{0,1,3,4}) full.set(i, small);
        check(com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(full, 3, 3, false, false, dust),
                "source upper-left four-input array is replaced");
        check(com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(java.util.Collections.nCopies(4, small), 4, 1, true, false, dust),
                "source shapeless four-input conversion is replaced");
        var raw = new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form("oreRaw", "Iron");
        for (String prefix : new String[]{"ore", "oreDeepslate", "oreDense"})
            check(com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(List.of(new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form(prefix, "Iron")),1,1,true,false,raw),
                    "source ordinary or dense ore conversion " + prefix);
        for (String prefix : new String[]{"oreSmall", "oreBedrock", "oreDust", "oreRaw"})
            check(!com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(List.of(new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form(prefix, "Iron")),1,1,true,false,raw),
                    "source excludes nonstandard ore " + prefix);
        var tinyPlate = new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form("plateTiny", "Iron");
        var casing = new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form("itemCasing", "Iron");
        check(com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(java.util.Collections.nCopies(5, tinyPlate),5,1,true,false,casing),
                "source five tiny plates match the port's identical casingSmall alias");
        check(com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(java.util.Collections.nCopies(9, tinyPlate),3,3,false,false,casing),
                "source nine tiny plates match the renamed casing output");
        check(!com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(java.util.Collections.nCopies(5, tinyPlate),5,1,true,false,
                new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form("itemCasing", "Copper")),
                "source casing aliases never merge different materials");
        check(!com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(java.util.Collections.nCopies(5, tinyPlate),5,1,true,true,casing),
                "GT casing recipes keep the source interface exemption");
        var chunks = new com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form("rawOreChunk", "Iron");
        check(com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(java.util.Collections.nCopies(3,chunks),3,1,true,false,raw),
                "source external raw chunk reverse conversion needs three separate inputs");
        check(!com.gregtech.gregtech.content.recipe.OriginalFormConversions.replaces(java.util.Collections.nCopies(2,chunks),2,1,true,false,raw),
                "source external raw chunk count is not inferred from approximate weights");
        var sourceChunk = com.gregtech.gregtech.content.recipe.OriginalFormConversions.FIXED.get(1);
        check(com.gregtech.gregtech.content.recipe.OriginalFormConversions.selector(sourceChunk).matches(9,1,1),
                "source external raw chunks retain the oreRaw alternate grid position");
        check(!com.gregtech.gregtech.content.recipe.OriginalFormConversions.selector(sourceChunk).matches(9,0,1),
                "raw chunk availability never overrides the first oreRaw result");
        check(com.gregtech.gregtech.api.material.MaterialItemDefinitions.candidatePrefixes().stream()
                .noneMatch(prefix -> prefix.getName().equals("rawOreChunk")), "source external chunk prefix gets no fabricated GT item");
        equal(60, com.gregtech.gregtech.content.recipe.OriginalFormConversions.FIXED.size(), "all fixed source constructors retained");
    }

    private static void autocraftingSourceSamples() {
        // RecipeMapAutocrafting:73-129 and Recipe:904-939: fixed source counts, not a copied oracle.
        var cells = List.of("wood", "wood", "tip", "tip", "", "", "", "", "");
        var grouped = com.gregtech.gregtech.content.recipe.AutocraftingRules.inputs(cells,
                String::isEmpty, "handtool"::equals, String::equals, "tip"::equals);
        equal(2, grouped.size(), "equal blueprint cells combine by exact identity");
        equal(2, grouped.get(0).count(), "two wood cells require two items before optimization");
        equal(2, grouped.get(1).count(), "repeated tip cells combine before becoming a zero-size input");
        check(grouped.get(1).retained(), "source infinite tool heads are retained");
        var plan = com.gregtech.gregtech.content.recipe.AutocraftingRules.optimize(grouped,
                List.of(new com.gregtech.gregtech.content.recipe.AutocraftingRules.Amount<>("sticks",4)), String::equals);
        equal(1, plan.inputs().get(0).count(), "two planks and four sticks optimize to one plank");
        equal(0, plan.inputs().get(1).count(), "the source retains one presence requirement, not one per tip cell");
        equal(2, plan.outputs().get(0).count(), "optimized stick yield is two");
        equal(512L, plan.duration(), "source whole-recipe optimization halves 1024 ticks");
        var withContainer = com.gregtech.gregtech.content.recipe.AutocraftingRules.optimize(grouped,
                List.of(new com.gregtech.gregtech.content.recipe.AutocraftingRules.Amount<>("sticks",4),
                        new com.gregtech.gregtech.content.recipe.AutocraftingRules.Amount<>("bucket",1)), String::equals);
        equal(1024L, withContainer.duration(), "a single returned container prevents count division");
        equal(2, withContainer.outputs().size(), "crafted output and container remain separate source entries");
        var distinct = com.gregtech.gregtech.content.recipe.AutocraftingRules.inputs(
                List.of("wood/plain", "wood/named", "", "", "", "", "", "", ""),
                String::isEmpty, "handtool"::equals, String::equals, "tip"::equals);
        equal(2, distinct.size(), "different stack data never merges into the same blueprint input");
        check(com.gregtech.gregtech.content.recipe.AutocraftingRules.inputs(
                List.of("handtool", "wood", "", "", "", "", "", "", ""),
                String::isEmpty, "handtool"::equals, String::equals, "tip"::equals) == null,
                "a real GT hand tool prevents autocrafting before recipe selection");
        check(com.gregtech.gregtech.content.recipe.AutocraftingRules.inputs(
                List.of("wood"), String::isEmpty, "handtool"::equals, String::equals, "tip"::equals) == null,
                "serialized patterns must describe all nine grid cells");
        equal(16L, com.gregtech.gregtech.content.recipe.AutocraftingRules.POWER, "source dynamic row uses sixteen EU per tick");
        equal(80, com.gregtech.gregtech.content.recipe.AutocraftingRules.SLOT_X, "source special slot x");
        equal(43, com.gregtech.gregtech.content.recipe.AutocraftingRules.SLOT_Y, "source special slot y");
    }

    private static void scannerEnergySourceSamples() {
        // MultiItemRandomTools:517-518 and EnergyStat:68-75,108-124; independent fixed values.
        var scanner = com.gregtech.gregtech.content.tool.ScannerEnergyRules.PORTABLE;
        var crop = com.gregtech.gregtech.content.tool.ScannerEnergyRules.CROP;
        var debug = com.gregtech.gregtech.content.tool.ScannerEnergyRules.DEBUG;
        equal(4096000L, scanner.capacity(), "source HV scanner capacity");
        equal(1024000L, crop.capacity(), "source MV cropnalyzer capacity");
        check(scanner.acceptsPacket(1,256), "HV half-voltage input is accepted");
        check(scanner.acceptsPacket(1,1024), "HV double-voltage input is accepted");
        check(!scanner.acceptsPacket(1,128), "MV input cannot charge the HV scanner");
        check(!scanner.acceptsPacket(1,2048), "EV input cannot charge the HV scanner");
        check(!scanner.acceptsPacket(2,512), "charging requires a single discharged item");
        check(crop.acceptsPacket(1,64) && crop.acceptsPacket(1,256), "MV crop input range is 64 through 256");
        equal(64L, scanner.injectionPackets(1,0,512,1000), "tools cap injection at 64 packets");
        equal(1L, scanner.injectionPackets(1,4095999,512,64), "source accepts one final oversized packet");
        equal(0L, scanner.injectionPackets(1,4096000,512,1), "a full buffer accepts no packets");
        equal(64L, scanner.injectionPackets(1,0,-512,1000), "GT signed packet magnitude is accepted");
        equal(0L, scanner.injectionPackets(1,0,Long.MIN_VALUE,1), "absolute packet overflow is rejected");
        var exact = com.gregtech.gregtech.content.tool.ScannerEnergyRules.use(1024,1024,false);
        check(exact.successful() && exact.remaining() == 0, "an exact payment reports the scan");
        var partial = com.gregtech.gregtech.content.tool.ScannerEnergyRules.use(1023,1024,false);
        check(!partial.successful() && partial.remaining() == 0, "an underfunded scan drains partial charge and reports nothing");
        var freeHeader = com.gregtech.gregtech.content.tool.ScannerEnergyRules.use(0,0,false);
        check(freeHeader.successful(), "plain block headers work on an empty scanner");
        var creative = com.gregtech.gregtech.content.tool.ScannerEnergyRules.use(27,1024,true);
        check(creative.successful() && creative.remaining() == 27, "creative and debug scan without payment");
        equal(32768L, com.gregtech.gregtech.content.tool.ScannerEnergyRules.CROP_DISCOVERY_COST, "first IC2 crop scan uses V[6]");
        equal(512L, com.gregtech.gregtech.content.tool.ScannerEnergyRules.CROP_RESCAN_COST, "subsequent IC2 crop scan uses V[3]");
        check(debug.scansBlocks() && !crop.scansBlocks(), "debug scans blocks; cropnalyzer scans crop providers");
        check(!com.gregtech.gregtech.content.tool.OriginalCropScan.supports(new Object()), "a non-IC2 object does not invent a crop analysis");
        // Non-public third-party implementation, with no IC2 API on the classpath.
        class AddonCrop implements com.gregtech.gregtech.api.crop.CropScanSource {
            int level = 1;
            boolean planted = true;
            public CropScanData cropScanData() {
                return planted ? new CropScanData("addon.crop", java.util.List.of("Green", "Food"),
                        "Addon Author", 3, 5, 7, 11, 13, 17, 19, 23, 29, level) : null;
            }
            public void setCropScanLevel(int value) { level = value; }
        }
        var addon = new AddonCrop();
        check(com.gregtech.gregtech.content.tool.OriginalCropScan.supports(addon), "addon contract is recognized without IC2");
        var found = com.gregtech.gregtech.content.tool.OriginalCropScan.scan(addon, 2, 4, 6, key -> "Translated Crop");
        equal(32768L, found.cost(), "addon first scan has source discovery cost");
        equal(4, addon.level, "addon discovery promotes the real provider before payment");
        check(found.lines().equals(java.util.List.of("--- X: 2 Y: 4 Z: 6 ---",
                "Type -- Name: Translated Crop   Growth: 3   Gain: 5   Resistance: 7",
                "Plant -- Fertilizer: 11   Water: 13   Weed-Ex: 17",
                "Environment -- Nutrients: 19   Humidity: 23   Air-Quality: 29",
                "Attributes: Green, Food", "Discovered by: Addon Author")), "all addon crop data reaches the source formatter");
        equal(512L, com.gregtech.gregtech.content.tool.OriginalCropScan.scan(addon, 0, 0, 0, key -> key).cost(), "addon rescan cost");
        addon.planted = false;
        check(com.gregtech.gregtech.content.tool.OriginalCropScan.scan(addon, 0, 0, 0, key -> key).lines().isEmpty(), "empty crop holder has no scan or cost");
    }

    private static void bedrockAndBoilerSourceSamples() {
        // WorldgenOresBedrock.generateVein: all 840 cells of the six-layer muffin are filled,
        // even when every random ore roll is blank. Absolute tail heights adapt the y=0 source.
        var filled = new HashSet<String>();
        int[] floorWrites = {0}, highestTail = {Integer.MIN_VALUE};
        boolean generated = com.gregtech.gregtech.worldgen.MineralWorldgenRules.bedrockVein(
                0, 0, -64, 63, bound -> bound - 1,
                new com.gregtech.gregtech.worldgen.MineralWorldgenRules.VeinSink() {
                    public boolean isBedrockFloor(int x,int y,int z) { return x == 8 && y == -64 && z == 8; }
                    public void prepareStone(int x,int y,int z) { filled.add(x + "," + y + "," + z); }
                    public boolean bedrock(int x,int y,int z,boolean small) { floorWrites[0]++; return y == -64; }
                    public boolean ore(int x,int y,int z,boolean small) { highestTail[0] = Math.max(highestTail[0],y); return true; }
                });
        check(generated, "a forced core counts even when all random core rolls are blank");
        equal(1, floorWrites[0], "blank bedrock rolls leave one forced large ore");
        equal(840, filled.size(), "GT6 six muffin layers contain 840 cells");
        check(filled.contains("8,-63,8"), "bedrock directly above the core becomes mother stone");
        check(!filled.contains("8,-64,8"), "mother stone never overwrites the floor");
        check(!filled.contains("0,-63,0"), "outside the source muffin remains untouched");
        check(filled.contains("0,-60,0"), "the fourth source layer spans the whole chunk");
        equal(62, highestTail[0], "small-ore trails reach one below the absolute sea level");

        // MultiTileEntityBoilerTank:165-175, 202-215, 242: independent threshold/cap samples.
        check(com.gregtech.gregtech.content.energy.BoilerHazards.contactDamage(2000,0) == 0, "boiler contact threshold is strictly above 2000 HU");
        check(com.gregtech.gregtech.content.energy.BoilerHazards.contactDamage(0,4002) > 1, "stored steam contributes half its amount to contact heat");
        check(com.gregtech.gregtech.content.energy.BoilerHazards.contactDamage(50000,0) == 10, "source contact damage caps at ten");
        check(com.gregtech.gregtech.content.energy.BoilerHazards.descalingDamage(50000,0,9999,15) == 25, "descaling damage has no contact cap");
        check(com.gregtech.gregtech.content.energy.BoilerHazards.descalingDamage(50000,0,10000,15) == 0, "clean boilers do not descale");
        check(com.gregtech.gregtech.content.energy.BoilerHazards.descalingDamage(50000,0,9999,16) == 0, "unsafe descaling explodes instead of applying heat");
        check(com.gregtech.gregtech.content.energy.BoilerHazards.explosionPower(9_000_000) == 30, "source boiler explosion has no invented eight-block strength cap");
    }

    private static void miniaturePortalSignals() {
        // MultiTileEntityMiniPortal:113-180: pinned inbox/phase/timeout trace, no game launch.
        var portal = new com.gregtech.gregtech.content.logistics.MiniPortalSignals();
        portal.receive(100, 4, 7, 3);
        portal.receive(100, 4, 11, 2);
        portal.receive(100, 5, 4, 9);
        portal.advance(100);
        equal(0, portal.redstone(4), "a scan never emits its own tick's input");
        // Receiving before the remote entity's tick must first apply the old inbox.
        portal.receive(101, 4, 2, 5);
        equal(11, portal.redstone(4), "same tick senders merge by maximum");
        equal(3, portal.comparator(4), "comparator merges independently");
        equal(9, portal.comparator(5), "opposite side inbox remains independent");
        portal.advance(101);
        equal(11, portal.redstone(4), "remote tick cannot apply an inbox twice");
        portal.advance(102);
        equal(2, portal.redstone(4), "next tick can lower redstone");
        equal(5, portal.comparator(4), "next tick advances comparator");
        for (long tick = 103; tick <= 122; tick++) portal.advance(tick);
        equal(2, portal.redstone(4), "twenty absent scans keep the previous redstone");
        portal.advance(123);
        equal(0, portal.redstone(4), "twenty-first absent scan clears disconnected redstone");
        equal(0, portal.comparator(4), "disconnected comparator also clears");
        portal.receive(123, 5, 12, 6);
        portal.disconnect(123);
        portal.advance(124);
        equal(0, portal.redstone(5), "counterpart deactivation overwrites a pending high input");
        portal.receive(124, 0, 15, 8);
        portal.advance(125);
        portal.clear();
        portal.advance(126);
        equal(0, portal.redstone(0), "deactivation clears outputs and pending inboxes");
        equal(0, portal.maximumComparator(), "deactivation clears every comparator side");
    }

    private static void sourceSteamConversion() {
        // MultiTileEntityTurbineSteam consumes a batch once, halves its energy over two ticks;
        // TE_Behavior_Energy_Converter emits one variable packet and wastes input maximum.
        var first = com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(0,0,96,0,48);
        equal(48, first.energy(), "96L source steam first half");
        equal(48, first.pending(), "96L source steam pending half");
        equal(96, first.consumed(), "consume the whole steam batch once");
        equal(96, first.remainder(), "condensate remainder below 160L");
        var second = com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(0,48,96,96,48);
        equal(48, second.energy(), "pending half restored next tick");
        equal(0, second.pending(), "pending half used once");
        equal(0, second.consumed(), "new steam waits for next batch");
        equal(16, com.gregtech.gregtech.content.energy.SteamTurbineConversion.output(48,48,16), "bronze turbine nominal RU");
        equal(32, com.gregtech.gregtech.content.energy.SteamTurbineConversion.output(96,48,16), "bronze turbine full packet");
        equal(22, com.gregtech.gregtech.content.energy.SteamTurbineConversion.output(32,32,22), "LV dynamo full packet EU");
        equal(11, com.gregtech.gregtech.content.energy.SteamTurbineConversion.output(16,32,22), "minimum dynamo packet EU");
        equal(0, com.gregtech.gregtech.content.energy.SteamTurbineConversion.waste(48,48), "turbine spends steam even without receiver");
        equal(4, com.gregtech.gregtech.content.energy.SteamTurbineConversion.waste(100,48), "subtract maximum input, not recommended input");
        var condensate = com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(0,0,64,96,48);
        equal(1, condensate.condensate(), "160L accumulated source steam yields one distilled water");
        equal(0, condensate.remainder(), "160L condensate resets remainder");
        var stopped = com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(7,48,96,96,48);
        equal(55, stopped.energy(), "stopping the inlet does not cancel a stored batch");
        equal(0, stopped.pending(), "stored second half is released");
        equal(0, stopped.consumed(), "staged half does not consume another steam batch");
        equal(0, com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(0,0,47,0,48).consumed(), "batch below source input minimum waits");
    }

    private static void fluidPipeCatalog() {
        // Fixed source rows: GT6 Loader_MultiTileEntities:1846-1885. Gas/acid/plasma/magic bits.
        String[] rows = {
            "wood,50,0000,340",
            "treated_wood,75,0000,340",
            "plastic,100,1000,370",
            "rubber,100,1000,350",
            "copper,100,1000,0",
            "aluminium,100,1000,0",
            "tin_alloy,125,1000,0",
            "bronze,150,1000,0",
            "invar,200,1000,0",
            "steel,200,1000,0",
            "galvanized_steel,250,1000,0",
            "hsla,250,1000,0",
            "gold,100,1100,0",
            "chrome,200,1100,0",
            "stainless_steel,250,1100,0",
            "vanadium_steel,400,1100,0",
            "desh,200,1001,0",
            "tungsten_alloy,300,1001,0",
            "tungsten_steel,400,1001,0",
            "tungsten_carbide,450,1001,0",
            "desh_alloy,350,1001,0",
            "palladium,400,1001,0",
            "carbon,1000,1000,0",
            "tantalum_hafnium_carbide,300,1001,0",
            "titanium,300,1000,0",
            "tungsten,350,1101,0",
            "efrine,250,1011,0",
            "netherite,300,1111,0",
            "iridium,500,1101,0",
            "ironwood,200,1001,0",
            "thaumium,250,1101,0",
            "manasteel,250,1101,0",
            "void_metal,500,1101,0",
            "terrasteel,500,1101,0",
            "gaia_spirit,1000,1111,0",
            "bedrock_hsla,1000,1001,0",
            "adamantium,10000,1111,0",
            "draconium,2500,1111,0",
            "awakened_draconium,10000,1111,0",
            "infinity,1000000000,1111,0",
        };
        var specs = com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions.pipes();
        equal(280, specs.size(), "40 original materials in seven sizes");
        Map<String, com.gregtech.gregtech.api.machine.PipeSpec> byId = new java.util.HashMap<>();
        for (var spec : specs) check(byId.put(spec.id(), spec) == null, "unique pipe registry ID");
        String[] sizes = {"tiny", "small", "medium", "large", "huge", "quadruple", "nonuple"};
        long[] multiples = {1, 2, 6, 12, 24, 6, 2};
        int[] channels = {1, 1, 1, 1, 1, 4, 9};
        for (String row : rows) {
            String[] fields = row.split(",");
            for (int i = 0; i < sizes.length; i++) {
                String id = "pipe_" + sizes[i] + "_" + fields[0];
                var spec = byId.get(id);
                check(spec != null, "expected pipe " + id);
                equal(Long.parseLong(fields[1]) * multiples[i], spec.capacity(), id + " capacity");
                equal(channels[i], spec.tankCount(), id + " channels");
                equal(fields[0].equals("wood") || fields[0].equals("treated_wood") ? 150 : 0,
                        spec.flammability(), id + " original flammability and fire spread");
                String flags = (spec.gasProof() ? "1" : "0") + (spec.acidProof() ? "1" : "0")
                        + (spec.plasmaProof() ? "1" : "0") + (spec.magicProof() ? "1" : "0");
                check(fields[2].equals(flags), id + " four independent proof flags");
                long explicit = Long.parseLong(fields[3]);
                if (explicit > 0) equal(explicit, spec.maxTemperature(), id + " explicit temperature");
            }
        }
        var steel = byId.get("pipe_medium_steel");
        var explicit = com.gregtech.gregtech.api.machine.PipeSpec.of("custom", steel.material(), steel.size(),
                200, true, false, false, true, 345);
        equal(345, explicit.maxTemperature(), "factory preserves explicit limit instead of recomputing it");
    }

    private static void fluidPipeSafety() {
        check(FluidPipeSafety.canIgnite(false, true, false, false), "air can ignite without checking flammability");
        check(FluidPipeSafety.canIgnite(false, true, true, true), "flammable GT noncolliding block may burn");
        check(!FluidPipeSafety.canIgnite(false, true, true, false), "nonflammable GT block remains protected");
        check(!FluidPipeSafety.canIgnite(true, true, false, true), "lava/fire/explicit protection wins over flammability");
        check(!FluidPipeSafety.canIgnite(false, false, false, true), "solid flammable blocks are not directly replaced");
        check(!FluidPipeSafety.canIgnite(false, false, true, false), "solid GT blocks stay intact");
        equal(4, FluidPipeSafety.magicLoss(true, false, false), "magic liquid loses four");
        equal(16, FluidPipeSafety.magicLoss(true, true, false), "magic gas loses sixteen before physical leakage");
        equal(0, FluidPipeSafety.magicLoss(true, false, true), "magic proof protects liquid");
        equal(0, FluidPipeSafety.magicLoss(true, true, true), "magic proof protects gas");
        equal(0, FluidPipeSafety.magicLoss(false, false, false), "ordinary liquid has no magic loss");
        equal(0, FluidPipeSafety.magicLoss(false, true, false), "ordinary gas has no magic loss");
        equal(24, FluidPipeSafety.magicLoss(true, true, false)
                + FluidPipeSafety.losses(true, false, false, false, false, false).gas(),
                "unprotected magical gas loses sixteen plus eight");
        var steelAcidGas = FluidPipeSafety.losses(true, false, true, true, false, false);
        equal(0, steelAcidGas.gas(), "gas proof excludes only gas loss");
        equal(16, steelAcidGas.acid(), "acid gas still corrodes steel");
        var acidProofGas = FluidPipeSafety.losses(true, false, true, false, false, true);
        equal(8, acidProofGas.gas(), "acid proof does not stop gas loss");
        equal(0, acidProofGas.acid(), "acid proof prevents chemical branch");
        var compound = FluidPipeSafety.losses(true, true, true, false, false, false);
        equal(88, compound.gas() + compound.plasma() + compound.acid(), "independent physical hazards accumulate");
        var proof = FluidPipeSafety.losses(true, true, true, true, true, true);
        equal(0, proof.gas() + proof.plasma() + proof.acid(), "all physical proofs stop losses");
        equal(300, FluidPipeSafety.observeTemperature(1300, 300, true), "first fluid replaces old hot temperature");
        equal(1300, FluidPipeSafety.observeTemperature(1300, 300, false), "later cold channel preserves hottest temperature");
        equal(1300, FluidPipeSafety.observeTemperature(300, 1300, false), "later hot channel raises temperature");
        equal(999, FluidPipeSafety.emptyTemperature(1000, 300), "empty pipe cools by one kelvin");
        equal(101, FluidPipeSafety.emptyTemperature(100, 300), "empty pipe warms by one kelvin");
        equal(300, FluidPipeSafety.emptyTemperature(300, 300), "ambient equilibrium");
        equal(Long.MAX_VALUE - 1, FluidPipeSafety.emptyTemperature(Long.MAX_VALUE, Long.MIN_VALUE), "extreme cooling cannot overflow");
        equal(Long.MIN_VALUE + 1, FluidPipeSafety.emptyTemperature(Long.MIN_VALUE, Long.MAX_VALUE), "extreme warming cannot overflow");
    }

    private static void fluidPipeChannels() {
        equal(2, FluidPipeChannels.select(4, i -> i == 2, i -> i == 0 || i == 3),
                "existing fluid wins over an earlier empty channel");
        equal(1, FluidPipeChannels.select(4, i -> false, i -> i == 1 || i == 3),
                "new fluid uses first empty channel");
        equal(-1, FluidPipeChannels.select(4, i -> false, i -> false), "occupied incompatible pipe refuses fluid");
        equal(0, FluidPipeChannels.select(1, i -> false, i -> true), "single-channel receiver is independent of source index");
        equal(-1, FluidPipeChannels.select(0, i -> false, i -> false), "absent channel rejected");
        equal(101, FluidPipeChannels.distributionLevel(101, new long[]{100}, 0), "odd shared mean rounds up");
        equal(200, FluidPipeChannels.distributionLevel(600, new long[]{0, 0}, 0), "three-way source-inclusive mean");
        equal(200, FluidPipeChannels.distributionLevel(600, new long[]{0}, 1), "machines share the same mean");
        equal(267, FluidPipeChannels.distributionLevel(600, new long[]{200}, 1), "round combined mean upward");
        equal(Long.MAX_VALUE, FluidPipeChannels.distributionLevel(Long.MAX_VALUE,
                new long[]{Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE}, 0), "long sum cannot overflow");
        equal(4611686018427387904L, FluidPipeChannels.distributionLevel(Long.MAX_VALUE, new long[]{}, 1),
                "large source and empty machine mean");
        equal(0, FluidPipeChannels.pressureShare(600, 1200, 2), "half capacity creates no pressure");
        equal(100, FluidPipeChannels.pressureShare(800, 1200, 2), "excess pressure split across pipes");
        equal(0, FluidPipeChannels.pressureShare(1200, 1200, 0), "no pipe pressure targets");
        equal(0, FluidPipeChannels.cauldronCost(0, 333), "partial cauldron step cannot consume fluid");
        equal(334, FluidPipeChannels.cauldronCost(0, 666), "one affordable cauldron step");
        equal(667, FluidPipeChannels.cauldronCost(0, 999), "two affordable cauldron steps");
        equal(1000, FluidPipeChannels.cauldronCost(0, 1000), "full cauldron costs one bucket");
        equal(667, FluidPipeChannels.cauldronCost(1, 1000), "two remaining steps paid together");
        equal(334, FluidPipeChannels.cauldronCost(2, 1000), "only last step paid");
        equal(0, FluidPipeChannels.cauldronCost(3, 1000), "full cauldron consumes nothing");
    }

    /** Original BlueprintRegressionTests.java:276-283 numeric fixtures. */
    private static void workCostGoldens() {
        var normal = MachineWorkCost.calculate(32, 192, 1, true, 10000, 128, 512, false);
        check(normal != null, "ordinary machine work exists");
        equal(128, normal.minimumPower(), "normal overclock minimum power");
        equal(12288, normal.totalWork(), "normal overclock total energy");

        var cheap = MachineWorkCost.calculate(32, 192, 4, true, 5000, 512, 4096, true);
        check(cheap != null, "cheap parallel work exists");
        equal(32, cheap.minimumPower(), "cheap overclock retains recipe power");
        equal(49152, cheap.totalWork(), "four parallel operations at half efficiency");

        var timed = MachineWorkCost.calculate(0, 256, 64, false, 10000, 1, 16, false);
        check(timed != null, "time-based work exists");
        equal(1, timed.minimumPower(), "zero-power timed work minimum");
        equal(256, timed.totalWork(), "TU parallel recipes retain duration");
        var poweredParallel = MachineWorkCost.calculate(32, 20, 4, false, 10000, 128, 512, false);
        check(poweredParallel != null, "powered parallel batch exists");
        equal(128, poweredParallel.minimumPower(), "non-duration parallel scales minimum power");
        equal(2560, poweredParallel.totalWork(), "non-duration parallel conserves total batch energy");
        check(MachineWorkCost.calculate(32, 20, 17, false, 10000, 128, 512, false)==null,
                "parallel power cannot exceed the machine maximum");
        var positiveTimed = MachineWorkCost.calculate(1, 20, 16, false, 10000, 1, 16, true, true);
        check(positiveTimed != null, "positive-power TU batch exists");
        equal(1, positiveTimed.minimumPower(), "TU does not scale power with parallel count");
        equal(20, positiveTimed.totalWork(), "TU retains source duration with positive power");
        check(MachineWorkCost.calculate(4096, Long.MAX_VALUE, 64, true, 2500, 512, 4096, true) == null,
                "overflow rejects before a platform consumes recipe ingredients");
        check(MachineWorkCost.calculate(513, 192, 1, true, 10000, 128, 512, false) == null,
                "insufficient voltage rejects work");
    }

    /** Original HazardDamageTests.java:208-222 tier boundaries and signed magnitude. */
    private static void voltageGoldens() {
        equal(8, GTVoltageTiers.VOLTAGES[0], "ULV volts");
        equal(32, GTVoltageTiers.VOLTAGES[1], "LV volts");
        equal(512, GTVoltageTiers.VOLTAGES[3], "HV volts");
        equal(8192, GTVoltageTiers.VOLTAGES[5], "IV volts");
        equal(8589934592L, GTVoltageTiers.VOLTAGES[15], "final voltage table entry");
        equal(0, GTVoltageTiers.tierMax(8), "ULV index determines zero wire hazard tier");
        equal(1, GTVoltageTiers.tierMax(9), "one volt above ULV selects LV");
        equal(1, GTVoltageTiers.tierMax(32), "exact LV upper bound");
        equal(3, GTVoltageTiers.tierMax(512), "exact HV upper bound");
        equal(5, GTVoltageTiers.tierMax(8192), "exact IV upper bound");
        equal(1, GTVoltageTiers.tierMin(32), "exact LV lower-tier lookup");
        equal(3, GTVoltageTiers.tierMin(512), "exact HV lower-tier lookup");
        equal(1, GTVoltageTiers.tierMin(40), "between LV and MV");
        equal(3, GTVoltageTiers.tierMin(520), "between HV and EV");
        equal(128, GTVoltageTiers.maxVoltageOf(40), "display voltage rounds up to MV");
        check("LV".equals(GTVoltageTiers.nameOf(32)), "LV label");
        check("PUV1".equals(GTVoltageTiers.nameOf(2097152)), "original post-ultimate label");
        check("XV".equals(GTVoltageTiers.nameOf(2147483648L)), "original maximum label");
        equal(2147483648L, com.gregtech.gregtech.data.GregTechConstants.V[14], "legacy voltage table uses original finite XV");
        equal(8589934592L, com.gregtech.gregtech.data.GregTechConstants.V[15], "legacy voltage table has no Long.MAX_VALUE sentinel");
        check(java.util.Arrays.equals(GTVoltageTiers.NAMES,com.gregtech.gregtech.data.GregTechConstants.VN), "one canonical voltage name table");
        equal(1, GTVoltageTiers.tierMax(-32), "signed voltage magnitude");
    }

    private static void machineEnergyGoldens() {
        equal(2, com.gregtech.gregtech.api.machine.BasicMachineEnergy.demanded(33, 64, 16), "GT6 whole-packet rounded demand");
        equal(2, com.gregtech.gregtech.api.machine.BasicMachineEnergy.accepted(33, 64, -16, 9), "signed packet admission");
        equal(64, com.gregtech.gregtech.api.machine.BasicMachineEnergy.add(33, 64, 16, 2), "inherited buffer clips last packet excess");
        equal(49, com.gregtech.gregtech.api.machine.BasicMachineEnergy.add(33, 64, 16, 1), "one admitted packet");
        equal(0, com.gregtech.gregtech.api.machine.BasicMachineEnergy.accepted(65, 64, 16, 9), "old over-capacity state never returns negative packets");
        equal(0, com.gregtech.gregtech.api.machine.BasicMachineEnergy.accepted(0, 64, Long.MIN_VALUE, 1), "unrepresentable signed magnitude rejected");
        equal(0, com.gregtech.gregtech.api.machine.BasicMachineEnergy.accepted(0, 64, 1, -1), "negative request rejected");
        equal(Long.MAX_VALUE, com.gregtech.gregtech.api.machine.BasicMachineEnergy.add(0, Long.MAX_VALUE, 2, Long.MAX_VALUE), "large packet train saturates without overflow");
        equal(32, com.gregtech.gregtech.api.machine.BasicMachineEnergy.drain(96,64), "per-tick rated drain keeps remaining input");
        equal(0, com.gregtech.gregtech.api.machine.BasicMachineEnergy.drain(16,64), "unused partial input discarded");
        equal(96, MachineWorkCost.advance(64,128,32), "ordinary recipe advances by supplied work");
        equal(128, MachineWorkCost.advance(120,128,32), "completed recipe stops at its cost");
        equal(Long.MAX_VALUE, MachineWorkCost.advance(Long.MAX_VALUE-4,Long.MAX_VALUE,32), "largest job can complete without wraparound");
    }

    private static void itemPipeRoutingAndDelivery() {
        var edges = Map.of("first", List.of("join"), "second", List.of("join"));
        var costs = Map.of("first", 5L, "second", 1L, "join", 4L);
        var selected = com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.bestExit(
                List.of("first", "second"), n -> costs.get(n), n -> edges.getOrDefault(n,List.of()), "join"::equals, 32);
        check(selected != null && "second".equals(selected.firstHop()), "shared exit reached by cheapest first hop");
        equal(5, selected.cost(), "minimum weighted path includes both pipe steps");
        equal(9, com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.minimumExitCost(
                "first", n -> costs.get(n), n -> edges.getOrDefault(n,List.of()), "join"::equals,32), "single-hop API retained");
        var tied = com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.bestExit(
                List.of("first","second"), n -> "first".equals(n)?3: "join".equals(n)?7:10,
                n -> "first".equals(n)?List.of("join"):List.of(), n -> !"first".equals(n),32);
        check(tied != null && "first".equals(tied.firstHop()), "equal costs retain original first-face ordering despite longer path");
        var saturated = com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.bestExit(
                List.of("first"), n -> Long.MAX_VALUE, n -> List.of(), n -> true,32);
        check(saturated != null && saturated.cost()==Long.MAX_VALUE, "saturated valid route is selectable");
        check(com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.bestExit(
                List.of("first","second"), n -> costs.get(n), n -> edges.getOrDefault(n,List.of()), "join"::equals,1)==null,
                "visit limit applies once to the combined scan");
        var cycle = com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.bestExit(
                List.of("first"), n -> 0, n -> "first".equals(n)?List.of("second"):List.of("first"), "second"::equals,32);
        check(cycle != null && "first".equals(cycle.firstHop()) && cycle.cost()==0, "zero-step cycles terminate and retain ingress hop");

        int[] actualUnits={0};boolean[] fails={false};
        var port = new com.gregtech.gregtech.content.transport.ItemPipeTransfer.Port<int[]>() {
            public boolean begin(boolean simulate){return true;}
            public int slots(){return 1;}
            public int[] stack(int slot){return new int[]{0,0};}
            public int count(int[] stack){return stack[1];}
            public int[] copyWithCount(int[] stack,int count){return new int[]{stack[0],count};}
            public boolean same(int[] a,int[] b){return a[0]==b[0];}
            public int[] insert(int slot,int[] offered,boolean simulate){
                if(fails[0])throw new IllegalStateException("foreign handler failed");
                int accepted=Math.min(offered[1],simulate?4:2);
                if(!simulate)actualUnits[0]+=accepted;
                return copyWithCount(offered,offered[1]-accepted);
            }
        };
        var probe=com.gregtech.gregtech.content.transport.ItemPipeTransfer.simulate(new int[]{1,6},port);
        equal(4,probe.planned(),"exit probe validates partial remainder");
        equal(0,actualUnits[0],"exit probe never performs actual insertion");
        var delivery=com.gregtech.gregtech.content.transport.ItemPipeTransfer.transfer(new int[]{1,6},port);
        equal(4,delivery.planned(),"normal delivery uses same destination plan");
        equal(2,delivery.accepted(),"only smaller actual commit is charged to source");
        equal(2,actualUnits[0],"actual provider commit matches source bill");
        fails[0]=true;
        var bad=com.gregtech.gregtech.content.transport.ItemPipeTransfer.simulate(new int[]{1,6},port);
        check(bad.handlerFailed()&&bad.planned()==0,"exceptional exit safely rejected");
        equal(2,actualUnits[0],"exceptional probe cannot execute a delivery");
    }

    private static void addonLifecycle() {
        var calls = new ArrayList<String>();
        var context = new com.gregtech.gregtech.api.addon.GregTechAddon.Context("neoforge", "1.21.1");
        for (String id : List.of("zz_example", "aa_example"))
            com.gregtech.gregtech.api.addon.GregTechAddons.register(new com.gregtech.gregtech.api.addon.GregTechAddon() {
                public String id() { return id; }
                public void onRecipesReady(Context actual) {
                    check(actual.equals(context) && actual.apiVersion()==1, "addon receives exact platform context");
                    calls.add(id);
                }
            });
        check(!com.gregtech.gregtech.api.addon.GregTechAddons.recipesReady(), "registration does not mean recipe lifecycle completed");
        com.gregtech.gregtech.api.addon.GregTechAddons.dispatchRecipesReady(context);
        check(calls.equals(List.of("aa_example", "zz_example")), "deterministic once-only addon ordering");
        check(com.gregtech.gregtech.api.addon.GregTechAddons.recipesReady(), "successful addon lifecycle ready");
        boolean repeated=false;
        try { com.gregtech.gregtech.api.addon.GregTechAddons.dispatchRecipesReady(context); }
        catch (IllegalStateException expected) { repeated=true; }
        check(repeated && calls.size()==2, "second dispatch rejects without another callback");
    }

    /** Source GTValues and GregTechConstants.L=144: one ingot is 144 L and one nugget 16 L. */
    private static void materialAndCrucibleUnits() {
        equal(648648000, GTValues.U, "source material unit is unchanged");
        equal(324324000, GTValues.U2, "half unit for one conductor");
        equal(216216000, GTValues.U3, "third unit");
        equal(162162000, GTValues.U4, "quarter unit");
        equal(72072000, GTValues.U9, "nugget unit");
        equal(9009000, GTValues.U72, "tiny material unit");
        equal(648648000, CrucibleMath.units(144, 144, GTValues.U, false), "144 L is one material unit");
        equal(72072000, CrucibleMath.units(16, 144, GTValues.U, false), "16 L is one nugget");
        equal(1945944000, CrucibleMath.units(432, 144, GTValues.U, false), "three ingots keep copper amount");
        equal(144, CrucibleMath.unitsScaled(GTValues.U, GTValues.U, 144, false), "material to fluid direction");
        equal(66, CrucibleMath.scale(2, 3, 100, false), "display truncation retains original semantics");
        equal(67, CrucibleMath.scale(2, 3, 100, true), "display ceiling retains original semantics");
        equal(100, CrucibleMath.scale(5, 3, 100, false), "display clamps above capacity");
        equal(0, CrucibleMath.units(144, 0, GTValues.U, false), "invalid denominator contributes no material");
    }

    /** AxialGeneratorTests.java:75-77 and AxleRepairTests.java:125,132 signed packet scenarios. */
    private static void signedPacketsAndFiniteBuffer() {
        equal(32, EnergyPackets.magnitude(-32, true), "negative RU remains a 32-unit packet");
        equal(0, EnergyPackets.magnitude(-32, false), "unsigned energy rejects negative packets");
        equal(0, EnergyPackets.magnitude(Long.MIN_VALUE, true), "signed overflow cannot become usable energy");
        equal(3, EnergyPackets.fitting(8, 100, 32), "buffer fits three whole LV packets, not fractions");
        equal(2, EnergyPackets.fitting(2, 100, 32), "requested packets cap buffer acceptance");
        equal(0, EnergyPackets.fitting(8, 31, 32), "a sub-packet buffer cannot accept one packet");
        equal(0, EnergyPackets.fitting(8, 100, 0), "zero packet magnitude rejected");
    }

    private static void atomicMassBounds() {
        equal(98, AtomicProperties.GT6_DEFAULT.mass(), "source default atomic mass");
        equal(0, AtomicProperties.ZERO.mass(), "zero-mass particle material");
        equal(-1, new AtomicProperties(0, 0, 0, -1).mass(), "negative additional mass remains allowed for Magic");
        rejects(IllegalArgumentException.class, () -> new AtomicProperties(-1, 0, 0, 0), "negative particle count");
        rejects(ArithmeticException.class, () -> new AtomicProperties(Long.MAX_VALUE, 0, 1, 0), "atomic mass overflow");
    }

    /** Original DefinitionCatalogTest.java:9-27 before the first platform registration. */
    private static void definitionIdentityAndSnapshot() {
        var source = new ArrayList<>(List.of("oven_steel", "electric_motor_lv"));
        var catalog = DefinitionCatalog.validated(source, id -> id);
        source.clear();
        check(catalog.equals(List.of("oven_steel", "electric_motor_lv")), "definition snapshot preserves stable IDs and order");
        rejects(UnsupportedOperationException.class, () -> catalog.add("extra"), "definition snapshot immutable");
        rejects(IllegalArgumentException.class,
                () -> DefinitionCatalog.validated(List.of("same", "same"), id -> id), "duplicate registration IDs");
        rejects(IllegalArgumentException.class,
                () -> DefinitionCatalog.validated(List.of("gregtech:oven"), id -> id), "namespace cannot enter a path ID");
    }

    private static void multiblockOwnershipLifecycle() {
        var bindings = new PartBindings<String, String>();
        var claimed = new HashSet<String>();
        var released = new HashSet<String>();
        check(bindings.update(Map.of("wall", "ENERGY_INPUT", "vent", "NONE"), p -> true,
                (p, role) -> claimed.add(p), released::add), "first valid structure claims parts");
        check(bindings.contains("wall") && bindings.contains("vent"), "structure keeps ownership for both parts");
        claimed.clear();
        check(!bindings.update(Map.of("wall", "ENERGY_INPUT", "foreign", "NONE"), p -> !p.equals("foreign"),
                (p, role) -> claimed.add(p), released::add), "foreign ownership invalidates structure");
        check(claimed.isEmpty(), "a failed structure cannot partially claim new parts");
        check(released.equals(Set.of("wall", "vent")), "invalidating a structure releases all its previous parts");
        check(!bindings.contains("wall") && !bindings.contains("vent"), "no stale claims after invalidation");
    }

    private static void equal(long expected, long actual, String behavior) {
        check(expected == actual, behavior + ": expected " + expected + ", actual " + actual);
    }

    private static void check(boolean passed, String behavior) {
        assertions++;
        if (!passed) throw new AssertionError(behavior);
    }

    private static void rejects(Class<? extends Throwable> expected, Runnable action, String behavior) {
        try {
            action.run();
        } catch (Throwable failure) {
            check(expected.isInstance(failure), behavior + ": unexpected " + failure);
            return;
        }
        check(false, behavior + ": expected " + expected.getSimpleName());
    }
}
