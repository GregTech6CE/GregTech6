package com.gregtech.gregtech.client;

import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?} else {
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
//?}
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Picks a pre-baked design model from {@code gt.design}. Index 0 keeps the block's own model. */
public final class PartDesignBakedModel extends BakedModelWrapper<BakedModel> {
    private final BakedModel[] designs;

    public PartDesignBakedModel(BakedModel original, BakedModel[] designs) {
        super(original);
        this.designs = designs;
    }

    private BakedModel choose(ModelData data) {
        Integer design = data == null ? null : data.get(MultiblockPortBlockEntity.DESIGN);
        if (design == null || design <= 0 || design >= designs.length || designs[design] == null) return originalModel;
        return designs[design];
    }

    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                             ModelData data, @Nullable net.minecraft.client.renderer.RenderType layer) {
        return choose(data).getQuads(state, side, random, data, layer);
    }

    @Override public TextureAtlasSprite getParticleIcon(ModelData data) {
        return choose(data).getParticleIcon(data);
    }
}
