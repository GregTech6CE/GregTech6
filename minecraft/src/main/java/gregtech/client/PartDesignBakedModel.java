package gregtech.client;

import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.engine.machine.OrthogonalMachineModels;
import gregtech.platform.Mc;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?} else {
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
//?}
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Picks a pre-baked design model from {@code gt.design}. Index 0 keeps the block's own model. */
public final class PartDesignBakedModel extends BakedModelWrapper<BakedModel> {
    private static final String DESIGN_DIRECTORY = "block/machine/multiblock/design/";

    private final BakedModel[] designs;

    public PartDesignBakedModel(BakedModel original, BakedModel[] designs) {
        super(original);
        this.designs = designs;
    }

    /** Model path of one design cube, relative to the {@code gt6ce} namespace. */
    public static String modelPath(String folder, int design) {
        return DESIGN_DIRECTORY + folder + "_" + design;
    }

    /** The same cube as a model id, registered and looked up by both platforms. */
    public static ResourceLocation modelId(String folder, int design) {
        return Mc.gregtechId(modelPath(folder, design));
    }

    /**
     * Whether the baked pack can serve {@code modelPath}: the face table always carries its own rows,
     * while any other cube needs a mounted pack that ships {@code models/<modelPath>.json} itself.
     * The resource manager already applies pack {@code filter} rules, so a pack that filters that
     * model out reports it missing.
     */
    public static boolean isDesignAvailable(@Nullable ResourceManager resources, String modelPath) {
        if (OrthogonalMachineModels.contains(modelPath)) return true;
        return resources != null && resources.getResource(Mc.gregtechId("models/" + modelPath + ".json")).isPresent();
    }

    private BakedModel choose(ModelData data) {
        Integer design = data == null ? null : data.get(MultiblockPortBlockEntity.DESIGN);
        if (design == null || design <= 0 || design >= designs.length || designs[design] == null) return originalModel;
        return designs[design];
    }

    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                             ModelData data, @Nullable net.minecraft.client.renderer.RenderType layer) {
        return choose(data).getQuads(state, side, random, data, layer);
    }

    @Override public TextureAtlasSprite getParticleIcon(ModelData data) {
        return choose(data).getParticleIcon(data);
    }

    @Override public ModelData getModelData(net.minecraft.world.level.BlockAndTintGetter level,
            net.minecraft.core.BlockPos pos, BlockState state, ModelData data) {
        return choose(data).getModelData(level, pos, state, data);
    }

//? if >=1.21.1 {
    /*@Override public net.neoforged.neoforge.client.ChunkRenderTypeSet getRenderTypes(
*///?} else {
    @Override public net.minecraftforge.client.ChunkRenderTypeSet getRenderTypes(
//?}
            BlockState state, RandomSource random, ModelData data) {
        return choose(data).getRenderTypes(state, random, data);
    }
}
