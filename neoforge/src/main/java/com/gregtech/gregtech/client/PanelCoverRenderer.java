package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.cover.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Original GT6 sprites, with the exact same face mapping used by the server's buttons. */
public final class PanelCoverRenderer {
    private static final String[] BUTTON_STYLES={"underlay","underlay_0_to_15","underlay_0_to_f","underlay_1_to_16","underlay_16_1_to_15","underlay_keypad_1_to_9","underlay_keypad_9_to_1","underlay_bits"};
    private PanelCoverRenderer(){}
    public static List<String> layers(ItemStack stack){
        var panel=PanelCover.of(stack);if(panel==null)return List.of();
        int value=PanelCoverRuntime.value(stack),style=PanelCoverRuntime.style(stack);
        return switch(panel){
            case MANUAL->List.of("manualselector/underlay","manualselector/"+(value&15));
            case REDSTONE->List.of("redstoneselector/underlay","redstoneselector/"+(value&15));
            case BUTTONS->List.of("buttonselector/"+BUTTON_STYLES[Math.floorMod(style,8)],"buttonselector/"+(value&15));
            case EMITTER->List.of("redstoneemitter/underlay","redstoneemitter/"+(value&15));
            case ENERGY_DISPLAY->List.of("energydisplay/underlay","energydisplay/"+Math.max(0,Math.min(10,value)));
            case STATUS->{
                String folder="statusdisplay/"+(Math.floorMod(style,2)==0?"bottom/":"top/");var layers=new ArrayList<String>();layers.add(folder+"base");
                for(int i=0;i<4;i++)if((value&(32<<i))!=0)layers.add(folder+(i+1)+((value&(1<<i))!=0?"_on":"_off"));yield layers;
            }
            case PROGRESS->List.of("progressredstone/circuit");case ENERGY->List.of("energyredstone/circuit");
            case CONDUCTOR_IN->List.of("redstoneconductor/in");case CONDUCTOR_OUT->List.of("redstoneconductor/out");
            case CONTROLLER->List.of("coverswitch/base","coverswitch/circuit");
            case SHUTTER->List.of("shutter/"+(MachineCoverSpec.inverted(stack)?"inverted":"normal"));
        };
    }
    public static boolean renderFace(PanelCoverHost host,Direction side,PoseStack pose,MultiBufferSource buffers,int fallbackLight){
        var stack=host.getCover(side);if(PanelCover.of(stack)==null)return false;
        var owner=host.coverOwner();var level=owner.getLevel();
        int light=level==null?fallbackLight:LevelRenderer.getLightColor(level,owner.getBlockPos().relative(side));
        var vc=buffers.getBuffer(RenderType.cutout());var matrix=pose.last().pose();
        float x0=0,y0=0,z0=0,x1=1,y1=1,z1=1,out=1/16f;
        switch(side){case NORTH->{z0=-out;z1=-.0005f;}case SOUTH->{z0=1.0005f;z1=1+out;}
            case WEST->{x0=-out;x1=-.0005f;}case EAST->{x0=1.0005f;x1=1+out;}
            case DOWN->{y0=-out;y1=-.0005f;}case UP->{y0=1.0005f;y1=1+out;}}
        var base=ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gregtech","block/machines/covers/base"));
        ArmRenderHelper.drawCuboid(matrix,vc,x0,y0,z0,x1,y1,z1,1,1,1,base,light);
        int layer=0;
        for(String texture:layers(stack)){
            var sprite=ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gregtech","block/machines/covers/"+texture));
            double distance=out+.0005+(layer++)*.0001;
            for(int corner=0;corner<4;corner++){
                double u=corner>=2?1:0,v=corner==1||corner==2?1:0;var pos=CoverFaceCoordinates.to(side,u,v,distance);
                vc.addVertex(matrix,(float)pos.x,(float)pos.y,(float)pos.z).setColor(1f,1f,1f,1f).setUv(sprite.getU((float)u),sprite.getV((float)v))
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),side.getStepX(),side.getStepY(),side.getStepZ());
            }
        }
        return true;
    }
}
