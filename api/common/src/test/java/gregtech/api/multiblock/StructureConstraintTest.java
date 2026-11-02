package gregtech.api.multiblock;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class StructureConstraintTest {
    private static final CellMatch IRON = CellMatch.part("iron_wall");
    private static final CellMatch COPPER = CellMatch.part("copper_wall");

    private static StructurePattern pattern() {
        return StructureLayout.pattern("choices").layer(0, "SAA")
                .where('A', CellMatch.oneOf(IRON, COPPER), StructureRole.CASING);
    }

    @Test
    void worldCountsAreIndependentOfSymbolCounts() {
        var rule = StructureConstraint.exactly(IRON, 1);
        var layout = pattern().count('A', 2, 2).require(rule).build();
        assertEquals(List.of(rule), layout.constraints());
        assertTrue(rule.selection().selects(IRON, StructureRole.CASING));
        assertFalse(rule.selection().selects(COPPER, StructureRole.CASING));
        assertThrows(UnsupportedOperationException.class, () -> layout.constraints().clear());
        assertTrue(new StructureLayout(layout.name(), layout.cells(), layout.repeats()).constraints().isEmpty());
    }

    @Test
    void rejectsInvalidSelectorsAndImpossibleBoundsBeforeRegistration() {
        assertThrows(IllegalArgumentException.class, () -> pattern().require(StructureConstraint.exactly(IRON, 3)).build());
        assertThrows(IllegalArgumentException.class, () -> pattern().require(
                StructureConstraint.sameBlock(CellMatch.part("missing"))).build());
        assertThrows(IllegalArgumentException.class, () -> StructureConstraint.count(IRON, 0, -2));
        assertThrows(IllegalArgumentException.class, () -> StructureLayout.pattern("fixed").layer(0, "SA")
                .where('A', IRON, StructureRole.CASING).require(StructureConstraint.exactly(IRON, 0)).build());
    }

    @Test
    void repeatCapacityAndRoleArePartOfCountValidation() {
        var layout = StructureLayout.pattern("tower").layer(0, "S.").ignore('.')
                .repeat("coils", 1, 2, 4, ".A").where('A', IRON, StructureRole.CASING)
                .require(StructureConstraint.count(IRON, 3, 4)).build();
        assertEquals(1, layout.constraints().size());
        assertThrows(IllegalArgumentException.class, () -> new StructureLayout(layout.name(), layout.cells(),
                layout.repeats(), List.of(StructureConstraint.exactly(IRON, 5))));
        assertThrows(IllegalArgumentException.class, () -> pattern().require(new StructureConstraint.Count(
                StructureConstraint.Selection.role(StructureRole.ENERGY_INPUT), 1, -1)).build());
    }
}
