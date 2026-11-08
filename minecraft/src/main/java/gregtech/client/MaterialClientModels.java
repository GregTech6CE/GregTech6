package gregtech.client;
import gregtech.engine.mod.GregTechIdentity;

import gregtech.api.material.MaterialTextureSet;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.platform.Mc;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}
import org.slf4j.Logger;

import java.util.IdentityHashMap;
import java.util.Map;

//? if >=1.21.1 {
/*/^* Material inventory models and tint; this subscriber is never loaded on a dedicated server. ^/
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
/**
 * Maps each material item to a shared icon model ({@code models/item/material/{textureSet}/{prefix}.json}).
 * Per-material color comes from item tint on layer 0  - no per-item JSON required.
 */
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            for (MaterialPrefix prefix : PrefixRegistry.all()) {
                if (!GTItems.hasBoundItems(prefix) || prefix.isAddon()) continue;
                ClientModelEvents.registerAdditional(event, MaterialIcons.sharedModelLocation(set, prefix));
            }
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        int aliased = 0;
        int missing = 0;
        Map<BakedModel, BakedModel> coinModels = new IdentityHashMap<>();

        for (var entry : GTItems.allEntries()) {
            if (!(entry.get() instanceof MaterialFormItem item) || item.getPrefix().isAddon()) {
                continue;
            }
            if (item instanceof gregtech.content.item.EnergyCrystalItem) {
                continue;
            }
            ResourceLocation itemId = Mc.itemId(entry.get());
            if (itemId == null) {
                continue;
            }
            BakedModel shared = resolveSharedModel(models, item);
            if (shared == null) {
                missing++;
                continue;
            }
            if (item instanceof gregtech.content.item.CoinItem) {
                shared = coinModels.computeIfAbsent(shared, CoinItemBakedModel::new);
            }
            models.put(itemId, "inventory", shared);
            aliased++;
        }

        LOGGER.info("[{}] Material model aliasing: {} mapped, {} missing shared model",
                GregTechIdentity.REGISTRY_NAMESPACE, aliased, missing);
    }

    private static BakedModel resolveSharedModel(BakedModelMap models, MaterialFormItem item) {
        MaterialTextureSet primary = MaterialIcons.resolveTextureSet(item.getMaterial());
        BakedModel model = models.find(MaterialIcons.sharedModelLocation(primary, item.getPrefix()));
        if (model != null) {
            return model;
        }
        // Preserve the original MODELED enum fallback order.
        for (MaterialTextureSet fallback : MaterialTextureSet.modeled()) {
            if (fallback == primary) {
                continue;
            }
            model = models.find(MaterialIcons.sharedModelLocation(fallback, item.getPrefix()));
            if (model != null) {
                return model;
            }
        }
        return null;
    }

//? if >=1.21.1 {
    /*@SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (var entry : GTItems.allEntries()) {
            if (entry.get() instanceof gregtech.content.item.EnergyCrystalItem) continue;
            MaterialFormItem item = (MaterialFormItem) entry.get();
            event.register(ItemColorARGB.opaque((stack, layer) -> layer == 0 ? item.getTintColor() : 0xFFFFFF), entry.get());
        }
    }
*///?}
}
