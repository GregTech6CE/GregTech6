package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.tool.CoinGeometry;
import com.gregtech.gregtech.item.CoinItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the minted item's actual NBT die; the untagged coin still uses its baked default model. */
public final class CoinItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static CoinItemRenderer instance;

    private CoinItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static CoinItemRenderer instance() {
        if (instance == null) instance = new CoinItemRenderer();
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof CoinItem coin)) return;
        CoinGeometry.MintPattern pattern = CoinGeometry.pattern(stack);
        var boxes = pattern.inventoryPixelBoxes();
        int color = coin.getMaterial().getColor();
        // As in coin_minted.json: base Y=7/16, top Y=(9-depth/4)/16. Render
        // the same two GT6 textures and face-culling rule as a placed coin pile.
        for (int pass = 0; pass < 2; pass++) {
            var out = buffers.getBuffer(RenderType.entityCutout(
                    pass == 0 ? CoinPileRenderer.TEXTURE : CoinPileRenderer.SIDE_TEXTURE));
            int index = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                if (pattern.pixelDepth(x, z) == 3) continue;
                var box = boxes.get(index++);
                if (pass == 0) {
                    CoinPileRenderer.quad(pose.last(), out, box, color, light, Direction.DOWN, 0, 0);
                    CoinPileRenderer.quad(pose.last(), out, box, color, light, Direction.UP, 0, 0);
                } else {
                    for (Direction side : Direction.Plane.HORIZONTAL)
                        if (pattern.isExposedSide(x, z, side))
                            CoinPileRenderer.quad(pose.last(), out, box, color, light, side, 0, 0);
                }
            }
        }
    }
}
