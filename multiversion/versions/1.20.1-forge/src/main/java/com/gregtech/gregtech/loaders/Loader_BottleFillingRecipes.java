package com.gregtech.gregtech.loaders;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.content.food.GTDrinks;
import com.gregtech.gregtech.item.BottleItem;
import com.gregtech.gregtech.recipe.FiniteBottleFillingRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's bottle filling rows ({@code MultiItemBottles}): one 1000&nbsp;mB container plus N empty bottles
 * gives N filled bottles.
 *
 * <p>GT6 writes them as shapeless recipes against its own container ore dictionary entries —
 * {@code CR.shapeless(make(4, 1100), CR.DEF, new Object[] {OD.container1000milk, OP.bottle.dat(MT.Empty)
 * ×4})} and the matching 3/2/1 rows ({@code MultiItemBottles:143-146}, {@code :151-154}, {@code :164-167},
 * {@code :129-132}, {@code :211-224}, {@code :232-235}, {@code :268-271}, {@code :277-280}). The port has
 * no ore dictionary, so a finite, drainable item fluid handler holding 1000&nbsp;mB of the
 * matching fluid takes its place. The creative-only {@code fluid_item_*} display items are rejected.
 *
 * <p>Not repeated here: GT6's 250&nbsp;mB variant of the juice row ({@code :236}, from
 * {@code OD.container250juice}) — the port registers one container item per fluid, and a second recipe
 * of the very same shape would collide with the 1-bottle row above. The honey/honeydew/royal-jelly rows
 * from GT drops ({@code :169-174}) need GT6's bee-drop items and are left to the bee batch.
 */
public final class Loader_BottleFillingRecipes {

    private static final List<String> REGISTERED = new ArrayList<>();
    private static final List<String> SKIPPED = new ArrayList<>();

    /**
     * One GT6 family: the port's bottle item and the GT6 fluid its 1000 mB container holds. The key is
     * resolved through {@link GTDrinks#fluidForField}, the same lookup the bottles themselves use.
     */
    private static final List<com.gregtech.gregtech.content.food.BottleFillingRows.Family> FAMILIES=com.gregtech.gregtech.content.food.BottleFillingRows.FAMILIES;

    private Loader_BottleFillingRecipes() {}

    /** Recipe ids the last server start added, for tests and reports. */
    public static List<String> registeredIds() { return List.copyOf(REGISTERED); }

    /** Families the port cannot express, with the reason. */
    public static List<String> skipped() { return List.copyOf(SKIPPED); }

    public static void apply(RecipeManager manager, net.minecraft.core.RegistryAccess access) {
        List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes());
        REGISTERED.clear();
        SKIPPED.clear();
        int added = 0;
        for (com.gregtech.gregtech.content.food.BottleFillingRows.Family family : FAMILIES) added += fill(recipes, family);
        added += fillLubricant(recipes);
        if (added > 0) com.gregtech.gregtech.recipe.RuntimeRecipeLifecycle.replaceGenerated(manager, recipes);
        GregTech.LOGGER.info("Registered {} GT6 bottle filling rows for {} families ({} skipped)",
                added, FAMILIES.size(), SKIPPED.size());
        for (String reason : SKIPPED) GregTech.LOGGER.debug("[gregtech] bottle filling row skipped: {}", reason);
    }

    /** The four GT6 counts (4/3/2/1 bottles) of one family, in GT6's order. */
    private static int fill(List<Recipe<?>> recipes, com.gregtech.gregtech.content.food.BottleFillingRows.Family family) {
        Item bottle = ForgeRegistries.ITEMS.getValue(GregTech.id(family.bottleId()));
        if (bottle == null || bottle == Items.AIR) {
            SKIPPED.add(family.bottleId() + ": the port registers no such bottle item");
            return 0;
        }
        Fluid fluid = GTDrinks.fluidForField(family.fluidKey());
        if (fluid == null) {
            SKIPPED.add(family.bottleId() + " (" + family.source() + "): no registered fluid for "
                    + family.fluidKey());
            return 0;
        }
        ItemStack empty = BottleItem.emptyBottle();
        int added = 0;
        for (int count = 4; count >= 1; count--) {
            NonNullList<Ingredient> ingredients = NonNullList.create();
            ingredients.add(FiniteBottleFillingRecipe.container(family.fluidKey()));
            for (int i = 0; i < count; i++) ingredients.add(Ingredient.of(empty));
            ResourceLocation id = GregTech.id("bottles/" + family.bottleId() + "_x" + count);
            ShapelessRecipe base = new ShapelessRecipe(id, "gt.bottles", CraftingBookCategory.MISC,
                    new ItemStack(bottle, count), ingredients);
            recipes.add(new FiniteBottleFillingRecipe(base));
            REGISTERED.add(id.toString());
            added++;
        }
        return added;
    }

    /** GT6 MultiItemBottles:390-396, with a real, fully consumed 1000 mB container. */
    private static int fillLubricant(List<Recipe<?>> recipes) {
        Item bottle = ForgeRegistries.ITEMS.getValue(GregTech.id("lubricant_bottle"));
        if (bottle == null || bottle == Items.AIR)
            throw new IllegalStateException("GT6 lubricant bottle item is not registered");
        ItemStack empty = BottleItem.emptyBottle();
        if (empty.isEmpty())
            throw new IllegalStateException("GT6 empty bottle item is not registered");
        int added = 0;
        for (int count = 4; count >= 1; count--) {
            NonNullList<Ingredient> ingredients = NonNullList.create();
            ingredients.add(FiniteBottleFillingRecipe.container("Lubricant"));
            for (int i = 0; i < count; i++) ingredients.add(Ingredient.of(empty));
            ResourceLocation id = GregTech.id("bottles/lubricant_bottle_x" + count);
            ShapelessRecipe base = new ShapelessRecipe(id, "gt.bottles", CraftingBookCategory.MISC,
                    new ItemStack(bottle, count), ingredients);
            recipes.add(new FiniteBottleFillingRecipe(base));
            REGISTERED.add(id.toString());
            added++;
        }
        return added;
    }
}
