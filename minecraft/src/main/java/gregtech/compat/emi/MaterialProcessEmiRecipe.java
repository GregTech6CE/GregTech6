package gregtech.compat.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.WidgetHolder;
import gregtech.client.MaterialProcessPanel;
import gregtech.compat.common.MaterialProcessDisplay;
import gregtech.compat.common.RecipeViewerIcons;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** EMI adapter over the same single-page, pannable process graph used by JEI. */
public final class MaterialProcessEmiRecipe implements EmiRecipe {
    private static final EmiRecipeCategory CATEGORY =
            new EmiRecipeCategory(
                    ResourceLocation.fromNamespaceAndPath("gt6ce", "material_process"),
                    EmiStack.of(RecipeViewerIcons.item("ore_iridium"))) {
                @Override
                public Component getName() {
                    return Component.translatable("gt6ce.jei.material_process.title");
                }
            };

    private final MaterialProcessDisplay.Page page;
    private MaterialProcessPanel panel;
    private final ResourceLocation id;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;

    public MaterialProcessEmiRecipe(MaterialProcessDisplay.Page page) {
        this.page = page;
        id = ResourceLocation.fromNamespaceAndPath("gt6ce", "/material_process/" + path(page.material()));
        var inputs = new ArrayList<EmiIngredient>();
        var catalysts = new ArrayList<EmiIngredient>();
        var outputs = new ArrayList<EmiStack>();
        for (var step : page.steps()) {
            for (var port : step.inputs()) add(port, inputs, catalysts, outputs);
            for (var port : step.outputs()) add(port, inputs, catalysts, outputs);
        }
        this.inputs = List.copyOf(inputs);
        this.catalysts = List.copyOf(catalysts);
        this.outputs = List.copyOf(outputs);
    }

    private static String path(String material) {
        String path = material.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        return path.isBlank() ? "unbound" : path;
    }

    private static void add(
            MaterialProcessDisplay.Port port,
            List<EmiIngredient> inputs,
            List<EmiIngredient> catalysts,
            List<EmiStack> outputs) {
        if (!port.hasIcons()) return;
        var stacks = stacks(port);
        var ingredient = EmiIngredient.of(stacks);
        switch (port.role()) {
            case CONSUMED -> inputs.add(ingredient);
            case CATALYST, PRESENCE -> catalysts.add(ingredient);
            case PRIMARY, BYPRODUCT, UNKNOWN -> outputs.addAll(stacks);
        }
    }

    private static List<EmiStack> stacks(MaterialProcessDisplay.Port port) {
        return port.icons().stream()
                .map(MaterialProcessEmiStacks::of)
                .filter(stack -> !stack.isEmpty())
                .map(stack -> port.chance().isPresent()
                        ? stack.setChance(port.chance().getAsInt() / 10_000F)
                        : stack)
                .toList();
    }

    public static void register(EmiRegistry registry, List<MaterialProcessDisplay.Page> pages) {
        registry.addCategory(CATEGORY);
        for (var page : pages) registry.addRecipe(new MaterialProcessEmiRecipe(page));
    }

    public static EmiRecipeCategory category() {
        return CATEGORY;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CATEGORY;
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

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public boolean hideCraftable() {
        return true;
    }

    @Override
    public int getDisplayWidth() {
        return MaterialProcessPanel.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return MaterialProcessPanel.HEIGHT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        if (panel == null) panel = new MaterialProcessPanel(page);
        widgets.add(new EmiInteractiveWidget() {
            @Override
            public Bounds getBounds() {
                return new Bounds(0, 0, MaterialProcessPanel.WIDTH, MaterialProcessPanel.HEIGHT);
            }

            @Override
            public void render(GuiGraphics graphics, int x, int y, float delta) {
                EmiWidgetInput.rendered(this, graphics);
                panel.draw(graphics, x, y);
            }

            @Override
            public EmiIngredient ingredientAt(double x, double y) {
                return EmiIngredient.of(panel.stacksAt(x, y).stream()
                        .map(MaterialProcessEmiStacks::of)
                        .filter(stack -> !stack.isEmpty())
                        .toList());
            }

            @Override
            public boolean mouseClicked(int x, int y, int button) {
                return panel.click(x, y, button);
            }

            @Override
            public boolean pressed(double x, double y, int button) {
                return panel.press(x, y, button);
            }

            @Override
            public boolean dragged(int button, double dx, double dy) {
                return panel.drag(button, dx, dy);
            }

            @Override
            public boolean scrolled(double x, double y, double delta) {
                return panel.scroll(x, y, delta);
            }

            @Override
            public List<ClientTooltipComponent> getTooltip(int x, int y) {
                return panel.tooltip(x, y).stream()
                        .map(line -> ClientTooltipComponent.create(line.getVisualOrderText()))
                        .toList();
            }
        });
    }
}
