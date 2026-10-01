package com.gregtech.gregtech.client;
import com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
/** GT6 bounds: rods are 4x16x4 pixels, from y=1 to y=17; the four centres are (4,4), (4,12), (12,4), (12,12). */
public final class ReactorRodRenderer<T extends ReactorCoreBlockEntity> implements BlockEntityRenderer<T> {
    private final net.minecraft.client.renderer.entity.ItemRenderer items;
    public ReactorRodRenderer(BlockEntityRendererProvider.Context context){items=context.getItemRenderer();}
    @Override public void render(T core,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        for(int i=0;i<core.rods.length;i++)if(!core.rods[i].isEmpty()){
            pose.pushPose();pose.translate(core.rods.length==1?.5:.25+(i/2)*.5,.5625,core.rods.length==1?.5:.25+(i%2)*.5);
            items.renderStatic(core.rods[i],net.minecraft.world.item.ItemDisplayContext.NONE,light,overlay,pose,buffers,core.getLevel(),i);pose.popPose();
        }
    }
}
