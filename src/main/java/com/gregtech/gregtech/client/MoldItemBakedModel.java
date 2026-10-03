package com.gregtech.gregtech.client;

import com.gregtech.gregtech.block.machine.MoldItemData;
import com.gregtech.gregtech.api.machine.crucible.MoldShapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import java.util.*;

/** The same 5x5 cavity grid as the world mold, selected from the native block-item shape data. */
public final class MoldItemBakedModel extends BakedModelWrapper<BakedModel> {
    private final List<BakedQuad> grid;
    private final ItemOverrides overrides;
    public MoldItemBakedModel(BakedModel hull) { this(hull,0,false); }
    private MoldItemBakedModel(BakedModel hull,int shape,boolean fixed) {
        super(hull);
        grid=grid(shape);
        var variants=new LinkedHashMap<Integer,BakedModel>(16,0.75f,true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Integer,BakedModel> entry) {return size()>32;}
        };
        overrides=fixed ? ItemOverrides.EMPTY : new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel original,ItemStack stack,@Nullable ClientLevel level,
                                                @Nullable LivingEntity entity,int seed) {
                int shape=MoldItemData.shape(stack)&MoldShapes.SHAPE_MASK;
                if(shape==0)return original;
                return variants.computeIfAbsent(shape,key->new MoldItemBakedModel(hull,key,true));
            }
        };
    }
    private static List<BakedQuad> grid(int shape) {
        var sprite=Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(ResourceLocation.fromNamespaceAndPath("gregtech","block/material_icons/metallic/blocksolid"));
        var bakery=new FaceBakery();
        var quads=new ArrayList<BakedQuad>();
        // Existing world mold geometry: 12px square, 2.4px cells, y=1..3px; bit 1 is a cavity.
        for(int row=0;row<5;row++)for(int col=0;col<5;col++) {
            if((shape&(1<<(row*5+col)))!=0)continue;
            float x=2+col*2.4f,z=2+row*2.4f;
            var from=new Vector3f(x,1,z);var to=new Vector3f(x+2.4f,3,z+2.4f);
            for(var direction:Direction.values()) {
                float[] uv=direction.getAxis()==Direction.Axis.Y ? new float[]{x,z,x+2.4f,z+2.4f}
                        : new float[]{x,13,x+2.4f,15};
                var face=new BlockElementFace(null,0,"",new BlockFaceUV(uv,0));
                quads.add(bakery.bakeQuad(from,to,face,sprite,direction,BlockModelRotation.X0_Y0,null,true,
                        ResourceLocation.fromNamespaceAndPath("gregtech","mold_item_grid")));
            }
        }
        return List.copyOf(quads);
    }
    private List<BakedQuad> withGrid(List<BakedQuad> hull,@Nullable Direction side) {
        if(side!=null||grid.isEmpty())return hull;
        var result=new ArrayList<BakedQuad>(hull);result.addAll(grid);return result;
    }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state,@Nullable Direction side,RandomSource random) {
        return withGrid(super.getQuads(state,side,random),side);
    }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state,@Nullable Direction side,RandomSource random,
                                             ModelData data,@Nullable net.minecraft.client.renderer.RenderType layer) {
        return withGrid(super.getQuads(state,side,random,data,layer),side);
    }
    @Override public ItemOverrides getOverrides(){return overrides;}
}
