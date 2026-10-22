package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.api.addon.BehaviorMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.ArrayList;
import java.util.List;
//? if >=1.21.1 {
/*import java.util.Optional;
*///?} else {
//?}

/** Draws a behavior machine from its declaration and the addon's GUI sheet. */
public class BehaviorMachineScreen extends AbstractContainerScreen<BehaviorMachineMenu> implements FluidRecipeScreen {
    @Override public FluidStack fluidAt(int mouseX, int mouseY) {
        for (int i = 0; i < declaration.fluids().size(); i++) {
            var tank = declaration.fluids().get(i);
            if (mouseX >= leftPos + tank.x() && mouseX < leftPos + tank.x() + BehaviorMachine.TANK_WIDTH
                    && mouseY >= topPos + tank.y() && mouseY < topPos + tank.y() + BehaviorMachine.TANK_HEIGHT)
                return menu.tankFluid(i).copy();
        }
        return FluidStack.EMPTY;
    }
    private final ResourceLocation texture;
    private final BehaviorMachine declaration;
    private final List<Hover> hovers = new ArrayList<>();
    private final List<ToggleButton> toggles = new ArrayList<>();

    public BehaviorMachineScreen(BehaviorMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.declaration = menu.machine().declaration();
        this.imageWidth = declaration.guiWidth();
        this.imageHeight = declaration.guiHeight();
        this.inventoryLabelY = imageHeight - 94;
        this.texture = ResourceLocation.tryParse(declaration.gui());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        hovers.clear();
        toggles.clear();
        if (texture != null) graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int i = 0; i < menu.machineSlotCount(); i++) {
            Slot slot = menu.slots.get(i);
            blit(graphics, leftPos + slot.x - 1, topPos + slot.y - 1,
                    BehaviorMachine.UV_SLOT_U, BehaviorMachine.UV_SLOT_V,
                    BehaviorMachine.SLOT, BehaviorMachine.SLOT);
        }
        drawEnergy(graphics);
        for (int tank = 0; tank < declaration.fluids().size(); tank++) drawTank(graphics, tank);
        drawProgress(graphics);
        for (int toggle = 0; toggle < declaration.toggles().size(); toggle++) drawToggle(graphics, toggle);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        for (Hover hover : hovers) {
//? if >=1.21.1 {
/*            if (hover.contains(mouseX, mouseY))
                graphics.renderTooltip(font, hover.lines, Optional.empty(), mouseX, mouseY);
*///?} else {
            if (hover.contains(mouseX, mouseY)) graphics.renderComponentTooltip(font, hover.lines, mouseX, mouseY);
//?}
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (ToggleButton toggle : toggles) {
                if (toggle.area.contains(mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, toggle.id);
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawEnergy(GuiGraphics graphics) {
        if (declaration.energy() == null) return;
        int left = leftPos + declaration.energyX();
        int top = topPos + declaration.energyY();
        blit(graphics, left, top, BehaviorMachine.UV_ENERGY_U, BehaviorMachine.UV_ENERGY_V,
                BehaviorMachine.ENERGY_WIDTH, BehaviorMachine.ENERGY_HEIGHT);
        long max = menu.machine().energyBarMax();
        int filled = scaled(menu.energy(), max, BehaviorMachine.ENERGY_HEIGHT);
        blit(graphics, left, top + BehaviorMachine.ENERGY_HEIGHT - filled,
                BehaviorMachine.UV_ENERGY_FULL_U, BehaviorMachine.ENERGY_HEIGHT - filled,
                BehaviorMachine.ENERGY_WIDTH, filled);
        hovers.add(new Hover(left, top, BehaviorMachine.ENERGY_WIDTH, BehaviorMachine.ENERGY_HEIGHT,
                List.of(Component.translatable("gui.gt6ce.behavior.energy", menu.energy(), max, declaration.energy().type()))));
    }

    private void drawTank(GuiGraphics graphics, int tank) {
        var slot = declaration.fluids().get(tank);
        int left = leftPos + slot.x();
        int top = topPos + slot.y();
        blit(graphics, left, top, BehaviorMachine.UV_TANK_U, BehaviorMachine.UV_TANK_V,
                BehaviorMachine.TANK_WIDTH, BehaviorMachine.TANK_HEIGHT);
        FluidStack fluid = menu.tankFluid(tank);
        int amount = menu.tankAmount(tank);
        int filled = scaled(amount, slot.capacity(), BehaviorMachine.TANK_HEIGHT - 2);
        if (filled > 0 && !fluid.isEmpty()) {
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
//? if >=1.21.1 {
/*            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(extensions.getStillTexture(fluid));
            int tint = extensions.getTintColor(fluid);
            float red = ((tint >> 16) & 0xFF) / 255.0F;
            float green = ((tint >> 8) & 0xFF) / 255.0F;
            float blue = (tint & 0xFF) / 255.0F;
*///?} else {
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture());
            int tint = extensions.getTintColor();
            graphics.setColor(((tint >> 16) & 0xFF) / 255.0F, ((tint >> 8) & 0xFF) / 255.0F, (tint & 0xFF) / 255.0F, 1.0F);
//?}
            int bottom = top + BehaviorMachine.TANK_HEIGHT - 1;
            for (int drawn = 0; drawn < filled; drawn += 16) {
                int height = Math.min(16, filled - drawn);
//? if >=1.21.1 {
/*                graphics.blit(left + 1, bottom - drawn - height, 0, BehaviorMachine.TANK_WIDTH - 2, height,
                        sprite, red, green, blue, 1.0F);
*///?} else {
                graphics.blit(left + 1, bottom - drawn - height, 0, BehaviorMachine.TANK_WIDTH - 2, height, sprite);
//?}
            }
//? if >=1.21.1 {
/**///?} else {
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
//?}
        }
        blit(graphics, left, top, BehaviorMachine.UV_TANK_MARKS_U, BehaviorMachine.UV_TANK_V,
                BehaviorMachine.TANK_WIDTH, BehaviorMachine.TANK_HEIGHT);
        Component name = fluid.isEmpty() ? Component.translatable("gui.gt6ce.empty") : fluid.getDisplayName();
        hovers.add(new Hover(left, top, BehaviorMachine.TANK_WIDTH, BehaviorMachine.TANK_HEIGHT,
                List.of(name, Component.translatable("gui.gt6ce.behavior.tank", amount, slot.capacity()))));
    }

    private void drawProgress(GuiGraphics graphics) {
        int left = leftPos + declaration.progress().x();
        int top = topPos + declaration.progress().y();
        blit(graphics, left, top, BehaviorMachine.UV_ARROW_U, BehaviorMachine.UV_ARROW_V,
                BehaviorMachine.PROGRESS_WIDTH, BehaviorMachine.PROGRESS_HEIGHT);
        int filled = Math.round(menu.progress() * BehaviorMachine.PROGRESS_WIDTH);
        blit(graphics, left, top, BehaviorMachine.UV_ARROW_U, BehaviorMachine.UV_ARROW_FULL_V,
                filled, BehaviorMachine.PROGRESS_HEIGHT);
        hovers.add(new Hover(left, top, BehaviorMachine.PROGRESS_WIDTH, BehaviorMachine.PROGRESS_HEIGHT,
                List.of(Component.translatable("gui.gt6ce.behavior.progress", Math.round(menu.progress() * 100)))));
    }

    private void drawToggle(GuiGraphics graphics, int toggle) {
        var placed = declaration.toggles().get(toggle);
        int left = leftPos + placed.x();
        int top = topPos + placed.y();
        boolean on = menu.isOn(toggle);
        blit(graphics, left, top, on ? BehaviorMachine.UV_TOGGLE_ON_U : BehaviorMachine.UV_TOGGLE_OFF_U,
                BehaviorMachine.UV_TOGGLE_V, BehaviorMachine.SLOT, BehaviorMachine.SLOT);
        Hover area = new Hover(left, top, BehaviorMachine.SLOT, BehaviorMachine.SLOT, List.of(
                Component.translatable(placed.translationKey()),
                Component.translatable(on ? "gui.gt6ce.behavior.on" : "gui.gt6ce.behavior.off")));
        hovers.add(area);
        toggles.add(new ToggleButton(area, toggle));
    }

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        if (texture != null && width > 0 && height > 0) graphics.blit(texture, x, y, u, v, width, height);
    }

    private static int scaled(long value, long max, int size) {
        if (value <= 0 || max <= 0) return 0;
        return (int) Math.max(1, Math.min(size, value * size / max));
    }

    private record Hover(int x, int y, int width, int height, List<Component> lines) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record ToggleButton(Hover area, int id) {}
}
