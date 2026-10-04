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
    static void check(com.google.gson.JsonObject result) {
        var manager = runtime.getRecipeManager();
        if (manager.createRecipeLookup(LootInfoCategories.LOOT).get().count() != LootViewerData.lootGroups().size()
                || manager.createRecipeLookup(LootInfoCategories.MOBS).get().count() != LootViewerData.mobGroups().size())
            throw new IllegalStateException("Actual JEI omitted GT loot rows");
        var row = LootViewerData.mobDrops().stream().filter(r -> r.output().getItem().toString().endsWith("cow_hoof")).findFirst().orElseThrow();
        var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK, row.output());
        if (manager.createRecipeLookup(LootInfoCategories.MOBS).limitFocus(List.of(focus)).get().findAny().isEmpty())
            throw new IllegalStateException("JEI cow-hoof output lookup is empty");
        int indexed=0,tooltips=0;
        var emptyFocus=runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
        for(var type:List.of(LootInfoCategories.LOOT,LootInfoCategories.MOBS)) {
            var category=manager.getRecipeCategory(type);
            for(var group:manager.createRecipeLookup(type).get().toList()) {
                var layout=manager.createRecipeLayoutDrawable(category,group,emptyFocus).orElseThrow();
                int count=layout.getRecipeSlotsView().getSlotViews(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT).size();
                if(count!=group.rows().size())throw new IllegalStateException("JEI grouped slots omitted outputs");
                for(var entry:group.rows()) {
                    var entryFocus=runtime.getJeiHelpers().getFocusFactory().createFocus(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,mezz.jei.api.constants.VanillaTypes.ITEM_STACK,entry.output());
                    if(manager.createRecipeLookup(type).limitFocus(List.of(entryFocus)).get().noneMatch(g->g.id().equals(group.id())))throw new IllegalStateException("JEI grouped reverse lookup omitted source "+group.id());
                    indexed++;
                }
                for(var slot:layout.getRecipeSlotsView().getSlotViews(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT)) {
                    var tooltip=((mezz.jei.api.gui.ingredient.IRecipeSlotDrawable)slot).getTooltip();
                    if(tooltip.stream().noneMatch(c->c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && (t.getKey().equals("gregtech.loot.chance")||t.getKey().equals("gregtech.loot.conditional"))))throw new IllegalStateException("JEI output tooltip omitted chance");
                    tooltips++;
                }
            }
        }
        result.addProperty("groupedOutputsIndexed",indexed);
        result.addProperty("groupedOutputTooltipsChecked",tooltips);
        var group=manager.createRecipeLookup(LootInfoCategories.LOOT).get().max(java.util.Comparator.comparingInt(g->g.rows().size())).orElseThrow();
        var layout=manager.createRecipeLayoutDrawable(manager.getRecipeCategory(LootInfoCategories.LOOT),group,emptyFocus).orElseThrow();
        var graphics=new net.minecraft.client.gui.GuiGraphics(net.minecraft.client.Minecraft.getInstance(),net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource());
        layout.setPosition(0,0);layout.drawRecipe(graphics,0,0);graphics.flush();
        if(!layout.getInputHandler().handleMouseScrolled(10,50,-1))throw new IllegalStateException("Native JEI loot grid did not scroll");
        layout.drawRecipe(graphics,0,0);graphics.flush();
        var actual=layout.getIngredientUnderMouse(4,42,mezz.jei.api.constants.VanillaTypes.ITEM_STACK).orElseThrow();
        if(!net.minecraft.world.item.ItemStack.isSameItemSameTags(actual,group.rows().get(9).output()))throw new IllegalStateException("Native JEI scrolled grid kept stale output");
        result.addProperty("jeiLootGridScrolled",true);
        result.addProperty("jeiStructureRows",manager.createRecipeLookup(com.gregtech.gregtech.jei.MultiblockInfoCategory.TYPE).get().count());
    }
    static void show(int page) {
        var manager=runtime.getRecipeManager();
        if(page==3) {
            var type=com.gregtech.gregtech.jei.MultiblockInfoCategory.TYPE;
            var info=manager.createRecipeLookup(type).get().findFirst().orElseThrow();
            runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(type),List.of(info),List.of());
        } else {
            var type=page==1?LootInfoCategories.MOBS:LootInfoCategories.LOOT;
            var group=page==1?manager.createRecipeLookup(type).get().filter(g->g.rows().stream().anyMatch(v->v.output().getItem().toString().endsWith("cow_hoof"))).findFirst().orElseThrow():manager.createRecipeLookup(type).get().max(java.util.Comparator.comparingInt(g->g.rows().size())).orElseThrow();
            runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(type),List.of(group),List.of());
        }
    }
}
