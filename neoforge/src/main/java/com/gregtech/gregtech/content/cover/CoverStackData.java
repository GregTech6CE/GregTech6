package com.gregtech.gregtech.content.cover;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
/** Immutable component boundary for the original gt.cover/gt.filter keys. */
public final class CoverStackData {
    private CoverStackData() {}
    public static boolean has(ItemStack stack){return stack.has(DataComponents.CUSTOM_DATA);}
    public static CompoundTag read(ItemStack stack){return has(stack)?com.gregtech.gregtech.platform.neoforge.StackCustomData.read(stack):null;}
    public static CompoundTag readOrEmpty(ItemStack stack){var tag=read(stack);return tag==null?new CompoundTag():tag;}
    public static void write(ItemStack stack,CompoundTag tag){if(tag.isEmpty())stack.remove(DataComponents.CUSTOM_DATA);else stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    public static void putInt(ItemStack stack,String key,int value){var tag=readOrEmpty(stack);tag.putInt(key,value);write(stack,tag);}
    public static void putBoolean(ItemStack stack,String key,boolean value){var tag=readOrEmpty(stack);tag.putBoolean(key,value);write(stack,tag);}
    /** Original cover/filter items always store one specimen; legacy custom tags remain opaque. */
    public static ItemStack readItem(net.minecraft.core.HolderLookup.Provider lookup,CompoundTag tag){
        var stack=ItemStack.parseOptional(lookup,tag);
        if(!stack.isEmpty()&&tag.contains("tag"))write(stack,tag.getCompound("tag"));
        return stack;
    }
}
