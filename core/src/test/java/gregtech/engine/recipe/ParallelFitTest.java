package gregtech.engine.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParallelFitTest {
    @Test
    void powerCapAppliesOnlyToPowerScaledBatches() {
        assertEquals(64, ParallelFit.powerCap(256, 512, 8, false, false));
        assertEquals(256, ParallelFit.powerCap(256, 8192, 8, false, false));
        assertEquals(256, ParallelFit.powerCap(256, 512, 8, true, false));
        assertEquals(256, ParallelFit.powerCap(256, 512, 8, false, true));
        assertEquals(256, ParallelFit.powerCap(256, 512, 0, false, false));
    }

    @Test
    void amountCapKeepsAmountsRepresentable() {
        assertEquals(256, ParallelFit.amountCap(256, 1000));
        assertEquals(2, ParallelFit.amountCap(256, 1_000_000_000));
        assertEquals(1, ParallelFit.amountCap(256, Integer.MAX_VALUE));
        assertEquals(256, ParallelFit.amountCap(256, 0));
    }

    @Test
    void itemsFitIntoEmptySlotsUpToCapacity() {
        int[] keys = {ParallelFit.EMPTY};
        int[] counts = {0};
        int[] capacities = {64};
        long[] outputs = {1};
        assertTrue(ParallelFit.itemsFit(keys, counts, capacities, outputs, 64));
        assertFalse(ParallelFit.itemsFit(new int[] {ParallelFit.EMPTY}, new int[] {0}, capacities, outputs, 65));
    }

    @Test
    void itemsMergeIntoMatchingSlots() {
        int[] capacities = {64};
        long[] outputs = {1};
        assertTrue(ParallelFit.itemsFit(new int[] {0}, new int[] {60}, capacities, outputs, 4));
        assertFalse(ParallelFit.itemsFit(new int[] {0}, new int[] {60}, capacities, outputs, 5));
    }

    @Test
    void itemsIgnoreUnrelatedSlots() {
        int[] keys = {ParallelFit.NO_MATCH};
        int[] counts = {1};
        int[] capacities = {64};
        long[] outputs = {1};
        assertFalse(ParallelFit.itemsFit(keys, counts, capacities, outputs, 1));
    }

    @Test
    void itemsOfDifferentOutputsDoNotShareASlot() {
        int[] keys = {ParallelFit.EMPTY, ParallelFit.EMPTY};
        int[] counts = {0, 0};
        int[] capacities = {64, 64};
        long[] outputs = {1, 1};
        assertTrue(ParallelFit.itemsFit(keys, counts, capacities, outputs, 64));
        // The first output owns both slots after its fill; the second has nowhere left.
        assertFalse(ParallelFit.itemsFit(
                new int[] {ParallelFit.EMPTY, ParallelFit.EMPTY}, new int[] {0, 0}, capacities, new long[] {1, 1}, 65));
    }

    @Test
    void fluidsFitIntoFreeCapacity() {
        int[] keys = {ParallelFit.EMPTY};
        long[] free = {1000};
        long[] outputs = {100};
        assertTrue(ParallelFit.fluidsFit(keys, free, outputs, 10));
        assertFalse(ParallelFit.fluidsFit(new int[] {ParallelFit.EMPTY}, new long[] {1000}, outputs, 11));
    }

    @Test
    void fluidsMergeIntoMatchingTanks() {
        long[] outputs = {100};
        assertTrue(ParallelFit.fluidsFit(new int[] {0}, new long[] {500}, outputs, 5));
        assertFalse(ParallelFit.fluidsFit(new int[] {0}, new long[] {500}, outputs, 6));
    }

    @Test
    void fluidsIgnoreUnrelatedTanks() {
        assertFalse(ParallelFit.fluidsFit(new int[] {ParallelFit.NO_MATCH}, new long[] {1000}, new long[] {100}, 1));
    }

    @Test
    void differentFluidOutputsDoNotShareATank() {
        // The adapter groups identical outputs; distinct groups each need their own tank.
        long[] outputs = {100, 100};
        assertFalse(ParallelFit.fluidsFit(new int[] {ParallelFit.EMPTY}, new long[] {200}, outputs, 1));
        assertTrue(ParallelFit.fluidsFit(
                new int[] {ParallelFit.EMPTY, ParallelFit.EMPTY}, new long[] {100, 100}, outputs, 1));
    }
}
