package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.machine.GTFacingMachineBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Picks facing + lit overlay variant; tint via block/item color handlers ({@code tintindex} 0). */
public final class BurningBoxBakedModel implements BakedModel {
    private static final ResourceLocation PARTICLE =
            GregTech.id("block/machines/generators/burning_solid/colored/top");

    private final Map<Direction, BakedModel> idleByFacing;
    private final Map<Direction, BakedModel> activeByFacing;
    private final BakedModel itemModel;
    @Nullable
    private TextureAtlasSprite particle;

    public BurningBoxBakedModel(Map<Direction, BakedModel> idleByFacing,
                                Map<Direction, BakedModel> activeByFacing,
                                BakedModel itemModel) {
        this.idleByFacing = idleByFacing;
        this.activeByFacing = activeByFacing;
        this.itemModel = itemModel;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return delegate(state).getQuads(state, direction, random);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        return delegate(state).getQuads(state, direction, random, data, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return delegate(state).getRenderTypes(state, rand, data);
    }

    private BakedModel delegate(@Nullable BlockState state) {
        if (state == null) {
            return itemModel;
        }
        Direction facing = state.getValue(GTFacingMachineBlock.FACING);
        boolean lit = state.getValue(GTFacingMachineBlock.LIT);
        Map<Direction, BakedModel> map = lit ? activeByFacing : idleByFacing;
        return map.getOrDefault(facing, idleByFacing.get(Direction.NORTH));
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        if (particle == null) {
            particle = Minecraft.getInstance().getModelManager()
                    .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(PARTICLE);
        }
        return particle;
    }

    @Override
    public ItemTransforms getTransforms() {
        ItemTransforms block = BlockItemModelHelper.blockItemTransforms();
        return block != ItemTransforms.NO_TRANSFORMS ? block : itemModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
