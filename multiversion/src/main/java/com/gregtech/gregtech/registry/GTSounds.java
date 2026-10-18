package com.gregtech.gregtech.registry;

//? if >=1.21.1 {
/*import com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries;
*///?} else {
import com.gregtech.gregtech.GregTech;
//?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//?}

/** GT6 custom sound events — registered to {@code ForgeRegistries.SOUND_EVENTS}. */

/** Original three sound identities, using the already registered Native sound registry. */
public final class GTSounds {
//? if >=1.21.1 {
    /*public static final DeferredRegister<SoundEvent> REGISTRY = FluidTransportRegistries.SOUNDS;
    public static final DeferredHolder<SoundEvent, SoundEvent> WRENCH = FluidTransportRegistries.WRENCH;
    public static final DeferredHolder<SoundEvent, SoundEvent> SCREWDRIVER = register("screwdriver");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEEP = register("beep");
*///?} else {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GregTech.NAMESPACE);
//?}

//? if >=1.21.1 {
    /*private GTSounds() {}
    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath("gregtech", name)));
*///?} else {
    public static final RegistryObject<SoundEvent> WRENCH   = register("wrench");
    public static final RegistryObject<SoundEvent> SCREWDRIVER = register("screwdriver");
    public static final RegistryObject<SoundEvent> BEEP     = register("beep");

    private static RegistryObject<SoundEvent> register(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(GregTech.id(name)));
//?}
    }

//? if >=1.21.1 {
    /*/^* Initialize before FluidTransportRegistries attaches this same register to the mod bus. ^/
    public static void bootstrap() {}
*///?} else {
    private GTSounds() {}
//?}
}
