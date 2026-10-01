package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.tool.CoinMoldBlockEntity;
import com.gregtech.gregtech.content.tool.CoinMoldGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlas;

/** GT6 coin mold material insert (pass 2): present for both the plate and the stamped coin. */
public final class CoinMoldRenderer implements BlockEntityRenderer<CoinMoldBlockEntity> {
    public CoinMoldRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(CoinMoldBlockEntity mold,float partialTick,PoseStack pose,
                                 MultiBufferSource buffers,int light,int overlay) {
        var material=mold.displayedMaterial();
        if(material==null||!material.isValid())return;
        var texture=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","block/material_icons/"+MaterialIcons.resolveTextureSet(material).folder()+"/blocksolid");
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        AnvilCuboidRenderer.draw(pose.last(),out,CoinMoldGeometry.CONTENT,CrucibleRenderSprites.resolve(texture),material.getColor(),light);
        var detail=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",texture.getPath()+"_overlay");
        if(CrucibleRenderSprites.hasTexture(detail))
            AnvilCuboidRenderer.draw(pose.last(),out,CoinMoldGeometry.CONTENT,CrucibleRenderSprites.resolve(detail),0xFFFFFF,light);
    }
}
