package com.gregtech.gregtech.blockentity;

import com.gregtech.gregtech.block.misc.CFoamBlock;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6's drying construction foam ({@code MultiTileEntityCFoam}).
 *
 * <p>GT6 rolls once per tick after a hundred tick grace period: {@code aTimer >= 100 && !mFoamDried
 * && rng(5900) == 0} sets {@code mFoamDried} and swaps in the hardened texture. A player can also dry
 * it by hand ({@code dryFoam}, which the port exposes as {@link #dry()}). The port turns the wet block
 * into {@code gregtech:cfoam} — the same block GT6 swaps to.
 */
public class CFoamBlockEntity extends BlockEntity {
    /** GT6 waits a hundred ticks before the first drying roll. */
    public static final int GRACE_TICKS = 100;
    /** GT6's {@code rng(5900) == 0} — about five minutes per block on average. */
    public static final int DRY_CHANCE = 5900;

    private long timer;
    private boolean dried;

    public CFoamBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.CFOAM.get(), pos, state);
    }

    public boolean dried() { return dried; }

    public long timer() { return timer; }

    public void tick() {
        if (level == null || level.isClientSide || dried) return;
        timer++;
        if (timer < GRACE_TICKS) return;
        if (level.random.nextInt(DRY_CHANCE) != 0) return;
        dry();
    }

    /**
     * GT6's {@code dryFoam}: the wet foam becomes the hardened block. Public so the GameTest (and a
     * future spray can or drying tool) can trigger it without waiting for the roll.
     *
     * @return true when this call dried the foam
     */
    public boolean dry() {
        if (level == null || level.isClientSide || dried) return false;
        dried = true;
        BlockState hardened = CFoamBlock.hardened().defaultBlockState();
        return level.setBlock(worldPosition, hardened, 3);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        timer = tag.getLong("timer");
        dried = tag.getBoolean("dried");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (timer != 0) tag.putLong("timer", timer);
        if (dried) tag.putBoolean("dried", true);
    }
}
