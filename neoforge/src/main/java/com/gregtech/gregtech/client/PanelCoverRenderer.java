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
        var panel=PanelCover.of(stack);
        var component=ComponentCoverRuntime.kind(stack);
        if(component!=null)return List.of(ComponentCoverRules.texture(component,ComponentCoverRuntime.visual(stack)));
        if(panel==null){
            var id=CoverItems.behavior(stack);int count=CoverUtilityBehaviors.designCount(id);
            if(count>0)return List.of((CoverUtilityBehaviors.BLANK_COVER.equals(id)?"blank/":"warning/")+Math.floorMod(CoverUtilityBehaviors.design(stack),count));
            return List.of();
        }
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
        var stack=host.getCover(side);if(!CoverItems.isCover(stack))return false;
        if(stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem material){
            var stone=MaterialCoverRules.stoneTextures(material.getPrefix().getName(),material.getMaterial().getName());
            if(!stone.isEmpty()){
                var vertices=buffers.getBuffer(RenderType.cutout());
                var sprite=ArmRenderHelper.getSprite(ResourceLocation.parse(stone.get(Math.floorMod(CoverUtilityBehaviors.design(stack),stone.size()))));
                CoverSurfaceRenderer.draw(pose,vertices,side,sprite,fallbackLight,0);return true;
            }
            var textures=MaterialCoverRules.textures(material.getPrefix().getName());
            if(!textures.isEmpty()){
                String file=textures.get(Math.floorMod(CoverUtilityBehaviors.design(stack),textures.size()));
                var set=MaterialIcons.resolveTextureSet(material.getMaterial());
                String path="block/material_icons/"+set.folder()+"/"+file;
                var vertices=buffers.getBuffer(RenderType.cutout());
                CoverSurfaceRenderer.draw(pose,vertices,side,ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gregtech",path)),fallbackLight,0,material.getMaterial().getColor());
                CoverSurfaceRenderer.draw(pose,vertices,side,ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gregtech",path+"_overlay")),fallbackLight,1);
                return true;
            }
        }
        if(PanelCover.of(stack)==null&&ComponentCoverRuntime.kind(stack)==null&&CoverUtilityBehaviors.designCount(CoverItems.behavior(stack))==0){
            var model=net.minecraft.client.Minecraft.getInstance().getItemRenderer().getModel(stack,host.coverOwner().getLevel(),null,0);
            var vertices=buffers.getBuffer(RenderType.cutout());
            CoverSurfaceRenderer.draw(pose,vertices,side,model.getParticleIcon(),fallbackLight,0,net.minecraft.client.Minecraft.getInstance().getItemColors().getColor(stack,0));
            return true;
        }
        var owner=host.coverOwner();var level=owner.getLevel();
        int light=level==null?fallbackLight:LevelRenderer.getLightColor(level,owner.getBlockPos().relative(side));
        var vc=buffers.getBuffer(RenderType.cutout());var matrix=pose.last().pose();
        var base=ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gregtech","block/machines/covers/base"));
        CoverSurfaceRenderer.draw(pose,vc,side,base,light,0);
        int layer=1;
        for(String texture:layers(stack)){
            var sprite=ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gregtech","block/machines/covers/"+texture));
            CoverSurfaceRenderer.draw(pose,vc,side,sprite,light,layer++);
        }
        return true;
    }
}
