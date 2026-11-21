package gregtech.content.block.misc;

import gregtech.content.blockentity.GTEnergyBlockEntity;
import gregtech.content.data.GregTechTags;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Packet input and persistence shared by automatic tools. */
public abstract class AutoToolBlockEntity extends GTEnergyBlockEntity {
    protected long energy;
    protected boolean stopped;

    protected AutoToolBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract GregTechTags.Tag energyType();

    public abstract void tick();

    public Direction facing() {
        return getBlockState().getValue(AutoToolBlock.FACING);
    }

    public long input() {
        return ((AutoToolBlock) getBlockState().getBlock()).input();
    }

    public int quality() {
        return ((AutoToolBlock) getBlockState().getBlock()).quality();
    }

    public boolean stopped() {
        return stopped;
    }

    public void setStopped(boolean value) {
        stopped = value;
        syncToClient();
    }

    public long storedEnergy() {
        return energy;
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) {
        return !emitting && type == energyType();
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) {
        return List.of(energyType());
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return isEnergyType(type, side, false) && (theoretical || !stopped) && acceptsSide(side);
    }

    protected abstract boolean acceptsSide(Direction side);

    @Override
    public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) {
        return isEnergyAcceptingFrom(type, side, false) ? 1 : 0;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) {
        return 0;
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) {
        return input();
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) {
        return 0;
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.putLong("gt.energy", energy);
        tag.putBoolean("gt.stopped", stopped);
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        energy = Math.max(0, tag.getLong("gt.energy"));
        stopped = tag.getBoolean("gt.stopped");
    }
}
