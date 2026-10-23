package com.gregtech.gregtech.api.fluid;
//? if >=1.21.1 {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import com.gregtech.gregtech.platform.neoforge.fluid.FluidDisplayItem;
import com.gregtech.gregtech.data.RegisteredFluids;

/^* Full source display payload, preserving external fluid identity and native components via provider serialization. ^/
public final class FluidDisplayBinding {

    private FluidDisplayBinding() {
    }

    public static final String TAG = "gt.display_fluid";
    private static volatile java.util.function.Supplier<HolderLookup.Provider> clientLookup = () -> null;

    public static void setClientLookup(java.util.function.Supplier<HolderLookup.Provider> lookup) {
        clientLookup = java.util.Objects.requireNonNull(lookup);
    }

    public static HolderLookup.Provider currentLookup() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? clientLookup.get() : server.registryAccess();
    }

    public static FluidStack resolveCurrent(ItemStack stack) {
        var lookup = currentLookup();
        return hasPayload(stack) && lookup == null ? FluidStack.EMPTY : resolve(lookup, stack);
    }
*///?} else {
//?}

//? if >=1.21.1 {
    /*public static boolean hasPayload(ItemStack stack) {
        var payloadData = com.gregtech.gregtech.content.data.UsbDataMedia.tag(stack);
        return payloadData != null && payloadData.contains(TAG, 10);
    }

    public static ItemStack display(HolderLookup.Provider lookup, FluidStack fluid) {
        if (fluid.isEmpty()) return ItemStack.EMPTY;
        var fluidId = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        net.minecraft.world.item.Item carrierItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                fluidId.getNamespace(), "fluid_item_" + fluidId.getPath()));
        if (!(carrierItem instanceof FluidDisplayItem))
            for (var handle : com.gregtech.gregtech.platform.neoforge.fluid.FluidRegistries.allItems().toList()) {
                if (!(handle.get() instanceof FluidDisplayItem)) continue;
                carrierItem = handle.get();
                break;
            }
        if (!(carrierItem instanceof FluidDisplayItem)) return ItemStack.EMPTY;
        var displayStack = new ItemStack(carrierItem);
        var payloadData = com.gregtech.gregtech.content.data.UsbDataMedia.tagOrEmpty(displayStack);
        payloadData.put(TAG, fluid.save(lookup));
        com.gregtech.gregtech.content.data.UsbDataMedia.store(displayStack, payloadData);
        return displayStack;
    }

    public static FluidStack resolve(HolderLookup.Provider lookup, ItemStack stack) {
        if (!(stack.getItem() instanceof FluidDisplayItem carrierItem)) return FluidStack.EMPTY;
        if (hasPayload(stack)) return FluidStack.parseOptional(lookup, com.gregtech.gregtech.content.data.UsbDataMedia.tag(stack).getCompound(TAG));
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var fluidId = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(),
                RegisteredFluids.sanitizePath(carrierItem.fluidEntry().registryName()));
        var fluid = BuiltInRegistries.FLUID.get(fluidId);
        return fluid == net.minecraft.world.level.material.Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, 1000);
    }

    public static String subtype(HolderLookup.Provider lookup, ItemStack stack) {
        var fluid = resolve(lookup, stack);
        return fluid.isEmpty() ? "empty" : BuiltInRegistries.FLUID.getKey(fluid.getFluid()) + "/" + fluid.getComponentsPatch();
    }
*///?} else {
import com.gregtech.gregtech.item.FluidItem;
import com.gregtech.gregtech.registry.GTFluidItems;
import com.gregtech.gregtech.data.RegisteredFluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Lossless actual-fluid payload for display items, including external fluids and fluid NBT. */
public final class FluidDisplayBinding {
    public static final String TAG = "gt.display_fluid";

    private FluidDisplayBinding() {
    }

    public static boolean hasPayload(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG, 10);
    }

    public static ItemStack display(FluidStack fluid) {
        if (fluid.isEmpty()) return ItemStack.EMPTY;
        var carrierItem = GTFluidItems.forFluid(fluid.getFluid());
        if (carrierItem == null) carrierItem = GTFluidItems.getFirst();
        if (carrierItem == null) return ItemStack.EMPTY;
        var stack = new ItemStack(carrierItem);
        stack.getOrCreateTag().put(TAG, fluid.writeToNBT(new CompoundTag()));
        return stack;
    }

    public static FluidStack resolve(ItemStack stack) {
        if (!(stack.getItem() instanceof FluidItem carrierItem)) return FluidStack.EMPTY;
        if (hasPayload(stack)) return FluidStack.loadFluidStackFromNBT(stack.getTag().getCompound(TAG));
        var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        var fluidId = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(),
                RegisteredFluids.sanitizePath(carrierItem.fluidEntry().registryName()));
        var fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
        return fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, 1000);
    }

    /** Amount is presentation data, not a JEI fluid subtype. */
    public static String subtype(ItemStack stack) {
        var fluid = resolve(stack);
        return fluid.isEmpty() ? "empty" : ForgeRegistries.FLUIDS.getKey(fluid.getFluid()) + "/" + (fluid.hasTag() ? fluid.getTag().toString() : "");
    }
//?}
}
