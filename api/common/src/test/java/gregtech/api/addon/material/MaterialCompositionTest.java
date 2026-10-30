/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import gregtech.api.material.GTValues;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaterialCompositionTest {
    @Test
    void fractionalPartsAreAcceptedDownToU72() {
        assertEquals(GTValues.U9, new MaterialComposition.Part("Iron", GTValues.U9).amount());
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialComposition.Part("Iron", GTValues.U72 - 1));
    }

    @Test
    void crucibleCompositionsNeedWholeUnitParts() {
        MaterialComposition.Part part = new MaterialComposition.Part("Iron", GTValues.U4);
        assertEquals(GTValues.U4, new MaterialComposition("TestMaterial", 1, List.of(part), false)
                .parts().get(0).amount());
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialComposition("TestMaterial", 1, List.of(part), true));
    }

    @Test
    void compositionRejectsInvalidPartsAndDivider() {
        MaterialComposition.Part iron = new MaterialComposition.Part("Iron", GTValues.U);
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialComposition("TestMaterial", 0, List.of(iron), false));
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialComposition("TestMaterial", 1, List.of(), false));
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialComposition("TestMaterial", 1, List.of(iron, iron), false));
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialComposition("bad name", 1, List.of(iron), false));
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialComposition.Part("bad name", GTValues.U));
    }
}
