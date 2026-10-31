package gregtech.compat.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import gregtech.content.recipe.ToolAssemblyCatalog;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** Native EMI uses the same material-specific assembly rows and legal handles as JEI. */
public final class ToolAssemblyEmiRecipe implements EmiRecipe {
    private final EmiRecipeCategory category;
    private final gregtech.content.recipe.GTToolRecipes.Pattern pattern;
    private final boolean headAssembly;
    private final ResourceLocation id;
    private final List<EmiIngredient> slots;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;

    public ToolAssemblyEmiRecipe(EmiRecipeCategory category, ToolAssemblyCatalog.ToolAssemblyInfo info) {
        this(category, info, new EmiItemCandidates());
    }

    private ToolAssemblyEmiRecipe(
            EmiRecipeCategory category, ToolAssemblyCatalog.ToolAssemblyInfo info, EmiItemCandidates candidates) {
        this.category = category;
        this.pattern = info.pattern();
        this.headAssembly = info.headAssembly();
        int material = gregtech.engine.tool.GTToolHelper.getHead(info.output()).getId();
        this.id = ResourceLocation.fromNamespaceAndPath(
                "gt6ce", "/tool_assembly/" + info.type().id() + "/" + material);
        this.slots = info.inputs().stream()
                .map(options -> (EmiIngredient) new MachineEmiIngredient(candidates.of(options), 1))
                .toList();
        var consumed = new ArrayList<EmiIngredient>();
        var tools = new ArrayList<EmiIngredient>();
        int slot = 0;
        if (info.pattern() == null) consumed.addAll(slots);
        else
            for (int y = 0; y < info.pattern().height(); y++)
                for (int x = 0; x < info.pattern().width(); x++) {
                    char cell = info.pattern().at(x, y);
                    if (cell != ' ')
                        (info.pattern().tools().containsKey(cell) ? tools : consumed).add(slots.get(slot++));
                }
        this.inputs = List.copyOf(consumed);
        this.catalysts = List.copyOf(tools);
        this.outputs = List.of(EmiStack.of(info.output()));
    }

    public static void register(EmiRegistry registry) {
        var candidates = new EmiItemCandidates();
        try {
            register(registry, candidates);
        } finally {
            candidates.finish();
        }
    }

    static void register(EmiRegistry registry, EmiItemCandidates candidates) {
        var rows = ToolAssemblyCatalog.build();
        if (rows.isEmpty()) return;
        var category =
                new EmiRecipeCategory(
                        ResourceLocation.fromNamespaceAndPath("gt6ce", "tool_assembly"),
                        EmiStack.of(gregtech.compat.common.RecipeViewerIcons.toolAssembly())) {
                    @Override
                    public Component getName() {
                        return Component.translatable("gt6ce.jei.category.tool_assembly");
                    }
                };
        registry.addCategory(category);
        registry.addWorkstation(category, EmiStack.of(Items.CRAFTING_TABLE));
        // Material rows repeat tool and handle candidates. Convert each exact list once per reload,
        // while keeping amount/chance and rendering state private to each ingredient wrapper.
        for (var row : rows) registry.addRecipe(new ToolAssemblyEmiRecipe(category, row, candidates));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return catalysts;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }
    // The output's handle is representative; do not promise automatic crafting of a different handle.
    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public int getDisplayWidth() {
        return 150;
    }

    @Override
    public int getDisplayHeight() {
        return 82;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int slot = 0;
        if (pattern == null) {
            for (var ingredient : slots) widgets.addSlot(ingredient, (slot++) * 18, 18);
        } else
            for (int y = 0; y < pattern.height(); y++)
                for (int x = 0; x < pattern.width(); x++) {
                    if (pattern.at(x, y) != ' ') widgets.addSlot(slots.get(slot++), x * 18, y * 18);
                }
        widgets.addSlot(outputs.get(0), 90, 18).recipeContext(this);
        widgets.addText(
                headAssembly
                        ? Component.translatable("gt6ce.jei.info.tool_head_and_handle")
                        : Component.translatable("gt6ce.jei.info.tool_material_forms", slots.size()),
                1,
                58,
                0xff404040,
                false);
        widgets.addText(Component.translatable("gt6ce.jei.info.tool_any_material"), 1, 68, 0xff707070, false);
    }
}
