package gregtech.client;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Node-neutral view of the bakery model map. Call sites never declare the version-specific key type:
 * 1.20.1 keys its map by {@link ResourceLocation} and 1.21.1 by {@link ModelResourceLocation}.
 */
public final class BakedModelMap {
    /** Same key as both bakeries; keep lookups independent of the renderer bootstrap. */
    private static final ModelResourceLocation MISSING =
            new ModelResourceLocation(ResourceLocation.parse("builtin/missing"), "missing");

    private final Map<Object, BakedModel> models;

    @SuppressWarnings("unchecked")
    private BakedModelMap(Map<?, BakedModel> models) {
        this.models = (Map<Object, BakedModel>) models;
    }

    public static BakedModelMap of(Map<?, BakedModel> models) {
        return new BakedModelMap(models);
    }

    public BakedModel get(ResourceLocation id, String variant) {
        return models.get(new ModelResourceLocation(id, variant));
    }

    /** 1.20.1 keeps a plain {@link ResourceLocation} key for {@code RegisterAdditional} entries. */
    public BakedModel getPlain(ResourceLocation id) {
        return models.get(id);
    }

    /** Resolve a shared model without treating the bakery missing sentinel as success. */
    public BakedModel find(ResourceLocation id) {
        // 1.20.1 RegisterAdditional stores plain keys; prefer them before block/item variants.
        BakedModel plain = getPlain(id);
        if (isUsable(plain)) return plain;
        // "standalone" is the 1.21.1 RegisterAdditional variant; the other keys cover block/item models.
        for (String variant : new String[]{"standalone", "", "inventory", "normal"}) {
            BakedModel model = get(id, variant);
            if (isUsable(model)) return model;
        }
        return null;
    }

    public boolean isUsable(BakedModel model) {
        return model != null && model != models.get(MISSING);
    }

    public BakedModel missing() {
        return models.get(MISSING);
    }

    public void put(ResourceLocation id, String variant, BakedModel model) {
        models.put(new ModelResourceLocation(id, variant), model);
    }

    public void putState(ResourceLocation blockId, BlockState state, BakedModel model) {
        models.put(BlockModelShaper.stateToModelLocation(blockId, state), model);
    }

    public void replaceAll(BiFunction<ResourceLocation, BakedModel, BakedModel> replacer) {
        models.replaceAll((key, model) -> replacer.apply(modelId(key), model));
    }

//? if >=1.21.1 {
    /*private static ResourceLocation modelId(Object key) {
        return ((ModelResourceLocation) key).id();
    }
*///?} else {
    private static ResourceLocation modelId(Object key) {
        return (ResourceLocation) key;
    }
//?}
}
