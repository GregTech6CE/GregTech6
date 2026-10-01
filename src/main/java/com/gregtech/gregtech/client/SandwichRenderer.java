package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.misc.SandwichBlockEntity;
import com.gregtech.gregtech.content.food.SandwichIngredients;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/** GT6's stacked sandwich: each occupied pixel slot renders its own ingredient texture and shape. */
public final class SandwichRenderer implements BlockEntityRenderer<SandwichBlockEntity> {
    public SandwichRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(SandwichBlockEntity sandwich, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        for (int slot = 0; slot < SandwichBlockEntity.SLOTS; slot++) {
            var layer = SandwichIngredients.of(sandwich.layerId(slot));
            if (layer == null) continue;
            for (AABB box : boxes(sandwich, slot, layer)) {
                // A BufferSource may share a builder between entityCutout render types.
                var top = buffers.getBuffer(RenderType.entityCutout(layer.texture(true)));
                AnvilCuboidRenderer.drawSurface(pose.last(), top, box, layer.tint(), light, true);
                var sides = buffers.getBuffer(RenderType.entityCutout(layer.texture(false)));
                AnvilCuboidRenderer.drawSurface(pose.last(), sides, box, layer.tint(), light, false);
            }
        }
    }

    /** Original model IDs 1/2/3, 14 (four slices), and 252/253/254 (spreads/toast). */
    public static AABB[] boxes(SandwichBlockEntity sandwich, int slot, SandwichIngredients.Layer layer) {
        double bottom = slot / 16.0;
        double top = Math.min(16, slot + layer.thickness()) / 16.0;
        int model = layer.footprint();
        if (model == 14) {
            return new AABB[]{
                    px(1, slot, 1, 7, slot + layer.thickness(), 7),
                    px(1, slot, 9, 7, slot + layer.thickness(), 15),
                    px(9, slot, 1, 15, slot + layer.thickness(), 7),
                    px(9, slot, 9, 15, slot + layer.thickness(), 15)};
        }
        if (model == 252 && slot > 0) {
            var below = SandwichIngredients.of(sandwich.layerId(slot - 1));
            int belowModel = below == null ? 1 : below.footprint();
            double offset = (16 - slot) * 0.0005;
            if (belowModel == 2 || belowModel == 3 || belowModel == 14 || belowModel == 252)
                return new AABB[]{new AABB(2 / 16.0 - offset, 0.5 / 16.0, 2 / 16.0 - offset,
                        14 / 16.0 + offset, top, 14 / 16.0 + offset)};
            return new AABB[]{new AABB(1 / 16.0 - offset, bottom - 0.5 / 16.0,
                    1 / 16.0 - offset, 15 / 16.0 + offset, top, 15 / 16.0 + offset)};
        }
        if (model == 253 || model == 254)
            return new AABB[]{new AABB(0.5 / 16, bottom, 0.5 / 16, 15.5 / 16, top, 15.5 / 16)};
        int inset = model == 2 || model == 3 ? model : 1;
        return new AABB[]{px(inset, slot, inset, 16 - inset, slot + layer.thickness(), 16 - inset)};
    }

    private static AABB px(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new AABB(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
    }
}
