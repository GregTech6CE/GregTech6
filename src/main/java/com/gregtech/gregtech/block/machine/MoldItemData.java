package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.content.recipe.ClayMoldCatalog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Native block-item shape data; normal BlockItem placement loads this into the mold entity. */
public final class MoldItemData {
    private MoldItemData() {}
    public static int shape(ItemStack stack) {
        return stack.getTagElement("BlockEntityTag")==null ? 0
                : stack.getTagElement("BlockEntityTag").getInt(ClayMoldCatalog.SHAPE_KEY);
    }
    public static ItemStack withShape(ItemStack stack,int shape) {
        var existing=stack.getTagElement("BlockEntityTag");
        var tag=existing==null ? new CompoundTag() : existing.copy();
        if(shape==0) {
            tag.remove(ClayMoldCatalog.SHAPE_KEY);
            if(tag.isEmpty() || (tag.size()==1 && tag.contains("id"))) {
                stack.removeTagKey("BlockEntityTag");
                return stack;
            }
        }
        tag.putString("id","gregtech:mold");
        if(shape!=0)tag.putInt(ClayMoldCatalog.SHAPE_KEY,shape);
        stack.addTagElement("BlockEntityTag",tag);
        return stack;
    }
}
