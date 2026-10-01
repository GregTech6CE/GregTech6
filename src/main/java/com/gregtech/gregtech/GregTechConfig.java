package com.gregtech.gregtech;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GregTechConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_MATERIALS = BUILDER
            .comment("Log material registry size during common setup")
            .define("logMaterials", true);

    private static final ForgeConfigSpec.BooleanValue MACHINE_EXPLOSIONS = BUILDER
            .comment("GT6 behaviour: machines explode when fed a higher voltage than they accept")
            .define("machineOvervoltageExplosions", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logMaterials = true;

    public static boolean machineOvervoltageExplosions() {
        try {
            return MACHINE_EXPLOSIONS.get();
        } catch (IllegalStateException e) {
            return true; // config not loaded yet (shouldn't happen during world ticks)
        }
    }

    private GregTechConfig() {}
}
