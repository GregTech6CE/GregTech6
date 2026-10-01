package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Client-only hooks callable from common code. This class references {@link Minecraft} and
 * must only be loaded behind an {@code isClientSide} check — never reference it from static
 * initializers or class signatures of common classes.
 */
public final class ClientShapeHooks {
    private ClientShapeHooks() {}

    /** True when the local player holds a machine wrench (and not a monkey wrench). */
    public static boolean holdingMachineWrench() {
        Player player = Minecraft.getInstance().player;
        return player != null
                && GTToolHelper.isMachineWrench(player.getMainHandItem())
                && !GTToolHelper.isMonkeyWrench(player.getMainHandItem());
    }
}
