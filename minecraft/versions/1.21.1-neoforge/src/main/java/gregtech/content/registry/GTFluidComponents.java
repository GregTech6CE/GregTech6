package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.*;

/** Native serialization/network boundary for finite physical vessels. */
public final class GTFluidComponents {
    private GTFluidComponents() {}

    public static final DeferredRegister<DataComponentType<?>> TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, REGISTRY_NAMESPACE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> VESSEL_CONTENTS =
            TYPES.register(
                    "vessel_contents",
                    () -> DataComponentType.<SimpleFluidContent>builder()
                            .persistent(SimpleFluidContent.CODEC)
                            .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                            .build());
}
