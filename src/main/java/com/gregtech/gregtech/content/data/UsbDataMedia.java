package com.gregtech.gregtech.content.data;

import com.gregtech.gregtech.content.recipe.GTMaterialDataRecipes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/** GT6's stick and HDD file layout, independent of the kind of data stored in a file. */
public final class UsbDataMedia {
    private UsbDataMedia() {}

    public static int stickTier(ItemStack stack) { return tier(stack, "_stick"); }
    public static int cableTier(ItemStack stack) { return tier(stack, "_cable"); }
    public static int driveTier(ItemStack stack) { return tier(stack, "_hdd"); }

    private static int tier(ItemStack stack, String suffix) {
        if (stack == null || stack.isEmpty()) return -1;
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !key.getNamespace().equals("gregtech")) return -1;
        String name = key.getPath();
        if (!name.startsWith("usb") || !name.endsWith(suffix) || name.length() != 4 + suffix.length()) return -1;
        char numeral = name.charAt(3);
        return numeral >= '1' && numeral <= '4' ? numeral - '0' : -1;
    }

    public static boolean isStick(ItemStack stack) { return stickTier(stack) >= 1; }
    public static boolean isDrive(ItemStack stack) { return driveTier(stack) >= 1; }

    /** The tier recorded on the file, or zero for a fresh/legacy untyped stick. */
    public static int stickFileTier(ItemStack stack) {
        CompoundTag tag = stack == null ? null : stack.getTag();
        return tag == null ? 0 : Byte.toUnsignedInt(tag.getByte(GTMaterialDataRecipes.NBT_USB_TIER));
    }

    @Nullable
    public static CompoundTag readStick(ItemStack stack, int requestedTier) {
        if (requestedTier < 0 || requestedTier > 4 || stickTier(stack) < requestedTier
                || stickFileTier(stack) > requestedTier) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(GTMaterialDataRecipes.NBT_USB_DATA, 10)) return null;
        CompoundTag file = tag.getCompound(GTMaterialDataRecipes.NBT_USB_DATA);
        return file.isEmpty() ? null : file.copy();
    }

    public static boolean writeStick(ItemStack stack, int requestedTier, @Nullable CompoundTag data) {
        if (requestedTier < 0 || requestedTier > 4 || stickTier(stack) < requestedTier) return false;
        if (data == null || data.isEmpty()) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(GTMaterialDataRecipes.NBT_USB_DATA);
                tag.remove(GTMaterialDataRecipes.NBT_USB_TIER);
                if (tag.isEmpty()) stack.setTag(null);
            }
        } else {
            CompoundTag tag = stack.getOrCreateTag();
            tag.put(GTMaterialDataRecipes.NBT_USB_DATA, data.copy());
            tag.putByte(GTMaterialDataRecipes.NBT_USB_TIER, (byte) requestedTier);
        }
        return true;
    }

    @Nullable
    public static CompoundTag readDrive(ItemStack drive, int slot, int requestedTier) {
        if (slot < 0 || slot >= GTMaterialDataRecipes.DRIVE_SLOTS || requestedTier < 0 || requestedTier > 4
                || driveTier(drive) < requestedTier) return null;
        CompoundTag tag = drive.getTag();
        if (tag == null || !tag.contains(GTMaterialDataRecipes.NBT_USB_DRIVE, 10)) return null;
        CompoundTag files = tag.getCompound(GTMaterialDataRecipes.NBT_USB_DRIVE);
        if (Byte.toUnsignedInt(files.getByte(GTMaterialDataRecipes.slotTierKey(slot))) > requestedTier
                || !files.contains(GTMaterialDataRecipes.slotDataKey(slot), 10)) return null;
        CompoundTag file = files.getCompound(GTMaterialDataRecipes.slotDataKey(slot));
        return file.isEmpty() ? null : file.copy();
    }

    public static boolean writeDrive(ItemStack drive, int slot, int requestedTier, @Nullable CompoundTag data) {
        if (slot < 0 || slot >= GTMaterialDataRecipes.DRIVE_SLOTS || requestedTier < 0 || requestedTier > 4
                || driveTier(drive) < requestedTier) return false;
        CompoundTag tag = drive.getOrCreateTag();
        CompoundTag files = tag.getCompound(GTMaterialDataRecipes.NBT_USB_DRIVE).copy();
        if (data == null || data.isEmpty()) {
            files.remove(GTMaterialDataRecipes.slotDataKey(slot));
            files.remove(GTMaterialDataRecipes.slotTierKey(slot));
        } else {
            files.put(GTMaterialDataRecipes.slotDataKey(slot), data.copy());
            files.putByte(GTMaterialDataRecipes.slotTierKey(slot), (byte) requestedTier);
        }
        if (files.isEmpty()) tag.remove(GTMaterialDataRecipes.NBT_USB_DRIVE);
        else tag.put(GTMaterialDataRecipes.NBT_USB_DRIVE, files);
        if (tag.isEmpty()) drive.setTag(null);
        return true;
    }
}
