package gregtech.client;

import gregtech.api.material.MaterialTextureSet;
import java.util.function.Function;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
//?}
import org.jetbrains.annotations.Nullable;

/** Model-event helpers shared by both nodes. */
public final class ClientModelEvents {
    /** Roots of the icon inheritance tree, tried last so a {@code STONE} set can still borrow {@code DULL}. */
    private static final MaterialTextureSet[] ROOT_FALLBACKS = {MaterialTextureSet.DULL, MaterialTextureSet.STONE};

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

    /**
     * Baked model for {@code primary}, walking {@link MaterialTextureSet#parent()} until one of the
     * shared models was actually baked. The two roots are retried last so a set whose own art is
     * missing still renders.
     */
    @Nullable
    public static BakedModel findInherited(
            BakedModelMap models, MaterialTextureSet primary, Function<MaterialTextureSet, ResourceLocation> location) {
        return findInherited(primary, location, models::find);
    }

    /** Same walk over any model source, such as the tool model cache. */
    @Nullable
    public static BakedModel findInherited(
            MaterialTextureSet primary,
            Function<MaterialTextureSet, ResourceLocation> location,
            Function<ResourceLocation, BakedModel> lookup) {
        for (MaterialTextureSet current = primary; current != null; current = current.parent()) {
            BakedModel model = lookup.apply(location.apply(current));
            if (model != null) return model;
        }
        for (MaterialTextureSet root : ROOT_FALLBACKS) {
            BakedModel model = lookup.apply(location.apply(root));
            if (model != null) return model;
        }
        return null;
    }
}
