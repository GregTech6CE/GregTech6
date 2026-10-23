package com.gregtech.gregtech.client;

//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.GregTech;
//?}
import com.gregtech.gregtech.api.material.MaterialTextureSet;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.api.prefix.PrefixRegistry;
*///?} else {
//?}
import com.gregtech.gregtech.data.MaterialPrefix;
//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
//?}
import com.gregtech.gregtech.item.MaterialItem;
//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.item.CoinItem;
//?}
import com.gregtech.gregtech.registry.GTItems;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
//? if >=1.21.1 {
/*import net.minecraft.client.resources.model.ModelBakery;
*///?} else {
//?}
import net.minecraft.client.resources.model.ModelResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
*///?} else {
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
//?}
import org.slf4j.Logger;

import java.util.Map;
//? if >=1.21.1 {
//?} else {
import java.util.IdentityHashMap;
//?}

//? if >=1.21.1 {
/*/^* Material inventory models and tint; this subscriber is never loaded on a dedicated server. ^/
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
/**
 * Maps each material item to a shared icon model ({@code models/item/material/{textureSet}/{prefix}.json}).
 * Per-material color comes from item tint on layer 0  - no per-item JSON required.
 */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
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
                if (!GTItems.hasBoundItems(prefix) || prefix.isAddon()) continue;
//? if >=1.21.1 {
                /*// Neo 1.21 requires side-loaded models to use the standalone variant.
                event.register(MaterialIcons.sharedModelStandalone(set, prefix));
*///?} else {
                event.register(MaterialIcons.sharedModelLocation(set, prefix));
//?}
            }
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
//? if >=1.21.1 {
        /*Map<ModelResourceLocation, BakedModel> models = event.getModels();
*///?} else {
        Map<ResourceLocation, BakedModel> models = event.getModels();
//?}
        int aliased = 0;
        int missing = 0;
//? if >=1.21.1 {
//?} else {
        Map<BakedModel, BakedModel> coinModels = new IdentityHashMap<>();

//?}
        for (var entry : GTItems.allEntries()) {
            if (entry.get() instanceof com.gregtech.gregtech.item.EnergyCrystalItem) continue;
//? if >=1.21.1 {
            /*com.gregtech.gregtech.api.material.MaterialFormItem item = (com.gregtech.gregtech.api.material.MaterialFormItem) entry.get();
            if (item.getPrefix().isAddon()) continue;
            BakedModel model = resolveSharedModel(models, item);
            if (model == null) {
                missing++;
            } else {
                models.put(ModelResourceLocation.inventory(entry.getId()),item instanceof com.gregtech.gregtech.item.CoinItem?new CoinItemBakedModel(model):model);
*///?} else {
            if (!(entry.get() instanceof com.gregtech.gregtech.api.material.MaterialFormItem materialItem)
                    || materialItem.getPrefix().isAddon()) {
                continue;
            }

            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.get());
            ModelResourceLocation itemModel = new ModelResourceLocation(itemId, "inventory");
            BakedModel shared = resolveSharedModel(models, materialItem);
            if (shared != null) {
                if (materialItem instanceof CoinItem)
                    shared = coinModels.computeIfAbsent(shared, CoinItemBakedModel::new);
                models.put(itemModel, shared);
//?}
                aliased++;
//? if >=1.21.1 {
//?} else {
            } else {
                missing++;
//?}
            }
        }
//? if >=1.21.1 {
        /*LOGGER.info("[gregtech] Material model aliasing: {} mapped, {} missing shared model", aliased, missing);
*///?} else {

        LOGGER.info("[{}] Material model aliasing: {} mapped, {} missing shared model",
                GregTech.NAMESPACE, aliased, missing);
//?}
    }

//? if >=1.21.1 {
    /*@SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (var entry : GTItems.allEntries()) {
            if (entry.get() instanceof com.gregtech.gregtech.item.EnergyCrystalItem) continue;
            com.gregtech.gregtech.api.material.MaterialFormItem item = (com.gregtech.gregtech.api.material.MaterialFormItem) entry.get();
            event.register(ItemColorARGB.opaque((stack, layer) -> layer == 0 ? item.getTintColor() : 0xFFFFFF), entry.get());
*///?} else {
    private static BakedModel resolveSharedModel(Map<ResourceLocation, BakedModel> models, com.gregtech.gregtech.api.material.MaterialFormItem materialItem) {
        MaterialTextureSet primary = MaterialIcons.resolveTextureSet(materialItem.getMaterial());
        BakedModel model = lookupModel(models, primary, materialItem.getPrefix());
        if (model != null) {
            return model;
//?}
        }
//? if >=1.21.1 {
    /*}
*///?} else {
//?}

//? if >=1.21.1 {
    /*private static BakedModel resolveSharedModel(Map<ModelResourceLocation, BakedModel> models, com.gregtech.gregtech.api.material.MaterialFormItem item) {
        MaterialTextureSet primary = MaterialIcons.resolveTextureSet(item.getMaterial());
        BakedModel model = lookupModel(models, primary, item.getPrefix());
        if (model != null) return model;
        // Preserve the original MODELED enum fallback order.
*///?} else {
//?}
        for (MaterialTextureSet fallback : MaterialTextureSet.MODELED) {
//? if >=1.21.1 {
            /*if (fallback == primary) continue;
            model = lookupModel(models, fallback, item.getPrefix());
            if (model != null) return model;
*///?} else {
            if (fallback == primary) {
                continue;
            }
            model = lookupModel(models, fallback, materialItem.getPrefix());
            if (model != null) {
                return model;
            }
//?}
        }
        return null;
    }

//? if >=1.21.1 {
    /*private static BakedModel lookupModel(Map<ModelResourceLocation, BakedModel> models,
                                          MaterialTextureSet set, MaterialPrefix prefix) {
        BakedModel candidate = models.get(MaterialIcons.sharedModelStandalone(set, prefix));
        return candidate == models.get(ModelBakery.MISSING_MODEL_VARIANT) ? null : candidate;
*///?} else {
    private static BakedModel lookupModel(Map<ResourceLocation, BakedModel> models,
                                          MaterialTextureSet set,
                                          MaterialPrefix prefix) {
        ResourceLocation id = MaterialIcons.sharedModelLocation(set, prefix);
        return BakedModelLookup.find(models, id);
//?}
    }
}
