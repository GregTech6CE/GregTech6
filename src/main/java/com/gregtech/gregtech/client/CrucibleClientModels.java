package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Map;

/** Crucibles: GT6 bowl hull via {@link CrucibleBlockBakedModel} + content BER top pass. */
@Mod.EventBusSubscriber(modid = GregTech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CrucibleClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CrucibleClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        for (MaterialTextureSet set : MaterialTextureSet.MODELED) {
            event.register(CrucibleBowlIcons.sharedModelLocation(set));
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        int aliased = 0;
        int missing = 0;

        for (var entry : MachineRegistry.smeltingCrucibles()) {
            if (!entry.isPresent()) {
                continue;
            }
            SmeltingCrucibleBlock block = entry.get();
            CrucibleSpec spec = block.spec();
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
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
            ResourceLocation blockModelId = GregTech.id("block/blocks/" + blockId.getPath());
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
            if (itemId == null) {
                itemId = blockId;
            }
            BlockItemClientModels.alias(models, blockId, blockModelId, worldModel, itemModel);
            BlockItemClientModels.aliasItemInventory(models, itemId, itemModel);
            aliased++;
        }

        LOGGER.info("[{}] Crucible model aliasing: {} mapped, {} missing", GregTech.MODID, aliased, missing);
    }

    private static BakedModel lookupModel(Map<ResourceLocation, BakedModel> models, ResourceLocation modelId) {
        for (String variant : new String[]{"", "inventory", "normal"}) {
            BakedModel model = models.get(new ModelResourceLocation(modelId, variant));
            if (model != null) {
                return model;
            }
        }
        return models.get(modelId);
    }
}
