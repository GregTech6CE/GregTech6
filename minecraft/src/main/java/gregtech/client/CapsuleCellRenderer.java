package gregtech.client;

import gregtech.content.blockentity.tool.PortableContainerBlockEntity;
import gregtech.content.item.PortableFluidContainerItem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidUtil;
*///?} else {
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidUtil;
//?}

/** GT6 cell side windows: fluid is behind the casing overlay, full-height even when partly filled. */
public final class CapsuleCellRenderer implements BlockEntityRenderer<PortableContainerBlockEntity> {
    public CapsuleCellRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(PortableContainerBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var stack=be.contents();
        if(!(stack.getItem() instanceof PortableFluidContainerItem item))return;
        String shape = item.spec().shapeId();
        if (!java.util.Set.of("cell", "cup", "measuring_pot", "jug").contains(shape)) return;
//? if >=1.21.1 {
        /*var fluid=FluidUtil.getFluidContained(stack).orElse(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
*///?} else {
        var fluid=FluidUtil.getFluidContained(stack).orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
//?}
        if(fluid.isEmpty())return;
        var visual=IClientFluidTypeExtensions.of(fluid.getFluid());var texture=visual.getStillTexture(fluid);
        if(texture==null)return;
        var sprite=CrucibleRenderSprites.resolve(texture);int tint=visual.getTintColor(fluid);
        var consumer=buffers.getBuffer(ConfiguredAlphaModels.gtTranslucent());
        if (!shape.equals("cell")) {
            float min = shape.equals("measuring_pot") ? 5/16f : 6/16f;
            float max = 1-min;
            int capacity = FluidUtil.getFluidHandler(stack).map(handler -> handler.getTankCapacity(0)).orElse(1);
            float bottom = shape.equals("jug") ? 10/16f : 1/16f;
            float top = shape.equals("jug") ? 14/16f : shape.equals("cup") ? 5/16f : 8/16f;
            float y = bottom + (top-bottom-.001f)*Math.min(1f,fluid.getAmount()/(float)Math.max(1,capacity));
            SmeltingCrucibleRenderer.drawFluidTop(pose, buffers, texture, tint, y, min, min, max, max,
                    light, ConfiguredAlphaModels.gtTranslucent());
            return;
        }
        for(int[] bounds:gregtech.content.tool.PortableContainerShapes.bounds("cell")) {
            float x0=bounds[0]/16f+.0005f,y0=bounds[1]/16f,z0=bounds[2]/16f+.0005f;
            float x1=bounds[3]/16f-.0005f,y1=bounds[4]/16f,z1=bounds[5]/16f-.0005f;
            float[][][] faces={{{x1,y0,z0},{x0,y0,z0},{x0,y1,z0},{x1,y1,z0}},{{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}},{{x0,y0,z0},{x0,y0,z1},{x0,y1,z1},{x0,y1,z0}},{{x1,y0,z1},{x1,y0,z0},{x1,y1,z0},{x1,y1,z1}}};
            int[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
            for(int face=0;face<4;face++)for(int v=0;v<4;v++) {
                var point=faces[face][v];var normal=normals[face];
                float u = switch (face) { case 0 -> 1-point[0]; case 1 -> point[0]; case 2 -> point[2]; default -> 1-point[2]; };
                float atlasU = sprite.getU0() + (sprite.getU1()-sprite.getU0())*u;
                float atlasV = sprite.getV0() + (sprite.getV1()-sprite.getV0())*(1-point[1]);
//? if >=1.21.1 {
                /*consumer.addVertex(pose.last().pose(),point[0],point[1],point[2]).setColor((tint>>16)&255,(tint>>8)&255,tint&255,(tint>>>24)==0?255:tint>>>24)
                        .setUv(atlasU,atlasV)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),normal[0],normal[1],normal[2]);
*///?} else {
                consumer.vertex(pose.last().pose(),point[0],point[1],point[2]).color((tint>>16)&255,(tint>>8)&255,tint&255,(tint>>>24)==0?255:tint>>>24)
                        .uv(atlasU,atlasV)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),normal[0],normal[1],normal[2]).endVertex();
//?}
            }
        }
    }
}
