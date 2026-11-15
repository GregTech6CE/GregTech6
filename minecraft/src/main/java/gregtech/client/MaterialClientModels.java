package gregtech.client;
import gregtech.engine.mod.GregTechIdentity;

import gregtech.api.material.MaterialTextureSet;
import gregtech.engine.material.MaterialFormItem;
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
import net.minecraftforge.fml.common.Mod;
//?}
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

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
     * Register exactly the combinations a registered item asks for, so this list and the model pack agree.
     */
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        Set<ResourceLocation> registered = new HashSet<>();
        for (MaterialSharedModelIndex.ItemModel model : MaterialSharedModelIndex.items()) {
            ResourceLocation location = MaterialIcons.sharedModelLocation(model.set(), model.prefix());
            if (registered.add(location)) {
                ClientModelEvents.registerAdditional(event, location);
            }
        }
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        int aliased = 0;
        int missing = 0;

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
            models.put(itemId, "inventory", shared);
            aliased++;
        }

        LOGGER.info("[{}] Material model aliasing: {} mapped, {} missing shared model",
                GregTechIdentity.REGISTRY_NAMESPACE, aliased, missing);
    }

    private static BakedModel resolveSharedModel(BakedModelMap models, MaterialFormItem item) {
        MaterialTextureSet primary = MaterialIcons.resolveTextureSet(item.getMaterial());
        return ClientModelEvents.findInherited(
                models, primary, set -> MaterialIcons.sharedModelLocation(set, item.getPrefix()));
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
