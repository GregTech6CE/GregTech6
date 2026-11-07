/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import static org.junit.jupiter.api.Assertions.*;

import gregtech.api.addon.material.FuelRule;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialTarget;
import gregtech.api.addon.material.OreProcessingRule;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
class MaterialProcessHelperTest {
    @Test
    void helpersMirrorLegacyMaterialFieldsDuringMigration() {
        var iron = new GTMaterial(1, "Iron", "Fe", 0xFFFFFF);
        var ash = new GTMaterial(2, "Ashes", "Ashes", 0xFFFFFF);
        iron.setSmelting(null, GTValues.U)
                .setCrushing(iron, GTValues.U2)
                .setPulver(iron, GTValues.U)
                .setFurnaceBurnTime(100)
                .setBurning(ash, GTValues.U9)
                .ores(ash)
                .setOreMultiplier(2)
                .setOreProcessingMultiplier(3)
                .setToolStats(3, 8F, 128, 2);

        assertEquals(
                new MaterialTarget(new MaterialKey("Iron"), GTValues.U),
                SmeltingMaterialHelper.target(iron).orElseThrow());
        assertEquals(
                new MaterialTarget(new MaterialKey("Iron"), GTValues.U2),
                CrushingMaterialHelper.target(iron).orElseThrow());
        assertEquals(
                new MaterialTarget(new MaterialKey("Iron"), GTValues.U),
                PulverizationMaterialHelper.target(iron).orElseThrow());
        assertEquals(
                new FuelRule(100, java.util.Optional.of(new MaterialTarget(new MaterialKey("Ashes"), GTValues.U9))),
                FuelMaterialHelper.rule(iron).orElseThrow());
        assertEquals(
                new OreProcessingRule(java.util.List.of(new MaterialKey("Ashes")), 2, 3),
                OreProcessingMaterialHelper.rule(iron));
        assertEquals(new MaterialToolProfile(3, 8F, 128, 2), MaterialToolHelper.profile(iron));
        assertSame(iron, iron.getTargetSmeltingMaterial());
        assertEquals(GTValues.U, iron.getTargetSmeltingAmount());
        assertEquals(GTValues.U2, iron.getTargetCrushingAmount());
        assertEquals(GTValues.U, iron.getTargetPulverAmount());
        assertEquals(100, iron.getFurnaceBurnTime());
        assertEquals(3, iron.getToolTypes());
        assertEquals(8F, iron.getToolSpeed());
        assertEquals(128, iron.getToolDurability());
        assertEquals(2, iron.getToolQuality());
    }
}
