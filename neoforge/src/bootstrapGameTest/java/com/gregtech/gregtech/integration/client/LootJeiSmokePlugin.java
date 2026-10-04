package com.gregtech.gregtech.integration.client;

import com.gregtech.gregtech.content.loot.LootViewerData;
import com.gregtech.gregtech.jei.LootInfoCategories;
import mezz.jei.api.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Only the isolated bootstrap source set: queries the real registered JEI runtime. */
@JeiPlugin
public final class LootJeiSmokePlugin implements IModPlugin {
    private static IJeiRuntime runtime;
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath("gregtech", "loot_smoke_runtime"); }
    @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
    static boolean ready() { return runtime != null; }
    static void check() {
        var manager = runtime.getRecipeManager();
        if (manager.createRecipeLookup(LootInfoCategories.LOOT).get().count() != LootViewerData.lootTables().size()
                || manager.createRecipeLookup(LootInfoCategories.MOBS).get().count() != LootViewerData.mobDrops().size())
            throw new IllegalStateException("Actual JEI omitted GT loot rows");
        var row = LootViewerData.mobDrops().stream().filter(r -> r.output().getItem().toString().endsWith("cow_hoof")).findFirst().orElseThrow();
        var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK, row.output());
        if (manager.createRecipeLookup(LootInfoCategories.MOBS).limitFocus(List.of(focus)).get().findAny().isEmpty())
            throw new IllegalStateException("JEI cow-hoof output lookup is empty");
    }
    static void show(boolean mobs) {
        var type = mobs ? LootInfoCategories.MOBS : LootInfoCategories.LOOT;
        var rows = mobs ? LootViewerData.mobDrops() : LootViewerData.lootTables();
        var row = rows.stream().filter(r -> mobs ? r.output().getItem().toString().endsWith("cow_hoof") : r.id().contains("table/gt/misc/"))
                .findFirst().orElseThrow();
        runtime.getRecipesGui().showRecipes(runtime.getRecipeManager().getRecipeCategory(type), List.of(row), List.of());
    }
}
