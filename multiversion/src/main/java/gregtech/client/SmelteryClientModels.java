package gregtech.client;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.api.machine.CrucibleSpec;

//? if >=1.21.1 {
//?} else {
import gregtech.api.machine.MachineRegistry;
//?}
import gregtech.api.material.MaterialTextureSet;
import gregtech.block.machine.CrucibleCrossingBlock;
import gregtech.block.machine.CrucibleFaucetBlock;
import gregtech.block.machine.MoldBasinBlock;
import gregtech.block.machine.MoldBlock;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/** Mold basin / mold / crossing / faucet hull models (same texture-set sharing as smelting crucibles). */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class SmelteryClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SmelteryClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        for (MaterialTextureSet set : MaterialTextureSet.MODELED) {
//? if >=1.21.1 {
            /*event.register(ModelResourceLocation.standalone(MoldBasinIcons.sharedModelLocation(set)));
            event.register(ModelResourceLocation.standalone(CrucibleMoldIcons.sharedModelLocation(set)));
            event.register(ModelResourceLocation.standalone(CrucibleCrossingIcons.sharedModelLocation(set)));
*///?} else {
            event.register(MoldBasinIcons.sharedModelLocation(set));
            event.register(CrucibleMoldIcons.sharedModelLocation(set));
            event.register(CrucibleCrossingIcons.sharedModelLocation(set));
//?}
            for (Direction facing : Direction.Plane.HORIZONTAL) {
//? if >=1.21.1 {
                /*event.register(ModelResourceLocation.standalone(CrucibleFaucetIcons.sharedModelLocation(set, facing)));
*///?} else {
                event.register(CrucibleFaucetIcons.sharedModelLocation(set, facing));
//?}
            }
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
//? if >=1.21.1 {
        /*Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        int basins = aliasHullBlocks(models, gregtech.platform.neoforge.smeltery.SmelteryRegistries.basins(), MoldBasinIcons::sharedModelLocation, false);
        int molds = aliasHullBlocks(models, gregtech.platform.neoforge.smeltery.SmelteryRegistries.molds(), CrucibleMoldIcons::sharedModelLocation, false);
        int crossings = aliasHullBlocks(models, gregtech.platform.neoforge.smeltery.SmelteryRegistries.crossings(), CrucibleCrossingIcons::sharedModelLocation, false);
        int faucets = aliasHullBlocks(models, gregtech.platform.neoforge.smeltery.SmelteryRegistries.faucets(), CrucibleFaucetIcons::sharedModelLocation, true);
*///?} else {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        int basins = aliasHullBlocks(models, MachineRegistry.moldBasins(), MoldBasinIcons::sharedModelLocation, false);
        int molds = aliasHullBlocks(models, MachineRegistry.molds(), CrucibleMoldIcons::sharedModelLocation, false);
        int crossings = aliasHullBlocks(models, MachineRegistry.crucibleCrossings(), CrucibleCrossingIcons::sharedModelLocation, false);
        int faucets = aliasHullBlocks(models, MachineRegistry.crucibleFaucets(), CrucibleFaucetIcons::sharedModelLocation, true);
//?}
        LOGGER.info("[{}] Smeltery hull aliasing: {} basins, {} molds, {} crossings, {} faucets",
//? if >=1.21.1 {
                /*"gt6ce", basins, molds, crossings, faucets);
*///?} else {
                GregTech.NAMESPACE, basins, molds, crossings, faucets);
//?}
    }

    private static <T extends Block> int aliasHullBlocks(
//? if >=1.21.1 {
            /*Map<ModelResourceLocation, BakedModel> models,
            Iterable<? extends net.neoforged.neoforge.registries.DeferredHolder<Block,T>> blocks,
*///?} else {
            Map<ResourceLocation, BakedModel> models,
            Iterable<net.minecraftforge.registries.RegistryObject<T>> blocks,
//?}
            Function<MaterialTextureSet, ResourceLocation> sharedModel,
            boolean faucet) {
        int aliased = 0;
        for (var entry : blocks) {
//? if >=1.21.1 {
            /*if (!entry.isBound()) {
*///?} else {
            if (!entry.isPresent()) {
//?}
                continue;
            }
            T block = entry.get();
            CrucibleSpec spec = hullSpec(block);
            if (spec == null) {
                continue;
            }
//? if >=1.21.1 {
            /*ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
*///?} else {
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
//?}
            if (blockId == null) {
                continue;
            }
            MaterialTextureSet textureSet = MaterialIcons.resolveTextureSet(spec.material());
//? if >=1.21.1 {
            /*ResourceLocation blockModelId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","block/blocks/" + blockId.getPath());
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(block.asItem());
*///?} else {
            ResourceLocation blockModelId = GregTech.id("block/blocks/" + blockId.getPath());
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
//?}
            if (itemId == null) {
                itemId = blockId;
            }
            if (faucet) {
                BakedModel itemModel = aliasFaucetBlock(models, blockId, blockModelId, textureSet);
                if (itemModel == null) {
                    continue;
                }
                BlockItemClientModels.aliasItemInventory(models, itemId, itemModel);
                aliased++;
                continue;
            }
            BakedModel shared = lookupModel(models, sharedModel.apply(textureSet));
            if (shared == null) {
                shared = fallbackModel(models, sharedModel, textureSet);
            }
            if (shared == null) {
                continue;
            }
            BakedModel worldModel = new CrucibleBlockBakedModel(shared);
            BakedModel itemModel = BlockItemClientModels.asBlockItem(shared);
            if (block instanceof MoldBlock) {
                BakedModel gridTemplate = lookupModel(models, CrucibleMoldIcons.sharedModelLocation(MaterialTextureSet.METALLIC));
                itemModel = new MoldItemBakedModel(itemModel,
                        MoldItemBakedModel.gridSprite(gridTemplate == null ? shared : gridTemplate));
            }
            BlockItemClientModels.alias(models, blockId, blockModelId, worldModel, itemModel);
            BlockItemClientModels.aliasItemInventory(models, itemId, itemModel);
            aliased++;
        }
        return aliased;
    }

    @Nullable
    private static CrucibleSpec hullSpec(Block block) {
        if (block instanceof MoldBasinBlock basin) {
            return basin.spec();
        }
        if (block instanceof MoldBlock mold) {
            return mold.spec();
        }
        if (block instanceof CrucibleCrossingBlock crossing) {
            return crossing.spec();
        }
        if (block instanceof CrucibleFaucetBlock faucet) {
            return faucet.spec();
        }
        return null;
    }

    private static BakedModel fallbackModel(
//? if >=1.21.1 {
            /*Map<ModelResourceLocation, BakedModel> models,
*///?} else {
            Map<ResourceLocation, BakedModel> models,
//?}
            Function<MaterialTextureSet, ResourceLocation> sharedModel,
            MaterialTextureSet textureSet) {
        for (MaterialTextureSet fallback : MaterialTextureSet.MODELED) {
            if (fallback == textureSet) {
                continue;
            }
            BakedModel model = lookupModel(models, sharedModel.apply(fallback));
            if (model != null) {
                return model;
            }
        }
        return null;
    }

//? if >=1.21.1 {
    /*private static BakedModel lookupModel(Map<ModelResourceLocation, BakedModel> models, ResourceLocation modelId) {
*///?} else {
    private static BakedModel lookupModel(Map<ResourceLocation, BakedModel> models, ResourceLocation modelId) {
//?}
        for (String variant : new String[]{"", "inventory", "normal"}) {
            BakedModel model = models.get(new ModelResourceLocation(modelId, variant));
            if (model != null) {
                return model;
            }
        }
//? if >=1.21.1 {
        /*return models.get(ModelResourceLocation.standalone(modelId));
*///?} else {
        return models.get(modelId);
//?}
    }

    /** Faucet blockstates use {@code facing=*}; delegate to per-facing GT6 geometry at bake time. */
    @Nullable
    private static BakedModel aliasFaucetBlock(
//? if >=1.21.1 {
            /*Map<ModelResourceLocation, BakedModel> models,
*///?} else {
            Map<ResourceLocation, BakedModel> models,
//?}
            ResourceLocation blockId,
            ResourceLocation blockModelId,
            MaterialTextureSet textureSet) {
        Map<Direction, BakedModel> byFacing = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BakedModel facingModel = lookupFacingModel(models, textureSet, facing);
            if (facingModel != null) {
                byFacing.put(facing, facingModel);
            }
        }
        if (byFacing.isEmpty()) {
            return null;
        }
        BakedModel itemInner = byFacing.getOrDefault(Direction.SOUTH, byFacing.values().iterator().next());
        BakedModel itemModel = BlockItemClientModels.asBlockItem(itemInner);
        BakedModel worldModel = new CrucibleFaucetBakedModel(byFacing, itemInner);

        models.put(new ModelResourceLocation(blockModelId, ""), worldModel);
        models.put(new ModelResourceLocation(blockId, ""), worldModel);
        models.put(new ModelResourceLocation(blockId, "inventory"), itemModel);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            String variant = "facing=" + facing.getSerializedName();
            models.put(new ModelResourceLocation(blockId, variant), worldModel);
            models.put(new ModelResourceLocation(blockModelId, variant), worldModel);
            models.put(new ModelResourceLocation(blockId, variant + ",waterlogged=false"), worldModel);
            models.put(new ModelResourceLocation(blockId, variant + ",waterlogged=true"), worldModel);
            models.put(new ModelResourceLocation(blockModelId, variant + ",waterlogged=false"), worldModel);
            models.put(new ModelResourceLocation(blockModelId, variant + ",waterlogged=true"), worldModel);
        }
        return itemModel;
    }

    @Nullable
    private static BakedModel lookupFacingModel(
//? if >=1.21.1 {
            /*Map<ModelResourceLocation, BakedModel> models,
*///?} else {
            Map<ResourceLocation, BakedModel> models,
//?}
            MaterialTextureSet textureSet,
            Direction facing) {
        BakedModel model = lookupModel(models, CrucibleFaucetIcons.sharedModelLocation(textureSet, facing));
        if (model != null) {
            return model;
        }
        for (MaterialTextureSet fallback : MaterialTextureSet.MODELED) {
            if (fallback == textureSet) {
                continue;
            }
            model = lookupModel(models, CrucibleFaucetIcons.sharedModelLocation(fallback, facing));
            if (model != null) {
                return model;
            }
        }
        return null;
    }
}
