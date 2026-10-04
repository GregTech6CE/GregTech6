package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GTToolModel {
    public static final ModelProperty<ItemStack> STACK = new ModelProperty<>();

    private GTToolModel() {}

    @SubscribeEvent
    public static void registerIconsetModels(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation icon : ToolIconSets.allRegisteredIcons()) {
            event.register(iconsetModelId(icon));
            event.register(iconsetOverlayModelId(icon));
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        GTToolModelResolver.rebuild(models, models.get(net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_LOCATION));

        for (var entry : GTToolItems.all().entrySet()) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.getValue().get());
            if (itemId == null) {
                continue;
            }
            ModelResourceLocation inventory = new ModelResourceLocation(itemId, "inventory");
            BakedModel base = models.get(inventory);
            if (base != null) {
                models.put(inventory, new GTToolBakedModel(base));
            }
        }
        for(var holder:com.gregtech.gregtech.registry.GTElectricItems.all()) {
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(holder.get());var inventory=new ModelResourceLocation(id,"inventory");
            var base=models.get(inventory);if(base!=null)models.put(inventory,new GTToolBakedModel(base));
        }
    }

    public static ModelData data(ItemStack stack) {
        return ModelData.builder().with(STACK, stack).build();
    }

    public static ResourceLocation iconsetModelId(ResourceLocation icon) {
        return GregTech.id("item/tool_iconset/" + ToolIconSets.baseName(icon));
    }

    public static ResourceLocation iconsetOverlayModelId(ResourceLocation icon) {
        return GregTech.id("item/tool_iconset/" + ToolIconSets.baseName(icon) + "_overlay");
    }
}
