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
    /*private static final ModConfigSpec.BooleanValue OVERLOAD_EXPLOSIONS = BUILDER
*///?} else {
    private static final ForgeConfigSpec.BooleanValue OVERLOAD_EXPLOSIONS = BUILDER
//?}
            .comment("Original GT6 machines.explode_by_overload; disabled by default.",
                    "The obsolete machineOvervoltageExplosions setting is ignored because older builds generated it as true.")
            .define("machines.explode_by_overload", false);

//? if >=1.21.1 {
    /*private static final ModConfigSpec.BooleanValue OVERLOAD_BREAKING = BUILDER
*///?} else {
    private static final ForgeConfigSpec.BooleanValue OVERLOAD_BREAKING = BUILDER
//?}
            .comment("Original GT6 machines.break_by_overload; remove only the overloaded machine without an area explosion.",
                    "Disabled by default. Enabling explode_by_overload also enables removal.")
            .define("machines.break_by_overload", false);

//? if >=1.21.1 {
    /*public static final ModConfigSpec SPEC = BUILDER.build();
*///?} else {
    static final ForgeConfigSpec SPEC = BUILDER.build();
//?}

    public static boolean logMaterials = true;

    public static boolean explodeByOverload() {
        try {
            return OVERLOAD_EXPLOSIONS.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static boolean breakByOverload() {
        if (explodeByOverload()) return true;
        try {
            return OVERLOAD_BREAKING.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private GregTechConfig() {}
}
