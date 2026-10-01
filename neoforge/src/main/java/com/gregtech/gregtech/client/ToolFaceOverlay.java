package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.util.GTPlacementCode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;

/** Highlights exactly the cells selected by the server's face resolver; invalid rotations are red. */
final class ToolFaceOverlay {
    private ToolFaceOverlay() {}
    private static final float[] EDGES = {-0.5F, -0.25F, 0.25F, 0.5F};

    static void drawSelection(PoseStack poses, VertexConsumer out, BlockHitResult hit, ToolInteractionSpec spec, net.minecraft.world.level.block.state.BlockState state) {
        Direction selected = ToolInteractions.selectedFace(hit);
        var mc = net.minecraft.client.Minecraft.getInstance();
        var tool = ToolInteractions.describe(state, mc.player.getMainHandItem()) != null
                ? mc.player.getMainHandItem() : mc.player.getOffhandItem();
        boolean valid = ToolInteractions.canApply(spec, state, mc.level, hit.getBlockPos(), mc.player, tool, selected);
        for (int u = 0; u < 3; u++) for (int v = 0; v < 3; v++) {
            float[] center = point(hit.getDirection(), (EDGES[u] + EDGES[u + 1]) / 2, (EDGES[v] + EDGES[v + 1]) / 2);
            Direction cell = GTPlacementCode.getSideWrenching(hit.getDirection(), center[0] + .5F, center[1] + .5F, center[2] + .5F);
            if (cell != selected) continue;
            float[][] corners = {point(hit.getDirection(), EDGES[u], EDGES[v]),
                    point(hit.getDirection(), EDGES[u+1], EDGES[v]),
                    point(hit.getDirection(), EDGES[u+1], EDGES[v+1]),
                    point(hit.getDirection(), EDGES[u], EDGES[v+1])};
            for (int i = 0; i < 4; i++) {
                float[] a = corners[i], b = corners[(i + 1) % 4];
                GTRenderHelper.line(out, poses.last().pose(), poses.last().normal(),
                        valid ? .2F : 1F, valid ? 1F : .2F, .2F, 1F,
                        a[0], a[1], a[2], b[0], b[1], b[2]);
            }
        }
    }
    private static float[] point(Direction face, float u, float v) {
        float normal = face.getAxisDirection().getStep() * .504F;
        return switch (face.getAxis()) {
            case X -> new float[]{normal, v, u};
            case Y -> new float[]{u, normal, v};
            case Z -> new float[]{u, v, normal};
        };
    }
}
