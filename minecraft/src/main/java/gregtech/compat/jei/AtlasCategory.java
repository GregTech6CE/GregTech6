package gregtech.compat.jei;

import gregtech.compat.common.AtlasPanel;
import gregtech.compat.common.MaterialAtlas;
import static mezz.jei.api.constants.VanillaTypes.ITEM_STACK;
//? if >=1.21.1 {
/*import static mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK;
*///?} else {
import static mezz.jei.api.forge.ForgeTypes.FLUID_STACK;
//?}
import java.util.function.Supplier;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class AtlasCategory implements IRecipeCategory<MaterialAtlas.Page> {
    private final MaterialAtlas.Kind kind;
    private final RecipeType<MaterialAtlas.Page> type;
    private final IDrawable background, icon;
    private final Supplier<IJeiRuntime> runtime;

    AtlasCategory(IGuiHelper gui, MaterialAtlas.Kind kind, Supplier<IJeiRuntime> runtime) {
        this.kind = kind;
        this.runtime = runtime;
        this.type = type(kind);
        this.background = new IDrawable() {
            @Override public int getWidth() { return AtlasPanel.WIDTH; }
            @Override public int getHeight() { return panelHeight(); }
            @Override public void draw(GuiGraphics g, int x, int y) {}
        };
        this.icon = gui.createDrawableItemStack(kind.icon());
    }
    static RecipeType<MaterialAtlas.Page> type(MaterialAtlas.Kind kind) {
        return RecipeType.create("gt6ce", kind.id, MaterialAtlas.Page.class);
    }
    @Override public RecipeType<MaterialAtlas.Page> getRecipeType() { return type; }
    @Override public Component getTitle() { return kind.title(); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, MaterialAtlas.Page page, IFocusGroup focuses) {
        var stacks = page.lookup().stream().filter(stack -> MaterialAtlas.toolMaterial(stack) == null).toList();
        var inputs = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(stacks);
        var outputs = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(stacks);
        if (kind == MaterialAtlas.Kind.ORE) outputs.addItemStacks(page.byproducts());
        for (var stack : stacks) {
            var fluid = MaterialAtlas.displayedFluid(stack);
            if (!fluid.isEmpty()) {
                inputs.addIngredient(FLUID_STACK, fluid);
                outputs.addIngredient(FLUID_STACK, fluid);
            }
        }
    }

    private void open(ItemStack stack, boolean uses) {
        var jei = runtime.get();
        if (jei == null) return;
        var factory = jei.getJeiHelpers().getFocusFactory();
        var role = uses ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
        var fluid = MaterialAtlas.displayedFluid(stack);
        if (fluid.isEmpty()) jei.getRecipesGui().show(factory.createFocus(role, ITEM_STACK, stack));
        else jei.getRecipesGui().show(factory.createFocus(role, FLUID_STACK, fluid));
    }
    private int panelHeight() {
        var client = net.minecraft.client.Minecraft.getInstance();
        return Math.max(64, Math.min(256, client.getWindow().getGuiScaledHeight() - 104));
    }

    public boolean needsRecipeBorder() { return false; }

    private void openProcess(MaterialAtlas.Preview preview) {
        var jei = runtime.get();
        if (jei == null) return;
        if (preview instanceof MaterialAtlas.MachineRow row) {
            show(jei, RecipeMapCategory.recipeType(row.map()), row.recipe());
        } else if (preview instanceof MaterialAtlas.Assembly assembly) {
            if (assembly.tool() != null) show(jei, ToolAssemblyCategory.TYPE, assembly.tool());
//? if >=1.21.1 {
            /*else show(jei, mezz.jei.api.constants.RecipeTypes.CRAFTING,
                    new net.minecraft.world.item.crafting.RecipeHolder<>(assembly.id(), assembly.crafting()));
*///?} else {
            else show(jei, mezz.jei.api.constants.RecipeTypes.CRAFTING, assembly.crafting());
//?}
        }
    }

    private static <T> void show(IJeiRuntime jei, RecipeType<T> type, T recipe) {
        var category = jei.getRecipeManager().getRecipeCategory(type);
        jei.getRecipesGui().showRecipes(category, java.util.List.of(recipe), java.util.List.of());
    }

    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, MaterialAtlas.Page page, IFocusGroup focuses) {
        int height = panelHeight();
        var panel = new AtlasPanel(page, height, this::openProcess, () -> {
            var jei = runtime.get();
            if (jei == null) return;
            var category = jei.getRecipeManager().getRecipeCategory(type(MaterialAtlas.Kind.MATERIAL));
            jei.getRecipesGui().showRecipes(category,
                    java.util.List.of(new MaterialAtlas.Page(MaterialAtlas.Kind.MATERIAL, page.entry())), java.util.List.of());
        });
        builder.addWidget(new mezz.jei.api.gui.widgets.IRecipeWidget() {
            @Override public void drawWidget(GuiGraphics g, double x, double y) {
                panel.draw(g, x, y);
            }
            @Override public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip, double x, double y) {
                var stack = panel.hovered(x, y);
                if (stack.isEmpty()) return;
                var jei = runtime.get();
                var fluid = MaterialAtlas.displayedFluid(stack);
                var client = net.minecraft.client.Minecraft.getInstance();
                if (jei != null && !fluid.isEmpty()) {
                    var manager = jei.getIngredientManager();
                    manager.createTypedIngredient(FLUID_STACK, fluid).ifPresent(tooltip::setIngredient);
                    tooltip.addAll(gregtech.client.gui.FluidSlotPresentation.viewerTooltip(fluid, fluid.getAmount()));
                } else {
                    if (jei != null) jei.getIngredientManager().createTypedIngredient(ITEM_STACK, stack)
                            .ifPresent(tooltip::setIngredient);
                    tooltip.addAll(net.minecraft.client.gui.screens.Screen.getTooltipFromItem(client, stack));
                }
                if (panel.workstationAt(x, y) != null) tooltip.add(MaterialAtlas.label("open_process"));
                var card = panel.card(x, y);
                if (card != null) tooltip.add(card);
            }
            @Override public net.minecraft.client.gui.navigation.ScreenPosition getPosition() {
                return new net.minecraft.client.gui.navigation.ScreenPosition(0, 0);
            }
        });
        builder.addGuiEventListener(new IJeiGuiEventListener() {
            private boolean dragging;
            @Override public ScreenRectangle getArea() { return new ScreenRectangle(0, 0, AtlasPanel.WIDTH, height); }
            @Override public boolean keyPressed(double x, double y, int key, int scan, int mods) {
                var jei = runtime.get();
                var stack = panel.hovered(x, y);
                if (jei == null || stack.isEmpty()) return false;
                var input = com.mojang.blaze3d.platform.InputConstants.getKey(key, scan);
                var mappings = jei.getKeyMappings();
                boolean uses = mappings.getShowUses().isActiveAndMatches(input);
                if (!uses && !mappings.getShowRecipe().isActiveAndMatches(input)) return false;
                open(stack, uses);
                return true;
            }
            @Override public boolean mouseClicked(double x, double y, int button) {
                if (button != 0 && button != 1 || net.minecraft.client.gui.screens.Screen.hasShiftDown()
                        || net.minecraft.client.gui.screens.Screen.hasControlDown()
                        || net.minecraft.client.gui.screens.Screen.hasAltDown()) return false;
                dragging = button == 0 && panel.scrollbar(x, y);
                if (dragging || button == 0 && panel.clickControl(x, y)) return true;
                var stack = panel.hovered(x, y);
                if (stack.isEmpty()) return false;
                open(stack, button == 1);
                return true;
            }
            @Override public boolean mouseReleased(double x, double y, int button) {
                boolean wasDragging = dragging;
                dragging = false;
                return wasDragging;
            }
            @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
                if (!dragging) return false;
                panel.drag(dy);
                return true;
            }
//? if >=1.21.1 {
            /*@Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return panel.wheel(y, dy); }
*///?} else {
            @Override public boolean mouseScrolled(double x, double y, double delta) { return panel.wheel(y, delta); }
//?}
        });
    }
}
