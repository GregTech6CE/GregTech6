package gregtech.engine.stack;

import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/**
 * Item/fluid stack identity across the two nodes.
 *
 * <p>1.20.1 keeps data in an item tag and compares it with {@code isSameItemSameTags} /
 * {@code isFluidEqual}; 1.21.1 keeps it in data components and compares whole stacks. Call sites in
 * the shared machine component read as one predicate instead of a Stonecutter branch.</p>
 */
public final class StackCompat {
    private StackCompat() {}

    /** Same item and same data: the pair a recipe output may merge into. */
    public static boolean sameItem(ItemStack first, ItemStack second) {
//? if >=1.21.1 {
        /*return ItemStack.isSameItemSameComponents(first, second);
*///?} else {
        return ItemStack.isSameItemSameTags(first, second);
//?}
    }

    /**
     * Whether {@code into} may absorb {@code from} in an item handler. 1.20.1 asks the item-handler
     * helper (item, share tags and stackability); 1.21.1 compares whole component stacks.
     */
    public static boolean mergeable(ItemStack into, ItemStack from) {
//? if >=1.21.1 {
        /*return ItemStack.isSameItemSameComponents(into, from);
*///?} else {
        return net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(into, from);
//?}
    }

    /** Same fluid and same data: the pair two tanks may share. */
    public static boolean sameFluid(FluidStack first, FluidStack second) {
//? if >=1.21.1 {
        /*return FluidStack.isSameFluidSameComponents(first, second);
*///?} else {
        return first.isFluidEqual(second);
//?}
    }
}
