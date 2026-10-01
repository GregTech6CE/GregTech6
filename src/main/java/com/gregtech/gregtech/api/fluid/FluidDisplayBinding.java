package com.gregtech.gregtech.api.fluid;

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
    public static final String TAG="gt.display_fluid";
    private FluidDisplayBinding() {}
    public static boolean hasPayload(ItemStack stack) { return stack.hasTag() && stack.getTag().contains(TAG,10); }
    public static ItemStack display(FluidStack fluid) {
        if(fluid.isEmpty()) return ItemStack.EMPTY;
        var item=GTFluidItems.forFluid(fluid.getFluid());
        if(item==null) item=GTFluidItems.getFirst();
        if(item==null) return ItemStack.EMPTY;
        var stack=new ItemStack(item);
        stack.getOrCreateTag().put(TAG,fluid.writeToNBT(new CompoundTag()));
        return stack;
    }
    public static FluidStack resolve(ItemStack stack) {
        if(!(stack.getItem() instanceof FluidItem item)) return FluidStack.EMPTY;
        if(hasPayload(stack)) return FluidStack.loadFluidStackFromNBT(stack.getTag().getCompound(TAG));
        var id=ResourceLocation.fromNamespaceAndPath("gregtech",RegisteredFluids.sanitizePath(item.fluidEntry().registryName()));
        var fluid=ForgeRegistries.FLUIDS.getValue(id);
        return fluid==null || fluid==net.minecraft.world.level.material.Fluids.EMPTY?FluidStack.EMPTY:new FluidStack(fluid,1000);
    }
    /** Amount is presentation data, not a JEI fluid subtype. */
    public static String subtype(ItemStack stack) {
        var fluid=resolve(stack);
        return fluid.isEmpty()?"empty":ForgeRegistries.FLUIDS.getKey(fluid.getFluid())+"/"+(fluid.hasTag()?fluid.getTag().toString():"");
    }
}
