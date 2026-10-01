package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.registry.GTBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Map;

/** Ensures GT stone blocks, slabs, and block items resolve to {@code models/block/stones/...}. */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StoneBlockClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StoneBlockClientModels() {}

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        int aliased = 0;
        int missing = 0;

        for (var entry : GTBlocks.allEntries()) {
            if (!entry.isPresent()) {
                continue;
            }
            Block block = entry.get();
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
            if (blockId == null) {
                continue;
            }
            if (!(block instanceof GTStoneBlock stoneBlock)) {
                continue;
            }
            String modelPath = stoneBlock.modelId();

            BakedModel shared = lookupStoneModel(models, modelPath);
            if (shared == null) {
                missing++;
                continue;
            }
            BakedModel itemModel = BlockItemClientModels.asBlockItem(shared);
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
            if (itemId == null) {
                itemId = blockId;
            }
            BlockItemClientModels.alias(models, itemId, GregTech.id(modelPath), shared, itemModel);
            aliased++;
        }

        LOGGER.info("[{}] Stone block model aliasing: {} mapped, {} missing", GregTech.NAMESPACE, aliased, missing);
    }

    private static BakedModel lookupStoneModel(Map<ResourceLocation, BakedModel> models, String modelPath) {
        ResourceLocation modelId = GregTech.id(modelPath);
        for (String variant : new String[]{"", "inventory", "normal"}) {
            BakedModel model = models.get(new ModelResourceLocation(modelId, variant));
            if (model != null) {
                return model;
            }
        }
        return models.get(modelId);
    }
}
