/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from MultiTileEntityHopper and MultiTileEntityQueueHopper.addToolTips. */
package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.machine.HopperSpec;
import com.gregtech.gregtech.content.storage.OriginalStorageTooltipData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Original saved slot limit and mode rows. The common handler adds harvest/material rows once. */
public final class HopperTooltips {
    private HopperTooltips() {}
    public static void appendHopper(HopperSpec spec, ItemStack stack, List<Component> lines) { append(spec, stack, false, lines); }
    public static void appendQueueHopper(HopperSpec spec, ItemStack stack, List<Component> lines) { append(spec, stack, true, lines); }
    private static void append(HopperSpec spec, ItemStack stack, boolean queue, List<Component> lines) {
        var data = CommonBlockTooltips.blockData(stack);
        Integer mode = data != null && data.contains("gt.mode", Tag.TAG_ANY_NUMERIC) ? (int)data.getByte("gt.mode") : null;
        var state = OriginalStorageTooltipData.hopper(spec.slotCount(), queue, mode, data != null && data.getBoolean("gt.exact"));
        number(lines, OriginalStorageTooltipData.SLOT_COUNT, state.slots());
        if (state.showStackSize()) number(lines, OriginalStorageTooltipData.STACK_SIZE, state.stackSize());
        if (state.exact()) lines.add(Component.translatable(OriginalStorageTooltipData.EXACT).withStyle(ChatFormatting.AQUA));
        StorageBlockTooltips.tools(lines, state.tools());
        StorageBlockTooltips.facing(lines);
        TooltipHelper.appendBlastResistance(spec.blastResistance(), lines);
    }
    private static void number(List<Component> lines, String key, int value) {
        lines.add(Component.translatable(key).withStyle(ChatFormatting.AQUA).append(Integer.toString(value)));
    }
}
