package gregtech.content.recipe;

//? if >=1.21.1 {
/*import java.util.Map;
import gregtech.content.data.OriginalCraftingJson;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import gregtech.GregTech;
//?}
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.tool.GTToolType;
import gregtech.content.plant.BedrockFlowers;
import gregtech.content.data.MaterialPrefix;

//? if >=1.21.1 {
//?} else {
import gregtech.content.recipe.RuntimeRecipeLifecycle;
//?}
import gregtech.content.recipe.ToolShapedRecipe;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTToolItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

//? if >=1.21.1 {
//?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}

import java.util.ArrayList;
import java.util.List;

/** GT6 BlockFlowersA/B shapeless dyes and B-family hand-cut wooden sticks. */
public final class BedrockFlowerCraftingRecipeInjector {
    private static final String[] A_DUST = {
            "Yellow", "Yellow", "Magenta", "Yellow", "Pink", "White", "LightBlue", "Brown"
    };
    private static final String[] B_DUST = {"Yellow", "Pink", "Yellow", "Purple"};

    private BedrockFlowerCraftingRecipeInjector() {}

//? if >=1.21.1 {
    /*public static void add(Map<ResourceLocation, byte[]> recipes) {
*///?} else {
    public static void apply(RecipeManager manager, RegistryAccess access) {
        List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes());
//?}
        int added = 0;
        for (BedrockFlowers.Flower flower : BedrockFlowers.ALL) {
            ItemStack input = flower(flower);
            ItemStack output = shapelessOutput(flower);
            if (!output.isEmpty()) {
//? if >=1.21.1 {
                /*OriginalCraftingJson.shapeless(recipes, id(flower, "hand"), "gt.bedrock_flowers", CraftingBookCategory.MISC, output, List.of(Ingredient.of(input.getItem())));
*///?} else {
                recipes.add(new ShapelessRecipe(id(flower, "hand"), "gt.bedrock_flowers",
                        CraftingBookCategory.MISC, output,
                        NonNullList.of(Ingredient.EMPTY, Ingredient.of(input.getItem()))));
//?}
                added++;
            }
            if (flower.family() == 'B' && (flower.meta() == 0 || flower.meta() == 1 || flower.meta() == 6)) {
                ItemStack twoSticks = stick(flower, 2);
                for (GTToolType tool : new GTToolType[]{GTToolType.SAW, GTToolType.KNIFE}) {
                    var toolItem = GTToolItems.get(tool);
                    if (toolItem == null) throw new IllegalStateException("Missing flower crafting tool " + tool);
                    var ingredients = NonNullList.withSize(2, Ingredient.EMPTY);
                    ingredients.set(0, Ingredient.of(toolItem));
                    ingredients.set(1, Ingredient.of(input.getItem()));
//? if >=1.21.1 {
                    /*OriginalCraftingJson.shaped(recipes, id(flower, tool.id()), "gt.bedrock_flowers", CraftingBookCategory.MISC, twoSticks.copy(), new String[]{"T", "F"}, Map.of('T', ingredients.get(0), 'F', ingredients.get(1)), false);
*///?} else {
                    ShapedRecipe base = new ShapedRecipe(id(flower, tool.id()), "gt.bedrock_flowers",
                            CraftingBookCategory.MISC, 1, 2, ingredients, twoSticks.copy());
                    recipes.add(new ToolShapedRecipe(base, false));
//?}
                    added++;
                }
            }
        }
//? if >=1.21.1 {

        /*com.mojang.logging.LogUtils.getLogger().info("Registered {} GT6 bedrock flower crafting recipes", added);
*///?} else {
        RuntimeRecipeLifecycle.replaceGenerated(manager, recipes);
        GregTech.LOGGER.info("Registered {} GT6 bedrock flower crafting recipes", added);
//?}
    }

    private static ItemStack shapelessOutput(BedrockFlowers.Flower flower) {
        if (flower.family() == 'A' && flower.meta() < A_DUST.length)
            return dust(A_DUST[flower.meta()]);
        if (flower.family() == 'B' && (flower.meta() == 0 || flower.meta() == 1 || flower.meta() == 6))
            return stick(flower, 1);
        if (flower.family() == 'B' && flower.meta() >= 2 && flower.meta() <= 5)
            return dust(B_DUST[flower.meta() - 2]);
        return ItemStack.EMPTY;
    }

    private static ItemStack stick(BedrockFlowers.Flower flower, int count) {
        String material = flower.meta() == 6 ? "Palm" : "Acacia";
        ItemStack output = GTItems.getStack(MaterialPrefix.stick, GTMaterialRegistry.get(material), count);
        if (output.isEmpty()) throw new IllegalStateException("Missing GT6 flower stick form " + material);
        return output;
    }

    private static ItemStack dust(String name) {
        ItemStack output = GTItems.getStack(MaterialPrefix.dust, GTMaterialRegistry.get(name));
        if (output.isEmpty()) throw new IllegalStateException("Missing GT6 flower dust form " + name);
        return output;
    }

    private static ItemStack flower(BedrockFlowers.Flower entry) {
//? if >=1.21.1 {
        /*var item = BuiltInRegistries.ITEM.get(
*///?} else {
        var item = ForgeRegistries.ITEMS.getValue(
//?}
                ResourceLocation.fromNamespaceAndPath("gt6ce", entry.id()));
        if (item == null) throw new IllegalStateException("Missing GT6 flower " + entry.id());
        return new ItemStack(item);
    }

    private static ResourceLocation id(BedrockFlowers.Flower flower, String process) {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "bedrock_flowers/" + flower.id() + "/" + process);
    }
}
