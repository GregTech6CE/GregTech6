package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.content.recipe.ClayMoldCatalog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Native block-item shape data; normal BlockItem placement loads this into the mold entity. */
public final class MoldItemData {
    private MoldItemData() {}
    public static int shape(ItemStack stack) {
        return stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA,CustomData.EMPTY)
                .copyTag().getInt(ClayMoldCatalog.SHAPE_KEY);
    }
    public static ItemStack withShape(ItemStack stack,int shape) {
        var tag=stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA,CustomData.EMPTY).copyTag();
        if(shape==0) {
            tag.remove(ClayMoldCatalog.SHAPE_KEY);
            if(tag.isEmpty() || (tag.size()==1 && tag.contains("id"))) {
                stack.remove(DataComponents.BLOCK_ENTITY_DATA);
                return stack;
            }
        }
        tag.putString("id","gregtech:mold");
        if(shape!=0)tag.putInt(ClayMoldCatalog.SHAPE_KEY,shape);
        stack.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));
        return stack;
    }
}
