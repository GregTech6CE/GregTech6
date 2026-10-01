package com.gregtech.gregtech.api.energy;

import net.minecraftforge.energy.IEnergyStorage;

/**
 * GT6 long-based energy buffer with Forge {@link IEnergyStorage} adapter.
 * Packet model: {@code size} (voltage) &times; {@code amount} (amperage).
 */
public class GTEnergyStore {
    private long energyStored;
    private long energyCapacity;
    private long lastSize = 32;
    private long lastAmount = 1;

    private final IEnergyStorage forgeAdapter = new ForgeAdapter();
    private boolean canReceive = true;
    private boolean canExtract;

    public GTEnergyStore(long capacity) {
        this.energyCapacity = Math.max(1, capacity);
    }

    // ── GT6 packet model ─────────────────────────────────────────────────

    /**
     * Inject energy using the GT6 packet model.
     * @param size     voltage per packet
     * @param amount   number of packets
     * @param doInject if false, only simulate
     * @return amount actually accepted (in packets)
     */
    public long receiveEnergy(long size, long amount, boolean doInject) {
        if (size <= 0 || amount <= 0 || !canReceive) return 0;
        long total = size * amount;
        long space = energyCapacity - energyStored;
        if (space <= 0) return 0;
        long accepted = Math.min(total, space);
        long acceptedPackets = accepted / size;
        if (acceptedPackets <= 0) return 0;
        if (doInject) {
            energyStored += acceptedPackets * size;
            lastSize = size;
            lastAmount = amount;
        }
        return acceptedPackets;
    }

    /**
     * Extract energy using the GT6 packet model.
     * @return amount actually extracted (in packets)
     */
    public long extractEnergy(long size, long amount, boolean doExtract) {
        if (size <= 0 || amount <= 0 || !canExtract) return 0;
        long total = size * amount;
        long available = Math.min(total, energyStored);
        long extractedPackets = available / size;
        if (extractedPackets <= 0) return 0;
        if (doExtract) {
            energyStored -= extractedPackets * size;
            lastSize = size;
            lastAmount = amount;
        }
        return extractedPackets;
    }

    // ── Long accessors ───────────────────────────────────────────────────

    public long stored() { return energyStored; }
    public long capacity() { return energyCapacity; }
    public long lastSize() { return lastSize; }
    public long lastAmount() { return lastAmount; }
    public boolean canReceive() { return canReceive; }
    public boolean canExtract() { return canExtract; }

    public void setStored(long v) { this.energyStored = Math.min(v, energyCapacity); }
    public void setCapacity(long v) { this.energyCapacity = Math.max(1, v); if (energyStored > energyCapacity) energyStored = energyCapacity; }
    public void setCanReceive(boolean v) { this.canReceive = v; }
    public void setCanExtract(boolean v) { this.canExtract = v; }

    // ── Forge IEnergyStorage adapter ─────────────────────────────────────

    public IEnergyStorage forgeAdapter() { return forgeAdapter; }

    private class ForgeAdapter implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) return 0;
            long packets = GTEnergyStore.this.receiveEnergy(lastSize, maxReceive / Math.max(1, lastSize), !simulate);
            return (int) Math.min(Integer.MAX_VALUE, packets * lastSize);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (maxExtract <= 0) return 0;
            long packets = GTEnergyStore.this.extractEnergy(lastSize, maxExtract / Math.max(1, lastSize), !simulate);
            return (int) Math.min(Integer.MAX_VALUE, packets * lastSize);
        }

        @Override
        public int getEnergyStored() {
            return energyStored > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) energyStored;
        }

        @Override
        public int getMaxEnergyStored() {
            return energyCapacity > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) energyCapacity;
        }

        @Override
        public boolean canExtract() { return canExtract; }

        @Override
        public boolean canReceive() { return canReceive; }
    }
}
