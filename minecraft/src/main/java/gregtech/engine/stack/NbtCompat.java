package gregtech.engine.stack;

import gregtech.engine.fluid.FluidTankGT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/**
 * Item/fluid stack and tank serialization across the two nodes.
 *
 * <p>1.21.1 requires a registry lookup for every stack codec; 1.20.1 does not. Callers pass the
 * lookup they already have (typed as {@link Object} because {@code HolderLookup.Provider} does not
 * exist on 1.20.1) and use {@code null} there, so the only version difference stays inside this
 * class. A load may run before the block entity has a level, which is why the lookup is passed in
 * rather than read from the level.</p>
 */
public final class NbtCompat {
    private NbtCompat() {}

    /** Saved form of an item stack: a compound tag on both nodes. */
    public static Tag saveItem(ItemStack stack, Object lookup) {
//? if >=1.21.1 {
        /*return stack.save(provider(lookup));
*///?} else {
        return stack.save(new CompoundTag());
//?}
    }

    /**
     * Reads an item stack. A legacy 1.20-style {@code tag} compound inside a 1.21 tag is restored as
     * custom data, so cover and filter contents survive the version change.
     */
    public static ItemStack readItem(CompoundTag tag, Object lookup) {
//? if >=1.21.1 {
        /*ItemStack stack = ItemStack.parseOptional(provider(lookup), tag);
        if (!stack.isEmpty() && tag.contains("tag"))
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag.getCompound("tag")));
        return stack;
*///?} else {
        return ItemStack.of(tag);
//?}
    }

    /** Saved form of a fluid stack: a compound tag on both nodes. */
    public static Tag saveFluid(FluidStack stack, Object lookup) {
//? if >=1.21.1 {
        /*return stack.save(provider(lookup));
*///?} else {
        return stack.writeToNBT(new CompoundTag());
//?}
    }

    public static FluidStack readFluid(CompoundTag tag, Object lookup) {
//? if >=1.21.1 {
        /*return FluidStack.parseOptional(provider(lookup), tag);
*///?} else {
        return FluidStack.loadFluidStackFromNBT(tag);
//?}
    }

    public static void writeTank(FluidTankGT tank, CompoundTag tag, Object lookup) {
//? if >=1.21.1 {
        /*tank.writeToNBT(tag, provider(lookup));
*///?} else {
        tank.writeToNBT(tag);
//?}
    }

    public static void readTank(FluidTankGT tank, CompoundTag tag, Object lookup) {
//? if >=1.21.1 {
        /*tank.readFromNBT(tag, provider(lookup));
*///?} else {
        tank.readFromNBT(tag);
//?}
    }

//? if >=1.21.1 {
    /*private static net.minecraft.core.HolderLookup.Provider provider(Object lookup) {
        return (net.minecraft.core.HolderLookup.Provider) java.util.Objects.requireNonNull(
                lookup, "stack codec needs the registry lookup");
    }
*///?}
}
