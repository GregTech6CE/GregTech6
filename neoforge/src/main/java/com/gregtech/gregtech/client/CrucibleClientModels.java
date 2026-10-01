package com.gregtech.gregtech.client;


import com.gregtech.gregtech.api.machine.CrucibleSpec;

import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

import java.util.Map;

/** Crucibles: GT6 bowl hull via {@link CrucibleBlockBakedModel} + content BER top pass. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CrucibleClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CrucibleClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        for (MaterialTextureSet set : MaterialTextureSet.MODELED) {
            event.register(ModelResourceLocation.standalone(CrucibleBowlIcons.sharedModelLocation(set)));
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        int aliased = 0;
        int missing = 0;

        for (var entry : com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries.crucibles()) {
            if (!entry.isBound()) {
                continue;
            }
            SmeltingCrucibleBlock block = entry.get();
            CrucibleSpec spec = block.spec();
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            if (blockId == null) {
                continue;
            }

            MaterialTextureSet textureSet = MaterialIcons.resolveTextureSet(spec.material());
            BakedModel shared = lookupModel(models, CrucibleBowlIcons.sharedModelLocation(textureSet));
            if (shared == null) {
                for (MaterialTextureSet fallback : MaterialTextureSet.MODELED) {
                    if (fallback == textureSet) {
                        continue;
                    }
                    shared = lookupModel(models, CrucibleBowlIcons.sharedModelLocation(fallback));
                    if (shared != null) {
                        break;
                    }
                }
            }
            if (shared == null) {
                missing++;
                continue;
            }

            // Hull tint via block/item color handlers (tintindex 0), not vertex multiply.
            BakedModel worldModel = new CrucibleBlockBakedModel(shared);
            BakedModel itemModel = BlockItemClientModels.asBlockItem(shared);
            ResourceLocation blockModelId = ResourceLocation.fromNamespaceAndPath("gregtech","block/blocks/" + blockId.getPath());
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(block.asItem());
            if (itemId == null) {
                itemId = blockId;
            }
            BlockItemClientModels.alias(models, blockId, blockModelId, worldModel, itemModel);
            BlockItemClientModels.aliasItemInventory(models, itemId, itemModel);
            aliased++;
        }

        LOGGER.info("[{}] Crucible model aliasing: {} mapped, {} missing", "gregtech", aliased, missing);
    }

    private static BakedModel lookupModel(Map<ModelResourceLocation, BakedModel> models, ResourceLocation modelId) {
        return BakedModelLookup.find(models, modelId);
    }
}
