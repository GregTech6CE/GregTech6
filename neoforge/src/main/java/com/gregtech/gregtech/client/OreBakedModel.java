package com.gregtech.gregtech.client;

import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.block.OreHostStone;
import com.gregtech.gregtech.block.stone.StoneType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic ore model: a stone background chosen by the ore block's {@link OreBlock#STONE} block-state
 * property (or, for items, the {@code BlockStateTag}/{@code BlockEntityTag} the stack carries) plus
 * the material's icon-set ore overlay tinted with the material color (tint index 0).
 *
 * <p>§103.B: the background used to come from the ore block entity's model data
 * ({@code OreBlockEntity.STONE_PROPERTY}); it is part of the block state now, so no model data and no
 * block entity are involved. Because it is a custom loader, the blockstate JSON stays the single
 * {@code ""} variant — the 39 host rocks need no asset expansion.
 *
 * <p>Quads are produced by UV-remapping the quads of a template cube model (vanilla stone)
 * onto the target sprites, cached per host rock.
 */
public final class OreBakedModel implements IDynamicBakedModel {
    /** Host-stone id → base texture, keyed by {@link OreHostStone#id()} (see {@code GTOreBlockResolver}). */
    private static final Map<String, ResourceLocation> STONE_TEXTURES = new HashMap<>();
    private static final Map<String, ResourceLocation> BROKEN_TEXTURES = new HashMap<>();
    static {
        STONE_TEXTURES.put("stone", ResourceLocation.fromNamespaceAndPath("minecraft", "block/stone"));
        STONE_TEXTURES.put("granite", ResourceLocation.fromNamespaceAndPath("minecraft", "block/granite"));
        STONE_TEXTURES.put("diorite", ResourceLocation.fromNamespaceAndPath("minecraft", "block/diorite"));
        STONE_TEXTURES.put("andesite", ResourceLocation.fromNamespaceAndPath("minecraft", "block/andesite"));
        STONE_TEXTURES.put("deepslate", ResourceLocation.fromNamespaceAndPath("minecraft", "block/deepslate"));
        STONE_TEXTURES.put("tuff", ResourceLocation.fromNamespaceAndPath("minecraft", "block/tuff"));
        STONE_TEXTURES.put("netherrack", ResourceLocation.fromNamespaceAndPath("minecraft", "block/netherrack"));
        STONE_TEXTURES.put("end_stone", ResourceLocation.fromNamespaceAndPath("minecraft", "block/end_stone"));
        // Loose sediments — small ores only (GT6 sand/gravel small ores).
        STONE_TEXTURES.put("sand", ResourceLocation.fromNamespaceAndPath("minecraft", "block/sand"));
        STONE_TEXTURES.put("red_sand", ResourceLocation.fromNamespaceAndPath("minecraft", "block/red_sand"));
        STONE_TEXTURES.put("gravel", ResourceLocation.fromNamespaceAndPath("minecraft", "block/gravel"));
        // Bedrock deposits (unbreakable ores).
        STONE_TEXTURES.put("bedrock", ResourceLocation.fromNamespaceAndPath("minecraft", "block/bedrock"));
        BROKEN_TEXTURES.putAll(STONE_TEXTURES);
        BROKEN_TEXTURES.put("stone", ResourceLocation.fromNamespaceAndPath("minecraft", "block/cobblestone"));
        BROKEN_TEXTURES.put("deepslate", ResourceLocation.fromNamespaceAndPath("minecraft", "block/cobbled_deepslate"));
        for (StoneType type : StoneType.values()) {
            STONE_TEXTURES.put(type.registryId(),
                    ResourceLocation.fromNamespaceAndPath("gregtech", "block/stones/" + type.textureFolder() + "/stone"));
            BROKEN_TEXTURES.put(type.registryId(),
                    ResourceLocation.fromNamespaceAndPath("gregtech", "block/stones/" + type.textureFolder() + "/cobble"));
        }
    }

    private final BakedModel template;
    private final ResourceLocation overlayTexture;
    private record StoneFace(String stone, boolean broken, @Nullable Direction side) {}
    private final BoundedCache<StoneFace, List<BakedQuad>> quads = new BoundedCache<>(512);
    private record ModelHost(OreHostStone stone, boolean broken) {}
    /** One host-pinned model for each intact/broken host-rock view. */
    private final Map<ModelHost, BakedModel> stoneModels = new HashMap<>();
    private final ItemOverrides overrides = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable net.minecraft.client.multiplayer.ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            return stoneModel(OreBlock.stoneOfStack(stack), OreBlock.isBrokenStack(stack));
        }
    };

    public OreBakedModel(BakedModel template, ResourceLocation overlayTexture) {
        this.template = template;
        this.overlayTexture = overlayTexture;
    }

    /**
     * The model to register for states (and to hand to item overrides) whose host rock is
     * {@code stone}: pinned, so the state-less {@code getQuads} and the break particle
     * ({@code BlockModelShaper#getParticleIcon(BlockState)} → {@code ModelData.EMPTY}) still pick
     * the right stone.
     */
    public BakedModel stoneModel(@Nullable OreHostStone stone) {
        return stoneModel(stone, false);
    }

    public BakedModel stoneModel(@Nullable OreHostStone stone, boolean broken) {
        OreHostStone key = stone == null ? OreHostStone.STONE : stone;
        return stoneModels.computeIfAbsent(new ModelHost(key, broken), StoneModel::new);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData extraData, @Nullable RenderType renderType) {
        return quadsFor(OreBlock.stoneOf(state), OreBlock.isBroken(state), side);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return quadsFor(OreBlock.stoneOf(state), OreBlock.isBroken(state), side);
    }

    private List<BakedQuad> quadsFor(OreHostStone stone, boolean broken, @Nullable Direction side) {
        OreHostStone key = stone == null ? OreHostStone.STONE : stone;
        return quads.computeIfAbsent(new StoneFace(key.id(), broken, side),
                k -> List.copyOf(buildQuads(k.stone(), k.broken(), k.side())));
    }

    private List<BakedQuad> buildQuads(String stone, boolean broken, @Nullable Direction side) {
        Map<String, ResourceLocation> textures = broken ? BROKEN_TEXTURES : STONE_TEXTURES;
        TextureAtlasSprite base = sprite(textures.getOrDefault(stone,
                STONE_TEXTURES.get(OreHostStone.STONE.id())));
        TextureAtlasSprite overlay = sprite(overlayTexture);
        List<BakedQuad> source = template.getQuads(null, side, RandomSource.create(42L));
        List<BakedQuad> out = new ArrayList<>(source.size() * 2);
        for (BakedQuad quad : source) {
            out.add(retexture(quad, base, -1));
            out.add(retexture(quad, overlay, 0));
        }
        return out;
    }

    /** Copies the quad with sprite + UVs swapped onto {@code to} and the given tint index. */
    private static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite to, int tintIndex) {
        TextureAtlasSprite from = quad.getSprite();
        int[] verts = quad.getVertices().clone();
        float u0 = from.getU0(), du = from.getU1() - u0;
        float v0 = from.getV0(), dv = from.getV1() - v0;
        float tu0 = to.getU0(), tdu = to.getU1() - tu0;
        float tv0 = to.getV0(), tdv = to.getV1() - tv0;
        for (int i = 0; i < 4; i++) {
            int idx = i * 8;
            float u = Float.intBitsToFloat(verts[idx + 4]);
            float v = Float.intBitsToFloat(verts[idx + 5]);
            float relU = du > 0 ? (u - u0) / du : 0;
            float relV = dv > 0 ? (v - v0) / dv : 0;
            verts[idx + 4] = Float.floatToRawIntBits(tu0 + relU * tdu);
            verts[idx + 5] = Float.floatToRawIntBits(tv0 + relV * tdv);
        }
        return new BakedQuad(verts, tintIndex, quad.getDirection(), to, quad.isShade(), quad.hasAmbientOcclusion());
    }

    private static TextureAtlasSprite sprite(ResourceLocation texture) {
        return Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(texture);
    }

    private static TextureAtlasSprite stoneSprite(OreHostStone stone, boolean broken) {
        OreHostStone key = stone == null ? OreHostStone.STONE : stone;
        Map<String, ResourceLocation> textures = broken ? BROKEN_TEXTURES : STONE_TEXTURES;
        return sprite(textures.getOrDefault(key.id(), STONE_TEXTURES.get(OreHostStone.STONE.id())));
    }

    /** A model locked to one host rock (block-state view or item NBT variant). */
    private final class StoneModel implements IDynamicBakedModel {
        private final OreHostStone stone;
        private final boolean broken;

        StoneModel(ModelHost host) {
            this.stone = host.stone();
            this.broken = host.broken();
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return quadsFor(stone, broken, side);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                        ModelData data, @Nullable RenderType renderType) {
            return quadsFor(stone, broken, side);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
            return OreBakedModel.this.getRenderTypes(state, rand, data);
        }

        @Override
        public boolean useAmbientOcclusion() { return true; }
        @Override
        public boolean isGui3d() { return true; }
        @Override
        public boolean usesBlockLight() { return true; }
        @Override
        public boolean isCustomRenderer() { return false; }
        @Override
        public TextureAtlasSprite getParticleIcon() { return stoneSprite(stone, broken); }
        @Override
        public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) { return stoneSprite(stone, broken); }
        @Override
        public ItemTransforms getTransforms() { return OreBakedModel.this.getTransforms(); }
        @Override
        public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
    }

    @Override
    public boolean useAmbientOcclusion() { return true; }

    @Override
    public boolean isGui3d() { return true; }

    @Override
    public boolean usesBlockLight() { return true; }

    @Override
    public boolean isCustomRenderer() { return false; }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return stoneSprite(OreHostStone.STONE, false);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        // BlockModelShaper#getParticleIcon(BlockState) passes ModelData.EMPTY: the stone is not in the
        // model data any more, and the per-state models (stoneModel) carry it instead.
        return stoneSprite(OreHostStone.STONE, false);
    }

    @Override
    public ItemTransforms getTransforms() {
        ItemTransforms block = BlockItemModelHelper.blockItemTransforms();
        return block != ItemTransforms.NO_TRANSFORMS ? block : template.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }
}
