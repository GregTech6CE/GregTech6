package gregtech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gregtech.api.material.*;
import gregtech.content.block.tool.MaterialAnvilBlock;
import gregtech.content.blockentity.tool.MaterialAnvilBlockEntity;
import gregtech.content.tool.AnvilWorkpieceGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.RenderShape;

/** GT6's two material-shaped workpieces; foreign blocks keep their actual block model. */
public final class MaterialAnvilRenderer implements BlockEntityRenderer<MaterialAnvilBlockEntity> {
    public MaterialAnvilRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            MaterialAnvilBlockEntity anvil,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        var facing = anvil.getBlockState().getValue(MaterialAnvilBlock.FACING);
        for (int slot = 0; slot < 2; slot++) {
            var hammer = anvil.workpiece(slot);
            if (gregtech.content.tool.AnvilHammerDisplay.isHammer(hammer)) {
                for (var part : gregtech.content.tool.AnvilHammerDisplay.parts(hammer, slot, facing))
                    drawHammerPart(pose, buffers, light, part.bounds(), part.material());
                return;
            }
        }
        for (int slot = 0; slot < 2; slot++) {
            var stack = anvil.workpiece(slot);
            if (stack.isEmpty()) continue;
            var form = gregtech.content.tool.AnvilWorkpieceDisplay.materialForm(stack);
            int shape = form != null
                    ? AnvilWorkpieceGeometry.shape(form.prefix().getName())
                    : stack.getItem() instanceof BlockItem ? 7 : 0;
            var bounds = AnvilWorkpieceGeometry.bounds(shape, slot, facing);
            if (form != null) {
                var material = form.material();
                String icon = shape == 6 ? "blockgem" : shape == 7 ? "blockraw" : "blocksolid";
                var texture = ResourceLocation.fromNamespaceAndPath(
                        "gt6ce",
                        "block/material_icons/"
                                + MaterialIcons.resolveTextureSet(material).folder() + "/" + icon);
                int rgb = material.getColor();
                AnvilCuboidRenderer.draw(
                        pose.last(),
                        buffers.getBuffer(RenderType.entityCutoutNoCull(
                                net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),
                        bounds,
                        CrucibleRenderSprites.resolve(texture),
                        rgb,
                        light);
                var detail =
                        ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), texture.getPath() + "_overlay");
                // Detail layers retain their own colors; a missing overlay must stay transparent.
                if (CrucibleRenderSprites.hasTexture(detail)) {
                    AnvilCuboidRenderer.draw(
                            pose.last(),
                            buffers.getBuffer(RenderType.entityCutoutNoCull(
                                    net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),
                            bounds,
                            CrucibleRenderSprites.resolve(detail),
                            0xFFFFFF,
                            light);
                }
            } else if (stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock().defaultBlockState().getRenderShape() == RenderShape.MODEL) {
                // A FIXED item transform may flatten, shrink or hide an ore item.
                // Render its block model inside the original ore-workpiece bounds,
                // including vanilla and other mods' ore models and render layers.
                pose.pushPose();
                pose.translate(bounds.minX, bounds.minY, bounds.minZ);
                pose.scale((float) bounds.getXsize(), (float) bounds.getYsize(), (float) bounds.getZsize());
                Minecraft.getInstance()
                        .getBlockRenderer()
                        .renderSingleBlock(
                                blockItem.getBlock().defaultBlockState(),
                                pose,
                                buffers,
                                light,
                                OverlayTexture.NO_OVERLAY);
                pose.popPose();
            } else {
                pose.pushPose();
                var renderer = Minecraft.getInstance().getItemRenderer();
                boolean solid =
                        renderer.getModel(stack, anvil.getLevel(), null, 0).isGui3d();
                pose.translate(bounds.getCenter().x, solid ? .9 : .755, bounds.getCenter().z);
                if (!solid) pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.scale(.28f, .28f, .28f);
                renderer.renderStatic(
                        stack,
                        ItemDisplayContext.FIXED,
                        light,
                        OverlayTexture.NO_OVERLAY,
                        pose,
                        buffers,
                        anvil.getLevel(),
                        slot);
                pose.popPose();
            }
        }
    }

    private static void drawHammerPart(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            net.minecraft.world.phys.AABB bounds,
            gregtech.api.material.GTMaterial material) {
        var texture = ResourceLocation.fromNamespaceAndPath(
                "gt6ce",
                "block/material_icons/"
                        + MaterialIcons.resolveTextureSet(material).folder() + "/blocksolid");
        int rgb = material.getColor();
        AnvilCuboidRenderer.draw(
                pose.last(),
                buffers.getBuffer(RenderType.entityCutoutNoCull(
                        net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),
                bounds,
                CrucibleRenderSprites.resolve(texture),
                rgb,
                light);
        var detail = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), texture.getPath() + "_overlay");
        if (CrucibleRenderSprites.hasTexture(detail))
            AnvilCuboidRenderer.draw(
                    pose.last(),
                    buffers.getBuffer(RenderType.entityCutoutNoCull(
                            net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),
                    bounds,
                    CrucibleRenderSprites.resolve(detail),
                    0xFFFFFF,
                    light);
    }
}
