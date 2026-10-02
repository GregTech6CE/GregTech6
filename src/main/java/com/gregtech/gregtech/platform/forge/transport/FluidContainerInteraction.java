package com.gregtech.gregtech.platform.forge.transport;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Filled-first container interaction, matching the NeoForge adapter and preserving inventory transactions. */
public final class FluidContainerInteraction {
    private FluidContainerInteraction() {}

    public static boolean use(Player player, InteractionHand hand, IFluidHandler tank) {
        var held = player.getItemInHand(hand);
        if (held.isEmpty()) return false;
        // Probe one item: stacked containers are processed one at a time by the platform utility.
        var item = FluidUtil.getFluidHandler(held.copyWithCount(1)).orElse(null);
        if (item == null) return false;
        boolean filled = false;
        for (int i = 0; i < item.getTanks(); i++) {
            if (!item.getFluidInTank(i).isEmpty()) { filled = true; break; }
        }
        var inventory = player.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (inventory == null) return false;
        var result = filled
                ? FluidUtil.tryEmptyContainerAndStow(held, tank, inventory, Integer.MAX_VALUE, player, true)
                : FluidUtil.tryFillContainerAndStow(held, tank, inventory, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess()) return false;
        player.setItemInHand(hand, result.getResult());
        return true;
    }
}
