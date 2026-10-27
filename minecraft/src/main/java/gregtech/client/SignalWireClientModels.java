package gregtech.client;

//? if >=1.21.1 {
/*import gregtech.engine.mod.GregTechIdentity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import gregtech.GregTech;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Map;

/** Registers {@link SignalWireGlowModel} on every signal wire model. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class SignalWireClientModels {
    private SignalWireClientModels() {}

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
//? if >=1.21.1 {
        /*Map<ModelResourceLocation, BakedModel> models = event.getModels();
*///?} else {
        Map<ResourceLocation, BakedModel> models = event.getModels();
//?}
        for (var key : new ArrayList<>(models.keySet())) {
            ResourceLocation id = modelId(key);
            if (id == null || !id.getNamespace().equals("gt6ce") || !id.getPath().startsWith("block/signal_wire/")) {
                continue;
            }
            models.put(key, new SignalWireGlowModel(models.get(key)));
        }
    }

//? if >=1.21.1 {
    /*private static ResourceLocation modelId(ModelResourceLocation key) {
        return key.id();
    }
*///?} else {
    private static ResourceLocation modelId(ResourceLocation key) {
        return key;
    }
//?}
}
