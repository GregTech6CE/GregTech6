package gregtech.compat.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.*;
import gregtech.compat.common.AtlasPanel;
import gregtech.compat.common.MaterialAtlas;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class AtlasEmiRecipe implements EmiRecipe {
    private static final Comparison TOOL_HEAD =
            Comparison.compareData(stack -> MaterialAtlas.toolMaterial(stack.getItemStack()));
    private final MaterialAtlas.Page page;
    private final EmiRecipeCategory category;
    private final List<EmiStack> lookups;
    private final List<EmiStack> outputs;

    private AtlasEmiRecipe(MaterialAtlas.Page page, EmiRecipeCategory category) {
        this.page = page;
        this.category = category;
        var stacks = new java.util.ArrayList<EmiStack>();
        for (var stack : page.lookup()) {
            var item = EmiStack.of(stack);
            if (MaterialAtlas.toolMaterial(stack) != null) item.comparison(TOOL_HEAD);
            stacks.add(item);
            var fluid = MaterialAtlas.displayedFluid(stack);
            if (!fluid.isEmpty()) stacks.add(MachineEmiRecipe.fluid(fluid));
        }
        this.lookups = List.copyOf(stacks);
        if (isOreChart()) page.byproducts().forEach(s -> stacks.add(EmiStack.of(s)));
        this.outputs = List.copyOf(stacks);
    }

    public boolean isOreChart() {
        return page.kind() == MaterialAtlas.Kind.ORE;
    }

    private static EmiStack ingredient(net.minecraft.world.item.ItemStack stack) {
        var fluid = MaterialAtlas.displayedFluid(stack);
        return fluid.isEmpty()
                ? EmiStack.of(stack)
                : new FluidDisplayEmiStack(MachineEmiRecipe.fluid(fluid), EmiStack.of(stack));
    }

    static void register(EmiRegistry registry, MaterialAtlas atlas) {
        for (var kind : MaterialAtlas.Kind.values()) {
            var category =
                    new EmiRecipeCategory(
                            ResourceLocation.fromNamespaceAndPath("gt6ce", kind.id), EmiStack.of(kind.icon())) {
                        @Override
                        public Component getName() {
                            return kind.title();
                        }
                    };
            registry.addCategory(category);
            for (var page : atlas.pages(kind)) registry.addRecipe(new AtlasEmiRecipe(page, category));
        }
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(
                "gt6ce", "/" + page.kind().id + "/" + page.entry().material.getId());
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.copyOf(lookups);
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
    public int getDisplayWidth() {
        return AtlasPanel.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        // EMI clips this request to its configured recipe space, then supplies that height to the widget.
        return net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        var panel = new AtlasPanel(
                page,
                widgets.getHeight(),
                preview -> {
                    var target = EmiApi.getRecipeManager().getRecipe(preview.id());
                    if (target != null) EmiApi.displayRecipe(target);
                },
                () -> {
                    var id = ResourceLocation.fromNamespaceAndPath(
                            "gt6ce",
                            "/" + MaterialAtlas.Kind.MATERIAL.id + "/"
                                    + page.entry().material.getId());
                    var material = EmiApi.getRecipeManager().getRecipe(id);
                    if (material != null) EmiApi.displayRecipe(material);
                });
        class AtlasSlot extends SlotWidget implements EmiWidgetInput.Input {
            private int mouseX, mouseY;

            AtlasSlot() {
                super(EmiStack.EMPTY, 0, 0);
            }

            @Override
            public EmiIngredient getStack() {
                return ingredient(panel.hovered(mouseX, mouseY));
            }

            @Override
            public Bounds getBounds() {
                return new Bounds(0, 0, AtlasPanel.WIDTH, widgets.getHeight());
            }

            @Override
            public void render(GuiGraphics g, int x, int y, float delta) {
                EmiWidgetInput.rendered(this, g);
                mouseX = x;
                mouseY = y;
                panel.draw(g, x, y);
            }

            @Override
            public List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> getTooltip(
                    int x, int y) {
                var stack = panel.hovered(x, y);
                if (stack.isEmpty()) return List.of();
                var lines = new java.util.ArrayList<>(ingredient(stack).getTooltip());
                if (panel.workstationAt(x, y) != null)
                    lines.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(
                            MaterialAtlas.label("open_process").getVisualOrderText()));
                var card = panel.card(x, y);
                if (card != null) lines.add(card);
                return lines;
            }

            @Override
            public boolean keyPressed(int key, int scan, int modifiers) {
                if (getStack().isEmpty()) return false;
                var screen = net.minecraft.client.Minecraft.getInstance().screen;
                boolean handled = super.keyPressed(key, scan, modifiers);
                if (handled) focusInResults(screen);
                return handled;
            }

            private void focusInResults(net.minecraft.client.gui.screens.Screen previousScreen) {
                if (net.minecraft.client.Minecraft.getInstance().screen == previousScreen) return;
                var card = panel.card(mouseX, mouseY);
                if (card == null) return;
                var target = EmiApi.getRecipeManager().getRecipe(card.preview().id());
                if (target != null) EmiApi.focusRecipe(target);
            }

            @Override
            public boolean pressed(double x, double y, int button) {
                return button == 0 && panel.scrollbar(x, y);
            }

            @Override
            public boolean dragged(int button, double dx, double dy) {
                panel.drag(dy);
                return true;
            }

            @Override
            public boolean scrolled(double x, double y, double delta) {
                return panel.wheel(y, delta);
            }

            @Override
            public boolean mouseClicked(int x, int y, int button) {
                mouseX = x;
                mouseY = y;
                if (button == 0
                        && !net.minecraft.client.gui.screens.Screen.hasShiftDown()
                        && !net.minecraft.client.gui.screens.Screen.hasControlDown()
                        && !net.minecraft.client.gui.screens.Screen.hasAltDown()
                        && panel.clickControl(x, y)) return true;
                if (getStack().isEmpty()) return false;
                var screen = net.minecraft.client.Minecraft.getInstance().screen;
                boolean handled = super.mouseClicked(x, y, button);
                if (handled) focusInResults(screen);
                return handled;
            }
        }
        widgets.add(new AtlasSlot());
    }
}
