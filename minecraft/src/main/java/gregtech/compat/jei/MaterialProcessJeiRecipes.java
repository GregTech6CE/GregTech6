package gregtech.compat.jei;

import gregtech.content.recipe.MaterialProcessSnapshot;
import gregtech.engine.material.MaterialProcessGraph.Port;
import gregtech.engine.material.MaterialProcessGraph.Role;
import gregtech.engine.recipe.Recipe;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import mezz.jei.api.neoforge.NeoForgeTypes;
import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import mezz.jei.api.forge.ForgeTypes;
import net.minecraftforge.fluids.FluidStack;
//?}

/** Captures JEI's final visible recipe rows into the same material-page view model used by EMI. */
final class MaterialProcessJeiRecipes {
    private static final IFocusGroup NO_FOCUS = new IFocusGroup() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public List<IFocus<?>> getAllFocuses() {
            return List.of();
        }

        @Override
        public Stream<IFocus<?>> getFocuses(RecipeIngredientRole role) {
            return Stream.empty();
        }

        @Override
        public <T> Stream<IFocus<T>> getFocuses(IIngredientType<T> type) {
            return Stream.empty();
        }

        @Override
        public <T> Stream<IFocus<T>> getFocuses(IIngredientType<T> type, RecipeIngredientRole role) {
            return Stream.empty();
        }
    };

    private MaterialProcessJeiRecipes() {}

    static MaterialProcessSnapshot capture(
            MaterialProcessSnapshot source, mezz.jei.api.runtime.IJeiRuntime runtime) {
        var builder = source.viewerBuilder();
        var manager = runtime.getRecipeManager();
        var categories = manager.createRecipeCategoryLookup().get()
                .filter(category -> !informationOnly(category.getRecipeType()))
                .toList();
        var ids = new HashSet<String>();
        var counters = new Counters();
        for (var category : categories) captureCategory(manager, category, builder, ids, counters);
        gregtech.platform.Mc.logger()
                .info("[gregtech] JEI material process index: {} visible categories, {} recipes, {} rows, {} omitted",
                        categories.size(), counters.recipes, counters.rows, counters.omitted);
        return builder.build();
    }

    private static boolean informationOnly(mezz.jei.api.recipe.RecipeType<?> type) {
        if (type.equals(MaterialProcessCategory.TYPE)
                || type.equals(MultiblockInfoCategory.TYPE)
                || type.equals(MoldChiselCategory.TYPE)
                || type.equals(LootInfoCategories.LOOT)
                || type.equals(LootInfoCategories.MOBS)
                || type.equals(WorldgenInfoCategories.VEIN_TYPE)
                || type.equals(WorldgenInfoCategories.SMALL_ORE_TYPE)
                || type.equals(WorldgenInfoCategories.LAYER_TYPE)
                || type.equals(WorldgenInfoCategories.BEDROCK_TYPE)) return true;
        for (var family : MaterialUnitTagCategory.FAMILIES)
            if (type.equals(MaterialUnitTagCategory.type(family))) return true;
        return false;
    }

    private static <T> void captureCategory(
            IRecipeManager manager,
            IRecipeCategory<T> category,
            MaterialProcessSnapshot.ViewerBuilder builder,
            Set<String> ids,
            Counters counters) {
        var recipes = manager.createRecipeLookup(category.getRecipeType()).get().toList();
        counters.recipes += recipes.size();
        for (int index = 0; index < recipes.size(); index++) {
            T recipe = recipes.get(index);
            ResourceLocation id = category.getRegistryName(recipe);
            if (id != null && !ids.add(category.getRecipeType().getUid() + "/" + id)) continue;
            if (category instanceof RecipeMapCategory mapCategory
                    && recipe instanceof Recipe nativeRecipe
                    && builder.machine(mapCategory.recipeMap(), nativeRecipe)) {
                counters.rows++;
                continue;
            }
            captureRecipe(manager, category, recipe, index, builder, counters);
        }
    }

    private static <T> void captureRecipe(
            IRecipeManager manager,
            IRecipeCategory<T> category,
            T recipe,
            int index,
            MaterialProcessSnapshot.ViewerBuilder builder,
            Counters counters) {
        Optional<IRecipeLayoutDrawable<T>> drawable =
                manager.createRecipeLayoutDrawable(category, recipe, NO_FOCUS);
        if (drawable.isEmpty()) {
            builder.omitted();
            counters.omitted++;
            return;
        }
        var inputs = new ArrayList<Port>();
        var outputs = new ArrayList<Port>();
        for (IRecipeSlotView slot : drawable.get().getRecipeSlotsView().getSlotViews()) {
            if (slot.isEmpty() || slot.getRole() == RecipeIngredientRole.RENDER_ONLY) continue;
            Port port = port(builder, slot);
            if (port == null) {
                builder.omitted();
                counters.omitted++;
                return;
            }
            switch (slot.getRole()) {
                case INPUT, CATALYST -> inputs.add(port);
                case OUTPUT -> outputs.add(port);
                default -> {
                    builder.omitted();
                    counters.omitted++;
                    return;
                }
            }
        }
        if (inputs.isEmpty() || outputs.isEmpty()) {
            builder.omitted();
            counters.omitted++;
            return;
        }
        ItemStack machine = manager.createRecipeCatalystLookup(category.getRecipeType()).getItemStack()
                .findFirst().orElseGet(() -> new ItemStack(Items.ANVIL));
        ResourceLocation type = category.getRecipeType().getUid();
        ResourceLocation registryName = category.getRegistryName(recipe);
        gregtech.api.addon.material.DataKey reference = new gregtech.api.addon.material.DataKey(
                "gt6ce",
                "process/jei/" + type.getNamespace() + "/" + type.getPath() + "/" + index + "/"
                        + Long.toUnsignedString(
                                builder.publicationRevision(), 36)
                        + "/"
                        + Long.toUnsignedString(builder.sessionGeneration(), 36));
        var details = new ArrayList<String>();
        details.add("viewer category: " + type);
        if (registryName != null) details.add("viewer recipe: " + registryName);
        details.add("matching constraints and remainders: see original recipe");
        builder.recipe(
                reference,
                type.toString(),
                category.getTitle().getString(),
                machine,
                List.copyOf(inputs),
                List.copyOf(outputs),
                List.copyOf(details),
                true);
        counters.rows++;
    }

    private static Port port(MaterialProcessSnapshot.ViewerBuilder builder, IRecipeSlotView slot) {
        var fluids = new ArrayList<FluidStack>();
        var items = new ArrayList<ItemStack>();
        for (ITypedIngredient<?> typed : slot.getAllIngredients().toList()) {
            if (typed == null) continue;
//? if >=1.21.1 {
            /*typed.getIngredient(NeoForgeTypes.FLUID_STACK)
                    .filter(fluid -> !fluid.isEmpty())
                    .ifPresent(fluids::add);
*///?} else {
            typed.getIngredient(ForgeTypes.FLUID_STACK)
                    .filter(fluid -> !fluid.isEmpty())
                    .ifPresent(fluids::add);
//?}
            typed.getIngredient(VanillaTypes.ITEM_STACK)
                    .filter(stack -> !stack.isEmpty())
                    .ifPresent(items::add);
        }
        Role role = switch (slot.getRole()) {
            case INPUT -> Role.CONSUMED;
            case CATALYST -> Role.CATALYST;
            case OUTPUT -> Role.UNKNOWN;
            default -> null;
        };
        if (role == null) return null;
        var candidates = new ArrayList<Port>();
        // Fluid renderers may also expose an item proxy. The native fluid is the authoritative slot value.
        if (!fluids.isEmpty()) {
            for (FluidStack fluid : fluids)
                candidates.add(builder.fluid(fluid, role, fluid.getAmount(), OptionalInt.empty()));
        } else {
            for (ItemStack stack : items)
                candidates.add(builder.item(stack, role, stack.getCount(), OptionalInt.empty()));
        }
        return candidates.isEmpty() ? null : builder.alternatives(candidates);
    }

    private static final class Counters {
        private int recipes;
        private int rows;
        private int omitted;
    }
}
