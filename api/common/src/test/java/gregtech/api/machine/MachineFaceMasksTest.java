/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineFaceMasksTest {
    @Test
    void machineFaceIndexesKeepTheGt6SideOrder() {
        assertEquals(0, MachineFace.BOTTOM.index());
        assertEquals(1, MachineFace.TOP.index());
        assertEquals(2, MachineFace.RIGHT.index());
        assertEquals(3, MachineFace.FRONT.index());
        assertEquals(4, MachineFace.LEFT.index());
        assertEquals(5, MachineFace.BACK.index());
    }

    @Test
    void builderCollectsNamedFaces() {
        MachineFaceMasks masks = MachineFaceMasks.builder()
                .itemIn(MachineFace.TOP, MachineFace.FRONT)
                .itemOut(MachineFace.BOTTOM)
                .energyIn(MachineFace.LEFT)
                .build();
        assertTrue(masks.itemInputs().contains(MachineFace.TOP));
        assertTrue(masks.itemInputs().contains(MachineFace.FRONT));
        assertFalse(masks.itemInputs().contains(MachineFace.BOTTOM));
        assertTrue(masks.itemOutputs().contains(MachineFace.BOTTOM));
        assertTrue(masks.energyInputs().contains(MachineFace.LEFT));
        assertEquals(Set.of(), masks.energyOutputs());
    }

    @Test
    void repeatedBuilderCallsUnionTheirFaces() {
        MachineFaceMasks masks = MachineFaceMasks.builder()
                .itemIn(MachineFace.TOP)
                .itemIn(MachineFace.BOTTOM)
                .build();
        assertEquals(Set.of(MachineFace.TOP, MachineFace.BOTTOM), masks.itemInputs());
    }

    @Test
    void allSidesEnablesEveryFaceForInputAndOutput() {
        assertEquals(Set.copyOf(MachineFace.all()), MachineFaceMasks.ALL_SIDES.itemInputs());
        assertEquals(Set.copyOf(MachineFace.all()), MachineFaceMasks.ALL_SIDES.itemOutputs());
        assertNull(MachineFaceMasks.ALL_SIDES.itemAutoInput());
    }

    @Test
    void builderRejectsNullFaces() {
        assertThrows(IllegalArgumentException.class,
                () -> MachineFaceMasks.builder().itemIn((MachineFace) null));
        assertThrows(IllegalArgumentException.class,
                () -> MachineFaceMasks.builder().energyOut((MachineFace[]) null));
    }

    @Test
    void autoFacesAreNullableMachineFaces() {
        assertNull(MachineFaceMasks.builder().itemAutoIn(null).build().itemAutoInput());
        assertEquals(MachineFace.LEFT,
                MachineFaceMasks.builder().fluidAutoOut(MachineFace.LEFT).build().fluidAutoOutput());
    }
}
