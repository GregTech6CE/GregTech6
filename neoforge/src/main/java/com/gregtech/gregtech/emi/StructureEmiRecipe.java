package com.gregtech.gregtech.emi;

import com.gregtech.gregtech.jei.*;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;

/** Actual controller geometry and parts, available without JEI installed. */
public final class StructureEmiRecipe implements EmiRecipe {
    private static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("gregtech", "multiblock_assembly"),
            EmiStack.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BRICKS))) {
        @Override public Component getName() { return Component.translatable("gregtech.jei.assembly"); }
    };
    private final MultiblockInfoData.Info info;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;
    private final Map<net.minecraft.core.BlockPos, net.minecraft.world.item.ItemStack> blocks;
    private final List<Integer> floors;
    private final PreviewCamera camera = new PreviewCamera();
    private int layer = -1;
    public StructureEmiRecipe(MultiblockInfoData.Info info) {
        this.info = info;
        inputs = MultiblockInfoData.parts(info).stream().map(stack -> (EmiIngredient)EmiStack.of(stack)).toList();
        outputs = List.of(EmiStack.of(info.controller()));
        blocks = MultiblockInfoData.previewBlocks(info);
        floors = PreviewLayers.levels(info.cells().keySet());
    }
    public static void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        for (var info : MultiblockInfoData.recipes()) {
            registry.addRecipe(new StructureEmiRecipe(info));
            registry.addWorkstation(CATEGORY, EmiStack.of(info.controller()));
        }
    }
    @Override public EmiRecipeCategory getCategory() { return CATEGORY; }
    @Override public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath("gregtech",
            "/structure/" + BuiltInRegistries.ITEM.getKey(info.controller().getItem()).getPath()); }
    @Override public List<EmiIngredient> getInputs() { return inputs; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public boolean supportsRecipeTree() { return false; }
    @Override public boolean hideCraftable() { return true; }
    @Override public int getDisplayWidth() { return 176; }
    @Override public int getDisplayHeight() { return 100; }
    public MultiblockInfoData.Info info() { return info; }
    @Override public void addWidgets(WidgetHolder widgets) {
        camera.reset(); layer = -1;
        widgets.addSlot(outputs.get(0), 0, 0).recipeContext(this);
        for (int i = 0; i < inputs.size(); i++) widgets.addSlot(inputs.get(i), 20 + i * 19, 0);
        widgets.addDrawable(0, 20, 176, 54, (g, mx, my, delta) -> {
            g.pose().pushPose();
            g.pose().translate(0, -20, 0);
            StructurePreview.draw(g, blocks, camera, layer < 0 ? null : floors.get(layer), 0, 20, 176, 74);
            g.pose().popPose();
        });
        String[] labels = {"<", ">", "-", "+", "All", "R", "-", "+"};
        int[] positions = {0, 20, 40, 60, 80, 108, 130, 150};
        for (int i = 0; i < labels.length; i++) {
            final int action = i;
            final String label = labels[i];
            final int x = positions[i], width = i == 4 ? 26 : 18;
            widgets.add(new ButtonWidget(x, 76, width, 14, 0, 0, () -> true,
                    (mouseX, mouseY, button) -> { if (button == 0) click(action); }) {
                @Override public void render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float delta) {
                    boolean hover = getBounds().contains(mouseX, mouseY);
                    g.fill(x, 76, x + width, 90, 0xff666666);
                    g.fill(x + 1, 77, x + width - 1, 89, hover ? 0xffeeeeee : 0xffcccccc);
                    var font = Minecraft.getInstance().font;
                    g.drawString(font, label, x + (width - font.width(label)) / 2, 79, 0xff333333, false);
                }
            });
        }
        widgets.addDrawable(6, 91, 164, 9, (g, x, y, delta) -> g.drawString(Minecraft.getInstance().font,
                Component.translatable(layer < 0 ? "gregtech.jei.preview.all" : "gregtech.jei.preview.layer", layer + 1), 0, 0, 0xff555555, false));
        widgets.addDrawable(0, 76, 176, 14, (g, x, y, delta) -> {
            if (x >= 0 && x < 176 && y >= 0 && y < 14)
                g.renderTooltip(Minecraft.getInstance().font, Component.translatable("gregtech.emi.preview.help"), x, y);
        });
    }
    private void click(int action) {
        switch (action) {
            case 0 -> camera.rotate(-90, 0);
            case 1 -> camera.rotate(90, 0);
            case 2 -> layer = layer < 0 ? floors.size() - 1 : Math.floorMod(layer - 1, floors.size());
            case 3 -> layer = layer < 0 ? 0 : (layer + 1) % floors.size();
            case 4 -> layer = -1;
            case 5 -> { camera.reset(); layer = -1; }
            case 6 -> camera.zoom(-1);
            case 7 -> camera.zoom(1);
        }
    }
}
