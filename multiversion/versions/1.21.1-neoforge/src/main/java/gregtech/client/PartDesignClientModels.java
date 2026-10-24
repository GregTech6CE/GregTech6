package gregtech.client;

import gregtech.api.mod.GregTechIdentity;
import gregtech.content.multiblock.PartDesigns;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.slf4j.Logger;

/** Loads one cube per design index and swaps it onto structural parts from {@code gt.design}. */
@EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PartDesignClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PartDesignClientModels() {}

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        for (var folder : PartDesigns.folders().entrySet()) {
            for (int design = 1; design <= folder.getValue(); design++)
                event.register(ModelResourceLocation.standalone(id(modelPath(folder.getKey(), design))));
        }
    }

    @SubscribeEvent
    public static void bake(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        int wrapped = 0;
        int missing = 0;
        for (String path : PartDesigns.paths()) {
            ResourceLocation blockId = id(path);
            if (!BuiltInRegistries.BLOCK.containsKey(blockId)) continue;
            var block = BuiltInRegistries.BLOCK.get(blockId);
            int max = PartDesigns.max(path);
            String folder = PartDesigns.folder(path);
            var designs = new BakedModel[max + 1];
            for (int design = 1; design <= max; design++) {
                designs[design] = models.get(ModelResourceLocation.standalone(id(modelPath(folder, design))));
                if (designs[design] == null) missing++;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation key = BlockModelShaper.stateToModelLocation(blockId, state);
                BakedModel original = models.get(key);
                if (original == null || original instanceof PartDesignBakedModel) continue;
                models.put(key, new PartDesignBakedModel(original, designs));
                wrapped++;
            }
        }
        LOGGER.info("[{}] Part design models: {} blockstates, {} missing design models",
                GregTechIdentity.MOD_ID, wrapped, missing);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(GregTechIdentity.MOD_ID, path);
    }

    private static String modelPath(String folder, int design) {
        return "block/machine/multiblock/design/" + folder + "_" + design;
    }
}
