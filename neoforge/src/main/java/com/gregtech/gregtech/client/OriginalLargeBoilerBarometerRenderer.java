package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** GT6 main-barometer pressure dial, drawn outside the controller's facing wall. */
public final class OriginalLargeBoilerBarometerRenderer
        implements BlockEntityRenderer<OriginalLargeBoilerControllerBlockEntity> {
    private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath(
            "gregtech", "block/machines/barometer/base");
    private static final ResourceLocation[] SCALE = new ResourceLocation[32];
    static {
        for (int i = 0; i < SCALE.length; i++) SCALE[i] = ResourceLocation.fromNamespaceAndPath(
                "gregtech", "block/machines/barometer/" + String.format(java.util.Locale.ROOT, "%02d", i));
    }
    public OriginalLargeBoilerBarometerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(OriginalLargeBoilerControllerBlockEntity boiler, float partialTick,
                                 PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Direction facing = boiler.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        var level = boiler.getLevel();
        if (level != null) packedLight = LevelRenderer.getLightColor(level, boiler.getBlockPos().relative(facing));
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite base = atlas.apply(BASE);
        TextureAtlasSprite needle = atlas.apply(SCALE[boiler.barometerValue()]);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        pose.pushPose();
        pose.translate(.5, .5, .5);
        switch (facing) {
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(-90));
            default -> {}
        }
        pose.translate(-.5, -.5, -.5);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        draw(matrix, normal, consumer, base, -.005f, packedLight, packedOverlay);
        draw(matrix, normal, consumer, needle, -.006f, packedLight, packedOverlay);
        pose.popPose();
    }

    private static void draw(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer,
                             TextureAtlasSprite sprite, float z, int light, int overlay) {
        float low = 1f / 16f, high = 15f / 16f;
        var direction=normal.transform(new org.joml.Vector3f(0,0,-1));
        consumer.addVertex(matrix, high, low, z).setColor(1f, 1f, 1f, 1f)
                .setUv(sprite.getU0(), sprite.getV1()).setOverlay(overlay).setLight(light)
                .setNormal(direction.x(),direction.y(),direction.z());
        consumer.addVertex(matrix, low, low, z).setColor(1f, 1f, 1f, 1f)
                .setUv(sprite.getU1(), sprite.getV1()).setOverlay(overlay).setLight(light)
                .setNormal(direction.x(),direction.y(),direction.z());
        consumer.addVertex(matrix, low, high, z).setColor(1f, 1f, 1f, 1f)
                .setUv(sprite.getU1(), sprite.getV0()).setOverlay(overlay).setLight(light)
                .setNormal(direction.x(),direction.y(),direction.z());
        consumer.addVertex(matrix, high, high, z).setColor(1f, 1f, 1f, 1f)
                .setUv(sprite.getU0(), sprite.getV0()).setOverlay(overlay).setLight(light)
                .setNormal(direction.x(),direction.y(),direction.z());
    }
}
