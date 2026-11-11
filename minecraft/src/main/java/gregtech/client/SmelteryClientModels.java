package gregtech.client;
import gregtech.engine.mod.GregTechIdentity;


import gregtech.engine.machine.CrucibleSpec;

import gregtech.api.material.MaterialTextureSet;
import gregtech.content.block.machine.CrucibleCrossingBlock;
import gregtech.content.block.machine.CrucibleFaucetBlock;
import gregtech.content.block.machine.MoldBasinBlock;
import gregtech.content.block.machine.MoldBlock;
import gregtech.content.registry.GTSmeltery;
import gregtech.platform.Mc;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
//?}
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/** Mold basin / mold / crossing / faucet hull models (same texture-set sharing as smelting crucibles). */
public final class SmelteryClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SmelteryClientModels() {}

    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            ClientModelEvents.registerAdditional(event, MoldBasinIcons.sharedModelLocation(set));
            ClientModelEvents.registerAdditional(event, CrucibleMoldIcons.sharedModelLocation(set));
            ClientModelEvents.registerAdditional(event, CrucibleCrossingIcons.sharedModelLocation(set));
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                ClientModelEvents.registerAdditional(event, CrucibleFaucetIcons.sharedModelLocation(set, facing));
            }
        }
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        int basins = aliasHullBlocks(models, GTSmeltery.basins(), MoldBasinIcons::sharedModelLocation, false);
        int molds = aliasHullBlocks(models, GTSmeltery.molds(), CrucibleMoldIcons::sharedModelLocation, false);
        int crossings = aliasHullBlocks(models, GTSmeltery.crossings(), CrucibleCrossingIcons::sharedModelLocation, false);
        int faucets = aliasHullBlocks(models, GTSmeltery.faucets(), CrucibleFaucetIcons::sharedModelLocation, true);
        LOGGER.info("[{}] Smeltery hull aliasing: {} basins, {} molds, {} crossings, {} faucets",
                GregTechIdentity.REGISTRY_NAMESPACE, basins, molds, crossings, faucets);
    }

    private static <T extends Block> int aliasHullBlocks(
            BakedModelMap models,
            Iterable<? extends gregtech.content.registry.RegisteredBlock<T>> blocks,
            Function<MaterialTextureSet, ResourceLocation> sharedModel,
            boolean faucet) {
        int aliased = 0;
        for (var entry : blocks) {
            if (!entry.isPresent()) {
                continue;
            }
            T block = entry.get();
            CrucibleSpec spec = hullSpec(block);
            if (spec == null) {
                continue;
            }
            ResourceLocation blockId = Mc.blockId(block);
            if (blockId == null) {
                continue;
            }
            MaterialTextureSet textureSet = MaterialIcons.resolveTextureSet(spec.material());
            if (faucet) {
                ResourceLocation blockModelId = Mc.gregtechId("block/blocks/" + blockId.getPath());
                BakedModel itemModel = aliasFaucetBlock(models, blockId, blockModelId, textureSet);
                if (itemModel == null) {
                    continue;
                }
                BlockItemClientModels.aliasBlockItem(models, block, itemModel);
                aliased++;
                continue;
            }
            BakedModel shared = models.find(sharedModel.apply(textureSet));
            if (shared == null) {
                shared = fallbackModel(models, sharedModel, textureSet);
            }
            if (shared == null) {
                continue;
            }
            BakedModel worldModel = new CrucibleBlockBakedModel(shared);
            BakedModel itemModel = BlockItemClientModels.asBlockItem(shared);
            if (block instanceof MoldBlock) {
                BakedModel gridTemplate = models.find(CrucibleMoldIcons.sharedModelLocation(MaterialTextureSet.METALLIC));
                itemModel = new MoldItemBakedModel(itemModel,
                        MoldItemBakedModel.gridSprite(gridTemplate == null ? shared : gridTemplate));
            }
            BlockItemClientModels.alias(models, block, worldModel, itemModel);
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
            BakedModelMap models,
            Function<MaterialTextureSet, ResourceLocation> sharedModel,
            MaterialTextureSet textureSet) {
        for (MaterialTextureSet fallback : MaterialTextureSet.modeled()) {
            if (fallback == textureSet) {
                continue;
            }
            BakedModel model = models.find(sharedModel.apply(fallback));
            if (model != null) {
                return model;
            }
        }
        return null;
    }

    /** Faucet blockstates use {@code facing=*}; delegate to per-facing GT6 geometry at bake time. */
    @Nullable
    private static BakedModel aliasFaucetBlock(
            BakedModelMap models,
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

        models.put(blockModelId, "", worldModel);
        models.put(blockId, "", worldModel);
        models.put(blockId, "inventory", itemModel);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            String variant = "facing=" + facing.getSerializedName();
            models.put(blockId, variant, worldModel);
            models.put(blockModelId, variant, worldModel);
            models.put(blockId, variant + ",waterlogged=false", worldModel);
            models.put(blockId, variant + ",waterlogged=true", worldModel);
            models.put(blockModelId, variant + ",waterlogged=false", worldModel);
            models.put(blockModelId, variant + ",waterlogged=true", worldModel);
        }
        return itemModel;
    }

    @Nullable
    private static BakedModel lookupFacingModel(
            BakedModelMap models,
            MaterialTextureSet textureSet,
            Direction facing) {
        BakedModel model = models.find(CrucibleFaucetIcons.sharedModelLocation(textureSet, facing));
        if (model != null) {
            return model;
        }
        for (MaterialTextureSet fallback : MaterialTextureSet.modeled()) {
            if (fallback == textureSet) {
                continue;
            }
            model = models.find(CrucibleFaucetIcons.sharedModelLocation(fallback, facing));
            if (model != null) {
                return model;
            }
        }
        return null;
    }
}
