package gregtech.content.blockentity.energy;

import gregtech.content.block.energy.ChemicalBatteryBlock;
import gregtech.content.blockentity.GTBlockEntity;
import gregtech.content.energy.PortableBatterySpec;
import gregtech.content.item.ChemicalBatteryItem;
import gregtech.content.registry.GTChemicalBatteries;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** No energy-net emission: GT6's placed chemical batteries are passive, not battery boxes. */
public final class ChemicalBatteryBlockEntity extends GTBlockEntity {
    private long charge;

    public ChemicalBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(GTChemicalBatteries.CHEMICAL_BATTERY.get(), pos, state);
    }

    public PortableBatterySpec spec() {
        return ((ChemicalBatteryBlock) getBlockState().getBlock()).spec();
    }

    public long stored() {
        return charge;
    }

    public void setCharge(long value) {
        charge = Math.max(0, Math.min(spec().capacity(), value));
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public ItemStack asItem() {
        var stack = new ItemStack(getBlockState().getBlock());
        ((ChemicalBatteryItem) stack.getItem()).setCharge(stack, charge);
        return stack;
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.putLong(ChemicalBatteryItem.CHARGE, charge);
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        charge = Math.max(0, Math.min(spec().capacity(), tag.getLong(ChemicalBatteryItem.CHARGE)));
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
