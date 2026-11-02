package gregtech.engine.addon;

import static org.junit.jupiter.api.Assertions.*;

import gregtech.api.addon.machine.BehaviorMachine;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyType;
import gregtech.api.machine.MachineCasing;
import gregtech.api.multiblock.*;
import gregtech.content.logistics.LogisticsCoreLayout;
import gregtech.content.multiblock.SharedFusionStructure;
import gregtech.content.multiblock.SharedLargeMachineLayouts;
import gregtech.content.multiblock.SharedLargeMachineParts;
import gregtech.engine.kubejs.StructureLayoutDocuments;
import java.util.List;
import org.junit.jupiter.api.Test;

class StructureConstraintIntegrationTest {
    @Test
    void rulesSurviveMachineBindingHostConversionAndDocumentRoundTrip() {
        var choices = CellMatch.oneOf(CellMatch.part("steel_wall"), CellMatch.part("titanium_wall"));
        var ports = StructurePorts.builder()
                .fluidOutput(PortRule.faces(gregtech.api.machine.MachineFace.BACK)
                        .slots(2)
                        .filter("minecraft:water", "#minecraft:lava"))
                .build();
        var source = StructureLayout.pattern("shared_shape")
                .layer(0, "SAA")
                .where('A', choices, StructureRole.ITEM_FLUID_IO, ports)
                .repeat("upper", 1, 1, 2, ".AA")
                .ignore('.')
                .require(StructureConstraint.sameBlock(choices))
                .require(StructureConstraint.count(CellMatch.part("steel_wall"), 0, 6))
                .build();
        var machine = BehaviorMachine.builder(
                        MachineCasing.builder("controller", "iron").build())
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .gui("minecraft:textures/gui/container/furnace.png")
                .build();
        var resolved = AddonMachineDeclarations.resolve(List.of(machine.withStructure(source)), List.of())
                .layouts()
                .get(0);
        assertEquals(source.constraints(), resolved.constraints());
        assertEquals("controller", resolved.name());
        var restored = StructureLayoutDocuments.parse(resolved.name(), StructureLayoutDocuments.canonical(resolved));
        assertEquals(resolved, restored);
        var script = gregtech.engine.recipe.data.JsonMaps.object(StructureLayoutDocuments.canonical(resolved));
        assertEquals(resolved, StructureLayoutDocuments.parse(resolved.name(), script));
        script.put("unknown_layout_option", true);
        assertThrows(IllegalArgumentException.class, () -> StructureLayoutDocuments.parse(resolved.name(), script));
        var host = AddonMachines.sharedLayout("test", restored);
        assertEquals(source.constraints(), host.constraints());
        assertEquals(ports, host.cells().get(0).ports());
        assertEquals(ports, host.repeats().get(0).cells().get(0).ports());
        assertEquals(1, host.cells().get(0).alternatives().size());
        assertEquals(1, host.repeats().get(0).cells().get(0).alternatives().size());
        assertThrows(
                IllegalArgumentException.class,
                () -> AddonMachineDeclarations.resolve(
                        List.of(machine.withStructure(source)),
                        List.of(new StructureLayout(resolved.name(), resolved.cells(), resolved.repeats()))));
    }

    @Test
    void originalMachinesDeclareTheirActualWorldRules() {
        var oven = SharedLargeMachineLayouts.layout("largeoven");
        assertEquals(1, oven.constraints().size());
        assertInstanceOf(StructureConstraint.SameBlock.class, oven.constraints().get(0));
        assertEquals(8, oven.cells().stream().filter(c -> c.part() == 18042).count());
        assertTrue(oven.cells().stream()
                .filter(c -> c.part() == 18042)
                .allMatch(c ->
                        c.alternatives().size() == 1 && c.alternatives().get(0).part() == 18043));
        var fusion = SharedFusionStructure.STRUCTURE;
        var matter = SharedLargeMachineLayouts.layout("largemassfab");
        assertEquals(
                List.of(4, 4),
                matter.constraints().stream()
                        .map(c -> ((StructureConstraint.Count) c).min())
                        .toList());
        assertEquals(
                8,
                matter.cells().stream().filter(c -> !c.alternatives().isEmpty()).count());
        assertEquals(
                List.of(3, 12, 12),
                fusion.constraints().stream()
                        .map(c -> ((StructureConstraint.Count) c).min())
                        .toList());
        assertEquals(
                27,
                fusion.cells().stream()
                        .filter(c -> c.match() instanceof CellMatch.OneOf)
                        .count());
        for (var rule : LogisticsCoreLayout.CONSTRAINTS) {
            var choices = (CellMatch.OneOf) rule.selection().match();
            for (var choice : choices.members()) {
                var part = (CellMatch.Part) choice;
                assertTrue(SharedLargeMachineParts.idForName(part.name()) >= 0, part.name());
            }
        }
    }
}
