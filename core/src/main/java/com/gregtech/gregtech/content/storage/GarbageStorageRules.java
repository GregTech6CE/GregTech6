package com.gregtech.gregtech.content.storage;

/** Port retention limits, GUI window and transfer schedule. */
public final class GarbageStorageRules {

    private GarbageStorageRules() {
    }

    public static final int MAX_ENTRIES = 256, VIEW_SLOTS = 54;

    public static boolean binDue(long time) {
        return time % 100 == 50;
    }

    public static boolean dumpDue(long time) {
        return time % 10 == 0;
    }

    public static int take(long count, int requested, int limit) {
        return (int) Math.min(Math.min(count, requested), limit);
    }

    public static int fluidMerge(int held, int added) {
        return (int) Math.min(Integer.MAX_VALUE, (long) held + added);
    }
}
