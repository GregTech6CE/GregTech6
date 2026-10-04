package com.gregtech.gregtech.emi;

import com.gregtech.gregtech.content.loot.LootViewerData;
import com.gregtech.gregtech.client.LootPageCaptions;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.List;

/** Native EMI lookup, with real output keys and no claim of craftability for random loot. */
public final class LootEmiRecipe implements EmiRecipe {
    private final EmiRecipeCategory category;
    private final LootViewerData.Row row;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;
    public LootEmiRecipe(EmiRecipeCategory category, LootViewerData.Row row) {
        this.category = category; this.row = row;
        inputs = List.of(EmiIngredient.of(row.inputs().stream().map(EmiStack::of).toList()));
        outputs = List.of(EmiStack.of(row.output()));
    }
    public LootViewerData.Row row() { return row; }
    public static void register(EmiRegistry registry) {
        for (boolean mobs : new boolean[]{false, true}) {
            String path = mobs ? "mob_drops" : "loot_tables";
            var category = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("gregtech", path),
                EmiStack.of(new ItemStack(mobs ? Items.ZOMBIE_SPAWN_EGG : Items.CHEST))) {
                    @Override public Component getName() { return Component.translatable("gregtech.jei.category." + path); }
                };
            registry.addCategory(category);
            for (var row : mobs ? LootViewerData.mobDrops() : LootViewerData.lootTables()) registry.addRecipe(new LootEmiRecipe(category, row));
        }
    }
    @Override public EmiRecipeCategory getCategory() { return category; }
    @Override public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath("gregtech", "/loot/" + row.id()); }
    @Override public List<EmiIngredient> getInputs() { return inputs; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public boolean supportsRecipeTree() { return false; }
    @Override public boolean hideCraftable() { return true; }
    @Override public int getDisplayWidth() { return LootPageCaptions.WIDTH; }
    @Override public int getDisplayHeight() { return LootPageCaptions.HEIGHT; }
    @Override public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 23, 21);
        var slot = widgets.addSlot(outputs.get(0), 125, 21).recipeContext(this);
        for (var line : LootPageCaptions.lines(row)) slot.appendTooltip(line);
        widgets.addDrawable(0, 0, getDisplayWidth(), getDisplayHeight(),
            (graphics, x, y, delta) -> LootPageCaptions.draw(graphics, row));
    }
}
