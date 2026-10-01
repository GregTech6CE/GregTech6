package com.gregtech.gregtech.api.energy;


/**
 * GT6 long-based energy buffer shared by the platform adapters.
 * Packet model: {@code size} (voltage) &times; {@code amount} (amperage).
 */
public class PacketEnergyBuffer {
    private long energyStored;
    private long energyCapacity;
    private long lastSize = 32;
    private long lastAmount = 1;

    private boolean canReceive = true;
    private boolean canExtract;

    public PacketEnergyBuffer(long capacity) {
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

}
