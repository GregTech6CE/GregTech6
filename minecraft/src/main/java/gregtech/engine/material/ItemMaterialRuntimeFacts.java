package gregtech.engine.material;

import gregtech.engine.stack.StackData;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Content-supplied item-stack semantics used by the material runtime. */
public final class ItemMaterialRuntimeFacts {
    @FunctionalInterface
    public interface StoredContentsPolicy {
        boolean hasStoredContents(ItemStack stack);

        default Durability compositionDurability(ItemStack stack) {
            return null;
        }
    }

    public record Durability(int remaining, int maximum) {}

    private static volatile StoredContentsPolicy storedContents = ItemMaterialRuntimeFacts::conservativeStoredContents;

    private ItemMaterialRuntimeFacts() {}

    public static void install(StoredContentsPolicy policy) {
        storedContents = Objects.requireNonNull(policy, "policy");
    }

    public static boolean hasStoredContents(ItemStack stack) {
        return storedContents.hasStoredContents(stack);
    }

    public static Durability compositionDurability(ItemStack stack) {
        var durability = storedContents.compositionDurability(stack);
        if (durability != null) return durability;
        int maximum = stack.getMaxDamage();
        if (!stack.isDamaged() || maximum <= 0) return null;
        int remaining = Math.max(0, Math.min(maximum, maximum - stack.getDamageValue()));
        return new Durability(remaining, maximum);
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
