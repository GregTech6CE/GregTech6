package com.gregtech.gregtech.client;


//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.GregTech;
//?}
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Burning-box shared model locations. Face mapping follows GT6 {@code CS.FACING_ROTATIONS}. */
public final class MachineBlockIcons {
    private MachineBlockIcons() {}

    public static ResourceLocation burningBoxVariant(Direction facing, boolean active, boolean brick) {
        String shell = brick ? "brick" : "solid";
//? if >=1.21.1 {
        /*return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", "block/machine/burning_box_" + shell + "/"
*///?} else {
        return GregTech.id("block/machine/burning_box_" + shell + "/"
//?}
                + facing.getSerializedName() + (active ? "_on" : "_off"));
    }
}
