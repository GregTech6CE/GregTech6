package gregtech.content.block.machine;

import gregtech.content.recipe.ClayMoldCatalog;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
*///?} else {
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.world.item.component.CustomData;
*///?} else {
//?}

/** Native block-item shape data; normal BlockItem placement loads this into the mold entity. */
public final class MoldItemData {
    private MoldItemData() {}
    public static int shape(ItemStack stack) {
//? if >=1.21.1 {
        /*return stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA,CustomData.EMPTY)
                .copyTag().getInt(ClayMoldCatalog.SHAPE_KEY);
*///?} else {
        return stack.getTagElement("BlockEntityTag")==null ? 0
                : stack.getTagElement("BlockEntityTag").getInt(ClayMoldCatalog.SHAPE_KEY);
//?}
    }
    public static ItemStack withShape(ItemStack stack,int shape) {
//? if >=1.21.1 {
        /*var tag=stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA,CustomData.EMPTY).copyTag();
*///?} else {
        var existing=stack.getTagElement("BlockEntityTag");
        var tag=existing==null ? new CompoundTag() : existing.copy();
//?}
        if(shape==0) {
            tag.remove(ClayMoldCatalog.SHAPE_KEY);
            if(tag.isEmpty() || (tag.size()==1 && tag.contains("id"))) {
//? if >=1.21.1 {
                /*stack.remove(DataComponents.BLOCK_ENTITY_DATA);
*///?} else {
                stack.removeTagKey("BlockEntityTag");
//?}
                return stack;
            }
        }
        tag.putString("id","gt6ce:mold");
        if(shape!=0)tag.putInt(ClayMoldCatalog.SHAPE_KEY,shape);
//? if >=1.21.1 {
        /*stack.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));
*///?} else {
        stack.addTagElement("BlockEntityTag",tag);
//?}
        return stack;
    }
}
