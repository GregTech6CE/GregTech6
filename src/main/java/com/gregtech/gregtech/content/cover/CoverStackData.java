package com.gregtech.gregtech.content.cover;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
/** Mutable-NBT boundary matching NeoForge's immutable component facade. */
public final class CoverStackData {
    private CoverStackData() {}
    public static CompoundTag readOrEmpty(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
    }
    public static boolean has(ItemStack stack){return stack.hasTag();}
    public static CompoundTag read(ItemStack stack){return stack.getTag();}
    public static void write(ItemStack stack,CompoundTag tag){stack.setTag(tag.isEmpty()?null:tag);}
    public static void putBoolean(ItemStack stack,String key,boolean value){stack.getOrCreateTag().putBoolean(key,value);}
    public static void putInt(ItemStack stack, String key, int value) { stack.getOrCreateTag().putInt(key, value); }
}
