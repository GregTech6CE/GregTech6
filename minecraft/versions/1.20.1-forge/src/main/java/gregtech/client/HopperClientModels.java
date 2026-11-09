package gregtech.client;

import com.mojang.logging.LogUtils;
import gregtech.GregTech;
import gregtech.engine.machine.MachineRegistry;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Hopper / queuehopper shared model registration and bake-time aliasing.
 * Follows the same pattern as {@link MachineBlockClientModels} for burning boxes.
 */
public final class HopperClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] HOPPER_TYPES = {"hopper", "queuehopper"};

    private HopperClientModels() {}

    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        for (String type : HOPPER_TYPES) {
            for (Direction facing : Direction.values()) {
                ClientModelEvents.registerAdditional(event, HopperIcons.sharedModelLocation(type, facing));
            }
        }
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());

        int hoppers = aliasHoppers(models, MachineRegistry.hoppers(), "hopper");
        int queueHoppers = aliasHoppers(models, MachineRegistry.queueHoppers(), "queuehopper");
        LOGGER.info(
                "[{}] Hopper model aliasing: {} hoppers, {} queuehoppers", GregTech.NAMESPACE, hoppers, queueHoppers);
    }

    private static <T extends Block> int aliasHoppers(
            BakedModelMap models, Iterable<net.minecraftforge.registries.RegistryObject<T>> blocks, String texPath) {
        // Collect per-facing shared models
        Map<Direction, BakedModel> byFacing = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.values()) {
            BakedModel facingModel = lookupFacingModel(models, texPath, facing);
            if (facingModel != null) {
                byFacing.put(facing, facingModel);
            }
        }
        if (byFacing.isEmpty()) {
            LOGGER.warn("[{}] No per-facing models found for hopper type '{}'", GregTech.NAMESPACE, texPath);
            return 0;
        }
        if (byFacing.size() < 6) {
            LOGGER.warn(
                    "[{}] Only {}/6 per-facing models found for '{}'", GregTech.NAMESPACE, byFacing.size(), texPath);
            return 0;
        }

        BakedModel itemInner = byFacing.get(Direction.DOWN);
        BakedModel worldModel = new HopperBakedModel(byFacing, itemInner);
        BakedModel itemModel = BlockItemClientModels.asBlockItem(itemInner);

        int aliased = 0;
        for (var entry : blocks) {
            if (!entry.isPresent()) continue;
            Block block = entry.get();
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
            if (blockId == null) continue;

            // Alias base model
            models.put(blockId, "", worldModel);
            // Cover every state, including waterlogged hoppers.
            for (var state : block.getStateDefinition().getPossibleStates()) {
                models.putState(blockId, state, worldModel);
            }
            // Alias item inventory
            BlockItemClientModels.aliasBlockItem(models, block, itemModel);
            aliased++;
        }
        return aliased;
    }

    @Nullable
    private static BakedModel lookupFacingModel(BakedModelMap models, String type, Direction facing) {
        return models.find(HopperIcons.sharedModelLocation(type, facing));
    }
}
