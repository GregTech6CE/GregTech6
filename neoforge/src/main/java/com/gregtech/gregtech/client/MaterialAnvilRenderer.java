package com.gregtech.gregtech.client;

import com.gregtech.gregtech.block.tool.MaterialAnvilBlock;
import com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity;
import com.gregtech.gregtech.content.tool.AnvilWorkpieceGeometry;
import com.gregtech.gregtech.item.MaterialItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/** GT6's two material-shaped workpieces; foreign items use their own item model. */
public final class MaterialAnvilRenderer implements BlockEntityRenderer<MaterialAnvilBlockEntity> {
    public MaterialAnvilRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(MaterialAnvilBlockEntity anvil, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        var facing = anvil.getBlockState().getValue(MaterialAnvilBlock.FACING);
        for (int slot=0;slot<2;slot++) {
            var hammer=anvil.workpiece(slot);
            if (com.gregtech.gregtech.content.tool.AnvilHammerDisplay.isHammer(hammer)) {
                for(var part:com.gregtech.gregtech.content.tool.AnvilHammerDisplay.parts(hammer,slot,facing))
                    drawHammerPart(pose,buffers,light,part.bounds(),part.material());
                return;
            }
        }
        for (int slot = 0; slot < 2; slot++) {
            var stack = anvil.workpiece(slot);
            if (stack.isEmpty()) continue;
            int shape = stack.getItem() instanceof MaterialItem item ? AnvilWorkpieceGeometry.shape(item.getPrefix().getName()) : 0;
            var bounds = AnvilWorkpieceGeometry.bounds(shape, slot, facing);
            if (stack.getItem() instanceof MaterialItem item) {
                var material = item.getMaterial();
                String icon = shape == 6 ? "blockgem" : shape == 7 ? "blockraw" : "blocksolid";
                var texture = ResourceLocation.fromNamespaceAndPath("gregtech", "block/material_icons/" + MaterialIcons.resolveTextureSet(material).folder() + "/" + icon);
                int rgb = material.getColor();
                AnvilCuboidRenderer.draw(pose.last(), buffers.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),
                        bounds, CrucibleRenderSprites.resolve(texture), rgb, light);
                var detail = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), texture.getPath() + "_overlay");
                // Detail layers retain their own colors; a missing overlay must stay transparent.
                if (CrucibleRenderSprites.hasTexture(detail)) {
                    AnvilCuboidRenderer.draw(pose.last(), buffers.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),
                            bounds, CrucibleRenderSprites.resolve(detail), 0xFFFFFF, light);
                }
            } else {
                pose.pushPose();
                var renderer = Minecraft.getInstance().getItemRenderer();
                boolean solid = renderer.getModel(stack, anvil.getLevel(), null, 0).isGui3d();
                pose.translate(bounds.getCenter().x, solid ? .9 : .755, bounds.getCenter().z);
                if (!solid) pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.scale(.28f, .28f, .28f);
                renderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, anvil.getLevel(), slot);
                pose.popPose();
            }
        }
    }

    private static void drawHammerPart(PoseStack pose,MultiBufferSource buffers,int light,
                                       net.minecraft.world.phys.AABB bounds,com.gregtech.gregtech.api.material.GTMaterial material) {
        var texture=ResourceLocation.fromNamespaceAndPath("gregtech","block/material_icons/"
                +MaterialIcons.resolveTextureSet(material).folder()+"/blocksolid");
        int rgb=material.getColor();
        AnvilCuboidRenderer.draw(pose.last(),buffers.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),
                bounds,CrucibleRenderSprites.resolve(texture),rgb,light);
        var detail=ResourceLocation.fromNamespaceAndPath(texture.getNamespace(),texture.getPath()+"_overlay");
        if(CrucibleRenderSprites.hasTexture(detail))
            AnvilCuboidRenderer.draw(pose.last(),buffers.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),
                    bounds,CrucibleRenderSprites.resolve(detail),0xFFFFFF,light);
    }
}
