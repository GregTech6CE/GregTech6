package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * Port of GT6's {@code WorldgenRocks} ({@code Loader_Worldgen:618}, registered as
 * {@code "overworld.rocks"} with {@code amount = 2, probability = 3}).
 *
 * <p>GT6's "rock litter" generator: in temperate biomes every chunk marks two target columns and
 * gives each a 1-in-3 chance to place a small rock on the surface (the same lying-item block the
 * ore indicators use). Half of those rocks carry an item: usually flint, and about one in twelve of
 * the item rocks is meteoric iron — three quarters of those as a GT rock, one quarter as a raw ore
 * chunk.
 *
 * <p>The skeleton is GT6's {@code WorldgenOnSurface.generate}: mark {@code amount} target cells in
 * the 16x16 grid, roll {@code 1/probability} per marked cell, then cast a ray from
 * {@code waterLevel - 1} down to {@code min(height-2, waterLevel-1)} and hand the first opaque
 * (non-wood, non-leaf) block to {@code tryPlaceStuff}.
 */
public class GTRocksFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6 {@code new WorldgenRocks("overworld.rocks", T, 2, 3, …)}. */
    public static final int AMOUNT = 2;
    public static final int PROBABILITY = 3;
    /** GT6's item roll: {@code aRandom.nextInt(mAmount) == 0} then {@code nextInt(12) == 0}. */
    public static final int METEORIC_CHANCE = 12;
    /** ... and of the meteoric iron rocks, {@code nextInt(4) == 0} are raw ore chunks. */
    public static final int RAW_ORE_CHANCE = 4;
    public static final String METEORIC_IRON = "MeteoricIron";
    public static final String FLINT = "minecraft:flint";

    /** What one rock carries (GT6's {@code ST.save(NBT_VALUE, …)}): an item and/or a material. */
    public record Litter(String itemId, String material, boolean rawOre) {
        public static final Litter PLAIN = new Litter(null, null, false);
    }

    public GTRocksFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        Set<ResourceLocation> biomes = GTWorldgenBiomes.chunkBiomes(level, minX, minZ);
        if (!GTWorldgenBiomes.anyOf(biomes, GTWorldgenBiomes.ROCK_BIOMES)) return false;

        // GT6 marks its target cells first and rolls per marked cell afterwards.
        boolean[][] targets = new boolean[16][16];
        for (int i = 0; i < AMOUNT; i++) {
            targets[random.nextInt(16)][random.nextInt(16)] = true;
        }
        int minHeight = Math.min(level.getMaxBuildHeight() - 2, level.getSeaLevel() - 1);
        int maxHeight = Math.min(level.getMaxBuildHeight() - 1, minHeight * 2 + 16);

        boolean placed = false;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                if (!targets[i][j]) continue;
                if (random.nextInt(PROBABILITY) != 0) continue;
                placed |= castRay(level, minX + i, minZ + j, maxHeight, minHeight, random);
            }
        }
        return placed;
    }

    /**
     * GT6's sky ray plus {@code tryPlaceStuff}: the first opaque block is the contact, and the rock
     * goes on top of it when that contact is grass, ground or sand.
     *
     * @return true when a rock was placed
     */
    public static boolean castRay(WorldGenLevel level, int x, int z, int maxHeight, int minHeight,
                                  RandomSource random) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, maxHeight, z);
        for (int y = maxHeight; y >= minHeight; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            // GT6 never puts rocks on farmland.
            if (state.is(Blocks.FARMLAND)) return false;
            // Ignore everything that is not a full block, except liquids (which stop nothing).
            if (!state.liquid()) {
                if (!state.isSolidRender(level, cursor) || state.is(BlockTags.LOGS)
                        || state.is(BlockTags.LEAVES)) {
                    continue;
                }
            }
            if (!rockGround(state)) return false;
            BlockPos above = cursor.above();
            // GT6's `WD.easyRep`: the block above must be replaceable — air, a snow layer or the
            // plants the vanilla REPLACEABLE tag covers, not just air (§59).
            if (!replaceable(level.getBlockState(above))) return false;
            return placeRock(level, above, litterFor(random));
        }
        return false;
    }

    /** GT6's contact test: {@code Material.grass} / {@code Material.ground} / {@code Material.sand}. */
    public static boolean rockGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM) || state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL);
    }

    /**
     * GT6's {@code WD.easyRep}: what a rock may be placed into — air, a snow layer, or anything the
     * vanilla replaceable tag covers (grass, ferns, flowers); GT6's rocks sit on top of low plants.
     */
    public static boolean replaceable(BlockState state) {
        return state.isAir() || state.is(BlockTags.REPLACEABLE) || state.is(Blocks.SNOW);
    }

    /**
     * GT6's NBT roll, verbatim:
     * {@code nextInt(amount) == 0 ? (nextInt(12) == 0 ? (nextInt(4) == 0 ? oreRaw : rockGt).mat(MeteoricIron)
     * : flint) : null}.
     */
    public static Litter litterFor(RandomSource random) {
        if (random.nextInt(AMOUNT) != 0) return Litter.PLAIN;
        if (random.nextInt(METEORIC_CHANCE) == 0) {
            return new Litter(null, METEORIC_IRON, random.nextInt(RAW_ORE_CHANCE) == 0);
        }
        return new Litter(FLINT, null, false);
    }

    /** Places GT6's lying-item rock (the port's rock block) carrying {@code litter}. */
    public static boolean placeRock(WorldGenLevel level, BlockPos pos, Litter litter) {
        String itemId = litter.itemId();
        if (itemId == null && litter.material() != null) {
            MaterialPrefix prefix = litter.rawOre() ? MaterialPrefix.oreRaw : MaterialPrefix.rockGt;
            itemId = itemId(prefix, litter.material());
        }
        return GTRockPlacement.place(level, pos, litter.material(), itemId, litter.rawOre());
    }

    /** Item id of {@code prefix:material}, or null when the material has no such item. */
    private static String itemId(MaterialPrefix prefix, String material) {
        GTMaterial resolved = GTMaterialRegistry.get(material);
        if (resolved == null || !resolved.resolve().isValid()) return null;
        ItemStack stack = MaterialStackItemHelper.mat(prefix, resolved.resolve(), 1);
        if (stack.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? null : id.toString();
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_rocks.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("gregtech", "gt_rocks"));
}
