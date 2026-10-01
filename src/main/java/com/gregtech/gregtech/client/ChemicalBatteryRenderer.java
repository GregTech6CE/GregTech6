package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.energy.ChemicalBatteryBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** GT6 integer charge indicator, coloured by chemistry; casing textures remain uncoloured. */
public final class ChemicalBatteryRenderer implements BlockEntityRenderer<ChemicalBatteryBlockEntity> {
    public ChemicalBatteryRenderer(BlockEntityRendererProvider.Context ctx){}
    @Override public void render(ChemicalBatteryBlockEntity be,float tick,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        var spec=be.spec();int displayed=spec.display(be.stored());
        if(displayed==0)return;
        int color=spec.chemistry().color;
        float lo=spec.inset()/16f-0.002f,hi=1-spec.inset()/16f+0.002f;
        var texture=ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gregtech",spec.texture()+"/bar"));
        ArmRenderHelper.drawCuboid(poses.last().pose(),buffers.getBuffer(RenderType.cutout()),lo,1/16f,lo,hi,(displayed+1)/16f,hi,
                ((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,texture,light,Direction.Axis.Y);
    }
}
