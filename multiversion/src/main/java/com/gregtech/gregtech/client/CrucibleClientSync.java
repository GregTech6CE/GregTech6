package com.gregtech.gregtech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}

@OnlyIn(Dist.CLIENT)
public final class CrucibleClientSync {
    private CrucibleClientSync() {}

    public static void refreshHull(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.levelRenderer == null) {
            return;
        }
        minecraft.levelRenderer.setBlocksDirty(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX(), pos.getY(), pos.getZ());
    }
}
