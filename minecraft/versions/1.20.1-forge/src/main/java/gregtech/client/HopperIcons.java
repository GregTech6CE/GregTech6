package gregtech.client;

import gregtech.GregTech;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** GT6 hopper / queuehopper per-facing model paths and JSON generation. */
public final class HopperIcons {
    private HopperIcons() {}

    public static ResourceLocation sharedModelLocation(String type, Direction facing) {
        return GregTech.id("block/machine/" + type + "_" + facing.getSerializedName());
    }
}
