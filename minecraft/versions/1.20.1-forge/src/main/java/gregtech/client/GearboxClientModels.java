package gregtech.client;

import com.mojang.logging.LogUtils;
import gregtech.GregTech;
import gregtech.content.block.energy.GearboxBlock;
import gregtech.content.registry.GTGearboxes;
import java.util.Map;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/** Replaces static six-sided gearbox cubes with GT6's per-face axle/gear textures. */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GearboxClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private GearboxClientModels() {}

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        GearboxBakedModel shared = new GearboxBakedModel();
        int replaced = 0;
        for (var entry : GTGearboxes.allGearboxes()) {
            if (!entry.isPresent()) continue;
            GearboxBlock block = entry.get();
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            if (id == null) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                models.put(BlockModelShaper.stateToModelLocation(id, state), shared);
                replaced++;
            }
            // Inventory JSON uses the original fresh six-sided housing.
        }
        LOGGER.info(
                "[{}] GT6 custom gearbox face models: {} blockstate variants replaced", GregTech.NAMESPACE, replaced);
    }
}
