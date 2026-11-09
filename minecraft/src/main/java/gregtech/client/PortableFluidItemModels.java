package gregtech.client;

import gregtech.content.item.PortableFluidContainerItem;
import net.minecraft.core.registries.BuiltInRegistries;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
//?}
public final class PortableFluidItemModels {
    private PortableFluidItemModels() {}

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
