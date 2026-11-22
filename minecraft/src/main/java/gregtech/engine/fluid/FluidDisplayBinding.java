package gregtech.engine.fluid;

import gregtech.content.data.RegisteredFluids;
import gregtech.content.item.FluidDisplayItemLike;
import gregtech.content.registry.GTFluidItems;
import gregtech.engine.stack.NbtCompat;
import gregtech.engine.stack.StackData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.server.ServerLifecycleHooks;
//?}

/** Lossless actual-fluid payload for display items, including external fluids and fluid NBT. */
public final class FluidDisplayBinding {
    public static final String TAG = "gt.display_fluid";
    private static volatile java.util.function.Supplier<HolderLookup.Provider> clientLookup = () -> null;

    private FluidDisplayBinding() {}

    public static void setClientLookup(java.util.function.Supplier<HolderLookup.Provider> lookup) {
        clientLookup = java.util.Objects.requireNonNull(lookup);
    }

    public static @Nullable HolderLookup.Provider currentLookup() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? clientLookup.get() : server.registryAccess();
    }

    public static FluidStack resolveCurrent(ItemStack stack) {
        var lookup = currentLookup();
//? if >=1.21.1 {
        /*return hasPayload(stack) && lookup == null ? FluidStack.EMPTY : resolve(lookup, stack);
*///?} else {
        return resolve(lookup, stack);
//?}
    }

    public static boolean hasPayload(ItemStack stack) {
        var tag = StackData.read(stack);
        return tag != null && tag.contains(TAG, 10);
    }

    public static ItemStack display(@Nullable HolderLookup.Provider lookup, FluidStack fluid) {
        if (fluid.isEmpty()) return ItemStack.EMPTY;
//? if >=1.21.1 {
        /*var fluidId = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        net.minecraft.world.item.Item carrierItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                fluid.getFluid() == Fluids.WATER || fluid.getFluid() == Fluids.LAVA
                        ? "gt6ce" : fluidId.getNamespace(), "fluid_item_" + fluidId.getPath()));
        if (!(carrierItem instanceof FluidDisplayItemLike))
            for (var handle : GTFluidItems.allEntries()) {
                if (!(handle.get() instanceof FluidDisplayItemLike)) continue;
                carrierItem = handle.get();
                break;
            }
        if (!(carrierItem instanceof FluidDisplayItemLike)) return ItemStack.EMPTY;
        var displayStack = new ItemStack(carrierItem);
        StackData.update(displayStack, tag -> tag.put(TAG, NbtCompat.saveFluid(fluid, lookup)));
        return displayStack;
*///?} else {
        var carrierItem = GTFluidItems.forFluid(fluid.getFluid());
        if (carrierItem == null) carrierItem = GTFluidItems.getFirst();
        if (carrierItem == null) return ItemStack.EMPTY;
        var stack = new ItemStack(carrierItem);
        StackData.update(stack, tag -> tag.put(TAG, NbtCompat.saveFluid(fluid, lookup)));
        return stack;
//?}
    }

    public static FluidStack resolve(@Nullable HolderLookup.Provider lookup, ItemStack stack) {
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
    public static String subtype(@Nullable HolderLookup.Provider lookup, ItemStack stack) {
        var fluid = resolve(lookup, stack);
//? if >=1.21.1 {
        /*return fluid.isEmpty() ? "empty" : BuiltInRegistries.FLUID.getKey(fluid.getFluid()) + "/" + fluid.getComponentsPatch();
*///?} else {
        return fluid.isEmpty() ? "empty" : BuiltInRegistries.FLUID.getKey(fluid.getFluid()) + "/" + (fluid.hasTag() ? fluid.getTag().toString() : "");
//?}
    }
}
