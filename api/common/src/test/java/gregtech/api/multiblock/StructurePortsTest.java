package gregtech.api.multiblock;

import static org.junit.jupiter.api.Assertions.*;
import gregtech.api.machine.MachineFace;
import java.util.List;
import org.junit.jupiter.api.Test;

class StructurePortsTest {
    @Test void rolesSeparateItemsFluidsAndEnergy() {
        assertTrue(StructureRole.FLUID_INPUT.allows(PortChannel.FLUID_INPUT));
        assertFalse(StructureRole.FLUID_INPUT.allows(PortChannel.FLUID_OUTPUT));
        assertFalse(StructureRole.FLUID_INPUT.allows(PortChannel.ITEM_INPUT));
        assertTrue(StructureRole.FLUID_IO.allows(PortChannel.FLUID_OUTPUT));
        assertTrue(StructureRole.ITEM_IO.allows(PortChannel.ITEM_INPUT));
        assertTrue(StructureRole.ITEM_IO.allows(PortChannel.ITEM_OUTPUT));
        assertFalse(StructureRole.ITEM_IO.allows(PortChannel.FLUID_INPUT));
        for (var channel : PortChannel.values()) assertFalse(StructureRole.CASING.allows(channel));
    }

    @Test void rulesSurviveSymbolsDesignAndRepeatsWithoutChangingLegacyDefaults() {
        var ports = StructurePorts.builder().fluidOutput(PortRule.faces(MachineFace.BACK).slots(1).filter("#minecraft:water")).build();
        var layout = StructureLayout.pattern("tower").layer(0, "SA")
                .where('A', CellMatch.part("wall"), StructureRole.FLUID_IO, 0, ports)
                .ignore('.').repeat("upper", 1, 0, 2, ".A").build();
        assertEquals(ports, layout.cells().get(0).ports());
        assertEquals(ports, layout.repeats().get(0).cells().get(0).ports());
        assertEquals(ports, layout.cells().get(0).withDesign(1).ports());
        assertEquals(StructurePorts.DEFAULT, new LayoutCell(1, 0, 0, CellMatch.part("wall"), StructureRole.FLUID_IO, -1).ports());
        assertTrue(ports.rule(PortChannel.FLUID_INPUT).allowsFace(null));
        assertFalse(ports.rule(PortChannel.FLUID_OUTPUT).allowsFace(null));
        assertFalse(ports.rule(PortChannel.FLUID_OUTPUT).allowsSlot(0));
    }

    @Test void malformedRestrictionsFailDuringDeclaration() {
        assertThrows(IllegalArgumentException.class, () -> PortRule.ALL.slots(-1));
        assertThrows(IllegalArgumentException.class, () -> PortRule.ALL.slots(1, 1));
        assertThrows(IllegalArgumentException.class, () -> PortRule.ALL.filter("water"));
        assertThrows(IllegalArgumentException.class, () -> StructurePorts.builder().energyInput(PortRule.ALL.slots(1)).build());
        assertThrows(IllegalArgumentException.class, () -> LayoutCell.at(1, 0, 0, CellMatch.part("wall"), StructureRole.CASING)
                .withPorts(StructurePorts.builder().fluidOutput(PortRule.NONE).build()));
        assertThrows(UnsupportedOperationException.class, () -> StructurePorts.DEFAULT.rules().put(PortChannel.ITEM_INPUT, PortRule.ALL));
    }
}
