package gregtech.content.cover;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
*///?} else {
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
/** Mutable-NBT boundary matching NeoForge's immutable component facade. */
//? if >=1.21.1 {
/*import net.minecraft.world.item.component.CustomData;
/^* Immutable component boundary for the original gt.cover/gt.filter keys. ^/
*///?} else {
//?}
public final class CoverStackData {
    private CoverStackData() {}

    /** GT6 AbstractCoverDefault.getCoverItem retains cover NBT (including filter templates).
     * Face visuals/values belong to the host and reset on removal, not to the dropped item. */
    public static ItemStack detached(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        var result = stack.copyWithCount(1);
        var tag = readOrEmpty(result);
        for (String key : new String[]{ComponentCoverRuntime.VISUAL, ComponentCoverRuntime.SLOT,
                "gt.cover.inverted", "gt.cover.strong", "gt.logistics.value", PanelCoverRuntime.VALUE,
                PanelCoverRuntime.STYLE, PanelCoverRuntime.RESET, PanelCoverRuntime.COUNTDOWN}) tag.remove(key);
        write(result, tag);
        return result;
    }
//? if >=1.21.1 {
    /*public static boolean has(ItemStack stack){return stack.has(DataComponents.CUSTOM_DATA);}
    public static CompoundTag read(ItemStack stack){return has(stack)?gregtech.platform.neoforge.StackCustomData.read(stack):null;}
    public static CompoundTag readOrEmpty(ItemStack stack){var tag=read(stack);return tag==null?new CompoundTag():tag;}
    public static void write(ItemStack stack,CompoundTag tag){if(tag.isEmpty())stack.remove(DataComponents.CUSTOM_DATA);else stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    public static void putInt(ItemStack stack,String key,int value){var tag=readOrEmpty(stack);tag.putInt(key,value);write(stack,tag);}
    public static void putBoolean(ItemStack stack,String key,boolean value){var tag=readOrEmpty(stack);tag.putBoolean(key,value);write(stack,tag);}
    /^* Original cover/filter items always store one specimen; legacy custom tags remain opaque. ^/
    public static ItemStack readItem(net.minecraft.core.HolderLookup.Provider lookup,CompoundTag tag){
        return gregtech.engine.stack.NbtCompat.readItem(tag,lookup);
*///?} else {
    public static CompoundTag readOrEmpty(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
//?}
    }
//? if >=1.21.1 {
//?} else {
    public static boolean has(ItemStack stack){return stack.hasTag();}
    public static CompoundTag read(ItemStack stack){return stack.getTag();}
    public static void write(ItemStack stack,CompoundTag tag){stack.setTag(tag.isEmpty()?null:tag);}
    public static void putBoolean(ItemStack stack,String key,boolean value){stack.getOrCreateTag().putBoolean(key,value);}
    public static void putInt(ItemStack stack, String key, int value) { stack.getOrCreateTag().putInt(key, value); }
//?}
}
