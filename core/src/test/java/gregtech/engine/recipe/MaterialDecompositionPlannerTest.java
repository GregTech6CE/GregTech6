package gregtech.engine.recipe;

import gregtech.api.addon.material.MaterialDecompositionRule;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.material.GTValues;
import gregtech.content.material.MaterialDefinitions;
import gregtech.content.material.MaterialTestFixture;
import gregtech.content.recipe.SourceDecompositionRules;
import gregtech.engine.material.MaterialInfoRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaterialDecompositionPlannerTest {
    @BeforeAll
    static void init() {
        MaterialTestFixture.ensureInitialized();
    }

    @Test
    void everyRoutedMaterialHasOneCompleteDeclaration() {
        for (var declaration : MaterialDefinitions.declarations()) {
            boolean centrifuge = declaration.material().properties().contains(
                    gregtech.api.material.MaterialProperty.CENTRIFUGE);
            boolean electrolyser = declaration.material().properties().contains(
                    gregtech.api.material.MaterialProperty.ELECTROLYSER);
            if (!centrifuge && !electrolyser) continue;
            var rule = MaterialInfoRegistry.processes(new MaterialKey(declaration.name()))
                    .findSingle(MaterialProcessKeys.DECOMPOSITION)
                    .orElseThrow(() -> new AssertionError("missing decomposition rule: " + declaration.name()));
            assertEquals(centrifuge, rule.value().routes().contains(MaterialDecompositionRule.Route.CENTRIFUGE));
            assertEquals(electrolyser, rule.value().routes().contains(MaterialDecompositionRule.Route.ELECTROLYSER));
            assertFalse(declaration.info().composition().parts().isEmpty(), declaration.name());
        }
    }

    @Test
    void powelliteSolidPlanKeepsElectrolyserSelectorAndOrderedOutputs() {
        var material = gregtech.engine.material.GTMaterialRegistry.get("Powellite");
        var rule = MaterialInfoRegistry.processes(new MaterialKey("Powellite"))
                .findSingle(MaterialProcessKeys.DECOMPOSITION)
                .orElseThrow()
                .value();
        var result = MaterialDecompositionPlanner.plan(
                material,
                rule,
                MaterialDecompositionRule.Route.ELECTROLYSER,
                6,
                2);
        assertTrue(result.planned(), result.status() + ": " + result.reason());
        var plan = result.plan();
        assertEquals(6L, plan.divider());
        assertEquals(6L * GTValues.U, plan.inputUnits());
        assertTrue(plan.solidInput());
        assertTrue(plan.electrolyserCatalyst());
        assertEquals(
                java.util.List.of(
                        new MaterialDecompositionPlanner.DustOutput(new MaterialKey("Calcium"), GTValues.U),
                        new MaterialDecompositionPlanner.DustOutput(new MaterialKey("Molybdenum"), GTValues.U)),
                plan.itemOutputs());
        assertEquals(
                java.util.List.of(new MaterialDecompositionPlanner.FluidOutput(
                        new MaterialKey("Oxygen"),
                        MaterialDecompositionRule.Phase.GAS,
                        "oxygen",
                        4000,
                        true)),
                plan.fluidOutputs());
        assertEquals(0, plan.droppedItemOutputs());
        assertEquals(0, plan.droppedFluidOutputs());
    }

    @Test
    void declaredWaterPhasesRemainAvailableWithoutInventingARoute() {
        var material = gregtech.engine.material.GTMaterialRegistry.get("Water");
        var rule = MaterialInfoRegistry.processes(new MaterialKey("Water"))
                .findSingle(MaterialProcessKeys.DECOMPOSITION)
                .orElseThrow()
                .value();
        assertTrue(rule.routes().isEmpty());
        assertTrue(rule.fluids().contains(new MaterialDecompositionRule.FluidBinding(
                MaterialDecompositionRule.Phase.LIQUID, "minecraft:water", SourceDecompositionRules.SOURCE_U, 1000)));
        assertTrue(rule.fluids().contains(new MaterialDecompositionRule.FluidBinding(
                MaterialDecompositionRule.Phase.GAS, "steam", SourceDecompositionRules.SOURCE_U, 160000)));
        assertEquals(
                MaterialDecompositionPlanner.Status.NO_PERMISSION,
                MaterialDecompositionPlanner.plan(
                                material, rule, MaterialDecompositionRule.Route.ELECTROLYSER, 2, 2)
                        .status());
    }

    @Test
    void glowstoneRetainsSolidFallbackAndDeclaredGasScale() {
        var rule = MaterialInfoRegistry.processes(new MaterialKey("Glowstone"))
                .findSingle(MaterialProcessKeys.DECOMPOSITION)
                .orElseThrow()
                .value();
        assertTrue(rule.keepSolidInput());
        assertTrue(rule.fluids().contains(new MaterialDecompositionRule.FluidBinding(
                MaterialDecompositionRule.Phase.LIQUID, "molten.glowstone", SourceDecompositionRules.SOURCE_U, 144)));
        assertTrue(rule.fluids().contains(new MaterialDecompositionRule.FluidBinding(
                MaterialDecompositionRule.Phase.GAS, "glowstone", SourceDecompositionRules.SOURCE_U, 250)));
        var result = MaterialDecompositionPlanner.plan(
                gregtech.engine.material.GTMaterialRegistry.get("Glowstone"),
                rule,
                MaterialDecompositionRule.Route.CENTRIFUGE,
                6,
                2);
        assertTrue(result.planned(), result.status() + ": " + result.reason());
        assertEquals(
                java.util.List.of(
                        new MaterialDecompositionPlanner.FluidInput(
                                MaterialDecompositionRule.Phase.LIQUID, "molten.glowstone", 1440, true),
                        new MaterialDecompositionPlanner.FluidInput(
                                MaterialDecompositionRule.Phase.GAS, "glowstone", 2500, true)),
                result.plan().fluidInputs());
    }

    @Test
    void undeclaredAmbientComponentUsesNativeFluidFormBeforePowder() {
        var material = gregtech.engine.material.GTMaterialRegistry.get("Datolite");
        var rule = MaterialInfoRegistry.processes(new MaterialKey("Datolite"))
                .findSingle(MaterialProcessKeys.DECOMPOSITION)
                .orElseThrow()
                .value();
        var result = MaterialDecompositionPlanner.plan(
                material,
                rule,
                MaterialDecompositionRule.Route.ELECTROLYSER,
                6,
                2);
        assertTrue(result.planned(), result.status() + ": " + result.reason());
        assertFalse(result.plan().itemOutputs().stream()
                .anyMatch(output -> output.material().equals(new MaterialKey("Hydrogen"))));
        assertTrue(result.plan().fluidOutputs().contains(new MaterialDecompositionPlanner.FluidOutput(
                new MaterialKey("Hydrogen"),
                MaterialDecompositionRule.Phase.GAS,
                "Hydrogen",
                2000,
                false)));
    }

    @Test
    void outputCapsPreserveOrderAndReportDroppedTail() {
        var material = gregtech.engine.material.GTMaterialRegistry.get("Powellite");
        var rule = MaterialInfoRegistry.processes(new MaterialKey("Powellite"))
                .findSingle(MaterialProcessKeys.DECOMPOSITION)
                .orElseThrow()
                .value();
        var result = MaterialDecompositionPlanner.plan(
                material,
                rule,
                MaterialDecompositionRule.Route.ELECTROLYSER,
                1,
                0);
        assertTrue(result.planned(), result.status() + ": " + result.reason());
        assertEquals(1, result.plan().itemOutputs().size());
        assertTrue(result.plan().fluidOutputs().isEmpty());
        assertEquals(1, result.plan().droppedItemOutputs());
        assertEquals(1, result.plan().droppedFluidOutputs());
    }

    @Test
    void permissionAndQuantityFailuresStayDistinct() {
        var material = gregtech.engine.material.GTMaterialRegistry.get("Powellite");
        var rule = MaterialInfoRegistry.processes(new MaterialKey("Powellite"))
                .findSingle(MaterialProcessKeys.DECOMPOSITION)
                .orElseThrow()
                .value();
        assertEquals(
                MaterialDecompositionPlanner.Status.NO_PERMISSION,
                MaterialDecompositionPlanner.plan(
                                material, rule, MaterialDecompositionRule.Route.CENTRIFUGE, 2, 2)
                        .status());
        var iron = gregtech.engine.material.GTMaterialRegistry.get("Iron");
        assertEquals(
                MaterialDecompositionPlanner.Status.NO_COMPOSITION,
                MaterialDecompositionPlanner.plan(
                                iron, rule, MaterialDecompositionRule.Route.ELECTROLYSER, 2, 2)
                        .status());
        assertEquals(-1, SourceDecompositionRules.units(Long.MAX_VALUE, 1, Long.MAX_VALUE, true));
    }
}
