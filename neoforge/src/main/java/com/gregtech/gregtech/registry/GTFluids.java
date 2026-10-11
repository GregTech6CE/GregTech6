package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.fluid.FluidVisualPolicy;
import com.gregtech.gregtech.data.RegisteredFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.LinkedHashMap;
import java.util.Map;

/** Neo fluid registry adapter for the same original catalog and registry paths as Forge. */
public final class GTFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, "gregtech");
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, "gregtech");
    private static final Map<String, DeferredHolder<Fluid, ? extends Fluid>> STILL = new LinkedHashMap<>(), FLOWING = new LinkedHashMap<>();
    private static final Map<ResourceLocation, RegisteredFluids.FluidEntry> ENTRIES = new LinkedHashMap<>();
    private GTFluids() {}
    public static void bindFluid(String field, DeferredHolder<Fluid, ? extends Fluid> still, DeferredHolder<Fluid, ? extends Fluid> flowing, ResourceLocation id, RegisteredFluids.FluidEntry entry) {
        STILL.put(field, still); FLOWING.put(field, flowing); ENTRIES.put(id, entry);
    }
    public static String sanitizePath(String name) { return RegisteredFluids.sanitizePath(name); }
    public static DeferredHolder<Fluid, ? extends Fluid> still(String field) { return STILL.get(RegisteredFluids.canonicalField(field)); }
    public static DeferredHolder<Fluid, ? extends Fluid> flowing(String field) { return FLOWING.get(RegisteredFluids.canonicalField(field)); }
    public static RegisteredFluids.FluidEntry entryForTypeId(ResourceLocation id) { return ENTRIES.get(id); }
    public static RegisteredFluids.FluidEntry entryForFluid(Fluid fluid) {
        var id = BuiltInRegistries.FLUID.getKey(fluid);
        var entry = ENTRIES.get(id);
        if (entry == null && id.getPath().endsWith("_flowing")) entry = ENTRIES.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring(0, id.getPath().length() - 8)));
        return entry;
    }
    public static Map<ResourceLocation, RegisteredFluids.FluidEntry> entries() { return Map.copyOf(ENTRIES); }
    public static FluidStack stack(String field, int mb) {
        var entry = RegisteredFluids.get(field);
        if (entry == null || mb <= 0) return FluidStack.EMPTY;
        if (entry.isVanillaWater()) return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, mb);
        if (entry.isVanillaLava()) return new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, mb);
        var holder = still(field);
        return holder == null || !holder.isBound() ? FluidStack.EMPTY : new FluidStack(holder.get(), mb);
    }
    public static FluidType createFluidType(RegisteredFluids.FluidEntry entry) {
        boolean water = com.gregtech.gregtech.api.fluid.GTWaterParity.isWorldWater(entry.registryName());
        var properties = FluidType.Properties.create().density(entry.density() != 0 ? entry.density() : water ? 1000 : entry.gas() ? 1 : entry.textureMode() == RegisteredFluids.FluidTextureMode.GENERIC_MOLTEN ? 3000 : 1000)
                .viscosity(entry.viscosity() != 0 ? entry.viscosity() : water ? 1000 : entry.gas() ? 200 : entry.textureMode() == RegisteredFluids.FluidTextureMode.GENERIC_MOLTEN ? 6000 : 1000)
                .temperature(entry.temperature()).lightLevel(entry.luminosity()).canConvertToSource(water).supportsBoating(water).canHydrate(water).canSwim(water).canDrown(water);
        return new FluidType(properties) {
            @Override public net.minecraft.network.chat.Component getDescription() { return GTFluidType.describe(entry, sanitizePath(entry.registryName())); }
            @Override public net.minecraft.network.chat.Component getDescription(FluidStack stack) { return getDescription(); }
        };
    }
    public static int fluidTemperature(RegisteredFluids.FluidEntry entry) { return entry.temperature(); }
    public static boolean isGas(FluidStack stack) { var entry = entryForFluid(stack.getFluid()); return entry != null ? entry.gas() : stack.getFluid().getFluidType().isLighterThanAir(); }
    public static int resolveTint(RegisteredFluids.FluidEntry entry) { return FluidVisualPolicy.rawTint(entry); }
}
