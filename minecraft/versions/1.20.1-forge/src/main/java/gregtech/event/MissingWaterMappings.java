package gregtech.event;

import gregtech.engine.fluid.LegacyWorldWater;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class MissingWaterMappings {
    private MissingWaterMappings() {}

    @SubscribeEvent
    public static void remap(MissingMappingsEvent event) {
        for (var mapping : event.getMappings(Registries.BLOCK, "gt6ce")) {
            if (LegacyWorldWater.BLOCK_PATHS.contains(mapping.getKey().getPath())) {
                // Both blocks serialize the same level property, including falling water (8..15).
                mapping.remap(Blocks.WATER);
            }
        }
        for (var mapping : event.getMappings(Registries.FLUID, "gt6ce")) {
            var replacement = LegacyWorldWater.fluid(mapping.getKey().getPath());
            if (replacement != null) mapping.remap(replacement);
        }
    }
}
