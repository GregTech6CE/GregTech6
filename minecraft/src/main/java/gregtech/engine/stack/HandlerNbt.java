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
/*        return handler.serializeNBT(java.util.Objects.requireNonNull(lookup, "handler codec needs the registry lookup"));
*///?} else {
        return handler.serializeNBT();
//?}
    }

    public static void load(ItemStackHandler handler, CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        java.util.Objects.requireNonNull(lookup, "handler codec needs the registry lookup");
        // The native handler calls ItemStack.parse directly. Upgrade only historical rows, retaining
        // its slot sizing, bounds checks and onLoad hook (including subclass overrides).
        var rows = tag.getList("Items", Tag.TAG_COMPOUND);
        var upgraded = tag;
        for (int i = rows.size() - 1; i >= 0; i--) {
            var row = rows.getCompound(i);
            if (!NbtCompat.needsItemUpgrade(row)) continue;
            if (upgraded == tag) upgraded = tag.copy();
            var upgradedRows = upgraded.getList("Items", Tag.TAG_COMPOUND);
            var stack = NbtCompat.readItem(row, lookup);
            if (stack.isEmpty()) {
                upgradedRows.remove(i);
            } else {
                var saved = new CompoundTag();
                saved.putInt("Slot", row.getInt("Slot"));
                upgradedRows.set(i, stack.save(lookup, saved));
            }
        }
        handler.deserializeNBT(lookup, upgraded);
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
}
