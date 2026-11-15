package gregtech.engine.stack;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
*///?}

/**
 * The block-entity payload an item stack carries: the 1.20.1 {@code BlockEntityTag} sub-tag and the
 * 1.21.1 {@code BLOCK_ENTITY_DATA} component.
 *
 * <p>{@code BLOCK_ENTITY_DATA} is backed by {@link CustomData}, the same CompoundTag shape as
 * {@code CUSTOM_DATA}, so it shares {@link StackData}'s read/write contract while staying a distinct
 * component key. Callers use the same {@link CompoundTag} API on both nodes; the storage difference
 * stays here. {@link #read} returns a copy, never the 1.20.1 live tag reference.</p>
 */
public final class BlockEntityData {
    private BlockEntityData() {}

    /** The block-entity payload, or null when the stack carries none. */
    public static CompoundTag read(ItemStack stack) {
//? if >=1.21.1 {
/*        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTag();
*///?} else {
        var tag = stack.getTagElement("BlockEntityTag");
        return tag == null ? null : tag.copy();
//?}
    }

    public static CompoundTag readOrEmpty(ItemStack stack) {
        var tag = read(stack);
        return tag == null ? new CompoundTag() : tag;
    }

    public static boolean has(ItemStack stack) {
//? if >=1.21.1 {
/*        return stack.has(DataComponents.BLOCK_ENTITY_DATA);
*///?} else {
        return stack.getTagElement("BlockEntityTag") != null;
//?}
    }

    /** Writes the payload; a null or empty tag removes it so {@link #has} stays consistent. */
    public static void write(ItemStack stack, CompoundTag tag) {
//? if >=1.21.1 {
/*        if (tag == null || tag.isEmpty()) stack.remove(DataComponents.BLOCK_ENTITY_DATA);
        else stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
*///?} else {
        if (tag == null || tag.isEmpty()) stack.removeTagKey("BlockEntityTag");
        else stack.addTagElement("BlockEntityTag", tag);
//?}
    }

    public static void remove(ItemStack stack) {
//? if >=1.21.1 {
/*        stack.remove(DataComponents.BLOCK_ENTITY_DATA);
*///?} else {
        stack.removeTagKey("BlockEntityTag");
//?}
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> action) {
        var tag = readOrEmpty(stack);
        action.accept(tag);
        write(stack, tag);
    }
}