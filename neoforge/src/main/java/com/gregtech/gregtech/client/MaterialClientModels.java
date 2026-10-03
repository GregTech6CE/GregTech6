package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTItems;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.slf4j.Logger;

import java.util.Map;

/** Material inventory models and tint; this subscriber is never loaded on a dedicated server. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MaterialClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MaterialClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        PrefixRegistry.ensurePrefixesLoaded();
        for (MaterialTextureSet set : MaterialTextureSet.MODELED) {
            for (MaterialPrefix prefix : PrefixRegistry.all()) {
                if (prefix.isHiddenFromCreative() && !GTItems.hasBoundItems(prefix)) continue;
                // Neo 1.21 requires side-loaded models to use the standalone variant.
                event.register(MaterialIcons.sharedModelStandalone(set, prefix));
            }
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        int aliased = 0;
        int missing = 0;
        for (var entry : GTItems.allEntries()) {
            com.gregtech.gregtech.api.material.MaterialFormItem item = (com.gregtech.gregtech.api.material.MaterialFormItem) entry.get();
            BakedModel model = resolveSharedModel(models, item);
            if (model == null) {
                missing++;
            } else {
                models.put(ModelResourceLocation.inventory(entry.getId()),item instanceof com.gregtech.gregtech.item.CoinItem?new CoinItemBakedModel(model):model);
                aliased++;
            }
        }
        LOGGER.info("[gregtech] Material model aliasing: {} mapped, {} missing shared model", aliased, missing);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (var entry : GTItems.allEntries()) {
            com.gregtech.gregtech.api.material.MaterialFormItem item = (com.gregtech.gregtech.api.material.MaterialFormItem) entry.get();
            event.register(ItemColorARGB.opaque((stack, layer) -> layer == 0 ? item.getTintColor() : 0xFFFFFF), entry.get());
        }
    }

    private static BakedModel resolveSharedModel(Map<ModelResourceLocation, BakedModel> models, com.gregtech.gregtech.api.material.MaterialFormItem item) {
        MaterialTextureSet primary = MaterialIcons.resolveTextureSet(item.getMaterial());
        BakedModel model = lookupModel(models, primary, item.getPrefix());
        if (model != null) return model;
        // Preserve the original MODELED enum fallback order.
        for (MaterialTextureSet fallback : MaterialTextureSet.MODELED) {
            if (fallback == primary) continue;
            model = lookupModel(models, fallback, item.getPrefix());
            if (model != null) return model;
        }
        return null;
    }

    private static BakedModel lookupModel(Map<ModelResourceLocation, BakedModel> models,
                                          MaterialTextureSet set, MaterialPrefix prefix) {
        BakedModel candidate = models.get(MaterialIcons.sharedModelStandalone(set, prefix));
        return candidate == models.get(ModelBakery.MISSING_MODEL_VARIANT) ? null : candidate;
    }
}
