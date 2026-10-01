package com.gregtech.gregtech.registry;
import net.minecraft.core.component.DataComponentType;import net.minecraft.core.registries.Registries;import net.neoforged.neoforge.registries.*;import net.neoforged.neoforge.fluids.SimpleFluidContent;
/** Native serialization/network boundary for finite physical vessels. */
public final class GTFluidComponents {
 private GTFluidComponents(){}public static final DeferredRegister<DataComponentType<?>> TYPES=DeferredRegister.create(Registries.DATA_COMPONENT_TYPE,"gregtech");
 public static final DeferredHolder<DataComponentType<?>,DataComponentType<SimpleFluidContent>> VESSEL_CONTENTS=TYPES.register("vessel_contents",()->DataComponentType.<SimpleFluidContent>builder().persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC).build());
}
