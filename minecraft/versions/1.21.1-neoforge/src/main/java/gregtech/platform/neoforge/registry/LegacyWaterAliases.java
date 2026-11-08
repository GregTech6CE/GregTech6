package gregtech.platform.neoforge.registry;

import gregtech.engine.fluid.LegacyWorldWater;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Resolve saved water palettes and fluid stacks without registering duplicate world fluids. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class LegacyWaterAliases {
    private LegacyWaterAliases() {}

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            for (String path : LegacyWorldWater.BLOCK_PATHS) {
                event.getRegistry()
                        .addAlias(
                                ResourceLocation.fromNamespaceAndPath("gt6ce", path),
                                ResourceLocation.withDefaultNamespace("water"));
            }
        } else if (event.getRegistryKey().equals(Registries.FLUID)) {
            for (String path : LegacyWorldWater.BLOCK_PATHS) {
                for (String suffix : new String[] {"", "_flowing"}) {
                    event.getRegistry()
                            .addAlias(
                                    ResourceLocation.fromNamespaceAndPath("gt6ce", path + suffix),
                                    BuiltInRegistries.FLUID.getKey(LegacyWorldWater.fluid(path + suffix)));
                }
            }
        }
    }
}
