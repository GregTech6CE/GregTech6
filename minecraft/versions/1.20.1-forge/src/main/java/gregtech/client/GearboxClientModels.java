package gregtech.client;

import com.mojang.logging.LogUtils;
import gregtech.GregTech;
import gregtech.content.block.energy.GearboxBlock;
import gregtech.content.registry.GTGearboxes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/** Replaces static six-sided gearbox cubes with GT6's per-face axle/gear textures. */
public final class GearboxClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private GearboxClientModels() {}

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        GearboxBakedModel shared = new GearboxBakedModel();
        int replaced = 0;
        for (var entry : GTGearboxes.allGearboxes()) {
            if (!entry.isPresent()) continue;
            GearboxBlock block = entry.get();
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            if (id == null) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                models.putState(id, state, shared);
                replaced++;
            }
            // Inventory JSON uses the original fresh six-sided housing.
        }
        LOGGER.info(
                "[{}] GT6 custom gearbox face models: {} blockstate variants replaced", GregTech.NAMESPACE, replaced);
    }
}
