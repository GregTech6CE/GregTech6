package gregtech.content.recipe;

import com.mojang.logging.LogUtils;
import gregtech.engine.stack.StackCompat;
import gregtech.mixin.PotionBrewingAccessor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.common.brewing.VanillaBrewingRecipe;
import net.minecraftforge.registries.ForgeRegistries;

/** Forge-side registration and probing of the vanilla brewing table. */
public final class ForgePotionBrewing {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final Item[] FALLBACK_CONTAINERS = {Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION};

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
        var containers = containers();
        var reagents = reagents();
        var rows = new ArrayList<PotionBrewingRecipes.Row>();
        for (Potion potion : ForgeRegistries.POTIONS) {
            for (ItemStack container : containers) {
                ItemStack input = PotionUtils.setPotion(container.copyWithCount(1), potion);
                for (ItemStack reagent : reagents) {
                    addRow(rows, input, reagent, output(input, reagent));
                }
            }
        }
        addCustomRecipes(rows);
        PotionBrewingRecipes.rebuild(rows);
    }

    private static List<ItemStack> containers() {
        var stacks = new ArrayList<ItemStack>();
        for (Ingredient ingredient : PotionBrewingAccessor.gregtech$allowedContainers())
            for (ItemStack stack : ingredient.getItems())
                if (stack.getItem() instanceof PotionItem) stacks.add(stack);
        var distinct = PotionBrewingRecipes.distinct(stacks);
        if (!distinct.isEmpty()) return distinct;
        var fallback = new ArrayList<ItemStack>();
        for (Item container : FALLBACK_CONTAINERS) fallback.add(new ItemStack(container));
        return fallback;
    }

    private static List<ItemStack> reagents() {
        var stacks = new ArrayList<ItemStack>();
        for (PotionBrewing.Mix<Potion> mix : PotionBrewingAccessor.gregtech$potionMixes())
            for (ItemStack stack : mix.ingredient.getItems()) stacks.add(stack);
        for (PotionBrewing.Mix<Item> mix : PotionBrewingAccessor.gregtech$containerMixes())
            for (ItemStack stack : mix.ingredient.getItems()) stacks.add(stack);
        for (IBrewingRecipe recipe : BrewingRecipeRegistry.getRecipes())
            if (recipe instanceof BrewingRecipe brewing)
                for (ItemStack stack : brewing.getIngredient().getItems()) stacks.add(stack);
        for (Item item : ForgeRegistries.ITEMS) {
            if (item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item);
            if (validIngredient(stack)) stacks.add(stack);
        }
        return PotionBrewingRecipes.distinct(stacks);
    }

    private static boolean validIngredient(ItemStack stack) {
        try {
            return BrewingRecipeRegistry.isValidIngredient(stack);
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.warn("[gregtech] Brewing ingredient check failed for {}", stack, failure);
            return false;
        }
    }

    private static ItemStack output(ItemStack input, ItemStack reagent) {
        try {
            return BrewingRecipeRegistry.getOutput(input, reagent);
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.warn("[gregtech] Brewing mix failed for {} + {}", input, reagent, failure);
            return ItemStack.EMPTY;
        }
    }

    private static void addRow(
            List<PotionBrewingRecipes.Row> rows, ItemStack input, ItemStack reagent, ItemStack output) {
        // The ordinary input slot rejects potion items, so a potion reagent cannot be matched.
        if (reagent.getItem() instanceof PotionItem) return;
        if (output.isEmpty() || StackCompat.sameItem(output, input)) return;
        rows.add(new PotionBrewingRecipes.Row(input.copyWithCount(1), reagent.copyWithCount(1), output.copy()));
    }

    private static void addCustomRecipes(List<PotionBrewingRecipes.Row> rows) {
        var unsupported = new HashSet<Class<?>>();
        for (IBrewingRecipe recipe : BrewingRecipeRegistry.getRecipes()) {
            if (recipe instanceof VanillaBrewingRecipe) continue;
            if (!(recipe instanceof BrewingRecipe brewing)) {
                if (unsupported.add(recipe.getClass()))
                    LOGGER.debug(
                            "[gregtech] Cannot enumerate brewing recipe class {}",
                            recipe.getClass().getName());
                continue;
            }
            ItemStack output = brewing.getOutput();
            if (output.isEmpty()) continue;
            for (ItemStack input : brewing.getInput().getItems()) {
                if (!(input.getItem() instanceof PotionItem)) continue;
                for (ItemStack reagent : brewing.getIngredient().getItems()) {
                    addRow(rows, input, reagent, output);
                }
            }
        }
    }
}
