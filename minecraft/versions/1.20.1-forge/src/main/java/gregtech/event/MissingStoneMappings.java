package gregtech.event;

import gregtech.content.block.stone.RetiredStoneIds;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;

/** Preserve old worlds and inventories without keeping the removed stone registrations. */
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class MissingStoneMappings {
    private static final Set<String> PATHS = Set.copyOf(RetiredStoneIds.blockPaths());

    private MissingStoneMappings() {}

    @SubscribeEvent
    public static void remap(MissingMappingsEvent event) {
        for (var mapping : event.getMappings(Registries.BLOCK, "gt6ce")) {
            String path = mapping.getKey().getPath();
            if (PATHS.contains(path)) mapping.remap(Blocks.STONE);
            else if (!RetiredStoneIds.materialPath(path).equals(path)) {
                var id = new net.minecraft.resources.ResourceLocation("gt6ce", RetiredStoneIds.materialPath(path));
                var registry = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
                mapping.remap(registry.containsKey(id) ? registry.get(id) : Blocks.STONE);
            }
        }
        for (var mapping : event.getMappings(Registries.ITEM, "gt6ce")) {
            String path = mapping.getKey().getPath();
            if (PATHS.contains(path)) mapping.remap(Items.STONE);
            else if (!RetiredStoneIds.materialPath(path).equals(path)) {
                var id = new net.minecraft.resources.ResourceLocation("gt6ce", RetiredStoneIds.materialPath(path));
                var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
                mapping.remap(registry.get(
                        registry.containsKey(id)
                                ? id
                                : new net.minecraft.resources.ResourceLocation("gt6ce", "rock_gt_stone")));
            }
        }
    }
}
