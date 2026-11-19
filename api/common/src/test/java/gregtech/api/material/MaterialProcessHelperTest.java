/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.FuelRule;
import gregtech.api.addon.material.MaterialInfoTool;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialTarget;
import gregtech.api.addon.material.OreProcessingRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class MaterialProcessHelperTest {
    @Test
    void helpersReadTheRegisteredAuxiliaryData() {
        var iron = new GTMaterial(1, "Iron", "Fe", 0xFFFFFF);
        var ash = new GTMaterial(2, "Ashes", "Ashes", 0xFFFFFF);
        MaterialInfoTestHost.reset(iron, ash);
        var ironKey = new MaterialKey("Iron");
        var ashKey = new MaterialKey("Ashes");
        var ironUnit = new MaterialTarget(ironKey, GTValues.U);
        MaterialInfoTestHost.setProcess(iron, MaterialProcessKeys.SMELTING, ironUnit);
        MaterialInfoTestHost.setProcess(iron, MaterialProcessKeys.CRUSHING, new MaterialTarget(ironKey, GTValues.U2));
        MaterialInfoTestHost.setProcess(iron, MaterialProcessKeys.PULVERIZING, ironUnit);
        MaterialInfoTestHost.setProcess(iron, MaterialProcessKeys.FUEL,
                new FuelRule(100, java.util.Optional.of(new MaterialTarget(ashKey, GTValues.U9))));
        MaterialInfoTestHost.setProcess(iron, MaterialProcessKeys.ORE_PROCESSING,
                new OreProcessingRule(java.util.List.of(ashKey), 2, 3));
        MaterialInfoTestHost.setTool(ironKey, new MaterialToolProfile(3, 8F, 128, 2));

        assertEquals(
                new MaterialTarget(new MaterialKey("Iron"), GTValues.U),
                SmeltingMaterialHelper.target(iron).orElseThrow());
        assertEquals(
                new FuelRule(100, java.util.Optional.of(new MaterialTarget(new MaterialKey("Ashes"), GTValues.U9))),
                FuelMaterialHelper.rule(iron).orElseThrow());
        assertEquals(new MaterialToolProfile(3, 8F, 128, 2), MaterialToolHelper.profile(iron));
        assertSame(iron, iron.getTargetSmeltingMaterial());
        assertEquals(GTValues.U, iron.getTargetSmeltingAmount());
        assertSame(iron, iron.getTargetCrushingMaterial());
        assertEquals(GTValues.U2, iron.getTargetCrushingAmount());
        assertSame(iron, iron.getTargetPulverMaterial());
        assertEquals(GTValues.U, iron.getTargetPulverAmount());
        assertEquals(java.util.List.of(ash), iron.getByProducts());
        assertEquals(2, iron.getOreMultiplier());
        assertEquals(3, iron.getOreProcessingMultiplier());
        assertEquals(3, iron.getToolTypes());
        assertEquals(8F, iron.getToolSpeed());
        assertEquals(128, iron.getToolDurability());
        assertEquals(2, iron.getToolQuality());
    }

    @Test
    void clearedSmeltingTargetDoesNotRestoreTheDefaultUnit() {
        var iron = new GTMaterial(1, "Iron", "Fe", 0xFFFFFF);
        MaterialInfoTestHost.reset(iron);

        var ironKey = new MaterialKey("Iron");
        var first = new MaterialTarget(ironKey, GTValues.U);
        var replacement = new MaterialTarget(ironKey, GTValues.U2);
        MaterialInfoTestHost.setProcess(iron, MaterialProcessKeys.SMELTING, first);
        assertEquals(first, SmeltingMaterialHelper.target(iron).orElseThrow());
        MaterialInfoTestHost.setProcess(iron, MaterialProcessKeys.SMELTING, replacement);
        assertEquals(replacement, SmeltingMaterialHelper.target(iron).orElseThrow());
        MaterialInfoTool.updateProcess(ironKey, MaterialProcessKeys.SMELTING, java.util.Optional.empty());

        assertTrue(SmeltingMaterialHelper.target(iron).isEmpty());
        assertEquals(0L, iron.getTargetSmeltingAmount());
    }
}
