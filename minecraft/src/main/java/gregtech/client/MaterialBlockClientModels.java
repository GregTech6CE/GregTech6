package gregtech.client;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.api.material.MaterialTextureSet;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.registry.GTBlocks;
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

//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class MaterialBlockClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MaterialBlockClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        BlockPrefixRegistry.ensurePrefixesLoaded();
        java.util.Set<ResourceLocation> registered = new java.util.HashSet<>();
        for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
            if (!prefix.isAddon()) continue;
            ResourceLocation location = BlockMaterialIcons.addonModel(prefix);
//? if >=1.21.1 {
            /*if (registered.add(location)) event.register(ModelResourceLocation.standalone(location));
*///?} else {
            if (registered.add(location)) event.register(location);
//?}
        }
        for (MaterialTextureSet set : MaterialTextureSet.MODELED) {
            for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
                if (prefix.isAddon()) continue;
//? if >=1.21.1 {
                /*// Ores are baked from the stone template by OreClientModels, not a material cube.
                if (prefix == BlockMaterialPrefix.ore || prefix == BlockMaterialPrefix.oreSmall) continue;
*///?} else {
                // World ores use OreClientModels; only NONE has the actual tab-icon templates.
                if ((prefix == BlockMaterialPrefix.ore || prefix == BlockMaterialPrefix.oreSmall)
                        && set != MaterialTextureSet.NONE) continue;
//?}
                ResourceLocation location = BlockMaterialIcons.sharedModelLocation(set, prefix);
//? if >=1.21.1 {
                /*if (registered.add(location)) event.register(ModelResourceLocation.standalone(location));
*///?} else {
                if (registered.add(location)) event.register(location);
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
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        int aliased = 0;
        int missing = 0;
        Map<BakedModel, BakedModel> tintedModels = new java.util.IdentityHashMap<>();

        for (var entry : GTBlocks.allEntries()) {
//? if >=1.21.1 {
            /*if (!entry.isBound() || !(entry.get() instanceof MaterialBlockLike materialBlock)) {
*///?} else {
            if (!entry.isPresent() || !(entry.get() instanceof MaterialBlockLike materialBlock)) {
//?}
                continue;
            }
            // Ore blocks render via OreBakedModel (NBT-driven stone background), see OreClientModels.
            if (entry.get() instanceof gregtech.content.block.OreBlock) {
                continue;
            }
//? if >=1.21.1 {
            /*ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(entry.get());
*///?} else {
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(entry.get());
//?}
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
//? if >=1.21.1 {
            /*ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.get().asItem());
*///?} else {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.get().asItem());
//?}
            if (itemId == null) {
                itemId = blockId;
            }
            BlockItemClientModels.alias(models, itemId,
//? if >=1.21.1 {
                    /*ResourceLocation.fromNamespaceAndPath("gt6ce","block/blocks/" + blockId.getPath()), worldModel, itemModel);
*///?} else {
                    GregTech.id("block/blocks/" + blockId.getPath()), worldModel, itemModel);
//?}
            for (var state : entry.get().getStateDefinition().getPossibleStates())
                models.put(net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(blockId, state), worldModel);
            aliased++;
        }

//? if >=1.21.1 {
        /*LOGGER.info("[{}] Material block model aliasing: {} mapped, {} missing", "gt6ce", aliased, missing);
*///?} else {
        LOGGER.info("[{}] Material block model aliasing: {} mapped, {} missing", GregTech.NAMESPACE, aliased, missing);
//?}
    }

//? if >=1.21.1 {
    /*private static BakedModel resolveSharedModel(Map<ModelResourceLocation, BakedModel> models, MaterialBlockLike materialBlock) {
*///?} else {
    private static BakedModel resolveSharedModel(Map<ResourceLocation, BakedModel> models, MaterialBlockLike materialBlock) {
//?}
        if (materialBlock.prefix().isAddon())
            return BakedModelLookup.find(models, BlockMaterialIcons.addonModel(materialBlock.prefix()));
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

//? if >=1.21.1 {
    /*private static BakedModel lookupModel(Map<ModelResourceLocation, BakedModel> models,
*///?} else {
    private static BakedModel lookupModel(Map<ResourceLocation, BakedModel> models,
//?}
                                          MaterialTextureSet set,
                                          BlockMaterialPrefix prefix) {
        ResourceLocation id = BlockMaterialIcons.sharedModelLocation(set, prefix);
        return BakedModelLookup.find(models, id);
    }
}
