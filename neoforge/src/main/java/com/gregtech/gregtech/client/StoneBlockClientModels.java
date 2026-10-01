package com.gregtech.gregtech.client;


import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.registry.GTBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

import java.util.Map;

/** Ensures GT stone blocks, slabs, and block items resolve to {@code models/block/stones/...}. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StoneBlockClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StoneBlockClientModels() {}

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        int aliased = 0;
        int missing = 0;

        for (var entry : GTBlocks.allEntries()) {
            if (!entry.isBound()) {
                continue;
            }
            Block block = entry.get();
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
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
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(block.asItem());
            if (itemId == null) {
                itemId = blockId;
            }
            BlockItemClientModels.alias(models, itemId, ResourceLocation.fromNamespaceAndPath("gregtech",modelPath), shared, itemModel);
            aliased++;
        }

        LOGGER.info("[{}] Stone block model aliasing: {} mapped, {} missing", "gregtech", aliased, missing);
    }

    private static BakedModel lookupStoneModel(Map<ModelResourceLocation, BakedModel> models, String modelPath) {
        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath("gregtech",modelPath);
        return BakedModelLookup.find(models, modelId);
    }

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        for (var entry : GTBlocks.allEntries()) {
            if (entry.isBound() && entry.get() instanceof GTStoneBlock stone) {
                event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("gregtech", stone.modelId())));
            }
        }
    }
}
