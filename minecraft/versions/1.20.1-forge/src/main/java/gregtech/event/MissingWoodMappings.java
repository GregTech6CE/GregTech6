package gregtech.event;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.wood.LegacyWoodIds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;

/** Resolve retired wood block and item IDs when loading old Forge worlds. */
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class MissingWoodMappings {
    private MissingWoodMappings() {}

    @SubscribeEvent
    public static void remap(MissingMappingsEvent event) {
        remap(event, Registries.BLOCK, BuiltInRegistries.BLOCK);
        remap(event, Registries.ITEM, BuiltInRegistries.ITEM);
    }

    private static <T> void remap(MissingMappingsEvent event, ResourceKey<Registry<T>> key, Registry<T> registry) {
        for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(key, REGISTRY_NAMESPACE)) {
            ResourceLocation target = target(mapping.getKey().getPath());
            if (target != null && registry.containsKey(target)) mapping.remap(registry.get(target));
        }
    }

    private static ResourceLocation target(String path) {
        String blueMahoe = LegacyWoodIds.BLUE_MAHOE.get(path);
        if (blueMahoe != null) return id(blueMahoe);
        if (!path.startsWith("beam_")) return null;
        String wood = path.substring("beam_".length());
        return switch (wood) {
            case "oak" -> vanilla("stripped_oak_log");
            case "spruce" -> vanilla("stripped_spruce_log");
            case "birch" -> vanilla("stripped_birch_log");
            case "jungle" -> vanilla("stripped_jungle_log");
            case "acacia" -> vanilla("stripped_acacia_log");
            case "darkoak" -> vanilla("stripped_dark_oak_log");
            case "blue_mahoe",
                    "rubber",
                    "maple",
                    "willow",
                    "hazel",
                    "cinnamon",
                    "coconut",
                    "rainbowood",
                    "bluespruce",
                    "white_mahoe" -> id("stripped_log_" + wood);
            default -> null;
        };
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, path);
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
