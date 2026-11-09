package gregtech.client;
import gregtech.engine.mod.GregTechIdentity;


import gregtech.engine.machine.CrucibleSpec;

//? if >=1.21.1 {
//?} else {
//?}
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.block.machine.SmeltingCrucibleBlock;
import gregtech.content.registry.GTSmeltery;
import gregtech.platform.Mc;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
//?}
import org.slf4j.Logger;

/** Crucibles: GT6 bowl hull via {@link CrucibleBlockBakedModel} + content BER top pass. */
public final class CrucibleClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CrucibleClientModels() {}

    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            ClientModelEvents.registerAdditional(event, CrucibleBowlIcons.sharedModelLocation(set));
        }
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        int aliased = 0;
        int missing = 0;

        for (var entry : GTSmeltery.crucibles()) {
            if (!entry.isBound()) {
                continue;
            }
            SmeltingCrucibleBlock block = entry.get();
            CrucibleSpec spec = block.spec();
            ResourceLocation blockId = Mc.blockId(block);
            if (blockId == null) {
                continue;
            }

            MaterialTextureSet textureSet = MaterialIcons.resolveTextureSet(spec.material());
            BakedModel shared = models.find(CrucibleBowlIcons.sharedModelLocation(textureSet));
            if (shared == null) {
                for (MaterialTextureSet fallback : MaterialTextureSet.modeled()) {
                    if (fallback == textureSet) {
                        continue;
                    }
                    shared = models.find(CrucibleBowlIcons.sharedModelLocation(fallback));
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
            BlockItemClientModels.alias(models, block, worldModel, itemModel);
            aliased++;
        }

        LOGGER.info("[{}] Crucible model aliasing: {} mapped, {} missing", GregTechIdentity.REGISTRY_NAMESPACE, aliased, missing);
    }
}
