package gregtech.client;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.engine.machine.CrucibleSpec;

//? if >=1.21.1 {
//?} else {
import gregtech.engine.machine.MachineRegistry;
//?}
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.block.machine.SmeltingCrucibleBlock;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
//?}
import org.slf4j.Logger;

import java.util.Map;

/** Crucibles: GT6 bowl hull via {@link CrucibleBlockBakedModel} + content BER top pass. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class CrucibleClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CrucibleClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
//? if >=1.21.1 {
            /*event.register(ModelResourceLocation.standalone(CrucibleBowlIcons.sharedModelLocation(set)));
*///?} else {
            event.register(CrucibleBowlIcons.sharedModelLocation(set));
//?}
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
        /*for (var entry : gregtech.platform.neoforge.smeltery.SmelteryRegistries.crucibles()) {
            if (!entry.isBound()) {
*///?} else {
        for (var entry : MachineRegistry.smeltingCrucibles()) {
            if (!entry.isPresent()) {
//?}
                continue;
            }
            SmeltingCrucibleBlock block = entry.get();
            CrucibleSpec spec = block.spec();
//? if >=1.21.1 {
            /*ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
*///?} else {
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
//?}
            if (blockId == null) {
                continue;
            }

            MaterialTextureSet textureSet = MaterialIcons.resolveTextureSet(spec.material());
            BakedModel shared = lookupModel(models, CrucibleBowlIcons.sharedModelLocation(textureSet));
            if (shared == null) {
                for (MaterialTextureSet fallback : MaterialTextureSet.modeled()) {
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
//? if >=1.21.1 {
            /*ResourceLocation blockModelId = ResourceLocation.fromNamespaceAndPath("gt6ce","block/blocks/" + blockId.getPath());
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(block.asItem());
*///?} else {
            ResourceLocation blockModelId = GregTech.id("block/blocks/" + blockId.getPath());
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
//?}
            if (itemId == null) {
                itemId = blockId;
            }
            BlockItemClientModels.alias(models, blockId, blockModelId, worldModel, itemModel);
            BlockItemClientModels.aliasItemInventory(models, itemId, itemModel);
            aliased++;
        }

//? if >=1.21.1 {
        /*LOGGER.info("[{}] Crucible model aliasing: {} mapped, {} missing", "gt6ce", aliased, missing);
*///?} else {
        LOGGER.info("[{}] Crucible model aliasing: {} mapped, {} missing", GregTech.NAMESPACE, aliased, missing);
//?}
    }

//? if >=1.21.1 {
    /*private static BakedModel lookupModel(Map<ModelResourceLocation, BakedModel> models, ResourceLocation modelId) {
        return BakedModelLookup.find(models, modelId);
*///?} else {
    private static BakedModel lookupModel(Map<ResourceLocation, BakedModel> models, ResourceLocation modelId) {
        for (String variant : new String[]{"", "inventory", "normal"}) {
            BakedModel model = models.get(new ModelResourceLocation(modelId, variant));
            if (model != null) {
                return model;
            }
        }
        return models.get(modelId);
//?}
    }
}
