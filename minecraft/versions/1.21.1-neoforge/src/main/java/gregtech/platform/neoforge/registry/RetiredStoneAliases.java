package gregtech.platform.neoforge.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.stone.RetiredStoneIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Aliases are decoded as vanilla stone; no obsolete block or item is registered. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class RetiredStoneAliases {
    private RetiredStoneAliases() {}

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void register(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK)
                && !event.getRegistryKey().equals(Registries.ITEM)) return;
        for (String path : RetiredStoneIds.blockPaths()) {
            event.getRegistry()
                    .addAlias(
                            ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, path),
                            ResourceLocation.withDefaultNamespace("stone"));
        }
        for (var entry : RetiredStoneIds.materialPaths().entrySet()) {
            ResourceLocation target = ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, entry.getValue());
            if (!event.getRegistry().containsKey(target)) {
                target = event.getRegistryKey().equals(Registries.BLOCK)
                        ? ResourceLocation.withDefaultNamespace("stone")
                        : ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, "rock_gt_stone");
            }
            event.getRegistry()
                    .addAlias(ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, entry.getKey()), target);
        }
    }
}
