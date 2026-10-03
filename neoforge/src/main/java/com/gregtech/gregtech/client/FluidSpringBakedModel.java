package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.FluidSpringBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Original solid spring: each face has the stored fluid beneath the fixed spring overlay. */
public final class FluidSpringBakedModel implements BakedModel {
    private final BakedModel inner;
    private final Fluid itemFluid;
    public FluidSpringBakedModel(BakedModel inner) { this(inner, Fluids.WATER); }
    private FluidSpringBakedModel(BakedModel inner, Fluid fluid) { this.inner=inner; this.itemFluid=fluid; }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random) {
        return remap(inner.getQuads(state, face, random), itemFluid);
    }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random,
                                             ModelData data, @Nullable net.minecraft.client.renderer.RenderType type) {
        Fluid fluid=data.get(FluidSpringBlockEntity.MODEL_FLUID);
        return remap(inner.getQuads(state,face,random,data,type), fluid==null?itemFluid:fluid);
    }
    private static List<BakedQuad> remap(List<BakedQuad> source, Fluid fluid) {
        var texture=IClientFluidTypeExtensions.of(fluid).getStillTexture();
        if (texture==null) return source;
        var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        var result=new ArrayList<BakedQuad>(source.size());
        for(var quad:source) {
            if(quad.getTintIndex()!=0) { result.add(quad); continue; }
            int[] vertices=quad.getVertices().clone();
            FluidItemBakedModel.remapUv(vertices,quad.getSprite(),sprite);
            result.add(new BakedQuad(vertices,0,quad.getDirection(),sprite,quad.isShade(),quad.hasAmbientOcclusion()));
        }
        return result;
    }
    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state,RandomSource random,ModelData data) {
        return inner.getRenderTypes(state,random,data);
    }
    @Override public List<net.minecraft.client.renderer.RenderType> getRenderTypes(ItemStack stack,boolean fabulous) {
        return inner.getRenderTypes(stack,fabulous);
    }
    @Override public ItemOverrides getOverrides() {
        return new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model,ItemStack stack,@Nullable ClientLevel level,@Nullable LivingEntity entity,int seed) {
                return new FluidSpringBakedModel(inner,FluidSpringClient.fluid(stack));
            }
        };
    }
    @Override public TextureAtlasSprite getParticleIcon(){return inner.getParticleIcon();}
    @Override public boolean useAmbientOcclusion(){return inner.useAmbientOcclusion();}
    @Override public boolean isGui3d(){return true;}
    @Override public boolean usesBlockLight(){return inner.usesBlockLight();}
    @Override public boolean isCustomRenderer(){return false;}
    @Override public ItemTransforms getTransforms(){return inner.getTransforms();}
}
