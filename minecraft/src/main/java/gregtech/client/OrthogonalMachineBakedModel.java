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
import net.minecraft.client.resources.model.BlockModelRotation;
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
    private final Map<ResourceLocation, TextureAtlasSprite> sprites;
    private final TextureAtlasSprite particle;
    private final RenderType baseRenderType;
    private final Map<Direction, List<BakedQuad>> baseFaces;
    private final Map<Direction, List<BakedQuad>> overlayFaces;
    private final List<BakedQuad> baseQuads;
    private final List<BakedQuad> overlayQuads;
    private final List<BakedModel> passes;

    private OrthogonalMachineBakedModel(
            OrthogonalMachineModels.Entry spec,
            Map<ResourceLocation, TextureAtlasSprite> sprites,
            TextureAtlasSprite particle,
            ModelState modelState) {
        this.spec = spec;
        this.sprites = sprites;
        this.particle = particle;
        // The shell is the opaque-or-cutout cube; the detail pass is always cutout.
        baseRenderType = spec.solidBase() ? RenderType.solid() : RenderType.cutout();
        FaceBakery bakery = new FaceBakery();
        baseFaces = bakeFaces(bakery, spec::baseFace, 0, modelState);
        overlayFaces = spec.hasOverlay() ? bakeFaces(bakery, spec::overlayFace, -1, modelState) : Map.of();
        baseQuads = flatten(baseFaces.values());
        overlayQuads = flatten(overlayFaces.values());
        LayerPass base = new LayerPass(this, baseRenderType, baseFaces, baseQuads);
        if (overlayQuads.isEmpty()) {
            passes = List.of(base);
        } else {
            passes = List.of(base, new LayerPass(this, RenderType.cutout(), overlayFaces, overlayQuads));
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

    /** A rotation-only variant of the same row, reusing this bake's sprites instead of the atlas. */
    public BakedModel rebake(ModelState modelState) {
        return new OrthogonalMachineBakedModel(spec, sprites, particle, modelState);
    }

    /** Sprite source of this bake, for callers that need another rotation of the same row. */
    public Function<ResourceLocation, TextureAtlasSprite> spriteLookup() {
        return id -> sprites.getOrDefault(id, particle);
    }

    /** Rotates the row; {@code uvLock} keeps the texture upright, like the blockstate's own flag. */
    public static ModelState modelState(int xRot, int yRot, boolean uvLock) {
        BlockModelRotation rotation = BlockModelRotation.by(xRot, yRot);
        return uvLock ? new LockedModelState(rotation) : rotation;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return getQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource random, ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> base = side == null ? baseQuads : baseFaces.getOrDefault(side, List.of());
        List<BakedQuad> overlay = side == null ? overlayQuads : overlayFaces.getOrDefault(side, List.of());
        // The unbuffered query (box/entity pass) takes every layer at once.
        if (renderType == null) return overlay.isEmpty() ? base : concat(base, overlay);
        if (overlay.isEmpty()) return renderType == baseRenderType ? base : List.of();
        // A cutout shell shares its layer with the detail pass, so that layer carries both.
        if (renderType == baseRenderType && baseRenderType == RenderType.cutout()) return concat(base, overlay);
        if (renderType == baseRenderType) return base;
        return renderType == RenderType.cutout() ? overlay : List.of();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        if (overlayQuads.isEmpty()) return ChunkRenderTypeSet.of(baseRenderType);
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
            FaceBakery bakery, java.util.function.IntFunction<String> textures, int tintIndex, ModelState modelState) {
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

    /** Every texture the row names, so a rebake of the same row needs no atlas access. */
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

    private static List<BakedQuad> flatten(java.util.Collection<List<BakedQuad>> faces) {
        List<BakedQuad> quads = new ArrayList<>();
        faces.forEach(quads::addAll);
        return Collections.unmodifiableList(quads);
    }

    private static List<BakedQuad> concat(List<BakedQuad> first, List<BakedQuad> second) {
        List<BakedQuad> quads = new ArrayList<>(first.size() + second.size());
        quads.addAll(first);
        quads.addAll(second);
        return Collections.unmodifiableList(quads);
    }

    /** {@link BlockModelRotation} reports an unlocked UV, so uvlock needs its own wrapper. */
    private record LockedModelState(BlockModelRotation rotation) implements ModelState {
        @Override
        public com.mojang.math.Transformation getRotation() {
            return rotation.getRotation();
        }

        @Override
        public boolean isUvLocked() {
            return true;
        }

//? if >=1.21.1 {
        /*@Override
        public boolean mayApplyArbitraryRotation() {
            // 1.21.1 skips the winding recalculation for arbitrary rotations; 1.20.1 always runs it,
            // and these quads are baked from axis-aligned elements, so keep the 1.20.1 winding.
            return false;
        }
*///?}
    }

    /** One render layer as its own item pass; the item renderer asks each pass for its own quads. */
    private static final class LayerPass implements BakedModel {
        private final OrthogonalMachineBakedModel owner;
        private final RenderType layer;
        private final Map<Direction, List<BakedQuad>> faces;
        private final List<BakedQuad> quads;

        private LayerPass(OrthogonalMachineBakedModel owner, RenderType layer,
                          Map<Direction, List<BakedQuad>> faces, List<BakedQuad> quads) {
            this.owner = owner;
            this.layer = layer;
            this.faces = faces;
            this.quads = quads;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return side == null ? quads : faces.getOrDefault(side, List.of());
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