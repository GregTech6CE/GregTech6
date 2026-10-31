package gregtech.engine.recipe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RecipeInputMarksTest {
    @Test
    void emptyMarksReadNothingAndExportNothing() {
        var marks = new RecipeInputMarks();
        assertTrue(marks.isEmpty());
        assertFalse(marks.get(0));
        assertFalse(marks.get(63));
        assertFalse(marks.get(10_000));
        assertArrayEquals(new int[0], marks.toArray());
    }

    @Test
    void marksKeepLargeIndexesAndExportInAscendingOrder() {
        var marks = new RecipeInputMarks();
        marks.set(70);
        marks.set(2);
        marks.set(63);
        marks.set(64);
        assertTrue(marks.get(63));
        assertTrue(marks.get(64));
        assertTrue(marks.get(70));
        assertFalse(marks.get(65));
        assertFalse(marks.isEmpty());
        assertArrayEquals(new int[] {2, 63, 64, 70}, marks.toArray());
    }

    @Test
    void repeatedMarksCollapseAndNegativeIndexesFail() {
        var marks = new RecipeInputMarks();
        marks.set(5);
        marks.set(5);
        assertArrayEquals(new int[] {5}, marks.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> marks.set(-1));
    }
}
