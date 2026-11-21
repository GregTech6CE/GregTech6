package gregtech.content.blockentity.machine;

import gregtech.engine.machine.MachineSpec;
import gregtech.content.block.machine.GTFacingMachineBlock;
import gregtech.content.blockentity.GTEnergyBlockEntity;
import gregtech.content.data.GregTechConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.items.ItemStackHandler;
//?}

import javax.annotation.Nullable;

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
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        gregtech.engine.stack.HandlerNbt.load(inventory, tag.getCompound("gt.inv"), lookup);
        if (tag.contains(GregTechConstants.NBT_EFFICIENCY)) {
            // spec is fixed per block; stored value ignored for now
        }
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.put("gt.inv", gregtech.engine.stack.HandlerNbt.save(inventory, lookup));
    }
}
