package gregtech.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

/** ModelResourceLocation construction across the two nodes. */
public final class ModelResourceLocationCompat {
    private ModelResourceLocationCompat() {}

    /** A standalone model id with no variant. */
    public static ModelResourceLocation standalone(ResourceLocation id) {
//? if >=1.21.1 {
/*        return ModelResourceLocation.standalone(id);
*///?} else {
        return new ModelResourceLocation(id, "");
//?}
    }

    /** The inventory variant of a model id. */
    public static ModelResourceLocation inventory(ResourceLocation id) {
//? if >=1.21.1 {
/*        return ModelResourceLocation.inventory(id);
*///?} else {
        return new ModelResourceLocation(id, "inventory");
//?}
    }
}
