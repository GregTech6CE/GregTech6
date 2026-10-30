/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GTMaterialTest {
    @Test
    void translationKeysDoNotDependOnTheDefaultLocale() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            GTMaterial iron = new GTMaterial(1, "Iron", "Iron", 0xFFFFFF);
            assertEquals("material.gt6ce.iron", iron.getTranslationKey());
            assertEquals("fiery", MaterialTextureSet.FIERY.folder());
            assertEquals("lignite", MaterialTextureSet.LIGNITE.folder());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void oreMultiplierClampsInsteadOfOverflowingAByte() {
        GTMaterial material = new GTMaterial(1, "Test", "Test", 0xFFFFFF);
        assertEquals(1, material.setOreMultiplier(0).getOreMultiplier());
        assertEquals(Byte.MAX_VALUE, material.setOreMultiplier(200).getOreMultiplier());
        assertEquals(Byte.MAX_VALUE, material.setOreMultiplier(256).getOreMultiplier());
        assertEquals(Byte.MAX_VALUE, material.setOreMultiplier(Integer.MAX_VALUE).getOreMultiplier());
    }

    @Test
    void oreProcessingMultiplierHasTheSameClamp() {
        GTMaterial material = new GTMaterial(1, "Test", "Test", 0xFFFFFF);
        assertEquals(1, material.setOreProcessingMultiplier(0).getOreProcessingMultiplier());
        assertEquals(Byte.MAX_VALUE, material.setOreProcessingMultiplier(300).getOreProcessingMultiplier());
    }

    @Test
    void compositionDropsInvalidComponentsAndDerivesTheDivider() {
        GTMaterial material = new GTMaterial(1, "Test", "Test", 0xFFFFFF);
        GTMaterial iron = new GTMaterial(2, "Iron", "Iron", 0xFFFFFF);
        material.setComposition(0, new MaterialComponent(iron, GTValues.U), new MaterialComponent(null, GTValues.U));

        assertTrue(material.hasComposition());
        assertEquals(1, material.getCompositionComponents().size());
        assertEquals(1, material.getCompositionDivider());
    }

    @Test
    void localNameIsRequired() {
        GTMaterial material = new GTMaterial(1, "Test", "Test", 0xFFFFFF);
        assertThrows(NullPointerException.class, () -> material.setLocalName(null));
    }

    @Test
    void sanitizeStripsSpacesDashesAndApostrophes() {
        assertEquals("WroughtIron", GTMaterial.sanitize("Wrought Iron"));
        assertEquals("ClayBrown", GTMaterial.sanitize("Clay-Brown"));
        assertEquals("KingsSteel", GTMaterial.sanitize("King's Steel"));
    }

    @Test
    void validityFollowsTheId() {
        assertTrue(new GTMaterial(1, "Test", "Test", 0xFFFFFF).isValid());
        assertFalse(new GTMaterial(0, "Test", "Test", 0xFFFFFF).isValid());
    }

    @Test
    void linksResolveOnReadSoALateRegistrationIsVisible() {
        GTMaterial alias = new GTMaterial(2, "Alias", "Alias", 0xFFFFFF);
        GTMaterial target = new GTMaterial(3, "Target", "Target", 0xFFFFFF);
        GTMaterial material = new GTMaterial(1, "Test", "Test", 0xFFFFFF);
        alias.setTooltipChemical("Alias");
        target.setTooltipChemical("Target");

        material.setPulver(alias, GTValues.U)
                .setCrushing(alias, GTValues.U)
                .setSmelting(alias, GTValues.U)
                .setBurning(alias, GTValues.U)
                .ores(alias);
        material.setComposition(1, new MaterialComponent(alias, GTValues.U));

        alias.setRegistration(target);

        assertSame(target, material.getTargetPulverMaterial());
        assertSame(target, material.getTargetCrushingMaterial());
        assertSame(target, material.getTargetSmeltingMaterial());
        assertSame(target, material.getTargetBurningMaterial());
        assertSame(target, material.getByProducts().get(0));
        assertSame(target, material.getCompositionComponents().get(0).material());
        assertEquals("Target", alias.getResolvedTooltipChemical());
        assertEquals("Alias", alias.getTooltipChemical());
    }

    @Test
    void registrationCyclesAreRejected() {
        GTMaterial first = new GTMaterial(1, "First", "First", 0xFFFFFF);
        GTMaterial second = new GTMaterial(2, "Second", "Second", 0xFFFFFF);
        first.setRegistration(second);
        assertThrows(IllegalArgumentException.class, () -> second.setRegistration(first));
    }

    @Test
    void statsRejectInvalidValues() {
        GTMaterial material = new GTMaterial(1, "Test", "Test", 0xFFFFFF);
        assertThrows(IllegalArgumentException.class, () -> material.setStats(-1, 1000, 1.0F));
        assertThrows(IllegalArgumentException.class, () -> material.setStats(300, -1, 1.0F));
        assertThrows(IllegalArgumentException.class, () -> material.setStats(300, 1000, -1.0F));
        assertThrows(IllegalArgumentException.class, () -> material.setStats(300, 1000, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> material.setStats(300, 1000, Float.POSITIVE_INFINITY));
        assertEquals(0, material.setStats(0, 0, 0).getMeltingPoint());
    }

    @Test
    void displayNameFollowsLiteralLocalNamesOnly() {
        GTMaterial material = new GTMaterial(1, "SulfuricAcid", "Sulfuric Acid", 0xFFFFFF);

        material.setLocalName("SulphuricAcid");
        assertEquals("SulphuricAcid", material.getLocalName());
        assertEquals("Sulfuric Acid", material.getDisplayNameFallback());

        material.setLocalName("Sulphuric Acid");
        assertEquals("Sulphuric Acid", material.getDisplayNameFallback());

        material.setDisplayName("Explicit");
        material.setLocalName("Another Literal Name");
        assertEquals("Explicit", material.getDisplayNameFallback());
    }

    @Test
    void frozenMaterialsRejectMutation() {
        GTMaterial material = new GTMaterial(1, "Test", "Test", 0xFFFFFF).freeze();
        assertThrows(IllegalStateException.class, () -> material.setStats(1, 2, 3));
        assertThrows(IllegalStateException.class, () -> material.put(MaterialProperty.HIDDEN));
        assertThrows(IllegalStateException.class, () -> material.setRegistration(material));
    }

    @Test
    void sentinelsAreFrozen() {
        assertThrows(IllegalStateException.class, () -> MaterialSentinels.INVALID.setStats(1, 2, 3));
        assertThrows(IllegalStateException.class, () -> MaterialSentinels.EMPTY.put(MaterialProperty.HIDDEN));
    }
}
