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
        renderLayers(sandwich::layerId, pose, buffers, light);
    }

    public static void renderLayers(java.util.function.IntUnaryOperator layers, PoseStack pose,
            MultiBufferSource buffers, int light) {
        for (int slot = 0; slot < SandwichBlockEntity.SLOTS; slot++) {
            var layer = SandwichIngredients.of(layers.applyAsInt(slot));
            if (layer == null) continue;
            for (AABB box : boxes(layers, slot, layer)) {
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
        return boxes(sandwich::layerId, slot, layer);
    }

    private static AABB[] boxes(java.util.function.IntUnaryOperator layers, int slot, SandwichIngredients.Layer layer) {
        var previous = slot == 0 ? null : SandwichIngredients.of(layers.applyAsInt(slot - 1));
        var shared = com.gregtech.gregtech.content.food.SandwichLayerCatalog.of(layer.id());
        return com.gregtech.gregtech.content.food.SandwichGeometry.boxes(slot, shared,
                previous == null ? 1 : previous.footprint()).stream()
                .map(b -> new AABB(b.x0(), b.y0(), b.z0(), b.x1(), b.y1(), b.z1())).toArray(AABB[]::new);
    }
}
