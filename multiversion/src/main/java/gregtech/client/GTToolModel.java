package gregtech.client;

//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.registry.GTToolItems;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
//?}

import java.util.Map;

//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class GTToolModel {
    public static final ModelProperty<ItemStack> STACK = new ModelProperty<>();

    private GTToolModel() {}

    @SubscribeEvent
    public static void registerIconsetModels(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation icon : ToolIconSets.allRegisteredIcons()) {
//? if >=1.21.1 {
            /*event.register(ModelResourceLocation.standalone(iconsetModelId(icon)));
            event.register(ModelResourceLocation.standalone(iconsetOverlayModelId(icon)));
*///?} else {
            event.register(iconsetModelId(icon));
            event.register(iconsetOverlayModelId(icon));
//?}
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
//? if >=1.21.1 {
        /*Map<ModelResourceLocation, BakedModel> models = event.getModels();
        GTToolModelResolver.rebuild(models, models.get(ModelResourceLocation.standalone(net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_LOCATION)));
*///?} else {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        GTToolModelResolver.rebuild(models, models.get(net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_LOCATION));
//?}

        for (var entry : GTToolItems.all().entrySet()) {
//? if >=1.21.1 {
            /*ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.getValue().get());
*///?} else {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.getValue().get());
//?}
            if (itemId == null) {
                continue;
            }
//? if >=1.21.1 {
            /*ModelResourceLocation inventory = ModelResourceLocation.inventory(itemId);
*///?} else {
            ModelResourceLocation inventory = new ModelResourceLocation(itemId, "inventory");
//?}
            BakedModel base = models.get(inventory);
            if (base != null) {
                models.put(inventory, new GTToolBakedModel(base));
            }
        }
//? if >=1.21.1 {
        /*for(var holder:gregtech.registry.GTElectricItems.tools()) {
            var id=BuiltInRegistries.ITEM.getKey(holder.get());var inventory=ModelResourceLocation.inventory(id);
*///?} else {
        for(var holder:gregtech.registry.GTElectricItems.all()) {
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(holder.get());var inventory=new ModelResourceLocation(id,"inventory");
//?}
            var base=models.get(inventory);if(base!=null)models.put(inventory,new GTToolBakedModel(base));
        }
    }

    public static ModelData data(ItemStack stack) {
        return ModelData.builder().with(STACK, stack).build();
    }

    public static ResourceLocation iconsetModelId(ResourceLocation icon) {
//? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath("gt6ce","item/tool_iconset/" + ToolIconSets.baseName(icon));
*///?} else {
        return GregTech.id("item/tool_iconset/" + ToolIconSets.baseName(icon));
//?}
    }

    public static ResourceLocation iconsetOverlayModelId(ResourceLocation icon) {
//? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath("gt6ce","item/tool_iconset/" + ToolIconSets.baseName(icon) + "_overlay");
*///?} else {
        return GregTech.id("item/tool_iconset/" + ToolIconSets.baseName(icon) + "_overlay");
//?}
    }
}
