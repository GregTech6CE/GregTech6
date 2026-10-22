package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.content.multiblock.PartDesigns;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Map;

/** Loads one cube per design index and swaps it onto structural parts from {@code gt.design}. */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PartDesignClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PartDesignClientModels() {}

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        for (var folder : PartDesigns.folders().entrySet()) {
            for (int design = 1; design <= folder.getValue(); design++)
                event.register(new ResourceLocation(GregTech.NAMESPACE, modelPath(folder.getKey(), design)));
        }
    }

    @SubscribeEvent
    public static void bake(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        int wrapped = 0;
        int missing = 0;
        for (String path : PartDesigns.paths()) {
            ResourceLocation id = new ResourceLocation(GregTech.NAMESPACE, path);
            if (!ForgeRegistries.BLOCKS.containsKey(id)) continue;
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null) continue;
            int max = PartDesigns.max(path);
            String folder = PartDesigns.folder(path);
            var designs = new BakedModel[max + 1];
            for (int design = 1; design <= max; design++) {
                designs[design] = lookup(models, folder, design);
                if (designs[design] == null) missing++;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ResourceLocation key = BlockModelShaper.stateToModelLocation(id, state);
                BakedModel original = models.get(key);
                if (original == null || original instanceof PartDesignBakedModel) continue;
                models.put(key, new PartDesignBakedModel(original, designs));
                wrapped++;
            }
        }
        LOGGER.info("[{}] Part design models: {} blockstates, {} missing design models",
                GregTech.NAMESPACE, wrapped, missing);
    }

    private static BakedModel lookup(Map<ResourceLocation, BakedModel> models, String folder, int design) {
        ResourceLocation id = new ResourceLocation(GregTech.NAMESPACE, modelPath(folder, design));
        for (String variant : new String[] {"", "inventory"}) {
            BakedModel model = models.get(new ModelResourceLocation(id, variant));
            if (model != null) return model;
        }
        return models.get(id);
    }

    private static String modelPath(String folder, int design) {
        return "block/machine/multiblock/design/" + folder + "_" + design;
    }
}
