package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.energy.GearboxBlock;
import com.gregtech.gregtech.registry.GTGearboxes;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Map;

/** Replaces static six-sided gearbox cubes with GT6's per-face axle/gear textures. */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GearboxClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private GearboxClientModels() {}

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        GearboxBakedModel shared = new GearboxBakedModel();
        BakedModel itemModel = BlockItemClientModels.asBlockItem(shared);
        int replaced = 0;
        for (var entry : GTGearboxes.allGearboxes()) {
            if (!entry.isPresent()) continue;
            GearboxBlock block = entry.get();
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            if (id == null) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                models.put(BlockModelShaper.stateToModelLocation(id, state), shared);
                replaced++;
            }
            BlockItemClientModels.aliasItemInventory(models, id, itemModel);
        }
        LOGGER.info("[{}] GT6 custom gearbox face models: {} blockstate variants replaced", GregTech.MODID, replaced);
    }
}
