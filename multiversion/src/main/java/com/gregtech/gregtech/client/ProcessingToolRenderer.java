package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity;
import com.gregtech.gregtech.block.tool.ProcessingToolBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;

/** Shared dynamic contents for GT6's open manual processing vessels. */
public final class ProcessingToolRenderer implements BlockEntityRenderer<ProcessingToolBlockEntity> {
    public ProcessingToolRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ProcessingToolBlockEntity tool,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        String id=((ProcessingToolBlock)tool.getBlockState().getBlock()).toolId();
        boolean juicer=id.equals("juicer");
        boolean table=id.contains("_table");
        boolean bowl=id.startsWith("mixing_bowl");
        float topY = bowl ? (table ? 15 / 16f : 7 / 16f) : table ? .875f : .375f;
        var fluid=tool.displayFluid();
        if(!fluid.isEmpty()) {
//? if >=1.21.1 {
            /*var visual=net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
*///?} else {
            var visual=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
//?}
            var texture=visual.getStillTexture(fluid);
            if(texture!=null) SmeltingCrucibleRenderer.drawTop(pose,buffers,texture,visual.getTintColor(fluid),
                    juicer ? .13f : topY,juicer ? .25f : .125f,juicer ? .25f : .125f,juicer ? .75f : .875f,juicer ? .75f : .875f,light);
            return;
        }
        var stack=tool.displayItem();
        if(stack.isEmpty()) return;
        if (bowl) {
            // MultiTileEntityMixingBowl:402-408: a blockDust top face, never the ingredient's item model.
            var material = com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(stack)
                    .map(com.gregtech.gregtech.api.material.ItemComposition::material)
                    .orElse(com.gregtech.gregtech.api.material.MaterialSentinels.Invalid).resolve();
            var set = material.isValid() ? MaterialIcons.resolveTextureSet(material)
                    : com.gregtech.gregtech.api.material.MaterialTextureSet.METALLIC;
            var texture = CrucibleContentIcons.blockIcon(set, "blockdust", false);
            int tint = material.isValid() ? material.getColor() | 0xFF000000 : 0xFF808080;
            SmeltingCrucibleRenderer.drawTop(pose, buffers, texture, tint, topY, .125f, .125f, .875f, .875f, light);
            var detail = CrucibleContentIcons.blockIcon(set, "blockdust", true);
            if (CrucibleRenderSprites.hasTexture(detail)) {
                SmeltingCrucibleRenderer.drawTop(pose, buffers, detail, 0xFFFFFFFF,
                        topY + .0001f, .125f, .125f, .875f, .875f, light, RenderType.cutout());
            }
            return;
        }
        pose.pushPose(); pose.translate(.5, table ? .7 : .2, .5); pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90)); pose.scale(.45f,.45f,.45f);
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(stack,net.minecraft.world.item.ItemDisplayContext.FIXED,
                light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,pose,buffers,tool.getLevel(),0);
        pose.popPose();
    }
}
