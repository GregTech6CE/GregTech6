package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * Covers on item pipe faces, so a player can see which cover sits on which side of a pipe.
 *
 * <p>The item pipe's half of {@link PipeCoverRenderer}, and byte for byte the same body: a pipe hosts
 * both kinds of cover, and they are drawn by two different paths. A panel cover
 * ({@code com.gregtech.gregtech.content.cover.PanelCover}) is a set of GT6 sprites and is drawn by
 * {@link PanelCoverRenderer#renderFace}, which takes any {@code PanelCoverHost} — the item pipe is one
 * after §112, so the very same call the machine and the fluid pipe make works here. Everything else —
 * the item filter, the item retriever and the plain texture covers — has no panel layers and falls
 * through to {@code MachineCoverRenderer}'s item-sprite plate, copied here because that class is typed
 * to {@code BasicMachineBlockEntity}.</p>
 *
 * <p><b>Cost:</b> a BER is called for every block entity of its type, and a world holds thousands of
 * item pipes, so the first thing this method does is ask the pipe whether it has any cover at all and
 * return ({@link ItemPipeBlockEntity#hasCovers()}). The overwhelming majority of pipes draw nothing and
 * touch nothing.</p>
 *
 * <p><b>Deliberate deviation from GT6:</b> the plate is a full-face 16×16 quad on the block boundary,
 * exactly as the machine's and the fluid pipe's covers are drawn, whereas GT6 draws a pipe cover inside
 * the pipe's own bounds ({@code CoverPressureValve.BOXES_VALVES:78}). The plate is what the port's cover
 * rendering already looks like; shrinking it per pipe diameter and per cover bounds would be a second
 * rendering convention for the same items.</p>
 */
public final class ItemPipeCoverRenderer implements BlockEntityRenderer<ItemPipeBlockEntity> {

    /** Plate thickness in block units (1 px), with a hair gap to the pipe's face. */
    private static final float OUT = 1.0f / 16.0f;
    private static final float IN = 0.0005f;

    public ItemPipeCoverRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ItemPipeBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Zero cost for a coverless pipe: no face walk, no model lookup, no vertex work.
        if (!be.hasCovers()) return;
        Level level = be.getLevel();
        Matrix4f mat = poseStack.last().pose();
        for (Direction dir : Direction.values()) {
            ItemStack cover = be.getCover(dir);
            if (cover.isEmpty()) continue;
            if (PanelCoverRenderer.renderFace(be, dir, poseStack, bufferSource, packedLight)) continue;

            BakedModel model = Minecraft.getInstance().getItemRenderer()
                    .getModel(cover, level, null, 0);
            TextureAtlasSprite sprite = model.getParticleIcon(
                    net.neoforged.neoforge.client.model.data.ModelData.EMPTY);

            int light = level != null
                    ? LevelRenderer.getLightColor(level, be.getBlockPos().relative(dir))
                    : packedLight;

            float x0 = 0, y0 = 0, z0 = 0, x1 = 1, y1 = 1, z1 = 1;
            switch (dir) {
                case NORTH -> { z0 = -OUT; z1 = -IN; }
                case SOUTH -> { z0 = 1 + IN; z1 = 1 + OUT; }
                case WEST  -> { x0 = -OUT; x1 = -IN; }
                case EAST  -> { x0 = 1 + IN; x1 = 1 + OUT; }
                case DOWN  -> { y0 = -OUT; y1 = -IN; }
                case UP    -> { y0 = 1 + IN; y1 = 1 + OUT; }
            }

            VertexConsumer vc = bufferSource.getBuffer(RenderType.cutout());
            var spec = com.gregtech.gregtech.content.cover.MachineCoverSpec.of(cover);
            if (spec != null) {
                var base = ArmRenderHelper.getSprite(net.minecraft.resources.ResourceLocation
                        .fromNamespaceAndPath("gregtech", "block/machines/covers/base"));
                ArmRenderHelper.drawCuboid(mat, vc, x0, y0, z0, x1, y1, z1, 1, 1, 1, base, light);
                // Only the outward plane receives the transparent circuit overlay.
                switch (dir) {
                    case NORTH -> { z0 = -OUT - IN; z1 = z0; }
                    case SOUTH -> { z1 = 1 + OUT + IN; z0 = z1; }
                    case WEST  -> { x0 = -OUT - IN; x1 = x0; }
                    case EAST  -> { x1 = 1 + OUT + IN; x0 = x1; }
                    case DOWN  -> { y0 = -OUT - IN; y1 = y0; }
                    case UP    -> { y1 = 1 + OUT + IN; y0 = y1; }
                }
            }
            ArmRenderHelper.drawCuboid(mat, vc, x0, y0, z0, x1, y1, z1,
                    1.0f, 1.0f, 1.0f, sprite, light);
        }
    }
}
