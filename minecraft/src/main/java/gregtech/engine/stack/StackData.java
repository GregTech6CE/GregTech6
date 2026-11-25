package gregtech.engine.stack;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
*///?} else {
import net.minecraft.world.item.WrittenBookItem;
//?}

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

    /**
     * Whether the stack carries free-form custom data.
     *
     * <p>1.21.1 inspects {@code DataComponents.CUSTOM_DATA}; 1.20.1 has only the unified stack
     * tag, so it falls back to {@link ItemStack#hasTag()}.</p>
     */
    public static boolean hasCustomData(ItemStack stack) {
//? if >=1.21.1 {
/*        return stack.has(DataComponents.CUSTOM_DATA);
*///?} else {
        return stack.hasTag();
//?}
    }

    /** Whether the stack carries any non-default data; 1.20.1 has only the tag. */
    public static boolean hasAnyData(ItemStack stack) {
//? if >=1.21.1 {
/*        return !stack.getComponentsPatch().isEmpty();
*///?} else {
        return stack.hasTag();
//?}
    }

    /**
     * Whether two stacks carry the same whole non-default data payload (components patch on
     * 1.21.1, full NBT tag on 1.20.1), independent of item identity or count.
     */
    public static boolean sameData(ItemStack first, ItemStack second) {
//? if >=1.21.1 {
/*        return first.getComponentsPatch().equals(second.getComponentsPatch());
*///?} else {
        return Objects.equals(first.getTag(), second.getTag());
//?}
    }

    /**
     * Value-equality key representing the stack's non-default data (components patch on 1.21.1,
     * full NBT tag or {@code null} on 1.20.1) for diagnostics and deduplication maps.
     */
    public static @Nullable Object dataKey(ItemStack stack) {
//? if >=1.21.1 {
/*        return stack.getComponentsPatch();
*///?} else {
        return stack.getTag();
//?}
    }

    /** The stack's data as a defensive copy, or {@code null} when it carries none. */
    public static @Nullable CompoundTag read(ItemStack stack) {
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

    /**
     * Writes free-form custom data onto {@code stack}.
     *
     * <p>A {@code null} or empty tag is normalized to removal on both nodes so {@link #hasCustomData}
     * and {@link #hasAnyData} never observe an empty payload. Both nodes store an independent copy
     * of {@code tag} so callers cannot mutate the stack's stored state through the passed tag.</p>
     */
    public static void write(ItemStack stack, @Nullable CompoundTag tag) {
//? if >=1.21.1 {
/*        if (tag == null || tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
*///?} else {
        stack.setTag(tag == null || tag.isEmpty() ? null : tag.copy());
//?}
    }

    /**
     * Copies everything {@code from} carries onto {@code to}: on 1.21.1 the whole component patch
     * (damage, custom name, enchantments, custom data), on 1.20.1 the whole tag, which holds the
     * same values. {@code to} keeps its own item defaults for everything {@code from} does not set.
     */
    public static void copyAll(ItemStack from, ItemStack to) {
//? if >=1.21.1 {
        /*to.applyComponents(from.getComponentsPatch());
*///?} else {
        if (from.hasTag()) to.setTag(from.getTag().copy());
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

    /**
     * Returns the written book generation of {@code stack}, or {@code 3} when the stack carries no
     * written-book payload (matching vanilla's non-copyable cap).
     */
    public static int writtenBookGeneration(ItemStack stack) {
//? if >=1.21.1 {
/*        var content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        return content == null ? 3 : content.generation();
*///?} else {
        return stack.hasTag() ? WrittenBookItem.getGeneration(stack) : 3;
//?}
    }

    /** Increments the written book generation of {@code stack} by {@code delta} when present. */
    public static void incrementWrittenBookGeneration(ItemStack stack, int delta) {
//? if >=1.21.1 {
/*        var content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null) return;
        stack.set(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                        content.title(),
                        content.author(),
                        content.generation() + delta,
                        content.pages(),
                        content.resolved()));
*///?} else {
        if (!stack.hasTag()) return;
        stack.getOrCreateTag().putInt("generation", WrittenBookItem.getGeneration(stack) + delta);
//?}
    }
}
