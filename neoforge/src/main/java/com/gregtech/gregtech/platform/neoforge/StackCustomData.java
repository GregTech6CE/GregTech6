package com.gregtech.gregtech.platform.neoforge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.function.Consumer;
/** Legacy GT custom NBT names retained inside 1.21 item components. */
public final class StackCustomData {
    private StackCustomData() {}
    public static CompoundTag read(ItemStack stack) {
        if(stack==null)return new CompoundTag();
        var data=stack.get(DataComponents.CUSTOM_DATA);return data==null?new CompoundTag():data.copyTag();
    }
    public static void update(ItemStack stack,Consumer<CompoundTag> edit) { CustomData.update(DataComponents.CUSTOM_DATA,stack,edit); }
}
