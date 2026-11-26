package gregtech.mixin;

import gregtech.compat.jei.AtlasLayoutLookup;
import java.util.*;
import java.util.function.IntConsumer;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.advanced.IRecipeButtonControllerFactory;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.gui.recipes.IRecipeLayoutWithButtons;
import mezz.jei.gui.recipes.RecipesGui;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mezz.jei.gui.recipes.layouts.LazyRecipeLayoutList", remap = false)
public abstract class JeiAtlasLayoutMixin<T> implements AtlasLayoutLookup {
    @Shadow
    @Final
    private List<IRecipeLayoutWithButtons<?>> results;

    @Shadow
    @Final
    private boolean matchingCraftable;

    @Shadow
    @Final
    private RecipesGui recipesGui;

    @Shadow
    @Final
    private IRecipeManager recipeManager;

    @Shadow
    private IRecipeLayoutDrawable<T> createRecipeLayout(T recipe) {
        throw new AssertionError();
    }

    @Shadow
    private IRecipeLayoutWithButtons<T> createRecipeLayoutWithButtons(
            IRecipeLayoutDrawable<T> layout,
            IIngredientManager ingredients,
            List<IRecipeButtonControllerFactory> factories) {
        throw new AssertionError();
    }

    @Unique
    private List<T> gregtech$ordered;

    @Unique
    private Map<Object, IRecipeLayoutWithButtons<?>> gregtech$created;

    @Unique
    private Object gregtech$waitingFor;

    @Unique
    private IntConsumer gregtech$selectPage;

    @Unique
    private int gregtech$checked;

    @Override
    @SuppressWarnings("unchecked")
    public void locate(Object target, List<?> recipes, IntConsumer selectPage) {
        for (int i = 0; i < results.size(); i++) {
            if (target.equals(results.get(i).getRecipeLayout().getRecipe())) {
                selectPage.accept(i);
                return;
            }
        }
        if (matchingCraftable) {
            // Transfer handlers need real layouts to rank craftability. Follow JEI's background sorting.
            gregtech$waitingFor = target;
            gregtech$selectPage = selectPage;
            gregtech$checked = results.size();
            return;
        }

        // JEI has already moved bookmarks to the front. The remaining records retain lookup order.
        gregtech$ordered = new ArrayList<>(recipes.size());
        gregtech$created = new HashMap<>();
        for (var layout : results) {
            var recipe = (T) layout.getRecipeLayout().getRecipe();
            gregtech$ordered.add(recipe);
            gregtech$created.put(recipe, layout);
        }
        for (var recipe : recipes) {
            if (!gregtech$created.containsKey(recipe)) gregtech$ordered.add((T) recipe);
        }
        int index = gregtech$ordered.indexOf(target);
        if (index >= 0) selectPage.accept(index);
    }

    @Inject(method = "subList", at = @At("HEAD"), cancellable = true)
    private void gregtech$visiblePage(int from, int to, CallbackInfoReturnable<List<IRecipeLayoutWithButtons<?>>> ci) {
        if (gregtech$ordered == null) return;
        var page = new ArrayList<IRecipeLayoutWithButtons<?>>(to - from);
        var ingredients = gregtech.compat.jei.GregTechJEIPlugin.ingredients();
        var factories = recipeManager.getRecipeButtonControllerFactories();
        for (T recipe : gregtech$ordered.subList(from, to)) {
            page.add(gregtech$created.computeIfAbsent(
                    recipe, key -> createRecipeLayoutWithButtons(createRecipeLayout(recipe), ingredients, factories)));
        }
        ci.setReturnValue(page);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void gregtech$skipPrefetch(CallbackInfo ci) {
        if (gregtech$ordered != null) ci.cancel();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void gregtech$finishSortedLookup(CallbackInfo ci) {
        if (gregtech$waitingFor == null) return;
        while (gregtech$checked < results.size()) {
            int index = gregtech$checked++;
            if (gregtech$waitingFor.equals(results.get(index).getRecipeLayout().getRecipe())) {
                gregtech$waitingFor = null;
                gregtech$selectPage.accept(index);
                gregtech$selectPage = null;
                var client = net.minecraft.client.Minecraft.getInstance();
                if (client.screen == recipesGui) recipesGui.resize(client, recipesGui.width, recipesGui.height);
                return;
            }
        }
    }
}
