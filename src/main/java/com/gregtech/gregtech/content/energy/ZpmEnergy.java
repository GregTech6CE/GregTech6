package com.gregtech.gregtech.content.energy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
/** GT6 14999: non-rechargeable 2 trillion QU artifact; absent charge is empty. */
public final class ZpmEnergy {
    public static final long CAPACITY=2_000_000_000_000L, PACKET=131072;
    public static final String KEY="gt.zpm.energy";
    private ZpmEnergy(){}
    public static boolean isModule(ItemStack stack){return stack.is(com.gregtech.gregtech.registry.GTLasers.ZPM.get().asItem());}
    public static long stored(ItemStack stack){
        if(!isModule(stack)||!stack.hasTag())return 0;
        return Math.max(0,Math.min(CAPACITY,stack.getTag().getCompound("BlockEntityTag").getLong(KEY)));
    }
    public static void set(ItemStack stack,long charge){
        if(!isModule(stack))throw new IllegalArgumentException("Not a ZPM");
        var data=stack.getOrCreateTagElement("BlockEntityTag");data.putLong(KEY,Math.max(0,Math.min(CAPACITY,charge)));
    }
    public static ItemStack charged(){var stack=new ItemStack(com.gregtech.gregtech.registry.GTLasers.ZPM.get());set(stack,CAPACITY);return stack;}
}
