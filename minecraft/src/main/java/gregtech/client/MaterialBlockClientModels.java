package gregtech.client;
import gregtech.engine.mod.GregTechIdentity;

import gregtech.api.material.MaterialTextureSet;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.registry.GTBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
//?}
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class MaterialBlockClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MaterialBlockClientModels() {}

    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        BlockPrefixRegistry.ensurePrefixesLoaded();
        Set<ResourceLocation> registered = new HashSet<>();
        for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
            if (!prefix.isAddon()) continue;
            register(event, registered, BlockMaterialIcons.addonModel(prefix));
        }
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
                if (prefix.isAddon()) continue;
                // World ores render through OreBakedModel; only NONE keeps the tab-icon templates.
                if ((prefix == BlockMaterialPrefix.ore || prefix == BlockMaterialPrefix.oreSmall)
                        && set != MaterialTextureSet.NONE) continue;
                register(event, registered, BlockMaterialIcons.sharedModelLocation(set, prefix));
            }
        }
    }

    private static void register(ModelEvent.RegisterAdditional event, Set<ResourceLocation> registered,
                                 ResourceLocation location) {
        if (registered.add(location)) {
            ClientModelEvents.registerAdditional(event, location);
        }
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        int aliased = 0;
        int missing = 0;
        Map<BakedModel, BakedModel> tintedModels = new IdentityHashMap<>();

        for (var entry : GTBlocks.allEntries()) {
            if (!entry.isPresent() || !(entry.get() instanceof MaterialBlockLike materialBlock)) {
                continue;
            }
            // Ore blocks render via OreBakedModel (NBT-driven stone background), see OreClientModels.
            if (entry.get() instanceof gregtech.content.block.OreBlock) {
                continue;
            }
            BakedModel shared = resolveSharedModel(models, materialBlock);
            if (shared == null) {
                missing++;
                continue;
            }
            BakedModel tintedShared = tintedModels.computeIfAbsent(shared, MaterialBlockBakedModel::new);
            BakedModel itemModel = BlockItemClientModels.asBlockItem(tintedShared);
            BlockItemClientModels.alias(models, entry.get(), tintedShared, itemModel);
            aliased++;
        }

        LOGGER.info("[{}] Material block model aliasing: {} mapped, {} missing",
                GregTechIdentity.REGISTRY_NAMESPACE, aliased, missing);
    }

    private static BakedModel resolveSharedModel(BakedModelMap models, MaterialBlockLike materialBlock) {
        if (materialBlock.prefix().isAddon()) {
            return models.find(BlockMaterialIcons.addonModel(materialBlock.prefix()));
        }
        MaterialTextureSet primary = MaterialIcons.resolveTextureSet(materialBlock.material());
        BakedModel model = models.find(BlockMaterialIcons.sharedModelLocation(primary, materialBlock.prefix()));
        if (model != null) {
            return model;
        }
        for (MaterialTextureSet fallback : MaterialTextureSet.modeled()) {
            if (fallback == primary) {
                continue;
            }
            model = models.find(BlockMaterialIcons.sharedModelLocation(fallback, materialBlock.prefix()));
            if (model != null) {
                return model;
            }
        }
        return null;
    }
}
