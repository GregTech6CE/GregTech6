package com.gregtech.gregtech;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.ModConfigSpec;
*///?} else {
import net.minecraftforge.common.ForgeConfigSpec;
//?}

public final class GregTechConfig {
//? if >=1.21.1 {
    /*private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
*///?} else {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
//?}

//? if >=1.21.1 {
    /*private static final ModConfigSpec.BooleanValue LOG_MATERIALS = BUILDER
*///?} else {
    private static final ForgeConfigSpec.BooleanValue LOG_MATERIALS = BUILDER
//?}
            .comment("Log material registry size during common setup")
            .define("logMaterials", true);

//? if >=1.21.1 {
    /*private static final ModConfigSpec.BooleanValue MACHINE_EXPLOSIONS = BUILDER
*///?} else {
    private static final ForgeConfigSpec.BooleanValue MACHINE_EXPLOSIONS = BUILDER
//?}
            .comment("GT6 behaviour: machines explode when fed a higher voltage than they accept")
            .define("machineOvervoltageExplosions", true);

//? if >=1.21.1 {
    /*public static final ModConfigSpec SPEC = BUILDER.build();
*///?} else {
    static final ForgeConfigSpec SPEC = BUILDER.build();
//?}

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
