package com.gregtech.gregtech.blockentity.tool;

import com.gregtech.gregtech.block.tool.DynamiteBlock;
import com.gregtech.gregtech.item.behavior.ItemBehaviors;
import com.gregtech.gregtech.item.behavior.BehaviorSprayExtinguisher;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** GT6 MultiTileEntityDynamite:61-138: persisted fuse, ignition and defusing. */
public final class DynamiteBlockEntity extends BlockEntity implements ItemBehaviors.Ignitable,
        BehaviorSprayExtinguisher.Extinguishable {
    private int remaining;
    private boolean firstTick = true;
    public DynamiteBlockEntity(BlockPos pos, BlockState state) { super(GTBlockEntities.DYNAMITE.get(), pos, state); }
    public int remainingTicks() { return remaining; }
    public void armRemote() { if (remaining == 0 || remaining > 20) setFuse(20); }
    private void setFuse(int ticks) {
        remaining = ticks;
        setChanged();
        if (level != null && !level.isClientSide && getBlockState().getBlock() instanceof DynamiteBlock) {
            var state = getBlockState();
            if (state.getValue(DynamiteBlock.ARMED) != (ticks > 0))
                level.setBlock(worldPosition, state.setValue(DynamiteBlock.ARMED, ticks > 0), Block.UPDATE_ALL);
        }
    }
    public void serverTick() {
        if (level == null || level.isClientSide || !(getBlockState().getBlock() instanceof DynamiteBlock block)) return;
        if (firstTick) {
            firstTick = false;
            block.refreshSupport(level, worldPosition);
            block.checkFuseInputs(level,worldPosition);
        }
        if (remaining > 0) {
            remaining--;
            setChanged();
            if (remaining == 0) block.detonate(level, worldPosition);
        }
    }
    @Override public long onIgnite(Level world, BlockPos pos, Direction side, Player player, ItemStack tool,
                                    boolean sneaking, float x, float y, float z) {
        if (world.isClientSide || remaining != 0) return 0;
        setFuse(100);
        world.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1, .5f);
        return 10000;
    }
    @Override public long onExtinguish(Level world, BlockPos pos, Direction side, Player player, ItemStack tool,
                                       boolean sneaking, float x, float y, float z) {
        if (world.isClientSide || remaining == 0) return 0;
        setFuse(0);
        world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, .5f);
        return 10000;
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("Fuse", remaining); }
    @Override public void load(CompoundTag tag) { super.load(tag); remaining = Math.max(0, Math.min(100, tag.getInt("Fuse"))); }
}
