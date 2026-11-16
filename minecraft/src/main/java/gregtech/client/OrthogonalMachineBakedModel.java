package gregtech.client;

import gregtech.engine.machine.OrthogonalMachineModels;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?} else {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
//?}
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Bakes one {@link OrthogonalMachineModels.Entry} into a cube: a material-tinted shell plus an
 * optional untinted detail pass.
 *
 * <p>The static JSON these replace spelled every face out as a {@code forge:composite} child with
 * {@code cullface} and {@code tintindex}; here {@link FaceBakery} does the same work for one
 * {@code [0,0,0]->[16,16,16]} element per face, and the per-variant rotation is a {@link ModelState}
 * instead of a blockstate file. Sprites come from the bake that produced the model, never from the
 * atlas afterwards, so the whole model map stays reload-safe.
 */
public final class OrthogonalMachineBakedModel implements IDynamicBakedModel {
    /**
     * The six table columns in vanilla direction order. Indexing the row by
     * {@link Direction#get3DDataValue()} is only valid while this holds, so the order is checked
     * instead of assumed. (The row index is not {@link Direction#ordinal()} — that order differs.)
     */
    private static final Direction[] DIRECTION_ORDER = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    static {
        for (int index = 0; index < DIRECTION_ORDER.length; index++) {
            if (DIRECTION_ORDER[index].get3DDataValue() != index)
                throw new IllegalStateException("Machine model face " + index + " is not " + DIRECTION_ORDER[index]);
        }
    }

    private static final Vector3f FROM = new Vector3f(0f, 0f, 0f);
    private static final Vector3f TO = new Vector3f(16f, 16f, 16f);

    /**
     * The chunk layers ({@code RenderType.solid()} / {@code cutout()}) use the {@code BLOCK} vertex
     * format, which {@code ItemRenderer} renders in the GUI and in hand without the three-directional
     * lighting; {@link Sheets#cutoutBlockSheet()} uses the {@code NEW_ENTITY} format the vanilla
     * {@code BlockModel} and {@code CompositeModel} item passes use, which restores the standard
     * directional shading.
     */
    private static final List<RenderType> ITEM_RENDER_TYPES = List.of(Sheets.cutoutBlockSheet());

    private final OrthogonalMachineModels.Entry spec;
    private final TextureAtlasSprite particle;
    private final RenderType baseRenderType;
    private final Map<Direction, List<BakedQuad>> baseFaces;
    private final Map<Direction, List<BakedQuad>> overlayFaces;
    private final boolean hasOverlay;
    private final List<BakedModel> passes;

    private OrthogonalMachineBakedModel(
            OrthogonalMachineModels.Entry spec,
            Map<ResourceLocation, TextureAtlasSprite> sprites,
            TextureAtlasSprite particle,
            ModelState modelState) {
        this.spec = spec;
        this.particle = particle;
        // The shell is the opaque-or-cutout cube; the detail pass is always cutout.
        baseRenderType = spec.solidBase() ? RenderType.solid() : RenderType.cutout();
        FaceBakery bakery = new FaceBakery();
        baseFaces = bakeFaces(bakery, sprites, spec::baseFace, 0, modelState);
        overlayFaces = spec.hasOverlay() ? bakeFaces(bakery, sprites, spec::overlayFace, -1, modelState) : Map.of();
        hasOverlay = !overlayFaces.isEmpty();
        LayerPass base = new LayerPass(this, baseRenderType, baseFaces);
        if (hasOverlay) {
            passes = List.of(base, new LayerPass(this, RenderType.cutout(), overlayFaces));
        } else {
            passes = List.of(base);
        }
    }

    /**
     * Bakes one table row. The lookup is the geometry loader's sprite function, so every sprite this
     * model keeps belongs to the bake that created it.
     */
    public static BakedModel bake(OrthogonalMachineModels.Entry spec,
                                  Function<ResourceLocation, TextureAtlasSprite> blockAtlasLookup,
                                  ModelState modelState) {
        return new OrthogonalMachineBakedModel(
                spec, resolveSprites(spec, blockAtlasLookup), particle(spec, blockAtlasLookup), modelState);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return getQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource random, ModelData data, @Nullable RenderType renderType) {
        // Every quad this model bakes carries a cullface direction, so no unculled quads exist and
        // the side-less query stays empty; item rendering walks all six sides and would otherwise
        // draw each face twice.
        if (side == null) return List.of();
        List<BakedQuad> base = baseFaces.getOrDefault(side, List.of());
        List<BakedQuad> overlay = overlayFaces.getOrDefault(side, List.of());
        // An unfiltered query takes every layer that covers this side.
        if (renderType == null) return overlay.isEmpty() ? base : concat(base, overlay);
        if (overlay.isEmpty()) return renderType == baseRenderType ? base : List.of();
        // A cutout shell shares its layer with the detail pass, so that layer carries both.
        if (renderType == baseRenderType && baseRenderType == RenderType.cutout()) return concat(base, overlay);
        if (renderType == baseRenderType) return base;
        return renderType == RenderType.cutout() ? overlay : List.of();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        if (!hasOverlay) return ChunkRenderTypeSet.of(baseRenderType);
        return spec.solidBase()
                ? ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout())
                : ChunkRenderTypeSet.of(RenderType.cutout());
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        return ITEM_RENDER_TYPES;
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        return passes;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return particle;
    }

    @Override
    public ItemTransforms getTransforms() {
        return BlockItemModelHelper.blockItemTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    private Map<Direction, List<BakedQuad>> bakeFaces(
            FaceBakery bakery,
            Map<ResourceLocation, TextureAtlasSprite> sprites,
            java.util.function.IntFunction<String> textures,
            int tintIndex,
            ModelState modelState) {
        Map<Direction, List<BakedQuad>> faces = new EnumMap<>(Direction.class);
        for (int index = 0; index < DIRECTION_ORDER.length; index++) {
            String texture = textures.apply(index);
            if (texture == null) continue;
            Direction direction = DIRECTION_ORDER[index];
            // The cullface is the same local direction: FaceBakery rotates it, and the resulting
            // quad keeps the rotated direction, exactly like the JSON cullface it replaces.
            BlockElementFace face = new BlockElementFace(direction, tintIndex, "", fullUv());
            var quad = bakeQuad(bakery, face, sprites.getOrDefault(textureId(texture), particle), direction, modelState);
            faces.computeIfAbsent(quad.getDirection(), key -> new ArrayList<>()).add(quad);
        }
        Map<Direction, List<BakedQuad>> copy = new EnumMap<>(Direction.class);
        faces.forEach((direction, quads) -> copy.put(direction, List.copyOf(quads)));
        return copy;
    }

    /**
     * The shell faces use the whole sprite; uvlock and the variant rotation come from the state.
     * Every face gets its own instance because {@link FaceBakery} rewrites the uv array of the face
     * it bakes, so a shared one would carry one face's adjustment into the next bake.
     */
    private static BlockFaceUV fullUv() {
        return new BlockFaceUV(new float[]{0f, 0f, 16f, 16f}, 0);
    }

    private static BakedQuad bakeQuad(FaceBakery bakery, BlockElementFace face,
                                      TextureAtlasSprite sprite, Direction direction, ModelState modelState) {
//? if >=1.21.1 {
        /*return bakery.bakeQuad(FROM, TO, face, sprite, direction, modelState, null, true);
*///?} else {
        return bakery.bakeQuad(FROM, TO, face, sprite, direction, modelState, null, true,
                ResourceLocation.fromNamespaceAndPath("gt6ce", "orthogonal_machine"));
//?}
    }

    /** Every texture the row names, captured from this bake's atlas sprites. */
    private static Map<ResourceLocation, TextureAtlasSprite> resolveSprites(
            OrthogonalMachineModels.Entry spec, Function<ResourceLocation, TextureAtlasSprite> lookup) {
        Map<ResourceLocation, TextureAtlasSprite> resolved = new HashMap<>();
        for (int index = 0; index < DIRECTION_ORDER.length; index++) {
            resolve(resolved, lookup, spec.baseFace(index));
            resolve(resolved, lookup, spec.overlayFace(index));
        }
        resolve(resolved, lookup, spec.particle());
        return resolved;
    }

    private static void resolve(Map<ResourceLocation, TextureAtlasSprite> resolved,
                                Function<ResourceLocation, TextureAtlasSprite> lookup, @Nullable String texture) {
        if (texture != null) resolved.put(textureId(texture), lookup.apply(textureId(texture)));
    }

    private static TextureAtlasSprite particle(
            OrthogonalMachineModels.Entry spec, Function<ResourceLocation, TextureAtlasSprite> lookup) {
        String texture = spec.particle();
        for (int index = 0; texture == null && index < DIRECTION_ORDER.length; index++)
            texture = spec.baseFace(index);
        return lookup.apply(texture == null ? MissingTextureAtlasSprite.getLocation() : textureId(texture));
    }

    private static ResourceLocation textureId(String texture) {
        return ResourceLocation.parse(OrthogonalMachineModels.fullTexturePath(texture));
    }

    private static List<BakedQuad> concat(List<BakedQuad> first, List<BakedQuad> second) {
        List<BakedQuad> quads = new ArrayList<>(first.size() + second.size());
        quads.addAll(first);
        quads.addAll(second);
        return Collections.unmodifiableList(quads);
    }

    /** One render layer as its own item pass; the item renderer asks each pass for its own quads. */
    private static final class LayerPass implements BakedModel {
        private final OrthogonalMachineBakedModel owner;
        private final RenderType layer;
        private final Map<Direction, List<BakedQuad>> faces;

        private LayerPass(
                OrthogonalMachineBakedModel owner, RenderType layer, Map<Direction, List<BakedQuad>> faces) {
            this.owner = owner;
            this.layer = layer;
            this.faces = faces;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            // As in the parent model, every baked face is culled, so the six-sided item loop already
            // covers the whole pass and the side-less query must stay empty.
            if (side == null) return List.of();
            return faces.getOrDefault(side, List.of());
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                        RandomSource random, ModelData data, @Nullable RenderType renderType) {
            return getQuads(state, side, random);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
            return ChunkRenderTypeSet.of(layer);
        }

        @Override
        public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return ITEM_RENDER_TYPES;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }

        @Override
        public boolean usesBlockLight() {
            return true;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return owner.particle;
        }

        @Override
        public ItemTransforms getTransforms() {
            return BlockItemModelHelper.blockItemTransforms();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }
}
