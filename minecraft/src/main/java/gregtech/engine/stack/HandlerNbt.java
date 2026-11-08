package gregtech.engine.stack;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
/*        return handler.serializeNBT(lookup);
*///?} else {
        return handler.serializeNBT();
//?}
    }

    public static void load(ItemStackHandler handler, CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        handler.deserializeNBT(lookup, tag);
*///?} else {
        handler.deserializeNBT(tag);
//?}
    }
}
