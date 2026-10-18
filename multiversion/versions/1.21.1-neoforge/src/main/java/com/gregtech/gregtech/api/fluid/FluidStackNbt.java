package com.gregtech.gregtech.api.fluid;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;

/** Registry-aware Neo codec plus the original Forge FluidName/Amount identity envelope. */
public final class FluidStackNbt {
    private FluidStackNbt() {}
    public static FluidStack read(HolderLookup.Provider lookup, CompoundTag tag) {
        if (!tag.contains("FluidName")) return FluidStack.parseOptional(lookup, tag);
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("FluidName"));
        if (id == null || !BuiltInRegistries.FLUID.containsKey(id)) return FluidStack.EMPTY;
        var fluid = BuiltInRegistries.FLUID.get(id);
        if (fluid == net.minecraft.world.level.material.Fluids.EMPTY) return FluidStack.EMPTY;
        var result = new FluidStack(fluid, Math.max(1, tag.getInt("Amount")));
        // Preserve opaque legacy metadata without pretending to interpret third-party fluid components.
        if (tag.contains("Tag")) result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.getCompound("Tag")));
        return result;
    }
}
