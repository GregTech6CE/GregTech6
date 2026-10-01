package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Static axial recipes must preserve GT6's exact-tier rechargeable battery groups. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class AxialBatteryRecipeTests {
    private static ItemStack item(String id) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(GregTech.id(id)));
    }

    @GameTest(template = "test_empty")
    public static void allAxialRecipesAcceptEveryChemistryAtOnlyTheOriginalTier(GameTestHelper helper) {
        String[] recipes = {"large_dynamo_main", "large_dynamo_titanium", "large_dynamo_tungstensteel",
                "large_dynamo_adamantium", "large_gas_turbine_main", "large_gas_turbine_trinitanium",
                "large_gas_turbine_graphene", "large_gas_turbine_vibramantium"};
        String[] tiers = {"ulv", "lv", "mv", "hv", "ev"};
        String[] chemistries = {"lead_acid", "alkaline", "nickel_cadmium", "lithium_cobalt", "lithium_manganese"};
        for (int index = 0; index < recipes.length; index++) {
            int expectedTier = index < 4 ? 1 : index - 3;
            var recipe = helper.getLevel().getRecipeManager().byKey(GregTech.id("axial/" + recipes[index])).orElseThrow();
            var battery = recipe.getIngredients().get(index < 4 ? 7 : 3);
            for (int tier = 0; tier < tiers.length; tier++) {
                for (String chemistry : chemistries) {
                    var stack = item("battery_" + chemistry + "_" + tiers[tier]);
                    helper.assertTrue(!stack.isEmpty(), "canonical battery exists");
                    helper.assertTrue(battery.test(stack) == (tier == expectedTier),
                            recipes[index] + " accepts exact voltage only: " + chemistry + " " + tiers[tier]);
                }
            }
            helper.assertTrue(battery.test(item("battery_" + tiers[expectedTier])), "legacy battery remains usable");
            helper.assertTrue(battery.test(item("battery_eu_" + (8L << (expectedTier * 2)))), "retained placed battery remains usable");
            helper.assertTrue(!battery.test(item("lead_acid_cell_filled")), "chemical cell is not a complete battery");
            var circuit = recipe.getIngredients().get(index < 4 ? 3 : 5);
            helper.assertTrue(circuit.test(item("circuit_ultimate")) && !circuit.test(item("circuit_master")),
                    "original circuit tier six requirement is retained");
        }
        helper.succeed();
    }
}
