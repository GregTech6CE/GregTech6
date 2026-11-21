package gregtech.engine.stack;

import gregtech.engine.fluid.FluidTankGT;
import net.minecraft.core.HolderLookup;
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
 * <p>1.21.1 requires a registry lookup for every stack codec; 1.20.1 ignores it.
 * {@code HolderLookup.Provider} exists on both nodes, so the parameter type is shared and only
 * the codec calls are version-specific. A load may run before the block entity has a level,
 * which is why the lookup is passed in rather than read from the level.</p>
 */
public final class NbtCompat {
    private NbtCompat() {}

    /** Saved form of an item stack (empty stacks encode cleanly as an empty/air tag on both nodes). */
    public static Tag saveItem(ItemStack stack, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return stack.saveOptional(provider(lookup));
*///?} else {
        return stack.save(new CompoundTag());
//?}
    }

    /**
     * Reads an item stack. A legacy 1.20-style {@code tag} compound inside a 1.21 tag is restored as
     * custom data, so cover and filter contents survive the version change.
     */
    public static ItemStack readItem(CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        ItemStack stack = ItemStack.parseOptional(provider(lookup), tag);
        if (!stack.isEmpty() && tag.contains("tag"))
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag.getCompound("tag")));
        return stack;
*///?} else {
        return ItemStack.of(tag);
//?}
    }

    /** Saved form of a fluid stack (empty stacks encode cleanly on both nodes). */
    public static Tag saveFluid(FluidStack stack, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return stack.saveOptional(provider(lookup));
*///?} else {
        return stack.writeToNBT(new CompoundTag());
//?}
    }

    public static FluidStack readFluid(CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        if (!tag.contains("FluidName")) return FluidStack.parseOptional(provider(lookup), tag);
        // Legacy Forge FluidName/Amount identity envelope.
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("FluidName"));
        if (id == null || !net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(id)) return FluidStack.EMPTY;
        var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id);
        if (fluid == net.minecraft.world.level.material.Fluids.EMPTY) return FluidStack.EMPTY;
        var result = new FluidStack(fluid, Math.max(1, tag.getInt("Amount")));
        if (tag.contains("Tag")) result.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag.getCompound("Tag")));
        return result;
*///?} else {
        return FluidStack.loadFluidStackFromNBT(tag);
//?}
    }

    public static void writeTank(FluidTankGT tank, CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        tank.writeToNBT(tag, provider(lookup));
*///?} else {
        tank.writeToNBT(tag);
//?}
    }

    public static void readTank(FluidTankGT tank, CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        tank.readFromNBT(tag, provider(lookup));
*///?} else {
        tank.readFromNBT(tag);
//?}
    }

    /** A block entity snapshot without metadata; 1.20.1 ignores the lookup. */
    public static CompoundTag saveWithoutMetadata(net.minecraft.world.level.block.entity.BlockEntity blockEntity,
            HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return blockEntity.saveWithoutMetadata(provider(lookup));
*///?} else {
        return blockEntity.saveWithoutMetadata();
//?}
    }

    /**
     * A block entity snapshot suitable for storing on a dropped/cloned {@link ItemStack}: 1.21.1
     * {@code BLOCK_ENTITY_DATA} requires the {@code "id"} entry while 1.20.1 {@code BlockEntityTag}
     * keeps empty shells clean without it.
     */
    public static CompoundTag saveForBlockItem(net.minecraft.world.level.block.entity.BlockEntity blockEntity,
            HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return blockEntity.saveWithId(provider(lookup));
*///?} else {
        return blockEntity.saveWithoutMetadata();
//?}
    }

    /** Reads client update tag from a block entity; 1.20.1 ignores the lookup. */
    public static CompoundTag getUpdateTag(net.minecraft.world.level.block.entity.BlockEntity blockEntity,
            HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return blockEntity.getUpdateTag(provider(lookup));
*///?} else {
        return blockEntity.getUpdateTag();
//?}
    }

    /** Loads serialized custom block entity data from {@code tag}; 1.20.1 ignores the lookup. */
    public static void loadBlockEntity(net.minecraft.world.level.block.entity.BlockEntity blockEntity,
            CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        blockEntity.loadCustomOnly(tag, provider(lookup));
*///?} else {
        blockEntity.load(tag);
//?}
    }

//? if >=1.21.1 {
    /*private static HolderLookup.Provider provider(HolderLookup.Provider lookup) {
        return java.util.Objects.requireNonNull(lookup, "stack codec needs the registry lookup");
    }
*///?}
}
