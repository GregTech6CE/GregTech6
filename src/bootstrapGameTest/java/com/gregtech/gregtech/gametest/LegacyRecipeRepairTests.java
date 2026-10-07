package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.data.MultiblockCraftingRecipes;
import com.gregtech.gregtech.loaders.Loader_TrackRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LegacyRecipeRepairTests {
    private LegacyRecipeRepairTests() {}

    @GameTest(template = "test_empty")
    public static void multiblockTableTargetsOnlyRegisteredBlocks(GameTestHelper h) {
        for (var entry : MultiblockCraftingRecipes.ENTRIES) {
            var id = ResourceLocation.fromNamespaceAndPath("gregtech", entry.blockId());
            h.assertTrue(ForgeRegistries.BLOCKS.containsKey(id)
                    && ForgeRegistries.BLOCKS.getValue(id) != Blocks.AIR,
                    "multiblock recipe target is registered: " + id);
        }
        for (String oldId : List.of("distillationtower_stainless_steel",
                "cryodistillationtower_stainless_steel", "fusionreactor_stainless_steel")) {
            h.assertTrue(MultiblockCraftingRecipes.find(oldId) == null,
                    "obsolete single-block controller has no recipe target: " + oldId);
        }
        for (String mainId : List.of("distillation_tower_main", "cryo_distillation_main",
                "fusion_reactor_main")) {
            h.assertTrue(MultiblockCraftingRecipes.find(mainId) != null,
                    "the registered multiblock controller retains its original recipe: " + mainId);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void railReplacementKeepsOtherRecipeKinds(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        var ingredients = NonNullList.withSize(1, Ingredient.of(Items.STICK));
        var shaped = new ShapedRecipe(ResourceLocation.fromNamespaceAndPath("other_mod", "powered_rail"),
                "", CraftingBookCategory.MISC, 1, 1, ingredients, new ItemStack(Items.POWERED_RAIL));
        var shapeless = new ShapelessRecipe(ResourceLocation.fromNamespaceAndPath("minecraft", "powered_rail"),
                "", CraftingBookCategory.MISC, new ItemStack(Items.POWERED_RAIL), ingredients);
        var smelting = new SmeltingRecipe(ResourceLocation.fromNamespaceAndPath("other_mod", "smelt_rail"),
                "", CookingBookCategory.MISC, Ingredient.of(Items.STICK),
                new ItemStack(Items.POWERED_RAIL), 0, 200);

        h.assertTrue(Loader_TrackRecipes.replacesVanillaRailRecipe(shaped, access),
                "GT6 replaces a conflicting shaped recipe even from another mod");
        h.assertTrue(!Loader_TrackRecipes.replacesVanillaRailRecipe(shapeless, access),
                "GT6 keeps a shapeless data-pack replacement");
        h.assertTrue(!Loader_TrackRecipes.replacesVanillaRailRecipe(smelting, access),
                "GT6 keeps a non-crafting rail recipe");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                "minecraft", "powered_rail")).isEmpty(),
                "Forge's built-in shaped powered rail recipe was removed at server start");
        h.succeed();
    }
}
