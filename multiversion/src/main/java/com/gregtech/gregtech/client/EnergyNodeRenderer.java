package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Direct control covers on energy nodes. Chemical batteries use their own renderer. */
public final class EnergyNodeRenderer implements BlockEntityRenderer<EnergyNodeBlockEntity> {
    public EnergyNodeRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(EnergyNodeBlockEntity be,float partialTick,PoseStack pose,
            MultiBufferSource buffers,int light,int overlay){
        if(be.spec()==null)return;
        for(var side:net.minecraft.core.Direction.values())PanelCoverRenderer.renderFace(be,side,pose,buffers,light);
    }
}
