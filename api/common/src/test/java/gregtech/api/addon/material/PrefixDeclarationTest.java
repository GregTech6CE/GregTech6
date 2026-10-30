/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import gregtech.api.material.GTValues;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrefixDeclarationTest {
    @Test
    void acceptsIdentifierMaterialNames() {
        PrefixDeclaration prefix = new PrefixDeclaration(
                "ingot", "", " Ingot", GTValues.U, "material/ingot",
                List.of("Iron", "CertusQuartz"), Set.of());
        assertEquals(List.of("Iron", "CertusQuartz"), prefix.materials());
    }

    @Test
    void rejectsMaterialNamesThatAreNotIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> new PrefixDeclaration(
                "ingot", "", " Ingot", GTValues.U, "material/ingot",
                List.of("Iron Ingot"), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new PrefixDeclaration(
                "ingot", "", " Ingot", GTValues.U, "material/ingot",
                List.of("gt:iron"), Set.of()));
    }

    @Test
    void rejectsInvalidExcludedMaterialNames() {
        assertThrows(IllegalArgumentException.class, () -> new PrefixDeclaration(
                "ingot", "", " Ingot", GTValues.U, "material/ingot",
                List.of("Iron"), Set.of(), List.of("bad name")));
    }
}
