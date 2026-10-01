package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.block.machine.GTFacingMachineBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Facing machine base (GT6 {@code TileEntityBase09FacingSingle} subset): horizontal front + 2-slot inventory.
 */
public abstract class GTFacingMachineBlockEntity extends GTEnergyBlockEntity {
    protected final MachineSpec spec;
    protected final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public int getSlotLimit(int slot) {
            return GTFacingMachineBlockEntity.this.getInventorySlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            GTFacingMachineBlockEntity.this.setChanged();
        }
    };

    protected int getInventorySlotLimit(int slot) {
        return 64;
    }

    protected GTFacingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineSpec spec) {
        super(type, pos, state);
        this.spec = spec;
    }

    public MachineSpec spec() {
        return spec;
    }

    public Direction getFrontFacing() {
        if (getBlockState().getBlock() instanceof GTFacingMachineBlock facingBlock) {
            return facingBlock.getFrontFacing(getBlockState());
        }
        return Direction.NORTH;
    }

    protected BlockPos frontPos() {
        return worldPosition.relative(getFrontFacing());
    }

    protected boolean isFrontAir() {
        if (level == null) {
            return false;
        }
        BlockState front = level.getBlockState(frontPos());
        return front.isAir() || front.canBeReplaced();
    }

    protected void setMachineLit(boolean lit) {
        if (level == null || level.isClientSide || !(getBlockState().getBlock() instanceof GTFacingMachineBlock machineBlock)) {
            return;
        }
        BlockState state = getBlockState();
        if (state.getValue(GTFacingMachineBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(GTFacingMachineBlock.LIT, lit), 3);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("gt.inv"));
        if (tag.contains(GregTechConstants.NBT_EFFICIENCY)) {
            // spec is fixed per block; stored value ignored for now
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("gt.inv", inventory.serializeNBT());
    }
}
