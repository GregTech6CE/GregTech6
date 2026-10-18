package com.gregtech.gregtech.emi;

import com.gregtech.gregtech.recipe.ToolAssemblyCatalog;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

/** Native EMI uses the same material-specific assembly rows and legal handles as JEI. */
public final class ToolAssemblyEmiRecipe implements EmiRecipe {
    private final EmiRecipeCategory category;
    private final ToolAssemblyCatalog.ToolAssemblyInfo info;
    private final ResourceLocation id;
    private final List<EmiIngredient> slots;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;

    public ToolAssemblyEmiRecipe(EmiRecipeCategory category, ToolAssemblyCatalog.ToolAssemblyInfo info) {
        this.category = category;
        this.info = info;
        int material = com.gregtech.gregtech.api.tool.GTToolHelper.getHead(info.output()).getId();
        this.id = ResourceLocation.fromNamespaceAndPath("gt6ce", "/tool_assembly/" + info.type().id() + "/" + material);
        this.slots = info.inputs().stream().map(options -> (EmiIngredient) new MachineEmiIngredient(options.stream().map(EmiStack::of).toList(), 1)).toList();
        var consumed = new ArrayList<EmiIngredient>();
        var tools = new ArrayList<EmiIngredient>();
        int slot = 0;
        if (info.pattern() == null) consumed.addAll(slots);
        else for (int y=0; y<info.pattern().height(); y++) for (int x=0; x<info.pattern().width(); x++) {
            char cell = info.pattern().at(x,y);
            if (cell != ' ') (info.pattern().tools().containsKey(cell) ? tools : consumed).add(slots.get(slot++));
        }
        this.inputs = List.copyOf(consumed);
        this.catalysts = List.copyOf(tools);
        this.outputs = List.of(EmiStack.of(info.output()));
    }

    public static void register(EmiRegistry registry) {
        var rows = ToolAssemblyCatalog.build();
        if (rows.isEmpty()) return;
        var category = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("gt6ce", "tool_assembly"),
                EmiStack.of(rows.get(0).output())) {
            @Override public Component getName() { return Component.translatable("gt6ce.jei.category.tool_assembly"); }
        };
        registry.addCategory(category);
        registry.addWorkstation(category, EmiStack.of(Items.CRAFTING_TABLE));
        for (var row : rows) registry.addRecipe(new ToolAssemblyEmiRecipe(category,row));
    }

    @Override public EmiRecipeCategory getCategory() { return category; }
    @Override public ResourceLocation getId() { return id; }
    @Override public List<EmiIngredient> getInputs() { return inputs; }
    @Override public List<EmiIngredient> getCatalysts() { return catalysts; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    // The output's handle is representative; do not promise automatic crafting of a different handle.
    @Override public boolean supportsRecipeTree() { return false; }
    @Override public int getDisplayWidth() { return 150; }
    @Override public int getDisplayHeight() { return 82; }
    @Override public void addWidgets(WidgetHolder widgets) {
        int slot = 0;
        if (info.pattern() == null) {
            for (var ingredient : slots) widgets.addSlot(ingredient, (slot++)*18, 18);
        } else for (int y=0; y<info.pattern().height(); y++) for (int x=0; x<info.pattern().width(); x++) {
            if (info.pattern().at(x,y) != ' ') widgets.addSlot(slots.get(slot++), x*18, y*18);
        }
        widgets.addSlot(outputs.get(0), 90, 18).recipeContext(this);
        widgets.addText(info.headAssembly()
                ? Component.translatable("gt6ce.jei.info.tool_head_and_handle")
                : Component.translatable("gt6ce.jei.info.tool_material_forms", info.inputs().size()), 1,58,0xff404040,false);
        widgets.addText(Component.translatable("gt6ce.jei.info.tool_any_material"),1,68,0xff707070,false);
    }
}
