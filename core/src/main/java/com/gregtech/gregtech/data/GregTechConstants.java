package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.material.GTValues;

/** Core constants from GregTech 6 CS.java (subset). Auto-transpiled skeleton. */
public class GregTechConstants {
    protected GregTechConstants() {}

    public static final boolean T = true, F = false;

    /** Material amount unit  - delegates to {@link GTValues#U}. */
    public static final long U = GTValues.U;
    public static final long U2 = U / 2, U3 = U / 3, U4 = U / 4, U9 = U / 9;
    public static final long U16 = U / 16, U32 = U / 32, U64 = U / 64, U72 = U / 72;

    public static final long L = 144;
    public static final long C = 273;
    public static final long DEF_ENV_TEMP = C + 20;

    public static final int J_PER_EU = 10;
    public static final int RF_PER_EU = 4;
    public static final int RF_PER_MJ = 10;
    /** Litres of steam per tick equivalent to 1 EU (GT6: 2 Steam = 1 EU). */
    public static final int STEAM_PER_EU = 2;

    /** NBT keys for energy block entities (from GT6 CS). */
    public static final String NBT_ENERGY = "gt.energy";
    public static final String NBT_ENERGY_ACCEPTED = "gt.energy.accepted";
    public static final String NBT_ENERGY_EMITTED = "gt.energy.emitted";
    public static final String NBT_INPUT = "gt.input";
    public static final String NBT_OUTPUT = "gt.output";
    public static final String NBT_PIPELOSS = "gt.pipeloss";
    public static final String NBT_PIPESIZE = "gt.pipesize";
    public static final String NBT_PIPEBANDWIDTH = "gt.pipebandwidth";
    public static final String NBT_ACTIVE = "gt.active";
    public static final String NBT_FACING = "gt.facing";
    public static final String NBT_EFFICIENCY = "gt.efficiency";

    /** GT6 {@code TICKS_PER_SMELT} — one vanilla furnace smelt duration. */
    public static final int TICKS_PER_SMELT = 200;

    /** GT6 {@code EU_PER_FURNACE_TICK} — furnace fuel power scale. */
    public static final int EU_PER_FURNACE_TICK = 25;
    public static final int EU_PER_LAVA = 80;

    public static final String[] VN = com.gregtech.gregtech.api.energy.GTVoltageTiers.NAMES.clone();

    /** Original GT6 CS.V; the last two finite entries are 2147483648 and 8589934592. */
    public static final long[] V = com.gregtech.gregtech.api.energy.GTVoltageTiers.VOLTAGES.clone();

    public static final String[] DYE_NAMES = {
            "Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "Light Gray",
            "Gray", "Pink", "Lime", "Yellow", "Light Blue", "Magenta", "Orange", "White"
    };

    /** Legacy registry namespace; loader identity is GregTechIdentity.MOD_ID. */
    public static final String MODID = "gregtech";

    public static void bootstrap() {
        // Reserved for future CS-side initialization.
    }
}
