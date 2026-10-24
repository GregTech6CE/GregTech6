package gregtech.client.gui;


//? if >=1.21.1 {
/**///?} else {
import gregtech.GregTech;
//?}

import gregtech.internal.fluid.FluidSlotAmountOverlay;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTFluidType;
import gregtech.content.registry.GTFluids;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.material.Fluid;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
//?}


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BasicMachineScreen extends AbstractContainerScreen<BasicMachineContainerMenu> implements FluidRecipeScreen {
    private final ResourceLocation texture;

    @Override public FluidStack fluidAt(int mouseX, int mouseY) {
        for (Slot slot : menu.slots) if (slot instanceof SlotFluid fluid
                && mouseX >= leftPos + slot.x && mouseX < leftPos + slot.x + 16
                && mouseY >= topPos + slot.y && mouseY < topPos + slot.y + 16) return fluid.fluid().copy();
        return FluidStack.EMPTY;
    }

    public BasicMachineScreen(BasicMachineContainerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.texture = resolveTexture(menu);
    }

    private static ResourceLocation resolveTexture(BasicMachineContainerMenu menu) {
        return gregtech.internal.recipe.MachineGuiLayout.texture(menu.recipeMap());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {


//? if >=1.21.1 {
/**///?} else {
        renderBackground(graphics);
//?}

        super.render(graphics, mouseX, mouseY, partialTick);
        renderFluidSlots(graphics);
        renderFluidTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && menu.programSlot() >= 0
                && hoveredSlot.index == menu.programSlot())
            graphics.renderTooltip(font, Component.translatable("gt.autocrafting.insert.blueprint"), mouseX, mouseY);
    }

    private void renderFluidTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (Slot slot : menu.slots) {
            if (!(slot instanceof SlotFluid sf)) continue;
            FluidStack fluid = sf.fluid();
            if (fluid.isEmpty()) continue;

            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            if (mouseX < x || mouseX >= x + 16 || mouseY < y || mouseY >= y + 16) continue;

            List<Component> tooltip = new ArrayList<>();
            Fluid f = fluid.getFluid();
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(f);
            // entryForFluid: keyed by the fluid, so it also resolves a flowing variant and survives
            // the three world waters reporting vanilla water's FluidType (see GTWorldWaterFluid).
            RegisteredFluids.FluidEntry entry = fluidId != null ? GTFluids.entryForFluid(f) : null;
            // Name line (GTFluidType.describeTooltip doesn't include the name)
            if (entry != null) {
                String path = RegisteredFluids.sanitizePath(entry.registryName());
                tooltip.add(GTFluidType.describe(entry, path));
            } else {
                tooltip.add(fluid.getDisplayName());
            }
            tooltip.addAll(GTFluidType.describeTooltip(fluid, fluid.getAmount(), false));
            graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
            break;
        }
    }

    private void renderFluidSlots(GuiGraphics graphics) {
        for (Slot slot : menu.slots) {
            if (!(slot instanceof SlotFluid sf)) continue;
            FluidStack fluid = sf.fluid();
            if (fluid.isEmpty()) continue;

            int x = leftPos + slot.x;
            int y = topPos + slot.y;

            IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(fluid.getFluid());
            ResourceLocation stillTex = props.getStillTexture(fluid);
            if (stillTex == null) continue;

            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTex);

            int color = props.getTintColor(fluid);
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            graphics.blit(x, y, 0, 16, 16, sprite, r, g, b, 1.0F);

            // Original FL.display(tank, true, true): capped bucket count, vanilla item overlay.
            String amount = FluidSlotAmountOverlay.text(fluid.getAmount());
            if (!amount.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                graphics.drawString(font, amount, FluidSlotAmountOverlay.x(x, font.width(amount)),
                        FluidSlotAmountOverlay.y(y), 0xFFFFFF, true);
                graphics.pose().popPose();
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Legacy machine layouts use the top row for input slots (for example the mixer).
        // Keep the title above the panel instead of drawing it through the inventory.
        graphics.drawString(font, title, titleLabelX, -font.lineHeight - 3, 0xFFFFFF, true);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, texture);
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Progress arrow overlay (UV 176,0 in the 256x256 texture, 20x18)
        int progress = menu.progressPercent();
        if (progress > 0) {
            int arrowWidth = Math.max(1, 20 * Math.min(progress, 95) / 95);
            graphics.blit(texture,
                    leftPos + 78, topPos + 24,  // render x, y
                    176, 0,                      // uv x, y
                    arrowWidth, 18,              // render width, height
                    256, 256);                   // texture size
        }
    }
}
