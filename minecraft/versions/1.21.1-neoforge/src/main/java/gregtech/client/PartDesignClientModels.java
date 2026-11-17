package gregtech.client;

import com.mojang.logging.LogUtils;
import gregtech.content.multiblock.PartDesigns;
import gregtech.engine.mod.GregTechIdentity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/** Loads one cube per design index and swaps it onto structural parts from {@code gt.design}. */
public final class PartDesignClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PartDesignClientModels() {}

    public static void register(ModelEvent.RegisterAdditional event) {
        ResourceManager resources = resourceManager();
        for (var folder : PartDesigns.folders().entrySet()) {
            for (int design = 1; design <= folder.getValue(); design++) {
                String modelPath = PartDesignBakedModel.modelPath(folder.getKey(), design);
                // Only the cubes the packs actually serve may be registered: an addon pack adds a
                // design model, a missing one would fail this bake instead of falling back.
                if (!PartDesignBakedModel.isDesignAvailable(resources, modelPath)) continue;
                ClientModelEvents.registerAdditional(event, PartDesignBakedModel.modelId(folder.getKey(), design));
            }
        }
    }

    public static void bake(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        ResourceManager resources = resourceManager();
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
                String modelPath = PartDesignBakedModel.modelPath(folder, design);
                // An unserved cube keeps the block's own model, so it is skipped rather than missing.
                if (!PartDesignBakedModel.isDesignAvailable(resources, modelPath)) continue;
                designs[design] = models.find(PartDesignBakedModel.modelId(folder, design));
                if (designs[design] == null) missing++;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                BakedModel original = models.get(blockId, BlockModelShaper.statePropertiesToString(state.getValues()));
                if (original == null || original instanceof PartDesignBakedModel) continue;
                models.putState(blockId, state, new PartDesignBakedModel(original, designs));
                wrapped++;
            }
        }
        LOGGER.info(
                "[{}] Part design models: {} blockstates, {} missing design models",
                GregTechIdentity.MOD_ID,
                wrapped,
                missing);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(GregTechIdentity.MOD_ID, path);
    }

    /** The running client's resource manager, or null while the client is still starting. */
    private static @Nullable ResourceManager resourceManager() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.getResourceManager();
    }
}
