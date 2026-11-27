package gregtech.client;

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;

import com.google.common.base.Suppliers;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.RockBlock;
import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.blockentity.RockBlockEntity;
import gregtech.content.registry.GTBlocks;
import gregtech.content.worldgen.GTStoneLayersGen;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.platform.Mc;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?} else {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
//?}
public final class SurfaceRockModels {
    private static final ResourceLocation RAW = Mc.gregtechId("block/rock_raw");
    private static final ResourceLocation VEIN = Mc.gregtechId("block/rock_vein");

    private SurfaceRockModels() {}

    public static void register(ModelEvent.RegisterAdditional event) {
        ClientModelEvents.registerAdditional(event, RAW);
    }

    public static void colors(RegisterColorHandlersEvent.Block event) {
        event.register(SurfaceRockModels::color, GTBlocks.ROCK.get());
    }

    private static int color(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int index) {
        if (index == 0 && level != null && pos != null && level.getBlockEntity(pos) instanceof RockBlockEntity rock) {
            var material = GTMaterialRegistry.get(rock.getMaterial()).resolve();
            if (material.isValid()) return material.getColor();
        }
        return 0xFFFFFFFF;
    }

    public static void bake(ModelEvent.ModifyBakingResult event) {
        var models = BakedModelMap.of(event.getModels());
        var raw = models.find(RAW);
        var block = GTBlocks.ROCK.get();
        var id = Mc.gregtechId("rock");
        var plain = block.defaultBlockState();
        var stone = models.getState(id, plain);
        var flint = models.getState(id, plain.setValue(RockBlock.APPEARANCE, RockBlock.Appearance.FLINT));
        if (stone == null) return;
        var palette = new Palette(stone, raw == null ? stone : raw, flint == null ? stone : flint);
        for (var state : block.getStateDefinition().getPossibleStates()) {
            if (state.getValue(RockBlock.APPEARANCE) != RockBlock.Appearance.FLINT)
                models.putState(id, state, new MarkerModel(palette, state.getValue(RockBlock.APPEARANCE)));
        }
    }

    private record Skin(
            BakedModel shape,
            @Nullable TextureAtlasSprite sprite,
            int tint,
            @Nullable TextureAtlasSprite overlay,
            int overlayTint) {
        Skin(BakedModel shape) {
            this(shape, null, -1, null, -1);
        }

        Skin(BakedModel shape, TextureAtlasSprite sprite, int tint) {
            this(shape, sprite, tint, null, -1);
        }
    }

    /** One palette per model bake; no per-position or per-world entries survive a resource reload. */
    private static final class Palette {
        private final Skin stone, flint;
        private final Supplier<Skin> stripe;
        private final BakedModel raw;
        private final Map<String, Block> rocks = new HashMap<>();
        private final Map<String, Skin> pebbles = new ConcurrentHashMap<>();
        private final Map<String, Skin> chunks = new ConcurrentHashMap<>();
        private final Cache<Faces, List<BakedQuad>> quads = CacheBuilder.newBuilder().maximumSize(512).build();

        private record Faces(List<BakedQuad> source, TextureAtlasSprite sprite, int tint, float offset) {}

        private Palette(BakedModel stone, BakedModel raw, BakedModel flint) {
            this.stone = new Skin(stone);
            // Use the same weighted shape for both rocks. Look up the new atlas after the bake completes.
            this.stripe = Suppliers.memoize(() -> new Skin(stone, null, -1,
                    Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(VEIN), 0));
            this.flint = new Skin(flint);
            this.raw = raw;
            rocks.put("Stone", Blocks.STONE);
            rocks.put("Deepslate", Blocks.DEEPSLATE);
            rocks.put("Tuff", Blocks.TUFF);
            rocks.put("Calcite", Blocks.CALCITE);
            rocks.put("Bedrock", Blocks.BEDROCK);
            rocks.put("Netherrack", Blocks.NETHERRACK);
            rocks.put("Endstone", Blocks.END_STONE);
            rocks.put("Obsidian", Blocks.OBSIDIAN);
            rocks.put("Sand", Blocks.SAND);
            rocks.put("RedSand", Blocks.RED_SAND);
            rocks.put("Gravel", Blocks.GRAVEL);
            for (var layer : GTStoneLayersGen.LAYERS) {
                if (layer.blockId() == null) continue;
                var id = layer.blockId().contains(":") ? ResourceLocation.parse(layer.blockId()) : Mc.gregtechId(layer.blockId());
                Block block = Mc.block(id);
                if (block != null && block != Blocks.AIR) rocks.putIfAbsent(layer.material(), block);
            }
            for (var type : StoneType.values()) {
                var state = GTBlocks.getStoneState(type, StoneVariant.STONE);
                if (state != null) rocks.put(type.material().getName(), state.getBlock());
            }
        }

        private static TextureAtlasSprite sprite(Block block) {
            return Minecraft.getInstance().getBlockRenderer().getBlockModel(block.defaultBlockState()).getParticleIcon(ModelData.EMPTY);
        }

        private Skin natural(String name) {
            if (name.equals(RockBlockEntity.DEFAULT_MATERIAL)) return stone;
            Block rock = rocks.get(name);
            if (rock != null) return new Skin(stone.shape(), sprite(rock), -1);
            var material = GTMaterialRegistry.get(name).resolve();
            if (!material.isValid()) return stone;
            return material.has(MaterialProperty.STONE) ? new Skin(stone.shape(), sprite(Blocks.STONE), 0) : stripe.get();
        }

        private Skin rawOre(String name) {
            Block vanilla = switch (name) {
                case "Copper" -> Blocks.RAW_COPPER_BLOCK;
                case "Iron" -> Blocks.RAW_IRON_BLOCK;
                case "Gold" -> Blocks.RAW_GOLD_BLOCK;
                default -> null;
            };
            if (vanilla != null) return new Skin(raw, sprite(vanilla), -1);
            GTMaterial material = GTMaterialRegistry.get(name).resolve();
            if (!material.isValid() || material.getTextureSet() == null) return new Skin(raw);
            var texture = BlockMaterialIcons.resolveTexture(material.getTextureSet(), "blockraw");
            var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
            var image = atlas.apply(texture);
            if (image.contents().name().equals(MissingTextureAtlasSprite.getLocation()))
                return new Skin(raw);
            var detail = BlockMaterialIcons.resolveOverlayTexture(material.getTextureSet(), "blockraw");
            var overlay = detail == null ? null : atlas.apply(detail);
            if (overlay != null && overlay.contents().name().equals(MissingTextureAtlasSprite.getLocation()))
                overlay = null;
            return new Skin(raw, image, 0, overlay, -1);
        }

        private Skin select(RockBlock.Appearance appearance, ModelData data) {
            if (Boolean.TRUE.equals(data.get(RockBlockEntity.FLINT_PROPERTY))) return flint;
            if (appearance == RockBlock.Appearance.INDICATOR) return stripe.get();
            String name = data.get(RockBlockEntity.MATERIAL_PROPERTY);
            if (name == null) return stone;
            // Cache registered identities only; legacy worlds can contain arbitrary NBT strings.
            var material = GTMaterialRegistry.get(name).resolve();
            if (!material.isValid()) return stone;
            return Boolean.TRUE.equals(data.get(RockBlockEntity.RAW_ORE_PROPERTY))
                    ? chunks.computeIfAbsent(material.getName(), this::rawOre)
                    : pebbles.computeIfAbsent(material.getName(), this::natural);
        }

        private List<BakedQuad> reskin(List<BakedQuad> source, @Nullable TextureAtlasSprite sprite, int tint, float offset) {
            if (source.isEmpty() || sprite == null) return source;
            return quads.asMap().computeIfAbsent(new Faces(source, sprite, tint, offset), faces ->
                    faces.source().stream().map(q -> BakedQuadCompat.retexture(q, faces.sprite(), faces.tint(), faces.offset())).toList());
        }
    }

    private static final class MarkerModel extends BakedModelWrapper<BakedModel> {
        private final Palette palette;
        private final RockBlock.Appearance appearance;

        private MarkerModel(Palette palette, RockBlock.Appearance appearance) {
            super(palette.stone.shape());
            this.palette = palette;
            this.appearance = appearance;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                ModelData data, @Nullable RenderType layer) {
            var skin = palette.select(appearance, data);
            var faces = skin.shape().getQuads(state, side, random, data, skin.overlay() == null ? layer : null);
            if (faces.isEmpty()) return faces;
            if (skin.overlay() == null || layer == RenderType.solid()) return palette.reskin(faces, skin.sprite(), skin.tint(), 0);
            var detail = palette.reskin(faces, skin.overlay(), skin.overlayTint(), 1f / 1024);
            if (layer != null) return layer == RenderType.cutout() ? detail : List.of();
            return ImmutableList.<BakedQuad>builder()
                    .addAll(palette.reskin(faces, skin.sprite(), skin.tint(), 0)).addAll(detail).build();
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
            var skin = palette.select(appearance, data);
            return skin.overlay() == null ? skin.shape().getRenderTypes(state, random, data)
                    : ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout());
        }

        @Override
        public TextureAtlasSprite getParticleIcon(ModelData data) {
            var skin = palette.select(appearance, data);
            return skin.sprite() == null ? skin.shape().getParticleIcon(data) : skin.sprite();
        }
    }
}
