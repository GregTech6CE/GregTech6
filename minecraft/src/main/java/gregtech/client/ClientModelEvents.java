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
     * 1.20.1 takes a plain {@link ResourceLocation} and loads it as a model; a
     * {@link ModelResourceLocation} with a non-{@code inventory} variant is interpreted as a
     * blockstate definition. 1.21.1 requires the explicit standalone variant.
     */
    public static void registerAdditional(ModelEvent.RegisterAdditional event, ResourceLocation id) {
//? if >=1.21.1 {
        /*event.register(new ModelResourceLocation(id, ModelResourceLocation.STANDALONE_VARIANT));
*///?} else {
        event.register(id);
//?}
    }
}
