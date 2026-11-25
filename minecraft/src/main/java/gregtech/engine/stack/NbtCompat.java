package gregtech.engine.stack;

import com.mojang.serialization.DynamicOps;
import gregtech.engine.fluid.FluidTankGT;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.1 {
/*import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
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
/*        return stack.saveOptional(java.util.Objects.requireNonNull(lookup, "stack codec needs the registry lookup"));
*///?} else {
        return stack.save(new CompoundTag());
//?}
    }

    /**
     * Reads stacks in GT-owned NBT fields, which vanilla's chunk fixer does not visit.
     */
    public static ItemStack readItem(CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        java.util.Objects.requireNonNull(lookup, "stack codec needs the registry lookup");
        if (hasLegacyCount(tag)) {
            int count = tag.getInt("Count");
            if (count <= 0) return ItemStack.EMPTY;
            if (!tag.contains("components")) {
                // 3465 is the data version of the Forge 1.20.1 node. Fix vanilla components as well as the count.
                var fixed = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, tag.copy()),
                        3465, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                return ItemStack.parseOptional(lookup, (CompoundTag) fixed.getValue());
            }
            tag = tag.copy();
            tag.putInt("count", count);
        }
        ItemStack stack = ItemStack.parseOptional(lookup, tag);
        if (!stack.isEmpty() && tag.contains("tag", Tag.TAG_COMPOUND)) {
            // Older cover/filter snapshots may already use the modern envelope but still carry a legacy tag.
            var merged = tag.getCompound("tag").copy();
            var current = stack.get(DataComponents.CUSTOM_DATA);
            if (current != null) merged.merge(current.copyTag());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(merged));
        }
        return stack;
*///?} else {
        return ItemStack.of(tag);
//?}
    }

//? if >=1.21.1 {
    /*static boolean needsItemUpgrade(CompoundTag tag) {
        return hasLegacyCount(tag) || tag.contains("tag", Tag.TAG_COMPOUND);
    }

    private static boolean hasLegacyCount(CompoundTag tag) {
        return tag.contains("Count", Tag.TAG_ANY_NUMERIC) && !tag.contains("count");
    }
*///?}

    /** Saved form of a fluid stack (empty stacks encode cleanly on both nodes). */
    public static Tag saveFluid(FluidStack stack, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return stack.saveOptional(java.util.Objects.requireNonNull(lookup, "stack codec needs the registry lookup"));
*///?} else {
        return stack.writeToNBT(new CompoundTag());
//?}
    }

    public static FluidStack readFluid(CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        if (!tag.contains("FluidName")) return FluidStack.parseOptional(java.util.Objects.requireNonNull(lookup, "stack codec needs the registry lookup"), tag);
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
        tank.writeToNBT(tag, lookup);
    }

    public static void readTank(FluidTankGT tank, CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        tank.readFromNBT(tag, lookup);
    }

    /** Writes a loader {@link FluidTank} into {@code tag}; 1.20.1 ignores the lookup. */
    public static CompoundTag writeTank(
            FluidTank tank, CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return tank.writeToNBT(Objects.requireNonNull(lookup, "tank codec needs the registry lookup"), tag);
*///?} else {
        return tank.writeToNBT(tag);
//?}
    }

    /** Restores a loader {@link FluidTank} from {@code tag}, supporting legacy nested {@code Fluid} envelopes. */
    public static void readTank(FluidTank tank, CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        Objects.requireNonNull(lookup, "tank codec needs the registry lookup");
        var fluid = readFluid(tag.contains("Fluid") ? tag.getCompound("Fluid") : tag, lookup);
        long amount = tag.contains("Fluid") && tag.contains("Amount") ? tag.getLong("Amount") : fluid.getAmount();
        if (!fluid.isEmpty()) fluid.setAmount((int) Math.max(0, Math.min(tank.getCapacity(), amount)));
        tank.setFluid(fluid);
*///?} else {
        if (tag.contains("Fluid")) {
            var fluid = FluidStack.loadFluidStackFromNBT(tag.getCompound("Fluid"));
            if (fluid.isEmpty()) {
                tank.setFluid(FluidStack.EMPTY);
                return;
            }
            long amount = tag.contains("Amount") ? tag.getLong("Amount") : fluid.getAmount();
            fluid.setAmount((int) Math.max(0, Math.min(tank.getCapacity(), amount)));
            tank.setFluid(fluid);
        } else {
            tank.readFromNBT(tag);
        }
//?}
    }

    /** Saves {@link NonNullList} items via {@link ContainerHelper}; 1.20.1 ignores the lookup. */
    public static CompoundTag saveAllItems(
            CompoundTag tag, NonNullList<ItemStack> items, @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return ContainerHelper.saveAllItems(
                tag, items, Objects.requireNonNull(lookup, "container codec needs the registry lookup"));
*///?} else {
        return ContainerHelper.saveAllItems(tag, items);
//?}
    }

    /** Loads {@link NonNullList} items via {@link ContainerHelper}; 1.20.1 ignores the lookup. */
    public static void loadAllItems(
            CompoundTag tag, NonNullList<ItemStack> items, @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        ContainerHelper.loadAllItems(
                tag, items, Objects.requireNonNull(lookup, "container codec needs the registry lookup"));
*///?} else {
        ContainerHelper.loadAllItems(tag, items);
//?}
    }

    /** Loads or creates a level {@link SavedData} entry across the two storage signatures. */
    public static <T extends SavedData> T computeSavedData(
            DimensionDataStorage storage,
            Supplier<T> constructor,
            BiFunction<CompoundTag, HolderLookup.Provider, T> deserializer,
            String name) {
//? if >=1.21.1 {
/*        return storage.computeIfAbsent(
                new SavedData.Factory<>(constructor, deserializer, DataFixTypes.LEVEL), name);
*///?} else {
        return storage.computeIfAbsent(tag -> deserializer.apply(tag, null), constructor, name);
//?}
    }

    /** NBT {@link DynamicOps} bound to {@code lookup} on 1.21.1, or plain {@link NbtOps#INSTANCE} on 1.20.1. */
    public static DynamicOps<Tag> nbtOps(@Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return Objects.requireNonNull(lookup, "nbt ops needs the registry lookup")
                .createSerializationContext(NbtOps.INSTANCE);
*///?} else {
        return NbtOps.INSTANCE;
//?}
    }

    /** A block entity snapshot without metadata; 1.20.1 ignores the lookup. */
    public static CompoundTag saveWithoutMetadata(net.minecraft.world.level.block.entity.BlockEntity blockEntity,
            @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return blockEntity.saveWithoutMetadata(Objects.requireNonNull(lookup, "stack codec needs the registry lookup"));
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
            @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return blockEntity.saveWithId(Objects.requireNonNull(lookup, "stack codec needs the registry lookup"));
*///?} else {
        return blockEntity.saveWithoutMetadata();
//?}
    }

    /** Reads client update tag from a block entity; 1.20.1 ignores the lookup. */
    public static CompoundTag getUpdateTag(net.minecraft.world.level.block.entity.BlockEntity blockEntity,
            @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        return blockEntity.getUpdateTag(Objects.requireNonNull(lookup, "stack codec needs the registry lookup"));
*///?} else {
        return blockEntity.getUpdateTag();
//?}
    }

    /** Loads serialized custom block entity data from {@code tag}; 1.20.1 ignores the lookup. */
    public static void loadBlockEntity(net.minecraft.world.level.block.entity.BlockEntity blockEntity,
            CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        blockEntity.loadCustomOnly(tag, Objects.requireNonNull(lookup, "stack codec needs the registry lookup"));
*///?} else {
        blockEntity.load(tag);
//?}
    }

    /** Loads full block entity state including components from {@code tag}; 1.20.1 delegates to {@code load}. */
    public static void loadBlockEntityWithComponents(net.minecraft.world.level.block.entity.BlockEntity blockEntity,
            CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        blockEntity.loadWithComponents(tag, Objects.requireNonNull(lookup, "stack codec needs the registry lookup"));
*///?} else {
        blockEntity.load(tag);
//?}
    }
}
