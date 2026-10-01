package com.gregtech.gregtech.client;


import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.CreativeTabIconItem;
import com.gregtech.gregtech.registry.GTCreativeTabIcons;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CreativeTabIconClientModels {
    private static final MaterialTextureSet ICON_SET = MaterialTextureSet.NONE;

    private CreativeTabIconClientModels() {}

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BlockItemModelHelper.captureFromVanillaBlockItem(models);

        for (var entry : GTCreativeTabIcons.allEntries()) {
            if (!(entry.get() instanceof CreativeTabIconItem iconItem)) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.get());
            if (itemId == null) {
                continue;
            }
            BakedModel shared = resolveIconModel(models, iconItem);
            if (shared == null) {
                continue;
            }
            models.put(new ModelResourceLocation(itemId, "inventory"), shared);
        }
    }

    @Nullable
    private static BakedModel resolveIconModel(Map<ModelResourceLocation, BakedModel> models, CreativeTabIconItem iconItem) {
        MaterialPrefix materialPrefix = iconItem.materialPrefix();
        if (materialPrefix != null) {
            return lookupItemModel(models, materialPrefix);
        }
        BlockMaterialPrefix blockPrefix = iconItem.blockPrefix();
        if (blockPrefix != null) {
            return lookupBlockModel(models, blockPrefix);
        }
        return null;
    }

    private static BakedModel lookupItemModel(Map<ModelResourceLocation, BakedModel> models, MaterialPrefix prefix) {
        ResourceLocation id = MaterialIcons.sharedModelLocation(ICON_SET, prefix);
        return lookupModel(models, id);
    }

    private static BakedModel lookupBlockModel(Map<ModelResourceLocation, BakedModel> models, BlockMaterialPrefix prefix) {
        ResourceLocation id = BlockMaterialIcons.sharedModelLocation(ICON_SET, prefix);
        BakedModel shared = lookupModel(models, id);
        return shared == null ? null : BlockItemClientModels.asBlockItem(shared);
    }

    private static BakedModel lookupModel(Map<ModelResourceLocation, BakedModel> models, ResourceLocation id) {
        for(String variant:new String[]{"standalone","inventory","","normal"}){var model=models.get(new ModelResourceLocation(id,variant));if(model!=null)return model;}
        return null;
    }
}
