/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.multiblock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructureLayoutTest {
    private static final CellMatch CASING = CellMatch.part("casing");

    @Test
    void buildsFixedCellsAndRepeats() {
        StructureLayout layout = StructureLayout.builder("test_layout")
                .cell(0, 0, 1, CASING, StructureRole.CASING)
                .repeat(LayoutRepeat.builder("tower", 0, 1, -1)
                        .cell(1, 0, 0, CASING, StructureRole.CASING)
                        .build())
                .build();
        assertEquals(1, layout.cells().size());
        assertEquals(1, layout.repeats().size());
    }

    @Test
    void rejectsDuplicateCellsAndRepeatKeys() {
        assertThrows(IllegalArgumentException.class, () -> StructureLayout.builder("test_layout")
                .cell(0, 0, 1, CASING, StructureRole.CASING)
                .cell(0, 0, 1, CASING, StructureRole.CASING)
                .build());
        LayoutRepeat repeat = LayoutRepeat.builder("tower", 0, 1, 1)
                .cell(1, 0, 0, CASING, StructureRole.CASING)
                .build();
        assertThrows(IllegalArgumentException.class, () -> StructureLayout.builder("test_layout")
                .repeat(repeat)
                .repeat(repeat)
                .build());
    }

    @Test
    void repeatCellCannotBeTheOrigin() {
        assertThrows(IllegalArgumentException.class, () -> StructureLayout.builder("test_layout")
                .repeat(LayoutRepeat.builder("tower", 0, 1, 1)
                        .cell(0, 0, 0, CASING, StructureRole.CASING)
                        .build())
                .build());
    }

    @Test
    void boundedRepeatChecksEveryCopyItMayContain() {
        assertThrows(IllegalArgumentException.class, () -> StructureLayout.builder("test_layout")
                .cell(1, 2, 0, CASING, StructureRole.CASING)
                .repeat(LayoutRepeat.builder("tower", 0, 1, 3)
                        .cell(1, 0, 0, CASING, StructureRole.CASING)
                        .build())
                .build());
    }

    @Test
    void unboundedRepeatOnlyChecksItsGuaranteedCopies() {
        LayoutRepeat tower = LayoutRepeat.builder("tower", 0, 1, -1)
                .cell(1, 0, 0, CASING, StructureRole.CASING)
                .build();
        StructureLayout capped = StructureLayout.builder("test_layout")
                .cell(1, 3, 0, CASING, StructureRole.CASING)
                .repeat(tower)
                .build();
        assertEquals(1, capped.repeats().size());

        assertThrows(IllegalArgumentException.class, () -> StructureLayout.builder("test_layout")
                .cell(1, 0, 0, CASING, StructureRole.CASING)
                .repeat(tower)
                .build());

        LayoutRepeat minimumThree = LayoutRepeat.builder("tower", 0, 3, -1)
                .cell(1, 0, 0, CASING, StructureRole.CASING)
                .build();
        assertThrows(IllegalArgumentException.class, () -> StructureLayout.builder("test_layout")
                .cell(1, 1, 0, CASING, StructureRole.CASING)
                .repeat(minimumThree)
                .build());
    }

    @Test
    void rejectsInvalidNamesAndEmptyLayouts() {
        assertThrows(IllegalArgumentException.class, () -> StructureLayout.builder("Bad Name").build());
        assertThrows(IllegalArgumentException.class, () -> StructureLayout.builder("test_layout").build());
    }
}
