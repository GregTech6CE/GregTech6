package gregtech.content.energy;

import gregtech.engine.energy.ZpmWorkRules;
import gregtech.engine.stack.StackData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
/** GT6 14999: non-rechargeable 2 trillion QU artifact; absent charge is empty. */
public final class ZpmEnergy {
    public static final long CAPACITY=ZpmWorkRules.CAPACITY, PACKET=ZpmWorkRules.PACKET;
    public static final String KEY=ZpmWorkRules.KEY;
    private ZpmEnergy(){}
    public static boolean isModule(ItemStack stack){return stack.is(gregtech.content.registry.GTLasers.ZPM.get().asItem());}
    public static long stored(ItemStack stack){
        if(!isModule(stack))return 0;
        return ZpmWorkRules.charge(gregtech.engine.stack.BlockEntityData.readOrEmpty(stack).getLong(KEY));
    }
    public static void set(ItemStack stack,long charge){
        if(!isModule(stack))throw new IllegalArgumentException("Not a ZPM");
        gregtech.engine.stack.BlockEntityData.update(stack, data -> {
//? if >=1.21.1 {
/*            data.putString("id","gt6ce:zpm_module");
*///?}
            data.putLong(KEY,ZpmWorkRules.charge(charge));
        });
    }
    public static ItemStack charged(){var stack=new ItemStack(gregtech.content.registry.GTLasers.ZPM.get());set(stack,CAPACITY);return stack;}
}
