package com.gregtech.gregtech.content.tool;

/** MultiItemRandomTools:516-518 and EnergyStat:64-75,108-124. */
public final class ScannerEnergyRules {
    private ScannerEnergyRules() {}

    public static final String ENERGY_KEY = "gt.energy";
    public static final long SECTION_COST = 512;
    public static final long CROP_DISCOVERY_COST = 32768;
    public static final long CROP_RESCAN_COST = 512;

    public record Spec(String id, long capacity, long voltage, int scanLevel, boolean debug) {
        public long minimumPacket() { return debug ? 1 : voltage / 2; }
        public long maximumPacket() { return debug ? Long.MAX_VALUE : voltage * 2; }
        public boolean scansBlocks() { return scanLevel > 0; }

        public boolean acceptsPacket(int count, long size) {
            return debug || count == 1 && size >= minimumPacket() && size <= maximumPacket();
        }

        /** The original tools cap each call at 64 packets, independently of their voltage. */
        public long injectionPackets(int count, long stored, long size, long requested) {
            if (debug) return requested;
            if (size == Long.MIN_VALUE || requested <= 0) return 0;
            long packet = Math.abs(size);
            if (!acceptsPacket(count, packet) || stored >= capacity) return 0;
            return Math.min(Math.min(64, requested), Math.max(1, (capacity - stored) / packet));
        }
    }

    public static final Spec PORTABLE = new Spec("portable_scanner", 4096000, 512, 2, false);
    public static final Spec CROP = new Spec("portable_cropnalyzer", 1024000, 128, 0, false);
    public static final Spec DEBUG = new Spec("debug_scanner", 8000000000000000000L,
            4000000000000000000L, Integer.MAX_VALUE, true);

    public static Spec forItem(String id) {
        return switch (id) {
            case "portable_scanner" -> PORTABLE;
            case "portable_cropnalyzer" -> CROP;
            case "debug_scanner" -> DEBUG;
            default -> null;
        };
    }

    public record Use(boolean successful, long remaining) {}

    /** An unsuccessful real use drains the partial charge too; creative/debug uses do not. */
    public static Use use(long stored, long cost, boolean free) {
        if (free) return new Use(true, stored);
        if (cost < 0) throw new IllegalArgumentException("Negative scan cost");
        return new Use(stored >= cost, Math.max(0, stored - cost));
    }
}
