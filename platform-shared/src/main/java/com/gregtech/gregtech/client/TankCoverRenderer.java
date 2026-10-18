package com.gregtech.gregtech.client;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
public final class TankCoverRenderer implements BlockEntityRenderer<TankBlockEntity> {
    public TankCoverRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(TankBlockEntity tank,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        for(var side:net.minecraft.core.Direction.values())PanelCoverRenderer.renderFace(tank,side,pose,buffers,light);
        if(tank instanceof com.gregtech.gregtech.content.logistics.LogisticsCoverHost host)LogisticsCoverRenderer.renderFaces(tank,host,pose,buffers,light);
    }
}
