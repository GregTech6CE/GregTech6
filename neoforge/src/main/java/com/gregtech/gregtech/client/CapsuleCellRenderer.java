package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.tool.PortableContainerBlockEntity;
import com.gregtech.gregtech.item.PortableFluidContainerItem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidUtil;

/** GT6 cell side windows: fluid is behind the casing overlay, full-height even when partly filled. */
public final class CapsuleCellRenderer implements BlockEntityRenderer<PortableContainerBlockEntity> {
    public CapsuleCellRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(PortableContainerBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var stack=be.contents();
        if(!(stack.getItem() instanceof PortableFluidContainerItem item)||!item.spec().shapeId().equals("cell"))return;
        var fluid=FluidUtil.getFluidContained(stack).orElse(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
        if(fluid.isEmpty())return;
        var visual=IClientFluidTypeExtensions.of(fluid.getFluid());var texture=visual.getStillTexture(fluid);
        if(texture==null)return;
        var sprite=CrucibleRenderSprites.resolve(texture);int tint=visual.getTintColor(fluid);
        var consumer=buffers.getBuffer(RenderType.translucent());
        for(int[] bounds:com.gregtech.gregtech.content.tool.PortableContainerShapes.bounds("cell")) {
            float x0=bounds[0]/16f+.0005f,y0=bounds[1]/16f,z0=bounds[2]/16f+.0005f;
            float x1=bounds[3]/16f-.0005f,y1=bounds[4]/16f,z1=bounds[5]/16f-.0005f;
            float[][][] faces={{{x1,y0,z0},{x0,y0,z0},{x0,y1,z0},{x1,y1,z0}},{{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}},{{x0,y0,z0},{x0,y0,z1},{x0,y1,z1},{x0,y1,z0}},{{x1,y0,z1},{x1,y0,z0},{x1,y1,z0},{x1,y1,z1}}};
            int[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
            for(int face=0;face<4;face++)for(int v=0;v<4;v++) {
                var point=faces[face][v];var normal=normals[face];
                consumer.addVertex(pose.last().pose(),point[0],point[1],point[2]).setColor((tint>>16)&255,(tint>>8)&255,tint&255,(tint>>>24)==0?255:tint>>>24)
                        .setUv(v==0||v==3?sprite.getU0():sprite.getU1(),v<2?sprite.getV1():sprite.getV0())
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),normal[0],normal[1],normal[2]);
            }
        }
    }
}
