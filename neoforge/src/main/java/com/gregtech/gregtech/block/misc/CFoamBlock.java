package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.blockentity.CFoamBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTConstructionBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.Collections;
import java.util.List;

/**
 * GT6's construction foam ({@code MultiTileEntityCFoam} and the hardened {@code BlocksGT.CFoam}).
 *
 * <p>The freshly sprayed block is wet: it cushions falls, does not count as a solid surface, and only
 * hardens after GT6's drying roll — 100 ticks of grace and then a {@code rng(5900) == 0} chance per
 * tick ({@code MultiTileEntityCFoam.onTick2}), about five minutes on average. A wet block that is
 * removed is scraped away (GT6's {@code removeFoam} sets the block to air), so it drops nothing; the
 * hardened block behaves like an ordinary building block.
 */
public class CFoamBlock extends Block implements EntityBlock {
    private final boolean fresh;

    public CFoamBlock(Properties properties) {
        this(false, properties);
    }

    public CFoamBlock(boolean fresh, Properties properties) {
        super(properties);
        this.fresh = fresh;
    }

    /** True for the still wet foam that dries over time. */
    public boolean fresh() { return fresh; }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 0.5f, level.damageSources().fall());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return fresh ? new CFoamBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!fresh || level.isClientSide) return null;
        return (l, p, s, be) -> {
            if (be instanceof CFoamBlockEntity foam) foam.tick();
        };
    }

    /** GT6's {@code removeFoam}: wet foam is scraped away, only the hardened block drops itself. */
    @Override
    public List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (fresh) return Collections.emptyList();
        return List.of(new net.minecraft.world.item.ItemStack(this));
    }

    /** The hardened block this foam turns into (GT6's {@code BlocksGT.CFoam}). */
    public static Block hardened() {
        return GTConstructionBlocks.CFOAM.get();
    }

    /** True when this block is the wet variant the port tracks with its block entity. */
    public static boolean isFresh(BlockState state) {
        return state.getBlock() instanceof CFoamBlock foam && foam.fresh();
    }

    /** The block entity type used by the wet foam. */
    public static BlockEntityType<?> blockEntityType() {
        return GTBlockEntities.CFOAM.get();
    }
}
