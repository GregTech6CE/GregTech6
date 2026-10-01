package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity;
import com.gregtech.gregtech.content.tool.BottleCrateGeometry;
import com.gregtech.gregtech.item.BottleItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

/** GT6's fluid inset and bottle shell/cap textures, with full-block UVs for the 3×3 sprite layout. */
public final class BottleCrateRenderer implements BlockEntityRenderer<BottleCrateBlockEntity> {
    public BottleCrateRenderer(BlockEntityRendererProvider.Context context) {}

    private static FluidStack fluid(ItemStack stack) {
        if(stack.is(Items.GLASS_BOTTLE) || stack.is(BottleItem.emptyBottle().getItem())) return FluidStack.EMPTY;
        if(stack.is(Items.EXPERIENCE_BOTTLE)) {
            var jump=com.gregtech.gregtech.content.food.GTDrinks.fluidForField("Potion_Jump_1");
            if(jump!=null) return new FluidStack(jump,250);
        }
        if(stack.getItem() instanceof BottleItem bottle && bottle.fluid()!=null) return new FluidStack(bottle.fluid(),250);
        return FluidUtil.getFluidContained(stack).orElse(new FluidStack(Fluids.WATER,250));
    }

    @Override public void render(BottleCrateBlockEntity crate,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var out=buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        for(int slot=0;slot<9;slot++) {
            var stack=crate.items().getStackInSlot(slot); if(stack.isEmpty()) continue;
            var fluid=fluid(stack); if(fluid.isEmpty()) continue;
            var properties=IClientFluidTypeExtensions.of(fluid.getFluid());
            var texture=properties.getStillTexture(fluid); if(texture==null) continue;
            var sprite=CrucibleRenderSprites.resolve(texture);
            int color=stack.getItem() instanceof PotionItem ? PotionUtils.getColor(stack)
                    : properties.getTintColor(fluid);
            surface(pose.last(),out,BottleCrateGeometry.bounds(slot,0),sprite.getU0(),sprite.getU1(),sprite.getV0(),sprite.getV1(),color,light,false,true);
        }
        // Complete each texture pass before changing buffers; a previous consumer may become invalid.
        for(int pass=0;pass<3;pass++) {
            String texture=pass==0?"sides":pass==1?"top":"cap";
            out=buffers.getBuffer(RenderType.entityCutout(GregTech.id("textures/block/iconsets/bottlecrate_bottle_"+texture+".png")));
            for(int slot=0;slot<9;slot++) if(!crate.items().getStackInSlot(slot).isEmpty()) {
                if(pass==0) for(int part=1;part<=2;part++) surface(pose.last(),out,BottleCrateGeometry.bounds(slot,part),0,1,0,1,0xFFFFFF,light,false,false);
                else surface(pose.last(),out,BottleCrateGeometry.bounds(slot,pass),0,1,0,1,0xFFFFFF,light,true,true);
            }
        }
    }

    private static void surface(PoseStack.Pose pose,VertexConsumer out,AABB b,float u0,float u1,float v0,float v1,int color,int light,boolean onlyTop,boolean top) {
        float x0=(float)b.minX,x1=(float)b.maxX,y0=(float)b.minY,y1=(float)b.maxY,z0=(float)b.minZ,z1=(float)b.maxZ;
        if(top) AnvilCuboidRenderer.face(pose,out,u0,u1,v0,v1,color,light,Direction.UP,new float[]{x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0});
        if(onlyTop)return;
        AnvilCuboidRenderer.face(pose,out,u0,u1,v0,v1,color,light,Direction.NORTH,new float[]{x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0});
        AnvilCuboidRenderer.face(pose,out,u0,u1,v0,v1,color,light,Direction.SOUTH,new float[]{x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1});
        AnvilCuboidRenderer.face(pose,out,u0,u1,v0,v1,color,light,Direction.WEST,new float[]{x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0});
        AnvilCuboidRenderer.face(pose,out,u0,u1,v0,v1,color,light,Direction.EAST,new float[]{x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1});
    }
}
