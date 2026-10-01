package com.gregtech.gregtech.content.energy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
/** GT6 14999: non-rechargeable 2 trillion QU artifact; absent charge is empty. */
public final class ZpmEnergy {
    public static final long CAPACITY=ZpmWorkRules.CAPACITY, PACKET=ZpmWorkRules.PACKET;
    public static final String KEY=ZpmWorkRules.KEY;
    private ZpmEnergy(){}
    public static boolean isModule(ItemStack stack){return stack.is(com.gregtech.gregtech.registry.GTZpmArtifacts.ZPM.get().asItem());}
    public static long stored(ItemStack stack){
        if(!isModule(stack))return 0;
        return ZpmWorkRules.charge(stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getLong(KEY));
    }
    public static void set(ItemStack stack,long charge){
        if(!isModule(stack))throw new IllegalArgumentException("Not a ZPM");
        var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();data.putString("id","gregtech:zpm_module");data.putLong(KEY,ZpmWorkRules.charge(charge));stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));
    }
    public static ItemStack charged(){var stack=new ItemStack(com.gregtech.gregtech.registry.GTZpmArtifacts.ZPM.get());set(stack,CAPACITY);return stack;}
}
