package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;

/** GT6 Loader_Recipes_Vanilla: masonry recycling, grinding and ice extraction. */
public final class VanillaProcessingRecipes {
    private int registered;

    public static int register() {
        VanillaProcessingRecipes recipes = new VanillaProcessingRecipes();
        recipes.masonry();
        recipes.grinding();
        recipes.ice();
        recipes.compression();
        return recipes.registered;
    }

    private void masonry() {
        masonry(Blocks.BRICKS, Items.BRICK, 3, 64, 10000, 9000, 8000, 7000);
        masonry(Blocks.BRICK_STAIRS, Items.BRICK, 2, 64, 10000, 9000, 8000);
        masonry(Blocks.BRICK_SLAB, Items.BRICK, 1, 32, 10000, 8000);
        masonry(Blocks.NETHER_BRICKS, Items.NETHER_BRICK, 3, 64, 10000, 9000, 8000, 7000);
        masonry(Blocks.NETHER_BRICK_STAIRS, Items.NETHER_BRICK, 2, 64, 10000, 9000, 8000);
        masonry(Blocks.NETHER_BRICK_FENCE, Items.NETHER_BRICK, 2, 64, 10000, 9000, 8000);
        masonry(Blocks.NETHER_BRICK_SLAB, Items.NETHER_BRICK, 1, 32, 10000, 8000);
    }

    private void masonry(ItemLike input, ItemLike brick, int hammerCount, long duration, long... chances) {
        item(MachineRecipeMaps.Hammer, input, new ItemStack(brick, hammerCount), 16);
        ItemStack[] outputs = new ItemStack[chances.length];
        for (int i = 0; i < outputs.length; i++) outputs[i] = new ItemStack(brick);
        add(MachineRecipeMaps.Crusher, new ItemStack(input), outputs, chances, null, duration);
    }

    private void grinding() {
        var mortar = MachineRecipeMaps.Mortar;
        item(mortar, Items.BONE, new ItemStack(Items.BONE_MEAL, 2), 32);
        item(mortar, Blocks.GRAVEL, new ItemStack(Items.FLINT), 32);
        item(mortar, Items.FLINT, dust("Flint", 1), 16);
        item(mortar, Items.COAL, dust("Coal", 1), 16);
        item(mortar, Items.CHARCOAL, dust("Charcoal", 1), 16);
        item(mortar, Items.ROTTEN_FLESH, dust("MeatRotten", 1), 16);
        glass(Blocks.GLASS, Blocks.GLASS_PANE);
        for (DyeColor color : DyeColor.values()) {
            glass(BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("minecraft", color.getName() + "_stained_glass")),
                    BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("minecraft", color.getName() + "_stained_glass_pane")));
        }
        var shredder = MachineRecipeMaps.Shredder;
        item(shredder, Items.FLINT, dust("Flint", 1), 16);
        item(shredder, Blocks.GRAVEL, new ItemStack(Blocks.SAND), 16);
        item(shredder, Blocks.COBWEB, new ItemStack(Items.STRING), 16);
        item(shredder, Blocks.COBBLESTONE, dust("Stone", 9), 16);
        item(shredder, Blocks.STONE, dust("Stone", 9), 16);
        item(shredder, Items.BONE, new ItemStack(Items.BONE_MEAL, 4), 32);
    }

    private void glass(ItemLike block, ItemLike pane) {
        item(MachineRecipeMaps.Mortar, block, dust("Glass", 9), 32);
        item(MachineRecipeMaps.Mortar, pane, dust("Glass", 1), 32);
    }

    private void ice() {
        ice(new ItemStack(Blocks.ICE), 1000, 64);
        ice(new ItemStack(Blocks.PACKED_ICE), 2000, 128);
        ice(new ItemStack(Items.SNOWBALL), 250, 64);
        ice(new ItemStack(Blocks.SNOW_BLOCK), 1000, 64);
        ice(dust("Ice", 1), 1000, 64);
        ice(material(MaterialPrefix.dustTiny, "Ice"), 111, 64);
        ice(material(MaterialPrefix.dustSmall, "Ice"), 250, 64);
        ice(material(MaterialPrefix.gemChipped, "Ice"), 250, 64);
        ice(material(MaterialPrefix.gemFlawed, "Ice"), 500, 64);
        ice(material(MaterialPrefix.gem, "Ice"), 1000, 64);
        ice(material(MaterialPrefix.dustTiny, "Snow"), 111, 64);
        ice(material(MaterialPrefix.dustSmall, "Snow"), 250, 64);
        ice(dust("Snow", 1), 1000, 64);
    }

    private void compression() {
        compress(new ItemStack(Blocks.ICE, 2), new ItemStack(Blocks.PACKED_ICE), 32, 64);
        compress(dust("Ice", 1), new ItemStack(Blocks.ICE), 32, 16);
        compress(material(MaterialPrefix.dustSmall, "Ice"), material(MaterialPrefix.gemChipped, "Ice"), 16, 16);
        compress(material(MaterialPrefix.gemChipped, "Ice").copyWithCount(4), new ItemStack(Blocks.ICE), 16, 16);
        compress(material(MaterialPrefix.gemFlawed, "Ice").copyWithCount(2), new ItemStack(Blocks.ICE), 16, 16);
        compress(material(MaterialPrefix.gem, "Ice"), new ItemStack(Blocks.ICE), 16, 16);
        compress(new ItemStack(Blocks.SNOW_BLOCK), new ItemStack(Blocks.ICE), 32, 16);
        compress(new ItemStack(Items.SNOWBALL, 4), new ItemStack(Blocks.SNOW_BLOCK), 32, 16);
        compress(new ItemStack(Items.QUARTZ, 4), new ItemStack(Blocks.QUARTZ_BLOCK), 16, 16);
        compress(new ItemStack(Items.WHEAT, 9), new ItemStack(Blocks.HAY_BLOCK), 16, 16);
        compress(new ItemStack(Blocks.SAND, 4), new ItemStack(Blocks.SANDSTONE), 32, 16);
    }

    private void compress(ItemStack input, ItemStack output, long duration, long usage) {
        if (input.isEmpty() || output.isEmpty()) throw new IllegalStateException("Missing compression material form");
        Recipe recipe = new Recipe(new ItemStack[]{input}, new ItemStack[]{output}, null, null, null, null, duration, usage, 0);
        if (MachineRecipeMaps.Compressor.addRecipe(recipe) != null) registered++;
    }

    private void ice(ItemStack input, int amount, long duration) {
        var fluid = GTFluids.still("Ice");
        if (fluid == null || !fluid.isPresent()) throw new IllegalStateException("GT6 ice fluid missing");
        add(MachineRecipeMaps.Squeezer, input, new ItemStack[0], null,
                new FluidStack[]{new FluidStack(fluid.get(), amount)}, duration);
    }

    private void item(RecipeMap map, ItemLike input, ItemStack output, long duration) {
        if (!output.isEmpty()) add(map, new ItemStack(input), new ItemStack[]{output}, null, null, duration);
    }

    private void add(RecipeMap map, ItemStack input, ItemStack[] outputs, long[] chances, FluidStack[] fluids, long duration) {
        if (input.isEmpty()) return; // Unregistered material forms remain explicit port gaps.
        Recipe recipe = new Recipe(new ItemStack[]{input}, outputs, null, chances, null, fluids, duration, 16, 0);
        if (map.addRecipe(recipe) != null) registered++;
    }

    private static ItemStack material(MaterialPrefix prefix, String name) {
        return GTItems.getStack(prefix, GTMaterialRegistry.get(name).resolve());
    }

    private static ItemStack dust(String name, int count) {
        return GTItems.getStack(MaterialPrefix.dust, GTMaterialRegistry.get(name).resolve(), count);
    }
}
