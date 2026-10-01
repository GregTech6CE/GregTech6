package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.content.recipe.GTMaterialDataRecipes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * GT6 {@code gregtech.items.behaviors.Behavior_DataStorage16} — the tooltip of a USB <em>drive</em>, the
 * sixteen-file medium ({@code MultiItemTechnological:814-817}).
 *
 * <p>The original ({@code Behavior_DataStorage16:37-58}) prints, in this order:</p>
 *
 * <ul>
 *   <li>no {@code gt.usb.drive} tag at all — {@code "Perfectly Formatted"} in cyan ({@code :54}): an
 *       untouched drive;</li>
 *   <li>the tag present but empty — {@code "Uncleanly Formatted"} ({@code :42}): a drive that was
 *       written and then wiped;</li>
 *   <li>otherwise one line per slot, 0 to 15 ({@code :44-51}): an empty slot reads
 *       {@code "Data Slot N is Empty"} in dark grey, a filled one goes through
 *       {@code UT.NBT.getDataToolTip(data, list, <b>false</b>)} — the short form, one line per file,
 *       because a drive holds sixteen.</li>
 * </ul>
 *
 * <p>The slot layout itself lives in {@code GTMaterialDataRecipes} (GT6 keys slot {@code i} as
 * {@code gt.usb.data<i>} / {@code gt.usb.tier<i>}, {@code MultiTileEntityHDDSwitch:61-83}); this class
 * only renders it.</p>
 */
public final class BehaviorDataStorage16 {

    private BehaviorDataStorage16() {}

    /** The drive's sixteen slot lines, or the two one-line states above. */
    public static void tooltip(ItemStack stack, List<Component> lines) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = com.gregtech.gregtech.content.data.UsbDataMedia.tag(stack);
        if (tag == null || !tag.contains(GTMaterialDataRecipes.NBT_USB_DRIVE)) {
            lines.add(Component.translatable("gt.tooltip.usb.formatted").withStyle(ChatFormatting.AQUA));
            return;
        }
        CompoundTag drive = tag.getCompound(GTMaterialDataRecipes.NBT_USB_DRIVE);
        if (drive.isEmpty()) {
            lines.add(Component.translatable("gt.tooltip.usb.unclean").withStyle(ChatFormatting.AQUA));
            return;
        }
        for (int slot = 0; slot < GTMaterialDataRecipes.DRIVE_SLOTS; slot++) {
            CompoundTag data = GTMaterialDataRecipes.driveSlot(stack, slot);
            if (data == null) {
                lines.add(Component.translatable("gt.tooltip.usb.slot_empty", String.valueOf(slot))
                        .withStyle(ChatFormatting.DARK_GRAY));
                continue;
            }
            BehaviorDataStorage.dataTooltip(data, lines, false);
        }
    }
}
