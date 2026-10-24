package gregtech.api.multiblock.tracking;

import gregtech.GregTechConfig;

/** {@code multiblock.structureTracking}. Tests can force a mode before the config file is loaded. */
public final class StructureTrackingMode {
    private static String override;

    private StructureTrackingMode() {}

    public static boolean legacy() {
        String forced = override;
        if (forced != null) return "legacy".equalsIgnoreCase(forced);
        return GregTechConfig.structureTrackingLegacy();
    }

    /** {@code null} clears the override. {@code tracked} and {@code legacy} are the config values. */
    public static void overrideForTests(String mode) {
        override = mode;
    }
}
