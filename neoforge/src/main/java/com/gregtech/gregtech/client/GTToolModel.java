package com.gregtech.gregtech.client;

import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Map;

@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GTToolModel {
    public static final ModelProperty<ItemStack> STACK = new ModelProperty<>();

    private GTToolModel() {}

    @SubscribeEvent
    public static void registerIconsetModels(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation icon : ToolIconSets.allRegisteredIcons()) {
            event.register(ModelResourceLocation.standalone(iconsetModelId(icon)));
            event.register(ModelResourceLocation.standalone(iconsetOverlayModelId(icon)));
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        GTToolModelResolver.rebuild(models, models.get(ModelResourceLocation.standalone(net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_LOCATION)));

        for (var entry : GTToolItems.all().entrySet()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.getValue().get());
            if (itemId == null) {
                continue;
            }
            ModelResourceLocation inventory = ModelResourceLocation.inventory(itemId);
            BakedModel base = models.get(inventory);
            if (base != null) {
                models.put(inventory, new GTToolBakedModel(base));
            }
        }
    }

    public static ModelData data(ItemStack stack) {
        return ModelData.builder().with(STACK, stack).build();
    }

    public static ResourceLocation iconsetModelId(ResourceLocation icon) {
        return ResourceLocation.fromNamespaceAndPath("gregtech","item/tool_iconset/" + ToolIconSets.baseName(icon));
    }

    public static ResourceLocation iconsetOverlayModelId(ResourceLocation icon) {
        return ResourceLocation.fromNamespaceAndPath("gregtech","item/tool_iconset/" + ToolIconSets.baseName(icon) + "_overlay");
    }
}
