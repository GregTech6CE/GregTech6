package gregtech.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
//?}

/** Model-event helpers shared by both nodes. */
public final class ClientModelEvents {
    private ClientModelEvents() {}

    /**
     * 1.20.1 accepts a {@link ResourceLocation}; its {@link ModelResourceLocation} is a subclass.
     * 1.21.1 requires the explicit standalone variant. The canonical constructor is valid on both.
     */
    public static void registerAdditional(ModelEvent.RegisterAdditional event, ResourceLocation id) {
//? if >=1.21.1 {
        /*event.register(new ModelResourceLocation(id, ModelResourceLocation.STANDALONE_VARIANT));
*///?} else {
        event.register(new ModelResourceLocation(id, ""));
//?}
    }
}
