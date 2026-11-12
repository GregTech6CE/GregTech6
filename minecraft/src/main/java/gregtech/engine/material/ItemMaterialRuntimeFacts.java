package gregtech.engine.material;

import gregtech.engine.stack.StackData;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Content-supplied item-stack semantics used by the material runtime. */
public final class ItemMaterialRuntimeFacts {
    @FunctionalInterface
    public interface StoredContentsPolicy {
        boolean hasStoredContents(ItemStack stack);
    }

    private static volatile StoredContentsPolicy storedContents = ItemMaterialRuntimeFacts::conservativeStoredContents;

    private ItemMaterialRuntimeFacts() {}

    public static void install(StoredContentsPolicy policy) {
        storedContents = Objects.requireNonNull(policy, "policy");
    }

    public static boolean hasStoredContents(ItemStack stack) {
        return storedContents.hasStoredContents(stack);
    }

    private static boolean conservativeStoredContents(ItemStack stack) {
        var tag = StackData.read(stack);
        return tag != null
                && (!tag.getCompound("BlockEntityTag").isEmpty()
                        || tag.contains("Items")
                        || tag.contains("EntityTag")
                        || tag.contains("ChargedProjectiles"));
    }
}
