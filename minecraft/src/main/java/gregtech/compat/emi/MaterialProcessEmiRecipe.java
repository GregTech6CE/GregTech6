package gregtech.compat.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import gregtech.client.MaterialProcessPanel;
import gregtech.compat.common.MaterialProcessDisplay;
import gregtech.compat.common.RecipeViewerIcons;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** EMI page adapter over the same bounded process display used by JEI. */
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
    private final List<MaterialProcessDisplay.Page> queryPages;
    private final int queryPage;
    private final ResourceLocation id;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;

    public MaterialProcessEmiRecipe(MaterialProcessDisplay.Page page) {
        this(List.of(page), 0);
    }

    public MaterialProcessEmiRecipe(List<MaterialProcessDisplay.Page> pages) {
        this(pages, 0);
    }

    private MaterialProcessEmiRecipe(List<MaterialProcessDisplay.Page> pages, int index) {
        queryPages = List.copyOf(pages);
        queryPage = index;
        this.page = queryPages.get(index);
        id = ResourceLocation.fromNamespaceAndPath(
                "gt6ce",
                "/material_process/"
                        + Integer.toUnsignedString(page.title().getString().hashCode(), 36)
                        + "/" + page.number()
                        + "/"
                        + page.steps().stream()
                                .map(step -> step.reference().toString())
                                .findFirst()
                                .orElse("empty")
                                .hashCode());
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
                .map(EmiStack::of)
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
        for (int row = 0; row < page.steps().size(); row++) {
            var step = page.steps().get(row);
            for (var slot : MaterialProcessPanel.slots(step, row)) {
                var port = slot.port();
                if (!port.hasIcons()) continue;
                var ingredient = EmiIngredient.of(stacks(port));
                var widget = widgets.add(new PortSlot(ingredient, slot.x(), slot.y(), page, port));
                if (!slot.output())
                    widget.catalyst(port.role() == MaterialProcessDisplay.Role.CATALYST
                            || port.role() == MaterialProcessDisplay.Role.PRESENCE);
            }
        }
        widgets.add(new EmiInteractiveWidget() {
            @Override
            public Bounds getBounds() {
                return new Bounds(0, 0, MaterialProcessPanel.WIDTH, MaterialProcessPanel.HEIGHT);
            }

            @Override
            public void render(GuiGraphics graphics, int x, int y, float delta) {
                EmiWidgetInput.rendered(this, graphics);
                MaterialProcessPanel.draw(page, graphics, x, y);
            }

            @Override
            public boolean mouseClicked(int x, int y, int button) {
                if (queryPages.size() > 1 && pageCounterContains(x, y) && (button == 0 || button == 1)) {
                    int next = Math.floorMod(queryPage + (button == 0 ? 1 : -1), queryPages.size());
                    dev.emi.emi.api.EmiApi.displayRecipe(new MaterialProcessEmiRecipe(queryPages, next));
                    return true;
                }
                return MaterialProcessPanel.click(page, x, y, button);
            }

            @Override
            public List<ClientTooltipComponent> getTooltip(int x, int y) {
                if (queryPages.size() > 1 && pageCounterContains(x, y))
                    return List.of(ClientTooltipComponent.create(
                            Component.translatable("gt6ce.jei.material_process.page_controls")
                                    .getVisualOrderText()));
                return MaterialProcessPanel.tooltip(page, x, y).stream()
                        .map(line -> ClientTooltipComponent.create(line.getVisualOrderText()))
                        .toList();
            }
        });
    }

    private static boolean pageCounterContains(int x, int y) {
        return x >= MaterialProcessPanel.WIDTH - 38 && x < MaterialProcessPanel.WIDTH && y >= 0 && y < 12;
    }

    private static final class PortSlot extends SlotWidget {
        private final MaterialProcessDisplay.Page page;
        private final MaterialProcessDisplay.Port port;

        private PortSlot(
                EmiIngredient stack, int x, int y, MaterialProcessDisplay.Page page, MaterialProcessDisplay.Port port) {
            super(stack, x, y);
            this.page = page;
            this.port = port;
            drawBack(false);
            for (Component line : port.tooltip()) appendTooltip(line);
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            if ((button == 0 || button == 1)
                    && getBounds().contains(mouseX, mouseY)
                    && page.navigation().openForm(port.resource(), button == 1)) return true;
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }
}
