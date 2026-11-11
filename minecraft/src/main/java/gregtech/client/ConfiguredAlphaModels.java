package gregtech.client;

import gregtech.GregTechConfig.Rule;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import java.util.List;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.ChunkRenderTypeSet;
//?}
public final class ConfiguredAlphaModels {
    private ConfiguredAlphaModels() {}

    public static boolean enabled(String namespace) {
        return !namespace.equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE) && !namespace.equals("minecraft")
                || (namespace.equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE) ? Rule.GT_ALPHA : Rule.MC_ALPHA).enabled();
    }

    public static RenderType gtTranslucent() {
        return Rule.GT_ALPHA.enabled() ? RenderType.translucent() : RenderType.cutout();
    }

    public static void models(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        // Keep the canonical missing sentinel unwrapped so BakedModelMap.find can reject it.
        BakedModel missing = models.missing();
        models.replaceAll((id, model) -> model == missing || enabled(id.getNamespace()) ? model : new Cutout(model));
        // Rebuild after the alpha pass so the tool renderer sees the final wrapped models.
        GTToolModelResolver.rebuild(models);
    }

    /** Keep the original model geometry and replace only its blended render pass. */
    private static final class Cutout extends BakedModelWrapper<BakedModel> {
        private Cutout(BakedModel original) { super(original); }

        @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
            return ChunkRenderTypeSet.of(originalModel.getRenderTypes(state, random, data).asList().stream()
                    .map(type -> type == RenderType.translucent() ? RenderType.cutout() : type)
                    .toArray(RenderType[]::new));
        }

        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
                                                   ModelData data, RenderType type) {
            if (state != null && type == RenderType.cutout()
                    && originalModel.getRenderTypes(state, random, data).contains(RenderType.translucent())) {
                var quads = new java.util.ArrayList<BakedQuad>();
                if (originalModel.getRenderTypes(state, random, data).contains(type))
                    quads.addAll(originalModel.getQuads(state, side, random, data, type));
                quads.addAll(originalModel.getQuads(state, side, random, data, RenderType.translucent()));
                return quads;
            }
            return originalModel.getQuads(state, side, random, data, type);
        }

        @Override public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            var atlas = net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
            return originalModel.getRenderTypes(stack, fabulous).stream().map(type ->
                    type == RenderType.translucent() || type == RenderType.entityTranslucent(atlas)
                    || type == RenderType.itemEntityTranslucentCull(atlas)
                            ? RenderType.entityCutout(atlas) : type).toList();
        }

        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return originalModel.getRenderPasses(stack, fabulous).stream()
                    .map(model -> model == originalModel ? (BakedModel) this : new Cutout(model)).toList();
        }
    }
}
