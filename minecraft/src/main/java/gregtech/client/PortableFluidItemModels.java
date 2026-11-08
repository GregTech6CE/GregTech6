package gregtech.client;

import gregtech.content.item.PortableFluidContainerItem;
import net.minecraft.core.registries.BuiltInRegistries;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ModelEvent;
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class PortableFluidItemModels {
    private PortableFluidItemModels() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void models(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof PortableFluidContainerItem vessel)
                    || !PortableFluidItemModel.supports(vessel.spec().shapeId())) continue;
            var id = BuiltInRegistries.ITEM.getKey(item);
            var model = models.get(id, "inventory");
            if (model == null) continue;
            models.put(id, "inventory", new PortableFluidItemModel(model, vessel.spec().shapeId()));
        }
    }
}
