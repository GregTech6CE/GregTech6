package com.gregtech.gregtech.api.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

/** Platform FE view of the shared long packet buffer; keeps the original API. */
public class GTEnergyStore extends PacketEnergyBuffer {
    private final IEnergyStorage adapter = new Adapter();
    public GTEnergyStore(long capacity) { super(capacity); }
    public IEnergyStorage forgeAdapter() { return adapter; }
    private final class Adapter implements IEnergyStorage {
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) return 0;
            long packets = GTEnergyStore.this.receiveEnergy(lastSize(), maxReceive / Math.max(1, lastSize()), !simulate);
            return (int) Math.min(Integer.MAX_VALUE, packets * lastSize());
        }
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (maxExtract <= 0) return 0;
            long packets = GTEnergyStore.this.extractEnergy(lastSize(), maxExtract / Math.max(1, lastSize()), !simulate);
            return (int) Math.min(Integer.MAX_VALUE, packets * lastSize());
        }
        public int getEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, stored()); }
        public int getMaxEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, capacity()); }
        public boolean canExtract() { return GTEnergyStore.this.canExtract(); }
        public boolean canReceive() { return GTEnergyStore.this.canReceive(); }
    }
}
