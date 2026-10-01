package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.GregTech;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** GT6 custom sound events — registered to {@code ForgeRegistries.SOUND_EVENTS}. */
public final class GTSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GregTech.NAMESPACE);

    public static final RegistryObject<SoundEvent> WRENCH   = register("wrench");
    public static final RegistryObject<SoundEvent> SCREWDRIVER = register("screwdriver");
    public static final RegistryObject<SoundEvent> BEEP     = register("beep");

    private static RegistryObject<SoundEvent> register(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(GregTech.id(name)));
    }

    private GTSounds() {}
}
