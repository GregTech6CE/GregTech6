package gregtech.content.energy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
/** GT6 14999: non-rechargeable 2 trillion QU artifact; absent charge is empty. */
public final class ZpmEnergy {
    public static final long CAPACITY=ZpmWorkRules.CAPACITY, PACKET=ZpmWorkRules.PACKET;
    public static final String KEY=ZpmWorkRules.KEY;
    private ZpmEnergy(){}
//? if >=1.21.1 {
    /*public static boolean isModule(ItemStack stack){return stack.is(gregtech.registry.GTZpmArtifacts.ZPM.get().asItem());}
*///?} else {
    public static boolean isModule(ItemStack stack){return stack.is(gregtech.registry.GTLasers.ZPM.get().asItem());}
//?}
    public static long stored(ItemStack stack){
//? if >=1.21.1 {
        /*if(!isModule(stack))return 0;
        return ZpmWorkRules.charge(stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getLong(KEY));
*///?} else {
        if(!isModule(stack)||!stack.hasTag())return 0;
        return ZpmWorkRules.charge(stack.getTag().getCompound("BlockEntityTag").getLong(KEY));
//?}
    }
    public static void set(ItemStack stack,long charge){
        if(!isModule(stack))throw new IllegalArgumentException("Not a ZPM");
//? if >=1.21.1 {
        /*var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();data.putString("id","gt6ce:zpm_module");data.putLong(KEY,ZpmWorkRules.charge(charge));stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));
*///?} else {
        var data=stack.getOrCreateTagElement("BlockEntityTag");data.putLong(KEY,ZpmWorkRules.charge(charge));
//?}
    }
//? if >=1.21.1 {
    /*public static ItemStack charged(){var stack=new ItemStack(gregtech.registry.GTZpmArtifacts.ZPM.get());set(stack,CAPACITY);return stack;}
*///?} else {
    public static ItemStack charged(){var stack=new ItemStack(gregtech.registry.GTLasers.ZPM.get());set(stack,CAPACITY);return stack;}
//?}
}
