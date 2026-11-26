package gregtech.engine.fluid;

import gregtech.content.data.RegisteredFluids;
import gregtech.content.item.FluidDisplayItemLike;
import gregtech.content.registry.GTFluidItems;
import gregtech.engine.stack.NbtCompat;
import gregtech.engine.stack.RegistryContext;
import gregtech.engine.stack.StackData;
import gregtech.platform.Mc;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Lossless actual-fluid payload for display items, including external fluids and fluid NBT. */
public final class FluidDisplayBinding {
    public static final String TAG = "gt.display_fluid";

    private FluidDisplayBinding() {}

    public static FluidStack resolveCurrent(ItemStack stack) {
        return resolve(stack, RegistryContext.current());
    }

    public static boolean hasPayload(ItemStack stack) {
        var tag = StackData.read(stack);
        return tag != null && tag.contains(TAG, 10);
    }

    /**
     * Wraps an arbitrary fluid into a display stack. Only a fluid with its own {@code fluid_item_*}
     * carrier keeps that item; everything else (vanilla water/lava and third-party fluids) falls
     * back to the generic carrier so a foreign fluid never borrows an unrelated GT fluid item.
     */
    public static ItemStack display(FluidStack fluid, @Nullable HolderLookup.Provider lookup) {
        if (fluid.isEmpty()) return ItemStack.EMPTY;
//? if >=1.21.1 {
        /*// The payload is saved against this lookup; without one the fluid cannot be encoded.
        if (lookup == null) return ItemStack.EMPTY;
*///?}
        var source = ContainerFluidIdentity.source(fluid.getFluid());
        var fluidId = BuiltInRegistries.FLUID.getKey(source);
        Item carrierItem = fluidId != null && !"minecraft".equals(fluidId.getNamespace())
                ? BuiltInRegistries.ITEM.get(Mc.id(fluidId.getNamespace(), "fluid_item_" + fluidId.getPath()))
                : null;
        if (!(carrierItem instanceof FluidDisplayItemLike)) {
            carrierItem = GTFluidItems.generic();
            if (carrierItem == null) {
                for (var handle : GTFluidItems.allEntries()) {
                    if (handle.isPresent() && handle.get() instanceof FluidDisplayItemLike) {
                        carrierItem = handle.get();
                        break;
                    }
                }
            }
        }
        if (!(carrierItem instanceof FluidDisplayItemLike)) return ItemStack.EMPTY;
        var displayStack = new ItemStack(carrierItem);
        StackData.update(displayStack, tag -> tag.put(TAG, NbtCompat.saveFluid(fluid, lookup)));
        return displayStack;
    }

    public static FluidStack resolve(ItemStack stack, @Nullable HolderLookup.Provider lookup) {
        if (!(stack.getItem() instanceof FluidDisplayItemLike carrierItem)) return FluidStack.EMPTY;
        var data = StackData.read(stack);
        if (data != null && data.contains(TAG, 10)) return NbtCompat.readFluid(data.getCompound(TAG), lookup);
        if (carrierItem.fluidEntry().isVanillaLava()) return new FluidStack(Fluids.LAVA, 1000);
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var fluidId = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(),
                RegisteredFluids.sanitizePath(carrierItem.fluidEntry().registryName()));
        var fluid = BuiltInRegistries.FLUID.get(fluidId);
        return fluid == null || fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, 1000);
    }

    /** Amount is presentation data, not a JEI fluid subtype. */
    public static String subtype(ItemStack stack, @Nullable HolderLookup.Provider lookup) {
        var fluid = resolve(stack, lookup);
        return fluid.isEmpty() ? "empty" : BuiltInRegistries.FLUID.getKey(fluid.getFluid()) + "/" + FluidCompat.metadataKey(fluid);
    }
}
