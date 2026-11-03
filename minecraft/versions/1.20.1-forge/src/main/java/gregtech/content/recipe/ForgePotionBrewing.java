package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.registries.ForgeRegistries;

/** Forge-side registration and probing of the vanilla brewing table. */
public final class ForgePotionBrewing {
    private static final Item[] CONTAINERS = {Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION};

    private ForgePotionBrewing() {}

    public static void registerExtraIngredients() {
        for (var route : PotionBrewingRoutes.all()) {
            Potion base = ForgeRegistries.POTIONS.getValue(new ResourceLocation("minecraft", route.basePotion()));
            Potion result = ForgeRegistries.POTIONS.getValue(new ResourceLocation("minecraft", route.resultPotion()));
            if (base == null || result == null || route.ingredient().isEmpty()) continue;
            BrewingRecipeRegistry.addRecipe(new IBrewingRecipe() {
                @Override public boolean isInput(ItemStack input) {
                    return input.getItem() instanceof PotionItem && PotionUtils.getPotion(input) == base;
                }

                @Override public boolean isIngredient(ItemStack ingredient) {
                    return ItemStack.isSameItemSameTags(ingredient, route.ingredient());
                }

                @Override public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
                    if (!isInput(input) || !isIngredient(ingredient)) return ItemStack.EMPTY;
                    return PotionUtils.setPotion(new ItemStack(input.getItem()), result);
                }
            });
        }
    }

    /** Reads the finished table; call only after every mod has registered its brewing recipes. */
    public static void probe() {
        var ingredients = new ArrayList<ItemStack>();
        for (Item item : ForgeRegistries.ITEMS) {
            if (item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item);
            if (stack.getItem() instanceof PotionItem || !BrewingRecipeRegistry.isValidIngredient(stack)) continue;
            ingredients.add(stack);
        }
        var rows = new ArrayList<PotionBrewingRecipes.Row>();
        for (Potion potion : ForgeRegistries.POTIONS) {
            for (Item container : CONTAINERS) {
                ItemStack input = PotionUtils.setPotion(new ItemStack(container), potion);
                for (ItemStack ingredient : ingredients) {
                    ItemStack output = BrewingRecipeRegistry.getOutput(input, ingredient);
                    if (output.isEmpty() || ItemStack.isSameItemSameTags(output, input)) continue;
                    rows.add(new PotionBrewingRecipes.Row(input, ingredient.copyWithCount(1), output));
                }
            }
        }
        PotionBrewingRecipes.rebuild(rows);
    }
}
