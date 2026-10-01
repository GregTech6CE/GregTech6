package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;

/** Renders the pressure gauge (barometer) on the front face of a steam boiler.
 *  Identical vertex/rotation pattern to {@link SteamEngineRenderer}. */
public class BoilerBarometerRenderer implements BlockEntityRenderer<BoilerTankBlockEntity> {
    private static final ResourceLocation[] NEEDLE = new ResourceLocation[32];
    private static final ResourceLocation GAUGE_BASE =
            ResourceLocation.fromNamespaceAndPath("gregtech", "block/machines/barometer/base");

    static {
        for (int i = 0; i < 32; i++) {
            NEEDLE[i] = ResourceLocation.fromNamespaceAndPath("gregtech", "block/machines/barometer/" + String.format("%02d", i));
        }
    }

    // Gauge covers 14 px of the 16-px block face, 1 px margin on each side
    private static final float X0 = 1f / 16f, X1 = 15f / 16f;
    private static final float Y0 = 1f / 16f, Y1 = 15f / 16f;

    // Base at z=-0.005, needle at z=-0.006 just in front of the block face.
    private static final float BASE_Z = -0.005f;
    private static final float NEEDLE_Z = -0.006f;

    // Exponential-moving-average speed — per-frame approach toward the raw target.
    // 0.20 works well for typical tick rates (20 tps) with minimal overshoot.
    private static final float SMOOTH_SPEED = 0.20f;

    public BoilerBarometerRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(BoilerTankBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        var tank = be.steamTank();
        if (tank == null) return;
        long cap = tank.getCapacity();
        if (cap <= 0) return;

        // Exponential moving average prevents needle oscillation when steam
        // hovers near the 50 % output threshold.
        int frame = be.smoothBarometerFrame(SMOOTH_SPEED);

        TextureAtlasSprite base = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(GAUGE_BASE);
        TextureAtlasSprite needle = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(NEEDLE[frame]);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));

        BlockState state = be.getBlockState();
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;

        ps.pushPose();
        applyFacing(ps, facing);
        if(be.getLevel()!=null) packedLight=net.minecraft.client.renderer.LevelRenderer.getLightColor(be.getLevel(),be.getBlockPos().relative(facing));

        Matrix4f m = ps.last().pose();

        // Gauge face — background dial with tick marks
        faceNorthZ(m, ps.last(), vc, X0, Y0, X1, Y1, BASE_Z,
                base.getU0(), base.getV0(), base.getU1(), base.getV1(),
                1, 1, 1, packedLight, packedOverlay);

        // Needle — rendered on top of the gauge face
        faceNorthZ(m, ps.last(), vc, X0, Y0, X1, Y1, NEEDLE_Z,
                needle.getU0(), needle.getV0(), needle.getU1(), needle.getV1(),
                1, 1, 1, packedLight, packedOverlay);

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BoilerTankBlockEntity be) {
        return true;
    }

    /** Facing rotation identical to {@link SteamEngineRenderer#applyFacing}. */
    private static void applyFacing(PoseStack ps, Direction facing) {
        ps.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case NORTH -> {}
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST  -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case EAST  -> ps.mulPose(Axis.YP.rotationDegrees(-90));
        }
        ps.translate(-0.5, -0.5, -0.5);
    }

    /** Emit a single north-facing quad — identical vertex layout to
     *  {@link SteamEngineRenderer#faceNorthZ}. */
    private static void faceNorthZ(Matrix4f m, PoseStack.Pose normal, VertexConsumer vc,
                                   float x0, float y0, float x1, float y1, float z,
                                   float u0, float v0, float u1, float v1,
                                   float r, float g, float b, int light, int overlay) {
        // From outside the north face, screen-right is world -X; texture V grows downward.
        vc.addVertex(m, x1, y0, z).setColor(r,g,b,1).setUv(u0,v1).setOverlay(overlay).setLight(light).setNormal(normal,0,0,-1);
        vc.addVertex(m, x0, y0, z).setColor(r,g,b,1).setUv(u1,v1).setOverlay(overlay).setLight(light).setNormal(normal,0,0,-1);
        vc.addVertex(m, x0, y1, z).setColor(r,g,b,1).setUv(u1,v0).setOverlay(overlay).setLight(light).setNormal(normal,0,0,-1);
        vc.addVertex(m, x1, y1, z).setColor(r,g,b,1).setUv(u0,v0).setOverlay(overlay).setLight(light).setNormal(normal,0,0,-1);
    }
}
