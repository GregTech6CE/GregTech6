package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.generated.OreMaterials;
import com.gregtech.gregtech.worldgen.GTColtanFeature;
import com.gregtech.gregtech.worldgen.GTFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GT6's coltan field ({@code WorldgenColtan}, {@code Loader_Worldgen:779}): one centre point per
 * world seed ({@code new Random(seed + 5).nextGaussian() * 1500}), small Coltan/Columbite/Tantalite
 * ores within 480 blocks of it and full blocks within 64, all between y = 20 and y = 40.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class ColtanTests {
    private static final int BASE_X = 46000;
    private static final int BASE_Z = 46000;
    /** World y the band maps to in the modern 1.20 overworld, well below the test terrain. */
    private static final int BAND_LOW = -40;
    private static final int BAND_HIGH = 0;

    /** GT6's registration line for the field. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void coltanFieldMatchesGt6Constants(GameTestHelper helper) {
        helper.assertTrue(GTColtanFeature.MIN_Y == 20, "GT6 starts the field at y = 20");
        helper.assertTrue(GTColtanFeature.MAX_Y == 40, "GT6 ends the field at y = 40");
        helper.assertTrue(GTColtanFeature.AMOUNT == 32, "GT6's amount is 32");
        helper.assertTrue(GTColtanFeature.RANGE == 480, "GT6's range is 480 blocks");
        helper.assertTrue(GTColtanFeature.LARGE_RANGE == 64, "GT6 switches to full ores inside 64 blocks");
        helper.assertTrue(GTColtanFeature.TABLE_SIZE == 5, "GT6 rolls nextInt(5) for the material");
        helper.assertTrue(GTFeatures.COLTAN.getId().getPath().equals("gt_coltan"), "feature id");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTColtanFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_coltan.json is loaded");
        helper.succeed();
    }

    /**
     * The centre point is a pure function of the world seed, spans the whole world (a bare gaussian
     * would stay inside ±1), and takes two independent draws.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void centreIsSeedDerived(GameTestHelper helper) {
        long seed = helper.getLevel().getSeed();
        BlockPos centre = GTColtanFeature.centre(seed);
        helper.assertTrue(centre.equals(GTColtanFeature.centre(seed)), "the centre must be deterministic");
        helper.assertTrue(centre.getY() == 0, "the centre only carries x/z");

        List<String> problems = new ArrayList<>();
        int far = 0;
        int equalDrawn = 0;
        int duplicates = 0;
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (long s = 0; s < 400; s++) {
            BlockPos c = GTColtanFeature.centre(s);
            int x = c.getX(), z = c.getZ();
            if (Math.abs(x) > 6000 || Math.abs(z) > 6000) problems.add("seed " + s + " -> " + x + "/" + z);
            if (Math.abs(x) > 1500 || Math.abs(z) > 1500) far++;
            if (x == z) equalDrawn++;
            if (!seen.add(((long) x << 32) ^ (z & 0xFFFFFFFFL))) duplicates++;
        }
        // A gaussian is inside ±1500 ≈ 68% of the time, so a healthy sample reaches beyond it.
        helper.assertTrue(far > 100, "the centre is scaled by 1500, only " + far + "/400 samples went past it");
        // x and z are two consecutive draws — they coincide only by chance.
        helper.assertTrue(equalDrawn < 40, "x and z are not independent draws: " + equalDrawn + "/400 equal");
        helper.assertTrue(duplicates == 0, "two seeds produced the same centre " + duplicates + " times");
        helper.assertTrue(problems.isEmpty(), "centres out of range: " + problems.subList(0, Math.min(5, problems.size())));
        // Same formula the port documents.
        Random reference = new Random(seed + 5);
        helper.assertTrue(centre.getX() == (int) (reference.nextGaussian() * 1500)
                        && centre.getZ() == (int) (reference.nextGaussian() * 1500),
                "GT6 uses new Random(seed + 5).nextGaussian() * 1500");
        helper.succeed();
    }

    /** GT6's 5-way material table and its count formula. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void materialTableAndCount(GameTestHelper helper) {
        GTMaterial[] table = new GTMaterial[GTColtanFeature.TABLE_SIZE];
        for (int roll = 0; roll < table.length; roll++) table[roll] = GTColtanFeature.materialFor(roll);
        helper.assertTrue(table[0] == OreMaterials.Columbite, "GT6's roll 0 is columbite");
        helper.assertTrue(table[1] == OreMaterials.Tantalite, "GT6's roll 1 is tantalite");
        for (int roll = 2; roll < table.length; roll++) {
            helper.assertTrue(table[roll] == OreMaterials.Coltan, "GT6's roll " + roll + " is coltan");
        }
        // Every ore the table names must have a registered block for both forms.
        List<String> missing = new ArrayList<>();
        for (GTMaterial material : table) {
            for (String prefix : new String[] {"ore", "ore_small"}) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gregtech",
                        prefix + "_" + material.getName().toLowerCase());
                Block block = ForgeRegistries.BLOCKS.getValue(id);
                if (block == null || block == Blocks.AIR) missing.add(id.toString());
            }
        }
        helper.assertTrue(missing.isEmpty(), "missing ore blocks: " + missing);

        RandomSource random = RandomSource.create(12345L);
        int low = Integer.MAX_VALUE, high = 0;
        for (int i = 0; i < 2000; i++) {
            int count = GTColtanFeature.count(random, GTColtanFeature.AMOUNT);
            low = Math.min(low, count);
            high = Math.max(high, count);
        }
        // amount/2 + rand(1+amount)/2 -> [16, 32] for amount = 32.
        helper.assertTrue(low == 16, "GT6's minimum for amount 32 is 16, got " + low);
        helper.assertTrue(high == 32, "GT6's maximum for amount 32 is 32, got " + high);
        helper.assertTrue(GTColtanFeature.count(random, 0) == 1, "the max(1, ...) floor keeps tiny amounts alive");
        helper.succeed();
    }

    /** The field only generates inside its radius, and switches to full ores near the centre. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void fieldScattersSmallOresAndFullOresNearCentre(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        GTColtanFeature feature = new GTColtanFeature();
        int minY = GTColtanFeature.minY(gen);

        // Outside the radius: nothing at all.
        clearAndFill(level, BASE_X, BASE_Z, minY);
        BlockPos centreFar = new BlockPos(BASE_X + GTColtanFeature.RANGE + 64, 0, BASE_Z);
        helper.assertTrue(!feature.placeField(gen, RandomSource.create(7L), BASE_X, BASE_Z, centreFar),
                "a chunk beyond the field radius must stay untouched");
        helper.assertTrue(countOres(level, BASE_X, BASE_Z, null) == 0, "no ore outside the radius");
        // Inside the radius but beyond 64 blocks: small ores only.
        clearAndFill(level, BASE_X, BASE_Z, minY);
        BlockPos centreNear = new BlockPos(BASE_X + 200, 0, BASE_Z);
        helper.assertTrue(feature.placeField(gen, RandomSource.create(7L), BASE_X, BASE_Z, centreNear),
                "a chunk inside the field radius scatters ore");
        int small = countOres(level, BASE_X, BASE_Z, true);
        int full = countOres(level, BASE_X, BASE_Z, false);
        helper.assertTrue(small > 0, "GT6 scatters small ores across the whole field");
        helper.assertTrue(full == 0, "GT6 only places full ores within 64 blocks, found " + full);

        // On the centre: GT6 runs the SAME small-ore scatter and adds a second full-ore scatter.
        clearAndFill(level, BASE_X, BASE_Z, minY);
        BlockPos centreHere = new BlockPos(BASE_X, 0, BASE_Z);
        helper.assertTrue(GTColtanFeature.inRange(centreHere, BASE_X, BASE_Z), "the centre chunk is in range");
        helper.assertTrue(GTColtanFeature.isCentreChunk(centreHere, BASE_X, BASE_Z), "the centre chunk is detected");
        helper.assertTrue(!GTColtanFeature.isCentreChunk(centreHere, BASE_X + 16, BASE_Z),
                "GT6 compares chunk coordinates, not block distances");
        helper.assertTrue(feature.placeField(gen, RandomSource.create(7L), BASE_X, BASE_Z, centreHere),
                "the centre chunk places ore");
        full = countOres(level, BASE_X, BASE_Z, false);
        small = countOres(level, BASE_X, BASE_Z, true);
        helper.assertTrue(full > 0, "the centre chunk gets full-block ores, small=" + small);
        helper.assertTrue(small > 0, "the centre chunk still gets the small-ore scatter too, small=" + small);

        // The whole band stays inside GT6's y = 20..40 after the depth remap.
        List<String> outside = new ArrayList<>();
        int bandTop = com.gregtech.gregtech.worldgen.GTWorldgenScale.remapY(gen, GTColtanFeature.MAX_Y);
        for (int y = BAND_LOW; y <= BAND_HIGH; y++) {
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    if (isColtanOre(level.getBlockState(new BlockPos(BASE_X + i, y, BASE_Z + j)))
                            && (y < minY || y >= bandTop)) {
                        outside.add("y=" + y);
                    }
                }
            }
        }
        helper.assertTrue(outside.isEmpty(), "ores outside GT6's band: " + outside);
        clearAndFill(level, BASE_X, BASE_Z, minY);
        helper.succeed();
    }

    /** Lays down the stone the field replaces and wipes any ore from a previous run. */
    private static void clearAndFill(ServerLevel level, int minX, int minZ, int minY) {
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int y = BAND_LOW; y <= BAND_HIGH; y++) {
                    level.setBlock(new BlockPos(minX + i, y, minZ + j),
                            y < minY ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
    }

    private static boolean isColtanOre(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null || !id.getNamespace().equals("gregtech")) return false;
        String path = id.getPath();
        return (path.startsWith("ore_") || path.startsWith("ore_small_"))
                && (path.endsWith("_coltan") || path.endsWith("_columbite") || path.endsWith("_tantalite"));
    }

    /** Counts the coltan-family ores in the test chunk, optionally only one of the two block forms. */
    private static int countOres(ServerLevel level, int minX, int minZ, Boolean small) {
        int found = 0;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int y = BAND_LOW; y <= BAND_HIGH; y++) {
                    BlockState state = level.getBlockState(new BlockPos(minX + i, y, minZ + j));
                    if (!isColtanOre(state)) continue;
                    if (small != null) {
                        boolean isSmall = ForgeRegistries.BLOCKS.getKey(state.getBlock()).getPath()
                                .startsWith("ore_small_");
                        if (isSmall != small) continue;
                    }
                    found++;
                }
            }
        }
        return found;
    }
}
