package com.gregtech.gregtech.api.machine;

import net.minecraftforge.client.model.data.ModelProperty;

/**
 * Model data keys shared between pipe/wire block entities (common code) and the
 * client-side dynamic baked model. Forge's model-data classes are server-safe.
 */
public final class PipeModelData {

    /**
     * Per-direction neighbor half-thickness in px (indexed by Direction ordinal);
     * {@link PipeGeometry#NONE} where the neighbor is not part of the same
     * pipe/wire family.
     */
    public static final ModelProperty<float[]> NEIGHBOR_HALVES = new ModelProperty<>();

    private PipeModelData() {}

    /**
     * Client chunk-border fix: when a pipe chunk loads, connectors already baked in
     * neighbouring chunks never learn about it and keep their stale thin-into-thick
     * extensions. Poke any connector neighbour that lives in another chunk.
     */
    public static void refreshCrossChunkNeighbors(net.minecraft.world.level.Level level,
                                                  net.minecraft.core.BlockPos pos) {
        if (level == null || !level.isClientSide) return;
        net.minecraft.world.level.ChunkPos myChunk = new net.minecraft.world.level.ChunkPos(pos);
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            net.minecraft.core.BlockPos npos = pos.relative(d);
            if (myChunk.equals(new net.minecraft.world.level.ChunkPos(npos))) continue;
            if (!level.hasChunkAt(npos)) continue;
            net.minecraft.world.level.block.entity.BlockEntity nbe = level.getBlockEntity(npos);
            if (nbe == null) continue;
            if (nbe.getModelData().has(NEIGHBOR_HALVES)) {
                nbe.requestModelDataUpdate();
                net.minecraft.world.level.block.state.BlockState ns = level.getBlockState(npos);
                level.sendBlockUpdated(npos, ns, ns, 8);
            }
        }
    }
}
