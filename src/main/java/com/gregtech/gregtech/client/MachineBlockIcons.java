package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Burning-box shared model locations. Face mapping follows GT6 {@code CS.FACING_ROTATIONS}. */
public final class MachineBlockIcons {
    private MachineBlockIcons() {}

    public static ResourceLocation burningBoxVariant(Direction facing, boolean active, boolean brick) {
        String shell = brick ? "brick" : "solid";
        return GregTech.id("block/machine/burning_box_" + shell + "/"
                + facing.getSerializedName() + (active ? "_on" : "_off"));
    }
}
