package gregtech.client;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.model.BakedModelWrapper;
*///?} else {
import net.minecraftforge.client.model.BakedModelWrapper;
//?}
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/** Block-item display transforms (inventory / first-person / third-person). */
public final class BlockItemModelHelper {
    /** The eight vanilla display contexts, which {@link ItemTransforms} keeps as plain fields. */
    private static final ItemDisplayContext[] VANILLA_CONTEXTS = {
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            ItemDisplayContext.HEAD,
            ItemDisplayContext.GUI,
            ItemDisplayContext.GROUND,
            ItemDisplayContext.FIXED
    };

    private static volatile ItemTransforms BLOCK_ITEM_TRANSFORMS = ItemTransforms.NO_TRANSFORMS;

    private BlockItemModelHelper() {}

    public static void captureFromVanillaBlockItem(BakedModelMap models) {
        // Every bake (including F3+T / resource-pack changes) owns new transforms.
        BLOCK_ITEM_TRANSFORMS = ItemTransforms.NO_TRANSFORMS;
        for (ResourceLocation id : new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath("minecraft", "iron_block"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "stone"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "dirt")
        }) {
            BakedModel model = models.get(id, "inventory");
            if (models.isUsable(model) && model.isGui3d() && model.getTransforms() != ItemTransforms.NO_TRANSFORMS) {
                BLOCK_ITEM_TRANSFORMS = model.getTransforms();
                return;
            }
        }
    }

    public static ItemTransforms blockItemTransforms() {
        return BLOCK_ITEM_TRANSFORMS;
    }

    /**
     * True when {@code transforms} displays nothing in any context. The {@code NO_TRANSFORMS}
     * identity is not a usable test: {@code BlockModel} hands out an all-{@code NO_TRANSFORM}
     * instance whenever its JSON has no {@code display} section.
     */
    public static boolean isEmpty(@Nullable ItemTransforms transforms) {
        if (transforms == null || transforms == ItemTransforms.NO_TRANSFORMS) return true;
        for (ItemDisplayContext context : VANILLA_CONTEXTS) {
            if (transforms.hasTransform(context)) return false;
        }
        return transforms.moddedTransforms.values().stream()
                .noneMatch(transform -> transform != ItemTransform.NO_TRANSFORM);
    }

    /**
     * The vanilla block-item display, with every context {@code provided} does set taking
     * precedence over it.
     *
     * <p>The vanilla side is read when this runs, never while a model is baked:
     * {@link #captureFromVanillaBlockItem} only runs after {@code ModelLoader.bake}, so a model that
     * copied the field at bake time would keep the empty value.
     */
    public static ItemTransforms resolveWithFallback(@Nullable ItemTransforms provided) {
        return mergeTransforms(provided, BLOCK_ITEM_TRANSFORMS);
    }

    /**
     * The display of {@code primary} over the display of {@code fallback}: a context {@code primary}
     * sets is kept, every other context and modded context comes from {@code fallback}.
     */
    public static ItemTransforms mergeTransforms(@Nullable ItemTransforms primary,
                                                 @Nullable ItemTransforms fallback) {
        ItemTransforms first = primary == null ? ItemTransforms.NO_TRANSFORMS : primary;
        ItemTransforms second = fallback == null ? ItemTransforms.NO_TRANSFORMS : fallback;
        if (isEmpty(first)) return second;
        if (isEmpty(second)) return first;
        Map<ItemDisplayContext, ItemTransform> modded = new LinkedHashMap<>(second.moddedTransforms);
        // Primary wins a shared key; ImmutableMap would reject the duplicate.
        modded.putAll(first.moddedTransforms);
        return new ItemTransforms(
                pick(first, second, ItemDisplayContext.THIRD_PERSON_LEFT_HAND),
                pick(first, second, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND),
                pick(first, second, ItemDisplayContext.FIRST_PERSON_LEFT_HAND),
                pick(first, second, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND),
                pick(first, second, ItemDisplayContext.HEAD),
                pick(first, second, ItemDisplayContext.GUI),
                pick(first, second, ItemDisplayContext.GROUND),
                pick(first, second, ItemDisplayContext.FIXED),
                ImmutableMap.copyOf(modded));
    }

    /**
     * Wraps a bake that was reached through another model's JSON in that JSON's display, overrides,
     * ambient occlusion and block-light settings.
     *
     * <p>Returns {@code inner} itself when the context asks for nothing the model does not already
     * do, which is the case for every template item of {@code gt6ce:shared_inventory}: those carry
     * no display of their own and no overrides.
     */
    public static @Nullable BakedModel applyContext(@Nullable BakedModel inner,
                                                    @Nullable ItemTransforms contextTransforms,
                                                    @Nullable ItemOverrides contextOverrides,
                                                    boolean useAmbientOcclusion,
                                                    boolean usesBlockLight) {
        if (inner == null) return null;
        ItemTransforms transforms = contextTransforms == null ? ItemTransforms.NO_TRANSFORMS : contextTransforms;
        ItemOverrides overrides = contextOverrides == null ? ItemOverrides.EMPTY : contextOverrides;
        boolean noTransforms = isEmpty(transforms);
        boolean noOverrides = overrides == ItemOverrides.EMPTY;
        if (noTransforms && noOverrides
                && useAmbientOcclusion == inner.useAmbientOcclusion()
                && usesBlockLight == inner.usesBlockLight()) {
            return inner;
        }
        return new ContextBakedModel(
                inner, transforms, overrides, noTransforms, noOverrides, useAmbientOcclusion, usesBlockLight);
    }

    /** The context {@code primary} sets, else the one {@code fallback} sets. */
    private static ItemTransform pick(ItemTransforms primary, ItemTransforms fallback, ItemDisplayContext context) {
        return primary.hasTransform(context) ? primary.getTransform(context) : fallback.getTransform(context);
    }

    /**
     * A model borrowed through another model's JSON: quads, particle and render types stay those of
     * the wrapped bake, only the display data comes from the JSON that referenced it.
     *
     * <p>The wrapped render passes are handed out unwrapped on purpose: {@code ItemRenderer} asks a
     * pass for its quads and render types only, and the camera transform is applied to this model.
     */
    private static final class ContextBakedModel extends BakedModelWrapper<BakedModel> {
        private final ItemTransforms contextTransforms;
        private final ItemOverrides contextOverrides;
        private final boolean noContextTransforms;
        private final boolean noContextOverrides;
        private final boolean ambientOcclusion;
        private final boolean blockLight;

        private ContextBakedModel(BakedModel inner,
                                  ItemTransforms contextTransforms,
                                  ItemOverrides contextOverrides,
                                  boolean noContextTransforms,
                                  boolean noContextOverrides,
                                  boolean ambientOcclusion,
                                  boolean blockLight) {
            super(inner);
            this.contextTransforms = contextTransforms;
            this.contextOverrides = contextOverrides;
            this.noContextTransforms = noContextTransforms;
            this.noContextOverrides = noContextOverrides;
            this.ambientOcclusion = ambientOcclusion;
            this.blockLight = blockLight;
        }

        @Override
        public ItemTransforms getTransforms() {
            // The wrapped model already carries the display of its own model chain, so its
            // contexts fill the ones this JSON leaves out.
            ItemTransforms inner = originalModel.getTransforms();
            return noContextTransforms ? inner : mergeTransforms(contextTransforms, inner);
        }

        @Override
        public ItemOverrides getOverrides() {
            return noContextOverrides ? originalModel.getOverrides() : contextOverrides;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return ambientOcclusion;
        }

        @Override
        public boolean usesBlockLight() {
            return blockLight;
        }

        /**
         * {@link BakedModelWrapper} forwards this to the wrapped model, which would then apply that
         * model's own display; the camera has to see the merged one instead.
         */
        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack,
                                         boolean applyLeftHandTransform) {
            getTransforms().getTransform(context).apply(applyLeftHandTransform, poseStack);
            return this;
        }
    }
}
