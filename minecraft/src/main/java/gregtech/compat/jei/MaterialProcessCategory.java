package gregtech.compat.jei;

import gregtech.client.MaterialProcessPanel;
import gregtech.compat.common.MaterialProcessDisplay.Page;
import gregtech.compat.common.MaterialProcessDisplay.Port;
import gregtech.content.recipe.MaterialProcessSnapshot;
import gregtech.engine.fluid.FluidDisplayBinding;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** JEI adapter only; producers register bounded pages through the normal JEI recipe registration/runtime API. */
public final class MaterialProcessCategory implements IRecipeCategory<Page> {
    public static final RecipeType<Page> TYPE = RecipeType.create("gt6ce", "material_process", Page.class);
    private final IDrawable icon;
    private final Map<Page, MaterialProcessPanel> views = new IdentityHashMap<>();
    private final Map<Page, IFocusGroup> viewFocuses = new IdentityHashMap<>();

    public MaterialProcessCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(Items.MAP));
    }

    @Override
    public RecipeType<Page> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gt6ce.jei.material_process.title");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return MaterialProcessPanel.preferredWidth();
    }

    @Override
    public int getHeight() {
        return MaterialProcessPanel.preferredHeight();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Page page, IFocusGroup focuses) {
        for (var port : page.indexedPorts()) {
            var ingredients = builder.addInvisibleIngredients(role(port));
            for (var icon : port.icons()) {
                if (!FluidDisplayBinding.hasPayload(icon)) ingredients.addItemStack(icon);
                else {
//? if >=1.21.1 {
                    /*var fluid = FluidDisplayBinding.resolveCurrent(icon);
                    if (!fluid.isEmpty()) ingredients.addIngredient(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, fluid);
*///?} else {
                    var fluid = FluidDisplayBinding.resolve(icon);
                    if (!fluid.isEmpty()) ingredients.addIngredient(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, fluid);
//?}
                }
            }
        }
    }

    private static RecipeIngredientRole role(Port port) {
        return switch (port.role()) {
            case CONSUMED -> RecipeIngredientRole.INPUT;
            case CATALYST, PRESENCE -> RecipeIngredientRole.CATALYST;
            case PRIMARY, BYPRODUCT, UNKNOWN -> RecipeIngredientRole.OUTPUT;
        };
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Page page, IFocusGroup focuses) {
        var panel = view(page);
        var identities = new HashSet<String>();
        focuses.getItemStackFocuses().forEach(focus -> identities.add(
                MaterialProcessSnapshot.itemDisplayState(focus.getTypedValue().getIngredient())));
//? if >=1.21.1 {
        /*focuses.getFocuses(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK).forEach(focus -> identities.add(
                MaterialProcessSnapshot.fluidDisplayState(focus.getTypedValue().getIngredient())));
*///?} else {
        focuses.getFocuses(mezz.jei.api.forge.ForgeTypes.FLUID_STACK).forEach(focus -> identities.add(
                MaterialProcessSnapshot.fluidDisplayState(focus.getTypedValue().getIngredient())));
//?}
        if (viewFocuses.put(page, focuses) != focuses) panel.focus(port -> identities.contains(port.identity()));
        builder.addGuiEventListener(new IJeiGuiEventListener() {
            @Override
            public ScreenRectangle getArea() {
                return new ScreenRectangle(0, 0, panel.width(), panel.height());
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return panel.click(mouseX, mouseY, button) || panel.press(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                return panel.drag(button, dragX, dragY);
            }

//? if >=1.21.1 {
            /*@Override
            public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
                return panel.scroll(mouseX, mouseY, scrollY);
            }
*///?} else {
            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
                return panel.scroll(mouseX, mouseY, delta);
            }
//?}
        });
    }

    @Override
    public void draw(Page page, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        view(page).draw(graphics, mouseX, mouseY);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, Page page, IRecipeSlotsView slots, double mouseX, double mouseY) {
        tooltip.addAll(view(page).tooltip(mouseX, mouseY));
    }

    private MaterialProcessPanel view(Page page) {
        var panel = views.computeIfAbsent(page, MaterialProcessPanel::new);
        panel.resize(getWidth(), getHeight());
        return panel;
    }
}
