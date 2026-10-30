/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.multiblock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CellMatchTest {
    @Test
    void parseRecognizesEveryForm() {
        assertEquals(CellMatch.air(), CellMatch.parse("air"));
        assertEquals(CellMatch.anyFluid(), CellMatch.parse("anyfluid"));
        assertEquals(CellMatch.anyOpaque(), CellMatch.parse("anyopaque"));
        assertEquals("minecraft:water", assertInstanceOf(CellMatch.AnyFluid.class,
                CellMatch.parse("anyfluid#minecraft:water")).tag());
        assertEquals("minecraft:logs", assertInstanceOf(CellMatch.Tag.class,
                CellMatch.parse("#minecraft:logs")).id());
        assertEquals("minecraft:stone", assertInstanceOf(CellMatch.Block.class,
                CellMatch.parse("minecraft:stone")).id());
        assertEquals("casing", assertInstanceOf(CellMatch.Part.class, CellMatch.parse("casing")).name());
    }

    @Test
    void parseRejectsBlankAndInvalidBlockIds() {
        assertThrows(IllegalArgumentException.class, () -> CellMatch.parse(null));
        assertThrows(IllegalArgumentException.class, () -> CellMatch.parse(" "));
        assertThrows(IllegalArgumentException.class, () -> CellMatch.parse("minecraft:"));
        assertThrows(IllegalArgumentException.class, () -> CellMatch.parse("#stone"));
    }

    @Test
    void partRejectsNamesThatBelongToOtherForms() {
        assertThrows(IllegalArgumentException.class, () -> CellMatch.part("minecraft:stone"));
        assertThrows(IllegalArgumentException.class, () -> CellMatch.part("#minecraft:logs"));
        assertThrows(IllegalArgumentException.class, () -> CellMatch.part("Casing"));
        assertEquals("casing", assertInstanceOf(CellMatch.Part.class, CellMatch.part("casing")).name());
    }

    @Test
    void oneOfNeedsTwoDistinctMembers() {
        CellMatch part = CellMatch.part("casing");
        assertThrows(IllegalArgumentException.class, () -> CellMatch.oneOf(part));
        assertThrows(IllegalArgumentException.class, () -> CellMatch.oneOf(part, part));
        assertThrows(IllegalArgumentException.class, () -> CellMatch.oneOf(part, CellMatch.oneOf(
                CellMatch.part("a"), CellMatch.part("b"))));
        CellMatch group = CellMatch.oneOf(part, CellMatch.air());
        assertEquals(2, ((CellMatch.OneOf) group).members().size());
    }
}
