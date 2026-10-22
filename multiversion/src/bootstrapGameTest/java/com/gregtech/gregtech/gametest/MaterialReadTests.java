package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.addon.platform.AddonItems;
import com.gregtech.gregtech.api.material.GTValues;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Material table reads and the item-to-unit lookup. */
@GameTestHolder("gregtech_card04_materials")
@PrefixGameTestTemplate(false)
public final class MaterialReadTests {
    private MaterialReadTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void ironIngotIsOneIronUnitAndDirtIsEmpty(GameTestHelper helper) {
        var iron = AddonItems.composition(new ItemStack(Items.IRON_INGOT));
        helper.assertTrue("ingot".equals(iron.prefix()), "iron ingot prefix");
        helper.assertTrue(iron.units().size() == 1
                        && "Iron".equals(iron.units().get(0).material())
                        && iron.units().get(0).amount() == GTValues.U,
                "iron ingot is one iron unit");
        var dirt = AddonItems.composition(new ItemStack(Items.DIRT));
        helper.assertTrue(dirt.prefix().isEmpty() && dirt.units().size() == 1
                        && "Soil".equals(dirt.units().get(0).material())
                        && dirt.units().get(0).amount() == GTValues.U,
                "dirt is one soil unit");
        helper.assertTrue(!AddonItems.composition(ItemStack.EMPTY).present(), "an empty stack has no material");
        helper.succeed();
    }
}
