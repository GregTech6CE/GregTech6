package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.material.MaterialTextureSet;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** GT6 faucet model (per-facing spout). Delegates geometry to {@link SmelteryModelHelper}. */
public final class CrucibleFaucetIcons {
    private CrucibleFaucetIcons() {}

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set) {
        return sharedModelLocation(set, Direction.SOUTH);
    }

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set, Direction facing) {
        return SmelteryModelHelper.modelPath("crucible_faucet", set, facing.getSerializedName());
    }

    public static String sharedModelJson(MaterialTextureSet set, Direction facing) {
        String elementsTinted = SmelteryModelHelper.faucetElements(facing, true);
        String elementsOverlay = SmelteryModelHelper.faucetElements(facing, false);
        return SmelteryModelHelper.compositeModelJson(set, elementsTinted, elementsOverlay);
    }
}
