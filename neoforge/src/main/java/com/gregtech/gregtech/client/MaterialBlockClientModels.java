package com.gregtech.gregtech.client;


import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.prefix.BlockPrefixRegistry;
import com.gregtech.gregtech.block.MaterialBlockLike;
import com.gregtech.gregtech.registry.GTBlocks;
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

@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MaterialBlockClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MaterialBlockClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        BlockPrefixRegistry.ensurePrefixesLoaded();
        java.util.Set<ResourceLocation> registered = new java.util.HashSet<>();
        for (MaterialTextureSet set : MaterialTextureSet.MODELED) {
            for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
                // Ores are baked from the stone template by OreClientModels, not a material cube.
                if (prefix == BlockMaterialPrefix.ore || prefix == BlockMaterialPrefix.oreSmall) continue;
                ResourceLocation location = BlockMaterialIcons.sharedModelLocation(set, prefix);
                if (registered.add(location)) event.register(ModelResourceLocation.standalone(location));
            }
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        int aliased = 0;
        int missing = 0;
        Map<BakedModel, BakedModel> tintedModels = new java.util.IdentityHashMap<>();

        for (var entry : GTBlocks.allEntries()) {
            if (!entry.isBound() || !(entry.get() instanceof MaterialBlockLike materialBlock)) {
                continue;
            }
            // Ore blocks render via OreBakedModel (NBT-driven stone background), see OreClientModels.
            if (entry.get() instanceof com.gregtech.gregtech.block.OreBlock) {
                continue;
            }
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(entry.get());
            if (blockId == null) {
                continue;
            }
            BakedModel shared = resolveSharedModel(models, materialBlock);
            if (shared == null) {
                missing++;
                continue;
            }
            int color = materialBlock.material().getColor();
            BakedModel tintedShared = tintedModels.computeIfAbsent(shared, MaterialBlockBakedModel::new);
            BakedModel worldModel = materialBlock.prefix().isCrate()
                    ? new CrateBlockBakedModel(tintedShared, color, CrateBlockBakedModel.DisplayMode.WORLD)
                    : tintedShared;
            BakedModel itemModel = materialBlock.prefix().isCrate()
                    ? BlockItemClientModels.asBlockItem(new CrateBlockBakedModel(tintedShared, color, CrateBlockBakedModel.DisplayMode.ITEM))
                    : BlockItemClientModels.asBlockItem(tintedShared);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.get().asItem());
            if (itemId == null) {
                itemId = blockId;
            }
            BlockItemClientModels.alias(models, itemId,
                    ResourceLocation.fromNamespaceAndPath("gregtech","block/blocks/" + blockId.getPath()), worldModel, itemModel);
            aliased++;
        }

        LOGGER.info("[{}] Material block model aliasing: {} mapped, {} missing", "gregtech", aliased, missing);
    }

    private static BakedModel resolveSharedModel(Map<ModelResourceLocation, BakedModel> models, MaterialBlockLike materialBlock) {
        MaterialTextureSet primary = MaterialIcons.resolveTextureSet(materialBlock.material());
        BakedModel model = lookupModel(models, primary, materialBlock.prefix());
        if (model != null) {
            return model;
        }
        for (MaterialTextureSet fallback : MaterialTextureSet.MODELED) {
            if (fallback == primary) {
                continue;
            }
            model = lookupModel(models, fallback, materialBlock.prefix());
            if (model != null) {
                return model;
            }
        }
        return null;
    }

    private static BakedModel lookupModel(Map<ModelResourceLocation, BakedModel> models,
                                          MaterialTextureSet set,
                                          BlockMaterialPrefix prefix) {
        ResourceLocation id = BlockMaterialIcons.sharedModelLocation(set, prefix);
        return BakedModelLookup.find(models, id);
    }
}
