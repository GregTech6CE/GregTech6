package gregtech.engine.stack;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.items.ItemStackHandler;
//?}

/** ItemStackHandler NBT across the two nodes; 1.20.1 ignores the lookup. */
public final class HandlerNbt {
    private HandlerNbt() {}

    public static CompoundTag save(ItemStackHandler handler, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return handler.serializeNBT(requireNonNull(lookup));
*///?} else {
        return handler.serializeNBT();
//?}
    }

    public static void load(ItemStackHandler handler, CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        handler.deserializeNBT(requireNonNull(lookup), tag);
*///?} else {
        handler.deserializeNBT(tag);
//?}
    }

    /** Saves the handler under {@code key}, patching {@code Size} to the live slot count. */
    public static void save(CompoundTag parent, String key, ItemStackHandler handler, HolderLookup.Provider lookup) {
        CompoundTag tag = save(handler, lookup);
        tag.putInt("Size", handler.getSlots());
        parent.put(key, tag);
    }

    /** Loads the handler from {@code key} when present, patching {@code Size} to the live slot count. */
    public static void load(CompoundTag parent, String key, ItemStackHandler handler, HolderLookup.Provider lookup) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) return;
        CompoundTag tag = parent.getCompound(key).copy();
        tag.putInt("Size", handler.getSlots());
        load(handler, tag, lookup);
    }

//? if >=1.21.1 {
    /*private static HolderLookup.Provider requireNonNull(HolderLookup.Provider lookup) {
        return java.util.Objects.requireNonNull(lookup, "handler codec needs the registry lookup");
    }
*///?}
}
