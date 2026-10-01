package com.gregtech.gregtech.platform.neoforge.smeltery;

import com.gregtech.gregtech.data.GregTechConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Registry-aware NBT/network/environment boundary for the Neo smeltery. */
public abstract class SmelteryEntity extends BlockEntity {
    protected SmelteryEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    protected long environmentTemperature() {
        return level == null ? GregTechConstants.DEF_ENV_TEMP
                : GregTechConstants.C + Math.round(level.getBiome(worldPosition).value().getBaseTemperature() * 20.0F);
    }
    protected void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    protected void meltdown() {
        if (level != null && !level.isClientSide)
            level.setBlock(worldPosition, Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 4), Block.UPDATE_ALL);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, lookup);
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
