package com.gregtech.gregtech.client;

import com.gregtech.gregtech.block.misc.ExtenderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;

public final class ExtenderCoverRenderer implements BlockEntityRenderer<ExtenderBlockEntity> {
    public ExtenderCoverRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(ExtenderBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        for(var side:Direction.values())PanelCoverRenderer.renderFace(be,side,pose,buffers,light);
    }
}
