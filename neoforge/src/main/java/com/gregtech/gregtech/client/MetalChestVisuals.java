package com.gregtech.gregtech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** GT6 MultiTileEntityChest's legacy 64x64 layout, shared by the block and its inventory item. */
final class MetalChestVisuals {
    private static final java.util.Map<com.gregtech.gregtech.block.inventory.MetalChestBlock.Shell, ResourceLocation[]> TEXTURES = new java.util.EnumMap<>(com.gregtech.gregtech.block.inventory.MetalChestBlock.Shell.class);
    static {
        for (var shell : com.gregtech.gregtech.block.inventory.MetalChestBlock.Shell.values()) {
            String path = "textures/block/machines/" + shell.texture() + "/";
            TEXTURES.put(shell, new ResourceLocation[]{ResourceLocation.fromNamespaceAndPath("gregtech", path + "colored.png"),
                    ResourceLocation.fromNamespaceAndPath("gregtech", path + "overlay.png")});
        }
    }
    private final ModelPart root = createLayer().bakeRoot();

    private static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();
        // Keep the legacy model coordinates as well as its UV offsets; modern chest UVs differ.
        parts.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19)
                .addBox(0, 0, 0, 14, 10, 14), PartPose.offset(1, 6, 1));
        parts.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0, -5, -14, 14, 5, 14), PartPose.offset(1, 7, 15));
        parts.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, -2, -15, 2, 4, 1), PartPose.offset(8, 7, 15));
        return LayerDefinition.create(mesh, 64, 64);
    }

    void render(Direction facing, com.gregtech.gregtech.block.inventory.MetalChestBlock block, float open, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int color = block.material().getColor();
        var textures = TEXTURES.get(block.shell());
        float angle = -(1 - (float) Math.pow(1 - open, 3)) * (float) Math.PI / 2;
        root.getChild("lid").xRot = angle;
        root.getChild("lock").xRot = angle;
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(-.5, 0, -.5);
        pose.translate(0, 1, 1);
        pose.scale(1, -1, -1);
        root.render(pose, buffers.getBuffer(RenderType.entityCutout(textures[0])), light, overlay,
                0xFF000000|color);
        root.render(pose, buffers.getBuffer(RenderType.entityCutout(textures[1])), light, overlay, -1);
        pose.popPose();
    }
}
