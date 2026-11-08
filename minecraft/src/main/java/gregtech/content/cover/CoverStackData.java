package gregtech.content.cover;

import gregtech.engine.stack.StackData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Cover/filter item data on top of the shared {@link StackData} boundary. */
public final class CoverStackData {
    private CoverStackData() {}

    /** GT6 AbstractCoverDefault.getCoverItem retains cover NBT (including filter templates).
     * Face visuals/values belong to the host and reset on removal, not to the dropped item. */
    public static ItemStack detached(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        var result = stack.copyWithCount(1);
        var tag = StackData.readOrEmpty(result);
        for (String key : new String[] {
            ComponentCoverRuntime.VISUAL,
            ComponentCoverRuntime.SLOT,
            "gt.cover.inverted",
            "gt.cover.strong",
            "gt.logistics.value",
            PanelCoverRuntime.VALUE,
            PanelCoverRuntime.STYLE,
            PanelCoverRuntime.RESET,
            PanelCoverRuntime.COUNTDOWN
        }) tag.remove(key);
        StackData.write(result, tag);
        return result;
    }

    public static boolean has(ItemStack stack) {
        return StackData.has(stack);
    }

    public static CompoundTag read(ItemStack stack) {
        return StackData.read(stack);
    }

    public static CompoundTag readOrEmpty(ItemStack stack) {
        return StackData.readOrEmpty(stack);
    }

    public static void write(ItemStack stack, CompoundTag tag) {
        StackData.write(stack, tag);
    }

    public static void putInt(ItemStack stack, String key, int value) {
        StackData.putInt(stack, key, value);
    }

    public static void putBoolean(ItemStack stack, String key, boolean value) {
        StackData.putBoolean(stack, key, value);
    }

    /** Original cover/filter items always store one specimen; legacy custom tags remain opaque. */
    public static ItemStack readItem(HolderLookup.Provider lookup, CompoundTag tag) {
        return gregtech.engine.stack.NbtCompat.readItem(tag, lookup);
    }
}
