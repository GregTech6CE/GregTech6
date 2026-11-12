package gregtech.content.recipe;

import gregtech.api.material.GTValues;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.generated.RecyclablePrefixes;
import gregtech.engine.machine.PipeSpec;
import gregtech.engine.recipe.chem.MaterialAmount;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pins the hand-written family tables to the original {@code Loader_Recipes_Handlers} rows. */
final class FamilyRowTablesTest {

    @Test
    void autoclaveRowsKeepTheTwelveCrystalLadderSteps() {
        var rows = AutoclaveRecipeRows.ROWS;
        assertEquals(12, rows.size());
        var first = rows.get(0);
        assertEquals(MaterialPrefix.dustSmall, first.input());
        assertEquals(1, first.inCount());
        assertEquals(25600, first.steam());
        assertEquals(120, first.distW());
        assertEquals(800, first.ticks());
        assertEquals(0, first.circuit());
        assertEquals(MaterialPrefix.gemChipped, first.output());
        var last = rows.get(11);
        assertEquals(MaterialPrefix.dust, last.input());
        assertEquals(8, last.inCount());
        assertEquals(819200, last.steam());
        assertEquals(3840, last.distW());
        assertEquals(5, last.circuit());
        assertEquals(MaterialPrefix.gemLegendary, last.output());
    }

    @Test
    void crusherRowsKeepTheGemLadderAndRockRow() {
        var rows = CrusherFamilyRecipeRows.ROWS;
        assertEquals(8, rows.size());
        assertEquals(MaterialPrefix.gemLegendary, rows.get(0).input());
        assertEquals(MaterialPrefix.gemExquisite, rows.get(0).output());
        assertEquals(2, rows.get(0).outCount());
        assertEquals(MaterialPrefix.bouleGt, rows.get(5).input());
        assertEquals(4, rows.get(5).outCount());
        assertEquals(MaterialPrefix.gemChipped, rows.get(6).input());
        assertNull(rows.get(6).output());
        assertEquals(MaterialPrefix.rockGt, rows.get(7).input());
        assertEquals(16, rows.get(7).multiplier());
    }

    @Test
    void welderRowsKeepTheCasingAndMultiIngotSteps() {
        var rows = WelderFamilyRecipeRows.ROWS;
        assertEquals(10, rows.size());
        assertTrue(rows.stream()
                .anyMatch(row -> row.input() == MaterialPrefix.itemCasing
                        && row.inCount() == 2
                        && row.circuit() == 2
                        && row.output() == MaterialPrefix.plate));
        // BlockMaterialPrefix constants are assigned by the registry bootstrap, so a bare unit
        // test only sees the row's shape: the nine-ingot row is the one with no item output.
        assertTrue(rows.stream()
                .anyMatch(row -> row.input() == MaterialPrefix.ingot
                        && row.inCount() == 9
                        && row.circuit() == 9
                        && row.output() == null));
        var rotor = rows.get(rows.size() - 1);
        assertEquals(MaterialPrefix.plateCurved, rotor.input());
        assertEquals(MaterialPrefix.ring, rotor.second());
        assertEquals(MaterialPrefix.rotor, rotor.output());
    }

    @Test
    void thermalRecyclingAdmitsTheCleanOreFormsAndRejectsTheDirtyOnes() {
        for (var prefix : List.of(
                MaterialPrefix.clump, MaterialPrefix.crystalline, MaterialPrefix.reduced, MaterialPrefix.cleanGravel)) {
            assertEquals(
                    SourceThermalRecyclingRules.Truth.YES,
                    SourceThermalInputRules.prefixDecision(prefix),
                    prefix.getName() + " is ORE_PROCESSING_CLEAN/REFINED");
        }
        for (var prefix : List.of(MaterialPrefix.oreRaw, MaterialPrefix.crushed, MaterialPrefix.crushedTiny)) {
            assertEquals(
                    SourceThermalRecyclingRules.Truth.NO,
                    SourceThermalInputRules.prefixDecision(prefix),
                    prefix.getName() + " is ORE/ORE_PROCESSING_DIRTY");
        }
    }

    @Test
    void cutterKeepsTheThreeRegisteredCoolantsInSourceOrder() {
        var coolants = SourceCutterRules.sourceCoolants();
        assertEquals(3, coolants.size());
        assertEquals("minecraft:water", coolants.get(0).field());
        assertEquals(4, coolants.get(0).multiplier());
        assertEquals("DistW", coolants.get(1).field());
        assertEquals(3, coolants.get(1).multiplier());
        assertEquals("Lubricant", coolants.get(2).field());
        assertEquals(1, coolants.get(2).multiplier());
    }

    @Test
    void naturalGasResolvesThroughTheSupplementalAliasTable() {
        assertTrue(OriginalFuelRecipeRows.ROWS.stream().noneMatch(row -> "NaturalGas".equals(row.input())));
    }

    @Test
    void chemicalBathKeepsTheFourReagentShapes() {
        var shapes = SourceOreWashingRules.bathShapes();
        assertEquals(4, shapes.size());
        assertEquals(MaterialPrefix.crushed, shapes.get(0).input());
        assertEquals(1, shapes.get(0).inputCount());
        assertEquals(MaterialPrefix.crushedTiny, shapes.get(1).input());
        assertEquals(9, shapes.get(1).inputCount());
        assertEquals(MaterialPrefix.dustTiny, shapes.get(2).reagent());
        assertEquals(9, shapes.get(2).reagentCount());
        assertEquals(SourceOreWashingRules.Size.TINY, shapes.get(3).outputSize());
    }

    @Test
    void shredderRecyclingUsesTheSourceRecyclablePrefixFilter() {
        assertTrue(RecyclablePrefixes.NAMES.contains("gearGt"));
        assertTrue(RecyclablePrefixes.NAMES.contains("toolHeadSaw"));
        assertTrue(!RecyclablePrefixes.NAMES.contains("dust"));
        assertTrue(!RecyclablePrefixes.NAMES.contains("crushed"));
        assertTrue(!RecyclablePrefixes.NAMES.contains("wireGt01"));
        assertTrue(RecoveryFormCatalog.shredder("gearGt"));
        assertFalse(RecoveryFormCatalog.thermal("crushed"));
        assertTrue(RecoveryFormCatalog.thermal("pipeSmall"));
        assertTrue(RecoveryFormCatalog.container("capcellcon"));
    }

    @Test
    void externalTagFormsCoverVariableCommonMaterialShapes() {
        var names = ExternalOreProcessingRules.TAGGED_MATERIAL_FORMS.stream()
                .map(MaterialPrefix::getName)
                .toList();
        assertTrue(names.containsAll(List.of("gearGt", "ring", "bolt", "screw", "foil")));
        assertEquals("storage_blocks", ExternalOreProcessingRules.TAGGED_STORAGE_BLOCK_GROUP);
        assertEquals("cables", ExternalOreProcessingRules.TAGGED_CABLE_GROUP);
    }

    @Test
    void fluidBedFuelsKeepTheirFormMatrix() {
        assertEquals(7, OriginalFluidBedFuelRows.FUELS.size());
        assertEquals(5, OriginalFluidBedFuelRows.FORMS.size());
        for (var fuel : OriginalFluidBedFuelRows.FUELS) {
            for (var form : OriginalFluidBedFuelRows.FORMS) {
                assertTrue(
                        OriginalFluidBedFuelRows.heatUnits(fuel, form) > 0,
                        fuel.sourceName() + "/" + form.prefix() + " burns");
            }
        }
    }

    @Test
    void fixedFormConversionsKeepTheDustAndBlockPairs() {
        var conversions = OriginalFormConversions.FIXED;
        assertTrue(conversions.stream()
                .anyMatch(c -> c.input().equals("dust")
                        && c.output().equals("dustTiny")
                        && c.inputCount() == 1
                        && c.outputCount() == 9));
        assertTrue(conversions.stream()
                .anyMatch(c -> c.input().equals("blockDust")
                        && c.output().equals("dust")
                        && c.inputCount() == 1
                        && c.outputCount() == 9));
        assertTrue(conversions.stream()
                .anyMatch(c -> c.input().equals("ingot")
                        && c.output().equals("nugget")
                        && c.inputCount() == 1
                        && c.outputCount() == 9));
        assertTrue(ExternalOreProcessingRules.isCompositionForm(ExternalOreProcessingRules.TAGGED_ORE_FORM));
    }

    @Test
    void centrifugeDefaultOutputCarriesMaterialUnits() {
        var binding = SourceCentrifugeItemOutputs.binding("Bone", "dust");
        var output = SourceCentrifugeItemOutputs.output(binding, 2);
        assertEquals("Bone", output.materialName());
        assertEquals(2 * 648_648_000L, output.units().units());
    }

    @Test
    void centrifugeOptionalMaterialTargetCarriesItsAmount() {
        var target = SourceCentrifugeOptionalBindings.Target.materialSpec("Ash", MaterialAmount.units(648_648_000L));
        assertEquals(SourceCentrifugeOptionalBindings.TargetKind.MATERIAL_SPEC, target.kind());
        assertEquals("Ash", target.value());
        assertEquals(648_648_000L, target.amount().units());
    }

    @Test
    void generifierKeepsItsSourceFormFilterAndCost() {
        assertTrue(GenerifyingRules.simplifiable("dust"));
        assertTrue(GenerifyingRules.simplifiable("ingot"));
        assertFalse(GenerifyingRules.simplifiable("oreRaw"));
        assertFalse(GenerifyingRules.simplifiable("crushed"));
        assertEquals(1, GenerifyingRules.duration(GTValues.U));
        assertEquals(2, GenerifyingRules.duration(2 * GTValues.U));
    }

    @Test
    void polymerRowsKeepTheirTwelveSourceForms() {
        assertEquals(12, PolymerFormingRecipeRows.forms().length);
    }

    @Test
    void pipeWeldingRowsKeepTheFiveWeldableSizes() {
        var rows = PipeWeldingRules.ROWS;
        assertEquals(5, rows.size());
        assertNotNull(rows.stream()
                .filter(row -> row.size() == PipeSpec.PipeSize.HUGE)
                .findFirst()
                .orElse(null));
        assertEquals(
                12,
                rows.stream()
                        .filter(row -> row.size() == PipeSpec.PipeSize.HUGE)
                        .findFirst()
                        .orElseThrow()
                        .curved());
    }
}
