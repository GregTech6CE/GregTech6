package com.gregtech.gregtech.content.logistics;

/** GT6 Logistics Core's previous-pass CPU meters (MultiTileEntityLogisticsCore:210, 300-317). */
public final class LogisticsDisplayRules {
    public record Usage(int logic, int control, int storage, int conversion) {
        public static final Usage ZERO = new Usage(0, 0, 0, 0);

        public int forCover(String type) {
            return switch (type) {
                case "CPU_LOGIC" -> logic;
                case "CPU_CONTROL" -> control;
                case "CPU_STORAGE" -> storage;
                case "CPU_CONVERSION" -> conversion;
                default -> 0;
            };
        }
    }

    private LogisticsDisplayRules() {
    }

    /** The GT6 cover emits both strong and weak redstone from this 0..15 value. */
    public static int signal(int usedProcessors, int totalProcessors) {
        if (usedProcessors <= 0 || totalProcessors <= 0)
            return 0;
        if (usedProcessors >= totalProcessors)
            return 15;
        return 14 - (int) Math.max(0, Math.min(13, ((totalProcessors - usedProcessors) * 14L) / totalProcessors));
    }

    /** The GT6 texture ladder uses 0..10, separately from the redstone signal. */
    public static int visual(int usedProcessors, int totalProcessors) {
        if (usedProcessors <= 0 || totalProcessors <= 0)
            return 0;
        if (usedProcessors >= totalProcessors)
            return 10;
        return 9 - (int) Math.max(0, Math.min(8, ((totalProcessors - usedProcessors) * 9L) / totalProcessors));
    }
}
