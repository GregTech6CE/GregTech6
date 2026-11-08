package gregtech.client;
import gregtech.engine.mod.GregTechIdentity;


import gregtech.engine.machine.CrucibleSpec;

//? if >=1.21.1 {
//?} else {
import gregtech.engine.machine.MachineRegistry;
//?}
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.block.machine.SmeltingCrucibleBlock;
import gregtech.platform.Mc;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}
import org.slf4j.Logger;

/** Crucibles: GT6 bowl hull via {@link CrucibleBlockBakedModel} + content BER top pass. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class CrucibleClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CrucibleClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            ClientModelEvents.registerAdditional(event, CrucibleBowlIcons.sharedModelLocation(set));
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
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
