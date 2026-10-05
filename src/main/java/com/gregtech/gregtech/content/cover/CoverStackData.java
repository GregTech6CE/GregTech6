package com.gregtech.gregtech.content.cover;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
/** Mutable-NBT boundary matching NeoForge's immutable component facade. */
public final class CoverStackData {
    private CoverStackData() {}
    public static CompoundTag readOrEmpty(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
    }
    public static void putInt(ItemStack stack, String key, int value) { stack.getOrCreateTag().putInt(key, value); }
}
