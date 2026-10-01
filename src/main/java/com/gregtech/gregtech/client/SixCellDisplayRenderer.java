package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.sensor.SixCellDisplay;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

/** Draws original character sprites into fixed cells; never invokes Minecraft's font renderer. */
public final class SixCellDisplayRenderer {
    private static final Map<String,ResourceLocation> TEXTURES = new HashMap<>();
    private SixCellDisplayRenderer() {}
    public static void draw(SixCellDisplay.Glyph[] cells, PoseStack poses, MultiBufferSource buffers, int light) {
        light=LightTexture.FULL_BRIGHT; // GT6 character overlays request constant maximum brightness.
        var pose=poses.last();
        for (int i=0;i<6;i++) {
            var cell=cells[i]; if(cell==null) continue;
            var texture=TEXTURES.computeIfAbsent(cell.texture(), name -> ResourceLocation.fromNamespaceAndPath("gregtech","textures/block/overlays/characters/"+name+".png"));
            var vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
            float left=(2+2*i)/16f-.5f, right=left+2/16f, top=.5f-2/16f, bottom=top-2/16f;
            int r=cell.rgb()>>16&255,g=cell.rgb()>>8&255,b=cell.rgb()&255;
            float tile=SixCellDisplay.TILE_UV;
            float u0=(i+1)*tile,u1=u0+tile,v0=tile,v1=2*tile;
            // GT6 sheets repeat the same glyph 8x8 times; a 2/16-wide cell samples one tile.
            vertices.vertex(pose.pose(),left,bottom,.001f).color(r,g,b,255).uv(u0,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,0,1).endVertex();
            vertices.vertex(pose.pose(),right,bottom,.001f).color(r,g,b,255).uv(u1,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,0,1).endVertex();
            vertices.vertex(pose.pose(),right,top,.001f).color(r,g,b,255).uv(u1,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,0,1).endVertex();
            vertices.vertex(pose.pose(),left,top,.001f).color(r,g,b,255).uv(u0,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,0,1).endVertex();
        }
    }
}
