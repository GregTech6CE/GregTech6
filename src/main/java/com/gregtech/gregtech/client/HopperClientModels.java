package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;

/**
 * Hopper / queuehopper shared model registration and bake-time aliasing.
 * Follows the same pattern as {@link MachineBlockClientModels} for burning boxes.
 */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HopperClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] HOPPER_TYPES = {"hopper", "queuehopper"};

    private HopperClientModels() {}

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        for (String type : HOPPER_TYPES) {
            for (Direction facing : Direction.values()) {
                event.register(HopperIcons.sharedModelLocation(type, facing));
            }
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();

        int hoppers = aliasHoppers(models, MachineRegistry.hoppers(), "hopper");
        int queueHoppers = aliasHoppers(models, MachineRegistry.queueHoppers(), "queuehopper");
        LOGGER.info("[{}] Hopper model aliasing: {} hoppers, {} queuehoppers",
                GregTech.MODID, hoppers, queueHoppers);
    }

    private static <T extends Block> int aliasHoppers(
            Map<ResourceLocation, BakedModel> models,
            Iterable<net.minecraftforge.registries.RegistryObject<T>> blocks,
            String texPath) {
        // Collect per-facing shared models
        Map<Direction, BakedModel> byFacing = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.values()) {
            BakedModel facingModel = lookupFacingModel(models, texPath, facing);
            if (facingModel != null) {
                byFacing.put(facing, facingModel);
            }
        }
        if (byFacing.isEmpty()) {
            LOGGER.warn("[{}] No per-facing models found for hopper type '{}'", GregTech.MODID, texPath);
            return 0;
        }
        if (byFacing.size() < 6) {
            LOGGER.warn("[{}] Only {}/6 per-facing models found for '{}'", GregTech.MODID, byFacing.size(), texPath);
            return 0;
        }

        BakedModel itemInner = byFacing.get(Direction.DOWN);
        BakedModel worldModel = new HopperBakedModel(byFacing, itemInner);
        BakedModel itemModel = BlockItemClientModels.asBlockItem(itemInner);

        int aliased = 0;
        for (var entry : blocks) {
            if (!entry.isPresent()) continue;
            Block block = entry.get();
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
            if (blockId == null) continue;

            // Alias base model
            models.put(new ModelResourceLocation(blockId, ""), worldModel);
            // Alias all 6 facing variants
            for (Direction facing : Direction.values()) {
                String variant = "facing=" + facing.getSerializedName();
                models.put(new ModelResourceLocation(blockId, variant), worldModel);
            }
            // Alias item inventory
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
            if (itemId == null) {
                itemId = blockId;
            }
            BlockItemClientModels.aliasItemInventory(models, itemId, itemModel);
            aliased++;
        }
        return aliased;
    }

    @Nullable
    private static BakedModel lookupFacingModel(
            Map<ResourceLocation, BakedModel> models,
            String type, Direction facing) {
        ResourceLocation modelId = HopperIcons.sharedModelLocation(type, facing);
        for (String variant : new String[]{"", "inventory", "normal"}) {
            BakedModel model = models.get(new ModelResourceLocation(modelId, variant));
            if (model != null) {
                return model;
            }
        }
        return models.get(modelId);
    }
}
