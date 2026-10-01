package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.machine.RotationEngineSpec;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTAxles;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTPumps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Original 13 material tiers and their survival assembly routes. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class RotationEngineRegistrationTests {
    @GameTest(template = "test_empty")
    public static void allOriginalRotationEngineTiersAreRegistered(GameTestHelper helper) {
        String[] materials = {"wood", "bronze", "brass", "arsenic_copper", "arsenic_bronze",
                "steel", "titanium", "tungsten_steel", "iridium", "iritanium", "trinitanium",
                "trinaquadalloy", "adamantium"};
        long[] input = {8, 32, 32, 32, 32, 128, 512, 2048, 8192, 32768, 131072, 524288, 2097152};
        helper.assertTrue(MachineRegistry.rotationEngines().size() == 13,
                "all 13 original rotational engine materials are registered");
        for (int i = 0; i < materials.length; i++) {
            String id = "engine_rotation_" + materials[i];
            var registered = MachineRegistry.rotationEngines().stream()
                    .filter(entry -> entry.getId().equals(GregTech.id(id))).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing original engine " + id));
            RotationEngineSpec spec = registered.get().engineSpec(RotationEngineSpec.class);
            helper.assertTrue(spec.inputRate() == input[i] && spec.outputRate() == input[i] / 2
                            && spec.hardness() == 6.0f && spec.blastResistance() == 6.0f,
                    "original input/output and physical rating: " + id);
            helper.assertTrue(registered.get().asItem().getDefaultInstance().getMaxStackSize() == 16,
                    "original engine stack limit: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void everyRotationEngineAndPumpHasResolvedCraftingRecipe(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (var entry : MachineRegistry.rotationEngines()) {
            String id = "engine_rotation/" + entry.getId().getPath();
            checkRecipe(helper, manager, id, entry.get().asItem());
        }
        helper.assertTrue(GTPumps.all().size() == 4, "four original rotational pump levels");
        for (var entry : GTPumps.all()) {
            String id = "rotational_pump/" + entry.getId().getPath();
            checkRecipe(helper, manager, id, entry.get().asItem());
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void bronzeEngineCraftConsumesLubricantAndWearsWrench(GameTestHelper helper) {
        var id = GregTech.id("hand/engine_rotation/engine_rotation_bronze");
        var recipe = helper.getLevel().getRecipeManager().byKey(id)
                .orElseThrow(() -> new IllegalStateException("Missing " + id));
        var menu = new AbstractContainerMenu(null, 0) {
            @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 3, 3);
        for (int i = 0; i < 9; i++) {
            var ingredient = recipe.getIngredients().get(i);
            if (ingredient.isEmpty()) continue;
            var example = ingredient.getItems()[0].copy();
            if (example.getItem() instanceof com.gregtech.gregtech.item.GTToolItem)
                example = com.gregtech.gregtech.item.GTToolItem.create(
                        com.gregtech.gregtech.api.tool.GTToolHelper.getType(example),
                        com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Steel"),
                        com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
            grid.setItem(i, example);
        }
        var bottle = ForgeRegistries.ITEMS.getValue(GregTech.id("lubricant_bottle"));
        helper.assertTrue(bottle instanceof com.gregtech.gregtech.item.BottleItem,
                "engine lubricant is an actual consumable GT6 bottle");
        grid.setItem(5, new ItemStack(bottle));
        var smallGear = GTItems.getStack(MaterialPrefix.gearGtSmall, Materials.Bronze, 1);
        var gear = GTItems.getStack(MaterialPrefix.gearGt, Materials.Bronze, 1);
        var casing = GTBlocks.getStack(BlockMaterialPrefix.casingMachine, Materials.Bronze);
        var mediumAxle = GTAxles.all().stream().map(holder -> holder.get())
                .filter(block -> block.spec().id().equals("axle_bronze_2"))
                .findFirst().orElseThrow();
        helper.assertTrue(!smallGear.isEmpty() && !gear.isEmpty() && !casing.isEmpty()
                        && grid.getItem(0).is(smallGear.getItem()) && grid.getItem(2).is(smallGear.getItem())
                        && grid.getItem(1).is(mediumAxle.asItem()) && grid.getItem(7).is(mediumAxle.asItem())
                        && grid.getItem(4).is(casing.getItem())
                        && grid.getItem(6).is(gear.getItem()) && grid.getItem(8).is(gear.getItem()),
                "bronze engine uses the original small-gear/axle/casing/large-gear layout");
        var wrench = grid.getItem(3);
        helper.assertTrue(wrench.getItem() instanceof com.gregtech.gregtech.item.GTToolItem,
                "engine recipe requires the reusable GT wrench");
        helper.assertTrue(recipe instanceof net.minecraft.world.item.crafting.CraftingRecipe crafted
                        && crafted.matches(grid, helper.getLevel()),
                "bronze engine assembles in a real 3x3 crafting grid");
        var crafted = (net.minecraft.world.item.crafting.CraftingRecipe) recipe;
        var remainders = crafted.getRemainingItems(grid);
        helper.assertTrue(remainders.get(5).is(com.gregtech.gregtech.item.BottleItem.emptyBottle().getItem())
                        && grid.getItem(5).is(bottle),
                "craft consumes 250 mB lubricant and returns the empty bottle without mutating input");
        helper.assertTrue(!remainders.get(3).isEmpty()
                        && remainders.get(3).getDamageValue() > wrench.getDamageValue(),
                "GT wrench is returned with crafting wear");
        grid.setItem(5, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
        helper.assertTrue(!crafted.matches(grid, helper.getLevel()),
                "an empty bottle cannot replace the lubricating fluid");
        helper.succeed();
    }

    private static void checkRecipe(GameTestHelper helper, net.minecraft.world.item.crafting.RecipeManager manager,
                                    String path, net.minecraft.world.item.Item output) {
        var id = GregTech.id("hand/" + path);
        var recipe = manager.byKey(id).orElseThrow(() -> new IllegalStateException("Missing " + id));
        helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).is(output),
                "recipe crafts its registered machine: " + id);
        for (var ingredient : recipe.getIngredients()) if (!ingredient.isEmpty())
            helper.assertTrue(ingredient.getItems().length > 0, "resolved survival ingredient: " + id);
    }
}
