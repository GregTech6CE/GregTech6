package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.content.recipe.TreatedWoodRecipes;
import com.gregtech.gregtech.content.recipe.VanillaWoodProcessingRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.loaders.Loader_WoodCraftingRecipes;
import com.gregtech.gregtech.recipe.ToolShapedRecipe;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;

/** Proves the source plank, oil, treated plate, bolt and both gears form one real recipe chain. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class TreatedWoodSurvivalTests {
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void treatedWoodPlateIsThePlaceablePlankAndDropsItself(GameTestHelper h) {
        ItemStack plate = form(MaterialPrefix.plate, "WoodTreated");
        h.assertTrue(plate.getItem() instanceof BlockItem, "GT6 treated wood plate resolves to a block item");
        Block block = ((BlockItem) plate.getItem()).getBlock();
        h.assertTrue(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block)
                        .equals(ResourceLocation.fromNamespaceAndPath("gregtech", "planks_treated")),
                "Treated wood plate is the registered plank block");
        MaterialEquivalence.Form materialForm = MaterialEquivalence.form(plate);
        h.assertTrue(materialForm != null && materialForm.prefix() == MaterialPrefix.plate
                        && materialForm.material() == GTMaterialRegistry.get("WoodTreated"),
                "Placeable plank retains its GT6 plate material identity");
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, block);
        BlockPos absolute = h.absolutePos(pos);
        h.assertTrue(h.getLevel().getBlockState(absolute).getBlock() == block,
                "Treated plank can be placed in the world");
        var drops = Block.getDrops(h.getLevel().getBlockState(absolute), h.getLevel(), absolute, null);
        h.assertTrue(drops.size() == 1 && ItemStack.isSameItem(drops.get(0), plate)
                        && drops.get(0).getCount() == 1,
                "Mining the treated plank returns the same placeable material form");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void gt6TreatedWoodBathConsumesOilAndProducesRecipeIngredients(GameTestHelper h) {
        var entries = TreatedWoodRecipes.INSTANCE.entries();
        h.assertTrue(entries.size() == TreatedWoodRecipes.expectedRows(),
                "Seven GT6 oils treat all mapped planks and wood part forms");
        for (var entry : entries) {
            Recipe recipe = entry.recipe();
            h.assertTrue(entry.map() == MachineRecipeMaps.Bath, "Treated wood uses the Bath: " + entry.id());
            h.assertTrue(recipe.mEUt == 0 && recipe.mDuration == 144
                            && recipe.mFluidInputs.length == 1 && recipe.mFluidInputs[0].getAmount() == 100,
                    "GT6 Bath cost is 100 mB, 144 ticks, 0 GU: " + entry.id());
            h.assertTrue(MachineRecipeMaps.Bath.findRecipe(Arrays.asList(recipe.mInputs),
                    Arrays.asList(recipe.mFluidInputs), false,
                    MachineRecipeMaps.Bath.mInputItemsCount, MachineRecipeMaps.Bath.mOutputItemsCount) == recipe,
                    "Bath can find " + entry.id());
        }
        Recipe plank = row("treated/planks/vanilla/oak/Oil_Creosote");
        ItemStack treatedPlate = form(MaterialPrefix.plate, "WoodTreated");
        h.assertTrue(plank.mInputs[0].is(Items.OAK_PLANKS) && ItemStack.isSameItem(plank.mOutputs[0], treatedPlate),
                "Vanilla oak plank becomes the exact plate used by the wooden engine");
        h.assertTrue(RecipeInputs.consume(plank, List.of(new ItemStack(Items.OAK_PLANKS)),
                List.of(plank.mFluidInputs[0].copy()), 1) != null, "Creosote bath can run");
        var shortOil = plank.mFluidInputs[0].copy();
        shortOil.shrink(1);
        h.assertTrue(RecipeInputs.consume(plank, List.of(new ItemStack(Items.OAK_PLANKS)),
                List.of(shortOil), 1) == null, "99 mB must not treat a plank");

        Recipe bolt = row("treated/bolt/wood/Oil_Creosote");
        h.assertTrue(ItemStack.isSameItem(bolt.mOutputs[0], form(MaterialPrefix.bolt, "WoodTreated")),
                "Hand-crafted Wood bolt can become the large gear's treated bolt");
        h.assertTrue(VanillaWoodProcessingRecipes.INSTANCE.entries().stream()
                .anyMatch(e -> e.id().equals("coke/oak_log") && e.recipe().mFluidOutputs.length == 1
                        && e.recipe().mFluidOutputs[0].getAmount() >= 100),
                "Existing Coke Oven recipes supply the required creosote");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void originalWoodGearPatternsAreRegisteredWithTheirRealInputs(GameTestHelper h) {
        var manager = h.getLevel().getRecipeManager();
        h.assertTrue(Loader_WoodCraftingRecipes.treatedRegisteredIds().size() == 3,
                "Wood bolts and both treated gears use the three original hand patterns");
        ToolShapedRecipe woodBolt = crafting(manager, "wood_bolt");
        ToolShapedRecipe smallGear = crafting(manager, "small_gear");
        ToolShapedRecipe gear = crafting(manager, "gear");
        h.assertTrue(woodBolt.getWidth() == 2 && woodBolt.getHeight() == 2
                        && woodBolt.getResultItem(RegistryAccess.EMPTY).getCount() == 2
                        && ItemStack.isSameItem(woodBolt.getResultItem(RegistryAccess.EMPTY),
                        form(MaterialPrefix.bolt, "Wood")),
                "GT6 saw + vanilla stick yields two Wood bolts");
        h.assertTrue(smallGear.getWidth() == 2 && smallGear.getHeight() == 2
                        && smallGear.getIngredients().get(0).test(form(MaterialPrefix.plate, "WoodTreated"))
                        && ItemStack.isSameItem(smallGear.getResultItem(RegistryAccess.EMPTY),
                        form(MaterialPrefix.gearGtSmall, "WoodTreated")),
                "Treated plank + saw yields the small gear");
        h.assertTrue(gear.getWidth() == 3 && gear.getHeight() == 3
                        && gear.getIngredients().get(0).test(form(MaterialPrefix.bolt, "WoodTreated"))
                        && gear.getIngredients().get(1).test(form(MaterialPrefix.plate, "WoodTreated"))
                        && ItemStack.isSameItem(gear.getResultItem(RegistryAccess.EMPTY),
                        form(MaterialPrefix.gearGt, "WoodTreated")),
                "Treated bolts and planks yield the large gear");
        h.succeed();
    }

    private static Recipe row(String id) {
        return TreatedWoodRecipes.INSTANCE.entries().stream()
                .filter(e -> e.id().equals(id)).findFirst().orElseThrow().recipe();
    }

    private static ItemStack form(MaterialPrefix prefix, String material) {
        return GTItems.getStack(prefix, GTMaterialRegistry.get(material), 1);
    }

    private static ToolShapedRecipe crafting(net.minecraft.world.item.crafting.RecipeManager manager, String path) {
        var recipe = manager.byKey(ResourceLocation.fromNamespaceAndPath("gregtech", "wood/treated/" + path))
                .orElseThrow(() -> new IllegalStateException("Missing treated wood crafting row " + path));
        if (recipe instanceof ToolShapedRecipe shaped) return shaped;
        throw new IllegalStateException("Wrong treated wood recipe type " + path);
    }
}
