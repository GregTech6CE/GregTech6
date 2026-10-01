package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.item.CoinItem;
import com.gregtech.gregtech.registry.GTItems;
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
import java.util.IdentityHashMap;

/**
 * Maps each material item to a shared icon model ({@code models/item/material/{textureSet}/{prefix}.json}).
 * Per-material color comes from item tint on layer 0  - no per-item JSON required.
 */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MaterialClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MaterialClientModels() {}

    /**
     * Shared models under {@code models/item/material/...} must be registered explicitly or they are never baked.
     */
    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        PrefixRegistry.ensurePrefixesLoaded();
        for (MaterialTextureSet set : MaterialTextureSet.MODELED) {
            for (MaterialPrefix prefix : PrefixRegistry.all()) {
                event.register(MaterialIcons.sharedModelLocation(set, prefix));
            }
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        int aliased = 0;
        int missing = 0;
        Map<BakedModel, BakedModel> coinModels = new IdentityHashMap<>();

        for (var entry : GTItems.allEntries()) {
            if (!(entry.get() instanceof MaterialItem materialItem)) {
                continue;
            }

            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.get());
            ModelResourceLocation itemModel = new ModelResourceLocation(itemId, "inventory");
            BakedModel shared = resolveSharedModel(models, materialItem);
            if (shared != null) {
                if (materialItem instanceof CoinItem)
                    shared = coinModels.computeIfAbsent(shared, CoinItemBakedModel::new);
                models.put(itemModel, shared);
                aliased++;
            } else {
                missing++;
            }
        }

        LOGGER.info("[{}] Material model aliasing: {} mapped, {} missing shared model",
                GregTech.MODID, aliased, missing);
    }

    private static BakedModel resolveSharedModel(Map<ResourceLocation, BakedModel> models, MaterialItem materialItem) {
        MaterialTextureSet primary = MaterialIcons.resolveTextureSet(materialItem.getMaterial());
        BakedModel model = lookupModel(models, primary, materialItem.getPrefix());
        if (model != null) {
            return model;
        }

        for (MaterialTextureSet fallback : MaterialTextureSet.MODELED) {
            if (fallback == primary) {
                continue;
            }
            model = lookupModel(models, fallback, materialItem.getPrefix());
            if (model != null) {
                return model;
            }
        }
        return null;
    }

    private static BakedModel lookupModel(Map<ResourceLocation, BakedModel> models,
                                          MaterialTextureSet set,
                                          MaterialPrefix prefix) {
        ResourceLocation id = MaterialIcons.sharedModelLocation(set, prefix);
        return BakedModelLookup.find(models, id);
    }
}
