package com.gregtech.gregtech.api.fluid;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Prepare one container on a copy, then commit one held item and return any output overflow. */
public final class HandContainerTransfer {
    private HandContainerTransfer() {}
    public record Filled(ItemStack container, int amount) {}
    public static Filled prepare(ItemStack held, FluidStack offered, boolean requireAll) {
        if (held.isEmpty() || offered.isEmpty()) return null;
        var handler = FluidUtil.getFluidHandler(held.copyWithCount(1)).orElse(null);
        if (handler == null) return null;
        int possible = handler.fill(offered.copy(), IFluidHandler.FluidAction.SIMULATE);
        if (possible <= 0 || requireAll && possible < offered.getAmount()) return null;
        int filled = handler.fill(offered.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (filled <= 0 || requireAll && filled < offered.getAmount()) return null;
        return new Filled(handler.getContainer(), filled);
    }
    public static void replaceOne(Player player, InteractionHand hand, ItemStack result) {
        ItemStack remainder = player.getItemInHand(hand).copy();
        remainder.shrink(1);
        if (remainder.isEmpty()) player.setItemInHand(hand, result);
        else {
            player.setItemInHand(hand, remainder);
            give(player, result);
        }
    }
    public static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) player.drop(stack, false);
    }
}
