package gregtech.engine.stack;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
*///?}

/**
 * Free-form item data: the 1.20.1 tag and the 1.21.1 {@code CUSTOM_DATA} component.
 *
 * <p>Callers use the same {@link CompoundTag} API on both nodes; the storage difference stays
 * here. This class covers only {@code CUSTOM_DATA}; the {@code BLOCK_ENTITY_DATA} and
 * {@code BLOCK_STATE} typed components have sibling facades {@link BlockEntityData} and
 * {@link BlockStateData}.</p>
 */
public final class StackData {
    private StackData() {}

    public static boolean has(ItemStack stack) {
//? if >=1.21.1 {
/*        return stack.has(DataComponents.CUSTOM_DATA);
*///?} else {
        return stack.hasTag();
//?}
    }

    /** Whether the stack carries free-form custom data. */
    public static boolean hasCustomData(ItemStack stack) {
        return has(stack);
    }

    /** Whether the stack carries any non-default data; 1.20.1 has only the tag. */
    public static boolean hasAnyData(ItemStack stack) {
//? if >=1.21.1 {
/*        return !stack.getComponentsPatch().isEmpty();
*///?} else {
        return stack.hasTag();
//?}
    }

    /** The stack's data, or null when it carries none. */
    public static CompoundTag read(ItemStack stack) {
//? if >=1.21.1 {
/*        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
*///?} else {
        return stack.hasTag() ? stack.getTag().copy() : null;
//?}
    }

    public static CompoundTag readOrEmpty(ItemStack stack) {
        var tag = read(stack);
        return tag == null ? new CompoundTag() : tag;
    }

    public static void write(ItemStack stack, CompoundTag tag) {
//? if >=1.21.1 {
/*        if (tag == null || tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
*///?} else {
        stack.setTag(tag == null || tag.isEmpty() ? null : tag);
//?}
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> action) {
        var tag = readOrEmpty(stack);
        action.accept(tag);
        write(stack, tag);
    }

    public static int getInt(ItemStack stack, String key, int def) {
        var tag = read(stack);
        return tag != null && tag.contains(key) ? tag.getInt(key) : def;
    }

    public static void putInt(ItemStack stack, String key, int value) {
        update(stack, tag -> tag.putInt(key, value));
    }

    public static String getString(ItemStack stack, String key, String def) {
        var tag = read(stack);
        return tag != null && tag.contains(key) ? tag.getString(key) : def;
    }

    public static void putString(ItemStack stack, String key, String value) {
        update(stack, tag -> tag.putString(key, value));
    }

    public static boolean getBoolean(ItemStack stack, String key, boolean def) {
        var tag = read(stack);
        return tag != null && tag.contains(key) ? tag.getBoolean(key) : def;
    }

    public static void putBoolean(ItemStack stack, String key, boolean value) {
        update(stack, tag -> tag.putBoolean(key, value));
    }

    public static void remove(ItemStack stack, String key) {
        update(stack, tag -> tag.remove(key));
    }
}
