package gregtech.blockentity;

import gregtech.internal.energy.EnergyBlockViews;
import gregtech.internal.energy.IEnergyBlock;
import gregtech.data.GregTechTags.Tag;
import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.util.function.BiFunction;

/** Forwards signed GT energy queries and external FE; subclasses own target resolution. */
public abstract class EnergyRelayBlockEntity extends CapabilityRelayBlockEntity implements IEnergyBlock {
//? if >=1.21.1 {
    /*private final Map<Direction, net.neoforged.neoforge.energy.IEnergyStorage> flux = new EnumMap<>(Direction.class);
*///?} else {
    private final Map<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.energy.IEnergyStorage>> flux = new EnumMap<>(
            Direction.class);
//?}
    private static final gregtech.content.logistics.RelayVisitSet<EnergyRelayBlockEntity> ACTIVE =
            new gregtech.content.logistics.RelayVisitSet<>();

    protected EnergyRelayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract boolean supportsEnergy();
//? if >=1.21.1 {
    /*public net.neoforged.neoforge.energy.IEnergyStorage fluxHandler(Direction side) {
        if (isRemoved() || !supportsEnergy() || side == null) return null;
        return flux.computeIfAbsent(side, key -> new net.neoforged.neoforge.energy.IEnergyStorage() {

            public int receiveEnergy(int amount, boolean simulate) {
                return amount <= 0 ? 0 : forward(key, net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, 0,
                        recipient -> recipient.receiveEnergy(amount, simulate));
            }

            public int extractEnergy(int amount, boolean simulate) {
                return amount <= 0 ? 0 : forward(key, net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, 0,
                        recipient -> recipient.extractEnergy(amount, simulate));
            }

            public int getEnergyStored() {
                return forward(key, net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, 0,
                        recipient -> recipient.getEnergyStored());
            }

            public int getMaxEnergyStored() {
                return forward(key, net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, 0,
                        recipient -> recipient.getMaxEnergyStored());
            }

            public boolean canExtract() {
                return forward(key, net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, false,
                        recipient -> recipient.canExtract());
            }

            public boolean canReceive() {
                return forward(key, net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, false,
                        recipient -> recipient.canReceive());
            }
        });
*///?} else {
    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,
            Direction side) {
        if (!isRemoved() && supportsEnergy() && side != null && cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY)
            return flux.computeIfAbsent(side, key -> net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.energy.IEnergyStorage() {

                public int receiveEnergy(int amount, boolean simulate) {
                    return amount <= 0 ? 0 : forward(key, net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, 0,
                            recipient -> recipient.receiveEnergy(amount, simulate));
                }

                public int extractEnergy(int amount, boolean simulate) {
                    return amount <= 0 ? 0 : forward(key, net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, 0,
                            recipient -> recipient.extractEnergy(amount, simulate));
                }

                public int getEnergyStored() {
                    return forward(key, net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, 0,
                            recipient -> recipient.getEnergyStored());
                }

                public int getMaxEnergyStored() {
                    return forward(key, net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, 0,
                            recipient -> recipient.getMaxEnergyStored());
                }

                public boolean canExtract() {
                    return forward(key, net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, false,
                            recipient -> recipient.canExtract());
                }

                public boolean canReceive() {
                    return forward(key, net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, false,
                            recipient -> recipient.canReceive());
                }
            })).cast();
        return super.getCapability(cap, side);
//?}
    }
//? if >=1.21.1 {
//?} else {
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        flux.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        flux.clear();
    }
//?}
    private <T> T forwardEnergy(Direction side, T absent, BiFunction<IEnergyBlock, Direction, T> operation) {
        if (!supportsEnergy() || side == null || level == null || isRemoved()) return absent;
        var visited = ACTIVE.get();
        if (!visited.add(this)) return absent;
        try {
            var destination = target(side);
            if (destination == null) return absent;
            var destinationLevel = targetLevel(destination);
            if (!destinationLevel.hasChunkAt(destination.position())) return absent;
            var entity = destinationLevel.getBlockEntity(destination.position());
            if (entity == null || entity.isRemoved()) return absent;
            IEnergyBlock energy = EnergyBlockViews.view(entity);
            if (energy == null) return absent;
            return operation.apply(energy, destination.side());
        } finally {
            visited.remove(this);
            if (visited.isEmpty()) ACTIVE.remove();
        }
    }

    @Override
    public boolean isEnergyType(Tag type, Direction side, boolean emitting) {
        return forwardEnergy(side, false, (recipient, recipientSide) -> recipient.isEnergyType(type, recipientSide, emitting));
    }

    @Override
    public Collection<Tag> getEnergyTypes(Direction side) {
        return forwardEnergy(side, List.of(), (recipient, recipientSide) -> List.copyOf(recipient.getEnergyTypes(recipientSide)));
    }

    @Override
    public boolean isEnergyAcceptingFrom(Tag type, Direction side, boolean theoretical) {
        return forwardEnergy(side, false, (recipient, recipientSide) -> recipient.isEnergyAcceptingFrom(type, recipientSide, theoretical));
    }

    @Override
    public boolean isEnergyEmittingTo(Tag type, Direction side, boolean theoretical) {
        return forwardEnergy(side, false, (recipient, recipientSide) -> recipient.isEnergyEmittingTo(type, recipientSide, theoretical));
    }

    @Override
    public long doEnergyInjection(Tag type, Direction side, long size, long amount, boolean execute) {
        return forwardEnergy(side, 0L, (recipient, recipientSide) -> recipient.doEnergyInjection(type, recipientSide, size, amount, execute));
    }

    @Override
    public long doEnergyExtraction(Tag type, Direction side, long size, long amount, boolean execute) {
        return forwardEnergy(side, 0L, (recipient, recipientSide) -> recipient.doEnergyExtraction(type, recipientSide, size, amount, execute));
    }

    @Override
    public long getEnergyDemanded(Tag type, Direction side, long size) {
        return size == 0 || size == Long.MIN_VALUE ? 0 :
                forwardEnergy(side, 0L, (recipient, recipientSide) -> recipient.getEnergyDemanded(type, recipientSide, size));
    }

    @Override
    public long getEnergyOffered(Tag type, Direction side, long size) {
        return size == 0 || size == Long.MIN_VALUE ? 0 :
                forwardEnergy(side, 0L, (recipient, recipientSide) -> recipient.getEnergyOffered(type, recipientSide, size));
    }

    @Override
    public long getEnergySizeInputMin(Tag type, Direction side) {
        return forwardEnergy(side, 0L, (recipient, recipientSide) -> recipient.getEnergySizeInputMin(type, recipientSide));
    }

    @Override
    public long getEnergySizeInputRecommended(Tag type, Direction side) {
        return forwardEnergy(side, 0L, (recipient, recipientSide) -> recipient.getEnergySizeInputRecommended(type, recipientSide));
    }

    @Override
    public long getEnergySizeInputMax(Tag type, Direction side) {
        return forwardEnergy(side, 0L, (recipient, recipientSide) -> recipient.getEnergySizeInputMax(type, recipientSide));
    }

    @Override
    public long getEnergySizeOutputMin(Tag type, Direction side) {
        return forwardEnergy(side, 0L, (recipient, recipientSide) -> recipient.getEnergySizeOutputMin(type, recipientSide));
    }

    @Override
    public long getEnergySizeOutputRecommended(Tag type, Direction side) {
        return forwardEnergy(side, 0L, (recipient, recipientSide) -> recipient.getEnergySizeOutputRecommended(type, recipientSide));
    }

    @Override
    public long getEnergySizeOutputMax(Tag type, Direction side) {
        return forwardEnergy(side, 0L, (recipient, recipientSide) -> recipient.getEnergySizeOutputMax(type, recipientSide));
    }

    @Override
    public boolean hasEnergySurface(Direction side) {
        return forwardEnergy(side, false, (recipient, recipientSide) -> recipient.hasEnergySurface(recipientSide));
    }

    @Override
    public boolean isEnergyCapacitorType(Tag type, Direction side) {
        return forwardEnergy(side, false, (recipient, recipientSide) -> recipient.isEnergyCapacitorType(type, recipientSide));
    }

    @Override
    public long getEnergyStored(Tag type, Direction side) {
        return forwardEnergy(side, 0L, (recipient, recipientSide) -> recipient.getEnergyStored(type, recipientSide));
    }

    @Override
    public long getEnergyCapacity(Tag type, Direction side) {
        return forwardEnergy(side, 0L, (recipient, recipientSide) -> recipient.getEnergyCapacity(type, recipientSide));
    }

    @Override
    public Collection<Tag> getEnergyCapacitorTypes(Direction side) {
        return forwardEnergy(side, List.of(), (recipient, recipientSide) -> List.copyOf(recipient.getEnergyCapacitorTypes(recipientSide)));
    }
}
