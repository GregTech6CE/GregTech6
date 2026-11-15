package gregtech.compat.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.Comparison;
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
    private List<EmiStack> focus = List.of();
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
        for (var port : page.indexedPorts()) add(port, inputs, catalysts, outputs);
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

    public void focus(EmiIngredient ingredient) {
        focus = ingredient.getEmiStacks().stream().map(EmiStack::copy).toList();
        if (panel != null) focusPanel();
    }

    private void focusPanel() {
        // Strict focus matching follows EMI's native state model: components on 1.21, NBT on 1.20.
//? if >=1.21.1 {
        /*var comparison = Comparison.compareComponents();
*///?} else {
        var comparison = Comparison.compareNbt();
//?}
        panel.focus(port -> port.icons().stream().anyMatch(icon -> {
            var stack = MaterialProcessEmiStacks.of(icon);
            return !stack.isEmpty() && focus.stream().anyMatch(target -> stack.isEqual(target, comparison));
        }));
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
        return MaterialProcessPanel.preferredWidth();
    }

    @Override
    public int getDisplayHeight() {
        return MaterialProcessPanel.preferredHeight();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        if (panel == null) {
            panel = new MaterialProcessPanel(page);
            focusPanel();
        }
        panel.resize(Math.min(getDisplayWidth(), widgets.getWidth()), Math.min(getDisplayHeight(), widgets.getHeight()));
        widgets.add(new EmiInteractiveWidget() {
            @Override
            public Bounds getBounds() {
                return new Bounds(0, 0, panel.width(), panel.height());
            }

            @Override
            public void render(GuiGraphics graphics, int x, int y, float delta) {
                EmiWidgetInput.rendered(this, graphics);
                panel.draw(graphics, x, y);
            }

            @Override
            public boolean mouseClicked(int x, int y, int button) {
                return panel.clickControls(x, y, button);
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
        for (var node : panel.nodes()) {
            var icons = panel.stacks(node);
            if (icons.isEmpty()) continue;
            widgets.add(new dev.emi.emi.api.widget.SlotWidget(MaterialProcessEmiStacks.of(icons.get(0)), 0, 0) {
                @Override
                public Bounds getBounds() {
                    var bounds = panel.bounds(node);
                    return new Bounds(
                            bounds.left(),
                            bounds.top(),
                            Math.max(0, bounds.right() - bounds.left()),
                            Math.max(0, bounds.bottom() - bounds.top()));
                }

                @Override
                public EmiIngredient getStack() {
                    return page.navigation().valid() ? super.getStack() : EmiStack.EMPTY;
                }

                @Override
                public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}

                @Override
                public List<ClientTooltipComponent> getTooltip(int x, int y) {
                    var details = panel.tooltip(x, y);
                    return details.size() > 1
                            ? details.stream()
                                    .map(line -> ClientTooltipComponent.create(line.getVisualOrderText()))
                                    .toList()
                            : super.getTooltip(x, y);
                }
            });
        }
    }
}
