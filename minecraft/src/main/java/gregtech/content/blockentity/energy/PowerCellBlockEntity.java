package gregtech.content.blockentity.energy;

import gregtech.content.block.energy.PowerCellBlock;
import gregtech.content.blockentity.GTBlockEntity;
import gregtech.content.energy.PowerCellSpec;
import gregtech.content.item.PowerCellItem;
import gregtech.content.registry.GTBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** No energy-net emission: GT6's placed power cells are passive, not battery boxes. */
public final class PowerCellBlockEntity extends GTBlockEntity {
    private long charge;

    public PowerCellBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.POWER_CELL.get(), pos, state);
        charge = spec().capacity();
    }

    public PowerCellSpec spec() {
        return ((PowerCellBlock) getBlockState().getBlock()).spec();
    }

    public long stored() {
        return charge;
    }

    public void setCharge(long value) {
        charge = spec().usableRemainder(Math.max(0, value));
        setChanged();
        if (level != null && !level.isClientSide) {
            var state = getBlockState().setValue(PowerCellBlock.CHARGE_LEVEL, spec().display(charge));
            level.setBlock(worldPosition, state, 3);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public ItemStack asItem() {
        var stack = new ItemStack(getBlockState().getBlock());
        ((PowerCellItem) stack.getItem()).setCharge(stack, charge);
        return ((PowerCellItem) stack.getItem()).afterEnergyTransfer(stack);
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.putLong(PowerCellItem.CHARGE, charge);
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        charge = Math.max(0, Math.min(spec().capacity(), tag.getLong(PowerCellItem.CHARGE)));
    }

    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        writeWithoutMetadata(tag, lookup);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
