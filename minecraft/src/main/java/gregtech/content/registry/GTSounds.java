package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
//?}

/** GT6 custom sound events — registered to the native sound registry. */
public final class GTSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
//? if >=1.21.1 {
            /*DeferredRegister.create(Registries.SOUND_EVENT, REGISTRY_NAMESPACE);
*///?} else {
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, REGISTRY_NAMESPACE);
//?}
    public static final Supplier<SoundEvent> WRENCH = register("wrench");
    public static final Supplier<SoundEvent> SCREWDRIVER = register("screwdriver");
    public static final Supplier<SoundEvent> BEEP = register("beep");

    private GTSounds() {}

    private static Supplier<SoundEvent> register(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, name)));
    }
}