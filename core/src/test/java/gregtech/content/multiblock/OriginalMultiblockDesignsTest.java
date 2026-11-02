package gregtech.content.multiblock;

import static org.junit.jupiter.api.Assertions.*;

import gregtech.api.machine.BlockFace;
import gregtech.api.multiblock.StructureRole;
import org.junit.jupiter.api.Test;

class OriginalMultiblockDesignsTest {
    @Test
    void fusionShellDeclaresIdleLaserInputsAndFourEnergyOutlets() {
        var cells = SharedFusionStructure.STRUCTURE.cells();
        var inputs = cells.stream()
                .filter(c -> c.role() == StructureRole.ENERGY_INPUT)
                .toList();
        var outputs = cells.stream()
                .filter(c -> c.role() == StructureRole.ENERGY_OUTPUT)
                .toList();
        assertFalse(inputs.isEmpty());
        assertTrue(inputs.stream().allMatch(c -> c.design() == 5));
        assertEquals(4, outputs.size());
        assertTrue(outputs.stream().allMatch(c -> c.design() == 2));
        assertEquals(5, resolve("fusionreactor", 18003, 5, BlockFace.NORTH, false, false));
        assertEquals(6, resolve("fusionreactor", 18003, 5, BlockFace.NORTH, true, true));
        assertEquals(2, resolve("fusionreactor", 18003, 2, BlockFace.NORTH, true, true));
    }

    @Test
    void movingPartsKeepGt6FacingAndActivityIndices() {
        for (var front : new BlockFace[] {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST}) {
            int stopped = front == BlockFace.EAST || front == BlockFace.WEST ? 2 : 0;
            assertEquals(stopped, resolve("largecrusher", 18107, 0, front, false, false));
            assertEquals(stopped + 1, resolve("largecrusher", 18107, 0, front, true, false));
            assertEquals(stopped + 1, resolve("largeshredder", 18108, 0, front, true, false));
            assertEquals(front.index() - 2, resolve("largesluice", 18106, 0, front, false, false));
            assertEquals(front.index() + 2, resolve("largesluice", 18106, 0, front, true, true));
        }
        assertEquals(0, resolve("largeelectrolyzer", 18105, 0, BlockFace.NORTH, true, false));
        assertEquals(7, resolve("largeelectrolyzer", 18105, 0, BlockFace.NORTH, true, true));
        assertEquals(1, resolve("largeelectrolyzer", 18105, 1, BlockFace.NORTH, true, true));
        assertEquals(-1, resolve("fusionreactor", 18003, -1, BlockFace.NORTH, true, true));
    }

    @Test
    void centrifugeQuadrantsStayInWorldCoordinatesWhenControllerRotates() {
        // The same local front-left corner lands NW, SE, SW and NE respectively.
        var fronts = new BlockFace[] {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST};
        int[] expected = {1, 8, 6, 3};
        for (int i = 0; i < fronts.length; i++) {
            assertEquals(
                    expected[i],
                    OriginalMultiblockDesigns.resolve(
                            "largecentrifuge", 18100, 1, -1, 0, fronts[i], false, false, bound -> 0));
            assertEquals(
                    0,
                    OriginalMultiblockDesigns.resolve(
                            "largecentrifuge", 18100, 0, 0, 1, fronts[i], false, false, bound -> 0));
        }
    }

    private static int resolve(
            String machine, int part, int declared, BlockFace front, boolean running, boolean active) {
        return OriginalMultiblockDesigns.resolve(
                machine, part, declared, 0, 0, front, running, active, bound -> bound - 1);
    }
}
