package gregtech.client.gui;

import gregtech.api.addon.machine.BehaviorMachine;
import gregtech.api.addon.machine.LayoutPart;
import gregtech.api.addon.platform.BehaviorState;
import gregtech.api.addon.platform.PartContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.ArrayList;
import java.util.List;

/** A part menu: its own slots, tanks and fields, using the behavior-machine widget sheet. */
public class PartScreen extends MachineContainerScreen<PartMenu> {
    private final ResourceLocation texture;
    private final LayoutPart part;
    private final List<Hover> hovers = new ArrayList<>();

    @Override protected ItemStack titleIcon() {
        var context = menu.runtime().context();
        return new ItemStack(context.level().getBlockState(context.pos()).getBlock());
    }

    public PartScreen(PartMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.part = menu.runtime().declaration();
        this.imageWidth = menu.layout().width;
        this.imageHeight = menu.layout().height;
        this.inventoryLabelY = imageHeight - 94;
        this.texture = ResourceLocation.tryParse(part.gui());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        hovers.clear();
        if (texture != null) graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int i = 0; i < menu.machineSlotCount(); i++) {
            Slot slot = menu.slots.get(i);
            blit(graphics, leftPos + slot.x - 1, topPos + slot.y - 1,
                    BehaviorMachine.UV_SLOT_U, BehaviorMachine.UV_SLOT_V,
                    BehaviorMachine.SLOT, BehaviorMachine.SLOT);
        }
        PartContext context = menu.runtime().context();
        for (int tank = 0; tank < context.tanks(); tank++) drawTank(graphics, tank, context);
        drawFields(graphics, context.state());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        for (Hover hover : hovers) {
//? if >=1.21.1 {
/*            if (hover.contains(mouseX, mouseY))
                graphics.renderTooltip(font, hover.lines, java.util.Optional.empty(), mouseX, mouseY);
*///?} else {
            if (hover.contains(mouseX, mouseY)) graphics.renderComponentTooltip(font, hover.lines, mouseX, mouseY);
//?}
        }
    }

    private void drawTank(GuiGraphics graphics, int tank, PartContext context) {
        int left = leftPos + menu.layout().tankX[tank];
        int top = topPos + menu.layout().tankY;
        blit(graphics, left, top, BehaviorMachine.UV_TANK_U, BehaviorMachine.UV_TANK_V,
                BehaviorMachine.TANK_WIDTH, BehaviorMachine.TANK_HEIGHT);
        FluidStack fluid = context.fluid(tank);
        int capacity = context.tankCapacity(tank);
        if (!fluid.isEmpty() && capacity > 0) {
            var extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
            int tint = extensions.getTintColor(fluid);
            float red = (tint >> 16 & 255) / 255.0F;
            float green = (tint >> 8 & 255) / 255.0F;
            float blue = (tint & 255) / 255.0F;
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(extensions.getStillTexture(fluid));
            int filled = (int) Math.max(1, Math.min(BehaviorMachine.TANK_HEIGHT - 2L,
                    fluid.getAmount() * (BehaviorMachine.TANK_HEIGHT - 2L) / capacity));
            int bottom = top + BehaviorMachine.TANK_HEIGHT - 1;
            int drawn = 0;
            while (drawn < filled) {
                int height = Math.min(16, filled - drawn);
//? if >=1.21.1 {
/*                graphics.blit(left + 1, bottom - drawn - height, 0, BehaviorMachine.TANK_WIDTH - 2, height,
                        sprite, red, green, blue, 1.0F);
*///?} else {
                graphics.setColor(red, green, blue, 1.0F);
                graphics.blit(left + 1, bottom - drawn - height, 0, BehaviorMachine.TANK_WIDTH - 2, height, sprite);
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
//?}
                drawn += height;
            }
        }
        blit(graphics, left, top, BehaviorMachine.UV_TANK_MARKS_U, BehaviorMachine.UV_TANK_V,
                BehaviorMachine.TANK_WIDTH, BehaviorMachine.TANK_HEIGHT);
        Component name = fluid.isEmpty() ? Component.translatable("tooltip.gt6ce.portable_fluid.empty") : fluid.getDisplayName();
        hovers.add(new Hover(left, top, BehaviorMachine.TANK_WIDTH, BehaviorMachine.TANK_HEIGHT,
                List.of(name, Component.literal(fluid.getAmount() + " / " + capacity))));
    }

    private void drawFields(GuiGraphics graphics, BehaviorState state) {
        int y = topPos + menu.layout().fieldY;
        for (String key : part.fields()) {
            graphics.drawString(font, key + " " + text(state, key), leftPos + 8, y, 0x404040, false);
            y += 10;
        }
    }

    private static String text(BehaviorState state, String key) {
        ItemStack stack = state.item(key);
        if (!stack.isEmpty()) return stack.getHoverName().getString();
        String written = state.getString(key);
        if (!written.isEmpty()) return written;
        if (state.getLong(key) != 0) return Long.toString(state.getLong(key));
        if (state.getBoolean(key)) return "true";
        return Integer.toString(state.getInt(key));
    }

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        if (texture != null && width > 0 && height > 0) graphics.blit(texture, x, y, u, v, width, height);
    }

    private record Hover(int x, int y, int width, int height, List<Component> lines) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
