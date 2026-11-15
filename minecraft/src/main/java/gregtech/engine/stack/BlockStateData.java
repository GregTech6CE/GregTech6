package gregtech.engine.stack;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BlockItemStateProperties;
*///?}

/**
 * The block-state properties an item stack carries: the 1.20.1 {@code BlockStateTag} sub-tag and the
 * 1.21.1 {@code BLOCK_STATE} component ({@link BlockItemStateProperties}).
 *
 * <p>Unlike {@link BlockEntityData}, this component is a structured string map rather than a free-form
 * tag, so the shared surface is a {@code Map<String, String>} of property name to serialized value.
 * The storage difference stays here; callers read and write the same property map on both nodes.</p>
 */
public final class BlockStateData {
    private BlockStateData() {}

    /** The property map, empty when the stack carries none. */
    public static Map<String, String> readProperties(ItemStack stack) {
//? if >=1.21.1 {
/*        return stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).properties();
*///?} else {
        var data = StackData.read(stack);
        Map<String, String> properties = new HashMap<>();
        if (data != null) {
            CompoundTag stateTag = data.getCompound("BlockStateTag");
            for (String key : stateTag.getAllKeys()) properties.put(key, stateTag.getString(key));
        }
        return properties;
//?}
    }

    /** A single property value, or null when absent. */
    public static String getProperty(ItemStack stack, String key) {
        return readProperties(stack).get(key);
    }

    /** Writes the property map; a null or empty map removes the component/sub-tag. */
    public static void writeProperties(ItemStack stack, Map<String, String> properties) {
//? if >=1.21.1 {
/*        if (properties == null || properties.isEmpty()) stack.remove(DataComponents.BLOCK_STATE);
        else stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(properties));
*///?} else {
        StackData.update(stack, tag -> {
            if (properties == null || properties.isEmpty()) {
                tag.remove("BlockStateTag");
            } else {
                CompoundTag stateTag = new CompoundTag();
                properties.forEach(stateTag::putString);
                tag.put("BlockStateTag", stateTag);
            }
        });
//?}
    }

    /** Adds or replaces one property, preserving the others. */
    public static void with(ItemStack stack, String key, String value) {
        Map<String, String> properties = new HashMap<>(readProperties(stack));
        properties.put(key, value);
        writeProperties(stack, properties);
    }

    public static void remove(ItemStack stack) {
//? if >=1.21.1 {
/*        stack.remove(DataComponents.BLOCK_STATE);
*///?} else {
        StackData.update(stack, tag -> tag.remove("BlockStateTag"));
//?}
    }
}