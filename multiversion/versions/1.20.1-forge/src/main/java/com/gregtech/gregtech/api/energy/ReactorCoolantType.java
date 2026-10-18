package com.gregtech.gregtech.api.energy;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

/** Coolant variants for reactor cores. Each provides different neutron moderation and heat transfer. */
public enum ReactorCoolantType {
    /** Plain water — baseline. */
    WATER(1.0F, 1.0F, 1.0F),
    /** Distilled water — slightly better heat transfer, no calcification. */
    DISTILLED_WATER(1.0F, 1.2F, 1.0F),
    /** Heavy water — better neutron moderation → more HU per rod. */
    HEAVY_WATER(1.5F, 1.0F, 1.2F),
    /** Molten sodium — fast heat transfer but no moderation bonus. */
    SODIUM(0.8F, 2.0F, 1.0F),
    /** Molten salt — high heat capacity reduces meltdown risk. */
    MOLTEN_SALT(1.0F, 1.0F, 3.0F);

    private final float neutronModeration;
    private final float heatTransfer;
    private final float heatCapacity;

    ReactorCoolantType(float neutronModeration, float heatTransfer, float heatCapacity) {
        this.neutronModeration = neutronModeration;
        this.heatTransfer = heatTransfer;
        this.heatCapacity = heatCapacity;
    }

    public float neutronModeration() { return neutronModeration; }
    public float heatTransfer() { return heatTransfer; }
    public float heatCapacity() { return heatCapacity; }

    /** Consumed coolant volume per rod per second (mB). */
    public long coolantPerRodSecond() {
        return 1;
    }

    /** Guess coolant type from a fluid's registry key. */
    public static ReactorCoolantType fromFluid(Fluid fluid) {
        if (fluid == null) return WATER;
        var key = ForgeRegistries.FLUIDS.getKey(fluid);
        if (key == null) return WATER;
        String path = key.getPath();
        if (path.contains("heavy_water") || path.contains("heavywater")) return HEAVY_WATER;
        if (path.contains("distilled")) return DISTILLED_WATER;
        if (path.contains("sodium") && (path.contains("molten") || path.contains("hot"))) return SODIUM;
        if (path.contains("molten_salt") || (path.contains("salt") && path.contains("molten"))) return MOLTEN_SALT;
        if (path.contains("water")) return WATER;
        return WATER;
    }
}
