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
        var fluid=tool.displayFluid();
        if(!fluid.isEmpty()) {
            var visual=net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
            var texture=visual.getStillTexture(fluid);
            if(texture!=null) SmeltingCrucibleRenderer.drawTop(pose,buffers,texture,visual.getTintColor(fluid),
                    juicer ? .13f : table ? .875f : .375f,juicer ? .25f : .125f,juicer ? .25f : .125f,juicer ? .75f : .875f,juicer ? .75f : .875f,light);
            return;
        }
        var stack=tool.displayItem();
        if(stack.isEmpty()) return;
        pose.pushPose(); pose.translate(.5, table ? .7 : .2, .5); pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90)); pose.scale(.45f,.45f,.45f);
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(stack,net.minecraft.world.item.ItemDisplayContext.FIXED,
                light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,pose,buffers,tool.getLevel(),0);
        pose.popPose();
    }
}
