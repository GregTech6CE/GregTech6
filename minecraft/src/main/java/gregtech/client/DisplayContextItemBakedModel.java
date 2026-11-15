package gregtech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?} else {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
//?}
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Item model that shows one bake in the inventory GUI and another everywhere else.
 *
 * <p>A basic machine's overlay work is what a player looks for when picking the machine, but the
 * same overlay on a held, dropped or item-framed copy makes an idle machine look busy. The item
 * transform is the only place the display context reaches a baked model, so this wrapper picks the
 * bake there and leaves the geometry — and every other query — to the GUI bake.
 */
public final class DisplayContextItemBakedModel implements BakedModel {
    private final BakedModel guiModel;
    private final BakedModel worldItemModel;

    public DisplayContextItemBakedModel(BakedModel guiModel, BakedModel worldItemModel) {
        this.guiModel = guiModel;
        this.worldItemModel = worldItemModel;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean applyLeftHandTransform) {
        BakedModel chosen = context == ItemDisplayContext.GUI ? guiModel : worldItemModel;
        return chosen.applyTransform(context, poseStack, applyLeftHandTransform);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return guiModel.getQuads(state, direction, random);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        return guiModel.getQuads(state, direction, random, data, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return guiModel.getRenderTypes(state, rand, data);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        return guiModel.getRenderTypes(stack, fabulous);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        return guiModel.getRenderPasses(stack, fabulous);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return guiModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return guiModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return guiModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return guiModel.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return guiModel.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return guiModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return guiModel.getOverrides();
    }
}