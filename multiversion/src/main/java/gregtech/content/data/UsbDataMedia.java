package gregtech.content.data;

import gregtech.content.recipe.GTMaterialDataRecipes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}
import org.jetbrains.annotations.Nullable;

/** GT6's stick and HDD file layout, independent of the kind of data stored in a file. */
public final class UsbDataMedia {

    private UsbDataMedia() {
    }
//? if >=1.21.1 {
    /*public static CompoundTag tag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static CompoundTag tagOrEmpty(ItemStack stack) {
        var tag = tag(stack);
        return tag == null ? new CompoundTag() : tag;
    }

    public static void store(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        else stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

*///?} else {
//?}

    public static int stickTier(ItemStack stack) {
        return tier(stack, "_stick");
    }

    public static int cableTier(ItemStack stack) {
        return tier(stack, "_cable");
    }

    public static int driveTier(ItemStack stack) {
        return tier(stack, "_hdd");
    }

    private static int tier(ItemStack stack, String suffix) {
        if (stack == null || stack.isEmpty()) return -1;
//? if >=1.21.1 {
        /*var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
*///?} else {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
//?}
        if (key == null || !key.getNamespace().equals("gt6ce")) return -1;
        return UsbDataRules.tier(key.getNamespace(), key.getPath(), suffix);
    }

    public static boolean isStick(ItemStack stack) {
        return stickTier(stack) >= 1;
    }

    public static boolean isDrive(ItemStack stack) {
        return driveTier(stack) >= 1;
    }

    /** The tier recorded on the file, or zero for a fresh/legacy untyped stick. */
    public static int stickFileTier(ItemStack stack) {
//? if >=1.21.1 {
        /*CompoundTag tag = stack == null ? null : tag(stack);
*///?} else {
        CompoundTag tag = stack == null ? null : stack.getTag();
//?}
        return tag == null ? 0 : Byte.toUnsignedInt(tag.getByte(GTMaterialDataRecipes.NBT_USB_TIER));
    }

    @Nullable

    public static CompoundTag readStick(ItemStack stack, int requestedTier) {
        if (!UsbDataRules.readable(stickTier(stack), requestedTier, stickFileTier(stack))) return null;
//? if >=1.21.1 {
        /*CompoundTag tag = tag(stack);
*///?} else {
        CompoundTag tag = stack.getTag();
//?}
        if (tag == null || !tag.contains(GTMaterialDataRecipes.NBT_USB_DATA, 10)) return null;
        CompoundTag file = tag.getCompound(GTMaterialDataRecipes.NBT_USB_DATA);
        return file.isEmpty() ? null : file.copy();
    }

    public static boolean writeStick(ItemStack stack, int requestedTier, @Nullable CompoundTag data) {
        if (!UsbDataRules.writable(stickTier(stack), requestedTier)) return false;
        if (data == null || data.isEmpty()) {
//? if >=1.21.1 {
            /*CompoundTag tag = tag(stack);
*///?} else {
            CompoundTag tag = stack.getTag();
//?}
            if (tag != null) {
                tag.remove(GTMaterialDataRecipes.NBT_USB_DATA);
                tag.remove(GTMaterialDataRecipes.NBT_USB_TIER);
//? if >=1.21.1 {
                /*store(stack, tag);
*///?} else {
                if (tag.isEmpty()) stack.setTag(null);
//?}
            }
        } else {
//? if >=1.21.1 {
            /*CompoundTag tag = tagOrEmpty(stack);
*///?} else {
            CompoundTag tag = stack.getOrCreateTag();
//?}
            tag.put(GTMaterialDataRecipes.NBT_USB_DATA, data.copy());
            tag.putByte(GTMaterialDataRecipes.NBT_USB_TIER, (byte) requestedTier);
//? if >=1.21.1 {
            /*store(stack, tag);
*///?} else {
//?}
        }
        return true;
    }

    @Nullable

    public static CompoundTag readDrive(ItemStack drive, int slot, int requestedTier) {
        if (!UsbDataRules.slot(slot) || !UsbDataRules.writable(driveTier(drive), requestedTier)) return null;
//? if >=1.21.1 {
        /*CompoundTag tag = tag(drive);
*///?} else {
        CompoundTag tag = drive.getTag();
//?}
        if (tag == null || !tag.contains(GTMaterialDataRecipes.NBT_USB_DRIVE, 10)) return null;
        CompoundTag files = tag.getCompound(GTMaterialDataRecipes.NBT_USB_DRIVE);
        if (Byte.toUnsignedInt(files.getByte(GTMaterialDataRecipes.slotTierKey(slot))) > requestedTier
                || !files.contains(GTMaterialDataRecipes.slotDataKey(slot), 10)) return null;
        CompoundTag file = files.getCompound(GTMaterialDataRecipes.slotDataKey(slot));
        return file.isEmpty() ? null : file.copy();
    }

    public static boolean writeDrive(ItemStack drive, int slot, int requestedTier, @Nullable CompoundTag data) {
        if (!UsbDataRules.slot(slot) || !UsbDataRules.writable(driveTier(drive), requestedTier)) return false;
//? if >=1.21.1 {
        /*CompoundTag tag = tagOrEmpty(drive);
*///?} else {
        CompoundTag tag = drive.getOrCreateTag();
//?}
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
//? if >=1.21.1 {
        /*store(drive, tag);
*///?} else {
        if (tag.isEmpty()) drive.setTag(null);
//?}
        return true;
    }
}
