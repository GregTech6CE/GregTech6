package gregtech.engine.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Item and fluid slot grid parity with GT6's {@code ContainerCommonBasicMachine.addSlots}. */
final class MachineGuiCoordinatesTest {
    @Test
    void printerInputsSitOnTheSecondRowWithSixTanks() {
        // Printer: two inputs and six input tanks; GT6 only lifts the row above six tanks.
        assertItems(true, 2, 6, "35,25 53,25");
        assertItems(false, 1, 0, "107,25");
    }

    @Test
    void shortInputRowsKeepTheOriginalRightAlignedColumns() {
        assertItems(true, 1, 7, "53,7");
        assertItems(true, 1, 6, "53,25");
        assertItems(true, 3, 6, "17,25 35,25 53,25");
        assertItems(true, 3, 7, "17,7 35,7 53,7");
    }

    @Test
    void fourToSixSlotsUseTheCompressedRows() {
        assertItems(true, 4, 4, "35,7 53,7 35,25 53,25");
        assertItems(true, 4, 3, "35,16 53,16 35,34 53,34");
        assertItems(true, 5, 4, "17,7 35,7 53,7 35,25 53,25");
        assertItems(true, 6, 3, "17,16 35,16 53,16 17,34 35,34 53,34");
    }

    @Test
    void sevenToTwelveSlotsKeepTheOriginalLastRowColumns() {
        assertItems(true, 7, 0, "17,7 35,7 53,7 17,25 35,25 53,25 17,43");
        assertItems(true, 8, 0, "17,7 35,7 53,7 17,25 35,25 53,25 17,43 35,43");
        assertItems(true, 10, 0, "17,7 35,7 53,7 17,25 35,25 53,25 17,43 35,43 53,43 53,61");
        assertItems(true, 11, 0, "17,7 35,7 53,7 17,25 35,25 53,25 17,43 35,43 53,43 35,61 53,61");
    }

    @Test
    void outputSlotsKeepTheOriginalLeftAlignedColumns() {
        assertItems(false, 1, 0, "107,25");
        assertItems(false, 2, 6, "107,25 125,25");
        assertItems(false, 3, 7, "107,7 125,7 143,7");
        assertItems(false, 4, 4, "107,7 125,7 107,25 125,25");
        assertItems(false, 5, 4, "107,7 125,7 143,7 107,25 125,25");
        assertItems(false, 10, 0, "107,7 125,7 143,7 107,25 125,25 143,25 107,43 125,43 143,43 143,61");
    }

    @Test
    void fluidSlotsKeepTheOriginalBottomUpColumns() {
        assertEquals(new MachineGuiCoordinates.Point(53, 63), MachineGuiCoordinates.fluid(true, 0));
        assertEquals(new MachineGuiCoordinates.Point(17, 63), MachineGuiCoordinates.fluid(true, 2));
        assertEquals(new MachineGuiCoordinates.Point(35, 45), MachineGuiCoordinates.fluid(true, 4));
        assertEquals(new MachineGuiCoordinates.Point(107, 63), MachineGuiCoordinates.fluid(false, 0));
        assertEquals(new MachineGuiCoordinates.Point(143, 45), MachineGuiCoordinates.fluid(false, 5));
    }

    private static void assertItems(boolean input, int count, int fluids, String expected) {
        String[] points = expected.split(" ");
        assertEquals(count, points.length, "case " + count + "/" + fluids);
        for (int index = 0; index < count; index++) {
            String[] xy = points[index].split(",");
            assertEquals(
                    new MachineGuiCoordinates.Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])),
                    MachineGuiCoordinates.item(input, index, count, fluids),
                    (input ? "input " : "output ") + count + "/" + fluids + "/" + index);
        }
    }
}
