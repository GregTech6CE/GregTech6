package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Original three sound identities, using the already registered Native sound registry. */
public final class GTSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY = FluidTransportRegistries.SOUNDS;
    public static final DeferredHolder<SoundEvent, SoundEvent> WRENCH = FluidTransportRegistries.WRENCH;
    public static final DeferredHolder<SoundEvent, SoundEvent> SCREWDRIVER = register("screwdriver");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEEP = register("beep");

    private GTSounds() {}
    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath("gregtech", name)));
    }

    /** Initialize before FluidTransportRegistries attaches this same register to the mod bus. */
    public static void bootstrap() {}
}
