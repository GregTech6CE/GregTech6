package gregtech.client;

import gregtech.content.registry.GTToolItems;
import gregtech.platform.Mc;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraft.core.registries.BuiltInRegistries;
//?}

public final class GTToolModel {
    public static final ModelProperty<ItemStack> STACK = new ModelProperty<>();

    private GTToolModel() {}

    public static void registerIconsetModels(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation icon : ToolIconSets.allRegisteredIcons()) {
            ClientModelEvents.registerAdditional(event, iconsetModelId(icon));
            ClientModelEvents.registerAdditional(event, iconsetOverlayModelId(icon));
        }
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());

        for (var entry : GTToolItems.all().entrySet()) {
            ResourceLocation itemId = Mc.itemId(entry.getValue().get());
            if (itemId == null) {
                continue;
            }
            BakedModel base = models.get(itemId, "inventory");
            if (base != null) {
                models.put(itemId, "inventory", new GTToolBakedModel(base));
            }
        }
//? if >=1.21.1 {
        /*for(var holder:gregtech.content.registry.GTElectricItems.tools()) {
            var id=BuiltInRegistries.ITEM.getKey(holder.get());
*///?} else {
        for(var holder:gregtech.content.registry.GTElectricItems.all()) {
            var id=BuiltInRegistries.ITEM.getKey(holder.get());
//?}
            var base=models.get(id, "inventory");
            if(base!=null)models.put(id, "inventory", new GTToolBakedModel(base));
        }
    }

    public static ModelData data(ItemStack stack) {
        return ModelData.builder().with(STACK, stack).build();
    }

    public static ResourceLocation iconsetModelId(ResourceLocation icon) {
        return Mc.gregtechId("item/tool_iconset/" + ToolIconSets.baseName(icon));
    }

    public static ResourceLocation iconsetOverlayModelId(ResourceLocation icon) {
        return Mc.gregtechId("item/tool_iconset/" + ToolIconSets.baseName(icon) + "_overlay");
    }
}
