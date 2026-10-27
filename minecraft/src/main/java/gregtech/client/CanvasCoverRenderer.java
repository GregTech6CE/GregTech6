/* GregTech-6 Team's copied block-face canvas rendering, LGPL-3.0-or-later. */
package gregtech.client;

import gregtech.content.cover.*;
import gregtech.content.item.CanvasItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;

/** Each installed face uses that face of the scanned block, rather than its particle sprite. */
public final class CanvasCoverRenderer {
    private CanvasCoverRenderer() {}
    public record Face(TextureAtlasSprite sprite,int color,boolean bright) {}
    public static Face imageFace(ItemStack canvas,Direction side,BlockAndTintGetter level,BlockPos pos) {
        var state=CanvasData.state(CanvasData.read(canvas));if(state==null)return null;
        var client=Minecraft.getInstance();
        if(!state.getFluidState().isEmpty()) {
//? if >=1.21.1 {
            /*var visual=net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(state.getFluidState().getType());
*///?} else {
            var visual=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(state.getFluidState().getType());
//?}
            return new Face(ArmRenderHelper.getSprite(visual.getStillTexture()),visual.getTintColor(),state.getLightEmission()>0);
        }
        var model=client.getBlockRenderer().getBlockModel(state);
        var quads=model.getQuads(state,side,RandomSource.create(0));
        if(quads.isEmpty())quads=model.getQuads(state,null,RandomSource.create(0)).stream().filter(q->q.getDirection()==side).toList();
        if(quads.isEmpty())return new Face(model.getParticleIcon(),0xffffff,state.getLightEmission()>0);
        var quad=quads.get(0);
        int rgb=quad.isTinted()?client.getBlockColors().getColor(state,level,pos,quad.getTintIndex()):0xffffff;
        return new Face(quad.getSprite(),rgb,state.getLightEmission()>0);
    }
    public static void render(ItemStack stack,Direction side,PoseStack pose,MultiBufferSource buffers,int light,net.minecraft.world.level.block.entity.BlockEntity owner) {
        var canvas=(CanvasItem)stack.getItem();var vertices=buffers.getBuffer(RenderType.cutout());
        CoverSurfaceRenderer.draw(owner,pose,vertices,side,ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce","block/machines/covers/canvas")),light,0,canvas.variant().rgb());
        var image=imageFace(stack,side,owner.getLevel(),owner.getBlockPos());
        if(image!=null)CoverSurfaceRenderer.draw(image.bright()?null:owner,pose,buffers.getBuffer(ConfiguredAlphaModels.gtTranslucent()),side,image.sprite(),image.bright()?LightTexture.FULL_BRIGHT:light,1,image.color());
    }
}
