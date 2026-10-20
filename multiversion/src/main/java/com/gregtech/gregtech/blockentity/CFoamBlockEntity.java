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
 * into {@code gt6ce:cfoam} — the same block GT6 swaps to.
 */
public class CFoamBlockEntity extends BlockEntity {
    /** GT6 waits a hundred ticks before the first drying roll. */
    public static final int GRACE_TICKS = com.gregtech.gregtech.block.ConstructionRules.FOAM_GRACE;
    /** GT6's {@code rng(5900) == 0} — about five minutes per block on average. */
    public static final int DRY_CHANCE = com.gregtech.gregtech.block.ConstructionRules.FOAM_DRY_CHANCE;

    private long timer;
    private boolean dried;

    public CFoamBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.CFOAM.get(), pos, state);
    }

    public boolean dried() { return dried; }

    public long timer() { return timer; }

    /** Repainting changes the registered block identity, but must not restart its drying grace period. */
    public void restoreTimer(long timer) { this.timer = Math.max(0, timer); setChanged(); }

    public void tick() {
        if (level == null || level.isClientSide || dried) return;
        timer++;
        if (!com.gregtech.gregtech.block.ConstructionRules.dries(timer,level.random::nextInt)) return;
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
        BlockState hardened = com.gregtech.gregtech.block.misc.FixedConstructionColor.recolor(
                CFoamBlock.hardened().defaultBlockState(),
                com.gregtech.gregtech.block.misc.FixedConstructionColor.colorOf(getBlockState()));
        return level.setBlock(worldPosition, hardened, 3);
    }

    @Override
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
        timer = tag.getLong("timer");
        dried = tag.getBoolean("dried");
    }

    @Override
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
        if (timer != 0) tag.putLong("timer", timer);
        if (dried) tag.putBoolean("dried", true);
    }
}
