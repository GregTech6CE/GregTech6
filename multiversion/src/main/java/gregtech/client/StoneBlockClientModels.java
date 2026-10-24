package gregtech.client;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.block.stone.GTStoneBlock;
import gregtech.registry.GTBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
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
import org.slf4j.Logger;

import java.util.Map;

/** Ensures GT stone blocks, slabs, and block items resolve to {@code models/block/stones/...}. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class StoneBlockClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StoneBlockClientModels() {}

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

        for (var entry : GTBlocks.allEntries()) {
//? if >=1.21.1 {
            /*if (!entry.isBound()) {
*///?} else {
            if (!entry.isPresent()) {
//?}
                continue;
            }
            Block block = entry.get();
//? if >=1.21.1 {
            /*ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
*///?} else {
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
//?}
            if (blockId == null) {
                continue;
            }
            if (!(block instanceof GTStoneBlock stoneBlock)) {
                continue;
            }
            String modelPath = stoneBlock.modelId();

            BakedModel shared = lookupStoneModel(models, modelPath);
            if (shared == null) {
                missing++;
                continue;
            }
            BakedModel itemModel = BlockItemClientModels.asBlockItem(shared);
//? if >=1.21.1 {
            /*ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(block.asItem());
*///?} else {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
//?}
            if (itemId == null) {
                itemId = blockId;
            }
//? if >=1.21.1 {
            /*BlockItemClientModels.alias(models, itemId, ResourceLocation.fromNamespaceAndPath("gt6ce",modelPath), shared, itemModel);
*///?} else {
            BlockItemClientModels.alias(models, itemId, GregTech.id(modelPath), shared, itemModel);
//?}
            aliased++;
        }

//? if >=1.21.1 {
        /*LOGGER.info("[{}] Stone block model aliasing: {} mapped, {} missing", "gt6ce", aliased, missing);
*///?} else {
        LOGGER.info("[{}] Stone block model aliasing: {} mapped, {} missing", GregTech.NAMESPACE, aliased, missing);
//?}
    }

//? if >=1.21.1 {
    /*private static BakedModel lookupStoneModel(Map<ModelResourceLocation, BakedModel> models, String modelPath) {
        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath("gt6ce",modelPath);
        return BakedModelLookup.find(models, modelId);
    }

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        for (var entry : GTBlocks.allEntries()) {
            if (entry.isBound() && entry.get() instanceof GTStoneBlock stone) {
                event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("gt6ce", stone.modelId())));
*///?} else {
    private static BakedModel lookupStoneModel(Map<ResourceLocation, BakedModel> models, String modelPath) {
        ResourceLocation modelId = GregTech.id(modelPath);
        for (String variant : new String[]{"", "inventory", "normal"}) {
            BakedModel model = models.get(new ModelResourceLocation(modelId, variant));
            if (model != null) {
                return model;
//?}
            }
        }
//? if >=1.21.1 {
//?} else {
        return models.get(modelId);
//?}
    }
}
