package gregtech.content.recipe;

import gregtech.content.block.wood.WoodSpecies;
import gregtech.content.recipe.WoodCraftingRow.IngredientSpec;
import gregtech.content.recipe.WoodCraftingRow.Item;
import gregtech.content.recipe.WoodCraftingRow.ItemSpec;
import gregtech.content.recipe.WoodCraftingRow.MaterialForm;
import gregtech.content.recipe.WoodCraftingRow.Tool;
import gregtech.content.recipe.WoodCraftingRow.WoodBlock;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTToolItems;
import gregtech.content.registry.GTWoods;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.tool.GTToolType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

//? if <1.21.1 {
import gregtech.GregTech;
import gregtech.content.recipe.WoodCraftingRow.Shaped;
import gregtech.content.recipe.WoodCraftingRow.Shapeless;
import gregtech.content.recipe.WoodCraftingRow.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jetbrains.annotations.Nullable;
//?}

/** Resolves original wood recipes. Forge publishes them directly to the recipe manager. */
public final class WoodCraftingRecipeInjector {
    private WoodCraftingRecipeInjector() {}

    public static ItemStack resolve(ItemSpec spec) {
        if (spec instanceof Item item)
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item.id())));
        if (spec instanceof MaterialForm form) {
            var material = GTMaterialRegistry.get(form.material());
            return material == null || !material.isValid()
                    ? ItemStack.EMPTY
                    : GTItems.getStack(form.prefix(), material, 1);
        }
        if (spec instanceof Tool tool) {
            var item = GTToolItems.get(switch (tool) {
                case SAW -> GTToolType.SAW;
                case FILE -> GTToolType.FILE;
            });
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
        var wood = (WoodBlock) spec;
        for (WoodSpecies species : WoodSpecies.values()) {
            if (!species.id().equals(wood.species())) continue;
            return switch (wood.form()) {
                case LOG -> new ItemStack(GTWoods.log(species));
                case PLANKS -> new ItemStack(GTWoods.planks(species));
                case STRIPPED -> new ItemStack(GTWoods.strippedLog(species));
            };
        }
        return ItemStack.EMPTY;
    }

    //? if <1.21.1 {
    private static final List<String> REGISTERED = new ArrayList<>();
    private static final List<String> SKIPPED = new ArrayList<>();
    private static final List<String> TREATED_REGISTERED = new ArrayList<>();

    /** Recipe ids the last server start added, for tests and reports. */
    public static List<String> registeredIds() {
        return REGISTERED.stream().filter(id -> !id.startsWith("gt6ce:wood/natural/")).toList();
    }

    public static List<String> treatedRegisteredIds() {
        return List.copyOf(TREATED_REGISTERED);
    }

    public static List<String> skipped() {
        return List.copyOf(SKIPPED);
    }

    public static List<String> speciesIds() {
        return GTWoodRecipes.SPECIES.stream().map(GTWoodRecipes.Species::id).toList();
    }

    public static void apply(RecipeManager manager, net.minecraft.core.RegistryAccess access) {
        List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes());
        REGISTERED.clear();
        SKIPPED.clear();
        TREATED_REGISTERED.clear();
        for (var row : GTWoodRecipes.buildingRows()) addRow(recipes, row, CraftingBookCategory.BUILDING);
        for (var row : TreatedWoodCraftingRows.ROWS) addRow(recipes, row, CraftingBookCategory.MISC);
        int added = REGISTERED.size() + TREATED_REGISTERED.size();
        if (added > 0) RuntimeRecipeLifecycle.replaceGenerated(manager, recipes);
        GregTech.LOGGER.info("Registered {} GT6 wood dictionary crafting rows for {} species ({} skipped)",
                added, speciesIds().size(), SKIPPED.size());
        for (String reason : SKIPPED) GregTech.LOGGER.debug("[gregtech] wood row skipped: {}", reason);
    }

    private static void addRow(List<Recipe<?>> recipes, WoodCraftingRow row, CraftingBookCategory category) {
        boolean treated = category == CraftingBookCategory.MISC;
        var id = ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, "wood/" + row.path());
        var output = resolve(row.output());
        if (output.isEmpty()) {
            unresolved(row, "result " + row.output(), treated);
            return;
        }
        output.setCount(row.count());
        if (row.shape() instanceof Shaped shaped) {
            var keys = new LinkedHashMap<Character, Ingredient>();
            for (var key : shaped.keys()) {
                var ingredient = ingredient(key.ingredient());
                if (ingredient == null) {
                    unresolved(row, "key '" + key.symbol() + "' (" + key.ingredient() + ")", treated);
                    return;
                }
                keys.put(key.symbol(), ingredient);
            }
            int width = shaped.pattern().get(0).length();
            int height = shaped.pattern().size();
            var grid = NonNullList.withSize(width * height, Ingredient.EMPTY);
            for (int y = 0; y < height; y++) {
                var patternRow = shaped.pattern().get(y);
                for (int x = 0; x < width; x++) {
                    char symbol = patternRow.charAt(x);
                    if (symbol != ' ') grid.set(x + y * width, keys.get(symbol));
                }
            }
            var base = new ShapedRecipe(id, "gt.wood", category, width, height, grid, output);
            recipes.add(new ToolShapedRecipe(base, shaped.allowMirror()));
        } else {
            var shapeless = (Shapeless) row.shape();
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (var spec : shapeless.ingredients()) {
                var ingredient = ingredient(spec);
                if (ingredient == null) {
                    unresolved(row, "ingredient " + spec, treated);
                    return;
                }
                ingredients.add(ingredient);
            }
            recipes.add(new ShapelessRecipe(id, "gt.wood", category, output, ingredients));
        }
        (treated ? TREATED_REGISTERED : REGISTERED).add(id.toString());
    }

    private static @Nullable Ingredient ingredient(IngredientSpec spec) {
        if (spec instanceof Tag tag) return Ingredient.of(ItemTags.create(ResourceLocation.parse(tag.id())));
        var stack = resolve((ItemSpec) spec);
        return stack.isEmpty() ? null : Ingredient.of(stack);
    }

    private static void unresolved(WoodCraftingRow row, String spec, boolean required) {
        String message = row.path() + " (" + row.source() + "): unresolved " + spec;
        if (required) throw new IllegalStateException(message);
        SKIPPED.add(message);
    }
    //?}
}
