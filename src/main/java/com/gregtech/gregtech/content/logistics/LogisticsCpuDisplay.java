package com.gregtech.gregtech.content.logistics;

/** GT6 Logistics Core's previous-pass CPU meters (MultiTileEntityLogisticsCore:210, 300-317). */
public final class LogisticsCpuDisplay {
    public record Usage(int logic, int control, int storage, int conversion) {
        public static final Usage ZERO = new Usage(0, 0, 0, 0);

        public int forCover(LogisticsCoverType type) {
            return switch (type) {
                case CPU_LOGIC -> logic;
                case CPU_CONTROL -> control;
                case CPU_STORAGE -> storage;
                case CPU_CONVERSION -> conversion;
                default -> 0;
            };
        }
    }

    private LogisticsCpuDisplay() {}

    /** The GT6 cover emits both strong and weak redstone from this 0..15 value. */
    public static int signal(int used, int total) {
        return LogisticsDisplayRules.signal(used,total);
    }

    /** The GT6 texture ladder uses 0..10, separately from the redstone signal. */
    public static int visual(int used, int total) {
        return LogisticsDisplayRules.visual(used,total);
    }
}
