package gregtech.platform.neoforge.registry;

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
                            ResourceLocation.fromNamespaceAndPath("gt6ce", path),
                            ResourceLocation.withDefaultNamespace("stone"));
        }
        for (var entry : RetiredStoneIds.materialPaths().entrySet()) {
            ResourceLocation target = ResourceLocation.fromNamespaceAndPath("gt6ce", entry.getValue());
            if (!event.getRegistry().containsKey(target)) {
                target = event.getRegistryKey().equals(Registries.BLOCK)
                        ? ResourceLocation.withDefaultNamespace("stone")
                        : ResourceLocation.fromNamespaceAndPath("gt6ce", "rock_gt_stone");
            }
            event.getRegistry().addAlias(ResourceLocation.fromNamespaceAndPath("gt6ce", entry.getKey()), target);
        }
    }
}
