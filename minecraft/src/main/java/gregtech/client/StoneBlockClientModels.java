package gregtech.client;
import gregtech.engine.mod.GregTechIdentity;

import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.registry.GTBlocks;
import gregtech.platform.Mc;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.Block;
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

/** Ensures GT stone blocks, slabs, and block items resolve to {@code models/block/stones/...}. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class StoneBlockClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StoneBlockClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        for (var entry : GTBlocks.allEntries()) {
            if (entry.isPresent() && entry.get() instanceof GTStoneBlock stone) {
                ClientModelEvents.registerAdditional(event, Mc.gregtechId(stone.modelId()));
            }
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        int aliased = 0;
        int missing = 0;

        for (var entry : GTBlocks.allEntries()) {
            if (!entry.isPresent()) {
                continue;
            }
            Block block = entry.get();
            if (!(block instanceof GTStoneBlock stoneBlock)) {
                continue;
            }
            BakedModel shared = models.find(Mc.gregtechId(stoneBlock.modelId()));
            if (shared == null) {
                missing++;
                continue;
            }
            BakedModel itemModel = BlockItemClientModels.asBlockItem(shared);
            BlockItemClientModels.aliasBlockItem(models, block, itemModel);
            aliased++;
        }

        LOGGER.info("[{}] Stone block model aliasing: {} mapped, {} missing",
                GregTechIdentity.REGISTRY_NAMESPACE, aliased, missing);
    }
}
