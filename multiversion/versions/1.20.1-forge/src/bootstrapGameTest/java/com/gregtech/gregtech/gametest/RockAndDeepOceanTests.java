package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.worldgen.GTDeepOceanFeature;
import com.gregtech.gregtech.worldgen.GTFeatures;
import com.gregtech.gregtech.worldgen.GTRocksFeature;
import com.gregtech.gregtech.worldgen.GTWorldgenBiomes;
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

import java.util.List;

/**
 * §52: GT6's surface rock litter ({@code WorldgenRocks}, {@code Loader_Worldgen:618}) and its deep
 * ocean prismarine pylons ({@code WorldgenDeepOcean}, {@code Loader_Worldgen:580}).
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class RockAndDeepOceanTests {
    private static final int BASE_X = 54000;
    private static final int BASE_Z = 54000;

    /** GT6's registration values and biome gate for the rock litter. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void rocksMatchGt6Table(GameTestHelper helper) {
        helper.assertTrue(GTRocksFeature.AMOUNT == 2 && GTRocksFeature.PROBABILITY == 3,
                "GT6 registers WorldgenRocks with amount 2 and probability 3");
        helper.assertTrue(GTRocksFeature.METEORIC_CHANCE == 12 && GTRocksFeature.RAW_ORE_CHANCE == 4,
                "GT6 rolls nextInt(12) for meteoric iron and nextInt(4) for its raw ore form");
        helper.assertTrue(GTRocksFeature.METEORIC_IRON.equals("MeteoricIron")
                        && GTRocksFeature.FLINT.equals("minecraft:flint"),
                "the two items GT6's rock litter carries");

        // GT6's canGenerate list: desert, mesa, taiga, swamp, savanna, plains, woods, mountains,
        // wastelands.
        for (String biome : new String[] {"plains", "desert", "badlands", "taiga", "swamp", "savanna",
                "forest", "windswept_hills"}) {
            helper.assertTrue(GTWorldgenBiomes.ROCK_BIOMES.contains(ResourceLocation.withDefaultNamespace(biome)),
                    "GT6 puts rock litter in " + biome);
        }
        for (String biome : new String[] {"deep_ocean", "ocean", "jungle", "bamboo_jungle", "nether_wastes"}) {
            helper.assertTrue(!GTWorldgenBiomes.ROCK_BIOMES.contains(ResourceLocation.withDefaultNamespace(biome)),
                    "GT6 does not put rock litter in " + biome);
        }
        helper.assertTrue(GTWorldgenBiomes.WASTELANDS.isEmpty(),
                "GT6's wastelands have no 1.20.1 vanilla equivalent (documented)");
        helper.assertTrue(GTFeatures.ROCKS.getId().getPath().equals("gt_rocks"), "feature id");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTRocksFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_rocks.json is loaded");
        helper.succeed();
    }

    /** GT6's NBT roll: half the rocks are plain, the rest flint, and 1/12 of those meteoric iron. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void rockLitterRollsMatchGt6(GameTestHelper helper) {
        RandomSource random = RandomSource.create(4242L);
        int plain = 0, flint = 0, meteoric = 0, rawOre = 0;
        int samples = 24000;
        for (int i = 0; i < samples; i++) {
            GTRocksFeature.Litter litter = GTRocksFeature.litterFor(random);
            if (litter.itemId() == null && litter.material() == null) {
                plain++;
            } else if (GTRocksFeature.FLINT.equals(litter.itemId())) {
                flint++;
            } else if (GTRocksFeature.METEORIC_IRON.equals(litter.material())) {
                meteoric++;
                if (litter.rawOre()) rawOre++;
            } else {
                helper.assertTrue(false, "unexpected litter: " + litter);
            }
        }
        // 1/2 plain, 1/2 flint, 1/24 meteoric iron, 1/4 of those raw ore.
        helper.assertTrue(Math.abs(plain - samples / 2) < samples / 20,
                "about half the rocks carry nothing, got " + plain + "/" + samples);
        helper.assertTrue(Math.abs(meteoric - samples / 24) < samples / 10,
                "about 1/24 are meteoric iron, got " + meteoric + "/" + samples);
        helper.assertTrue(flint > meteoric * 5, "flint is by far the most common item, got " + flint);
        helper.assertTrue(rawOre > 0 && rawOre < meteoric,
                "a quarter of the meteoric rocks are raw ore chunks, got " + rawOre + "/" + meteoric);

        // The items the litter names must exist.
        helper.assertTrue(ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse(GTRocksFeature.FLINT)),
                "minecraft:flint is a real item");
        Block rockBlock = ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", "rock"));
        helper.assertTrue(rockBlock != null && rockBlock != Blocks.AIR,
                "gregtech:rock carries every litter variant");
        helper.succeed();
    }

    /** GT6's ray: rocks land on grass/sand, stop at farmland, and never on stone. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void rockRayPlacesRocksOnGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        int minHeight = level.getSeaLevel() - 1;
        int maxHeight = minHeight * 2 + 16;
        Block rockBlock = ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", "rock"));

        // Grass at the test site: the ray finds it and puts a rock on top.
        int x = BASE_X, z = BASE_Z;
        buildColumn(level, x, z, Blocks.GRASS_BLOCK);
        boolean placed = false;
        for (long seed = 0; seed < 40 && !placed; seed++) {
            placed = GTRocksFeature.castRay(gen, x, z, maxHeight, minHeight, RandomSource.create(seed));
        }
        helper.assertTrue(placed, "a grass column gets a rock");
        boolean found = false;
        for (int y = minHeight; y <= maxHeight; y++) {
            if (level.getBlockState(new BlockPos(x, y, z)).is(rockBlock)) {
                found = true;
                break;
            }
        }
        helper.assertTrue(found, "the rock sits on the grass column");

        // Sand works too (GT6 accepts Material.sand).
        int sx = BASE_X + 3;
        buildColumn(level, sx, z, Blocks.SAND);
        placed = false;
        for (long seed = 0; seed < 40 && !placed; seed++) {
            placed = GTRocksFeature.castRay(gen, sx, z, maxHeight, minHeight, RandomSource.create(seed));
        }
        helper.assertTrue(placed, "a sand column gets a rock");

        // Stone is not grass/ground/sand: no rock.
        int tx = BASE_X + 6;
        buildColumn(level, tx, z, Blocks.STONE);
        for (long seed = 0; seed < 20; seed++) {
            helper.assertTrue(!GTRocksFeature.castRay(gen, tx, z, maxHeight, minHeight,
                            RandomSource.create(seed)),
                    "GT6 never litters bare stone");
        }

        // Farmland stops the ray (GT6's explicit check).
        int fx = BASE_X + 9;
        buildColumn(level, fx, z, Blocks.FARMLAND);
        for (long seed = 0; seed < 20; seed++) {
            helper.assertTrue(!GTRocksFeature.castRay(gen, fx, z, maxHeight, minHeight,
                            RandomSource.create(seed)),
                    "GT6 never puts rocks on farmland");
        }
        // §59: GT6's `easyRep` — a rock may also land on a low plant or a snow layer.
        helper.assertTrue(GTRocksFeature.replaceable(Blocks.AIR.defaultBlockState())
                        && GTRocksFeature.replaceable(Blocks.GRASS.defaultBlockState())
                        && GTRocksFeature.replaceable(Blocks.SNOW.defaultBlockState()),
                "air, grass and snow layers are replaceable (GT6's WD.easyRep)");
        helper.assertTrue(!GTRocksFeature.replaceable(Blocks.STONE.defaultBlockState()),
                "solid blocks are not replaceable");
        // A grass column carrying a plant still gets a rock (the plant is replaced).
        int px = BASE_X + 12;
        buildColumn(level, px, z, Blocks.GRASS_BLOCK);
        level.setBlock(new BlockPos(px, minHeight + 1, z), Blocks.GRASS.defaultBlockState(), 2);
        placed = false;
        for (long seed = 0; seed < 40 && !placed; seed++) {
            placed = GTRocksFeature.castRay(gen, px, z, maxHeight, minHeight, RandomSource.create(seed));
        }
        helper.assertTrue(placed, "a rock lands on grass even when a plant is in the way");

        for (int cx : new int[] {x, sx, tx, fx, px}) clearColumn(level, cx, z, minHeight, maxHeight);
        helper.succeed();
    }

    /** GT6's deep ocean pylons: deep ocean only, four mirrored tiers, 1/8 ore sprinkles. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void deepOceanPylonsMatchGt6(GameTestHelper helper) {
        helper.assertTrue(GTDeepOceanFeature.NOISE_OPTIONS == 16
                        && GTDeepOceanFeature.NOISE_XZ == 8
                        && GTDeepOceanFeature.NOISE_Y == 32.0F,
                "GT6 samples noise(minX + 8, 32, minZ + 8, 16)");
        helper.assertTrue(GTDeepOceanFeature.DARK_NOISE_MIN == 12 && GTDeepOceanFeature.DARK_NOISE_MAX == 13
                        && GTDeepOceanFeature.LIGHT_NOISE_MIN == 14 && GTDeepOceanFeature.LIGHT_NOISE_MAX == 15,
                "GT6 builds dark prismarine for noise 12-13 and light for 14-15");
        helper.assertTrue(GTDeepOceanFeature.ORE_CHANCE == 8, "one ore per eight prismarine blocks");
        helper.assertTrue(GTDeepOceanFeature.TIERS.length == 4
                        && GTDeepOceanFeature.TIERS[0].fromL() == 8 && GTDeepOceanFeature.TIERS[0].radius() == 0
                        && GTDeepOceanFeature.TIERS[1].radius() == 1
                        && GTDeepOceanFeature.TIERS[2].radius() == 2
                        && GTDeepOceanFeature.TIERS[3].fromL() == 0 && GTDeepOceanFeature.TIERS[3].radius() == 3,
                "GT6's four tiers: radius 0 (l 8-10), 1 (l 5-7), 2 (l 2-4), 3 (l 0-1)");
        helper.assertTrue(GTFeatures.DEEP_OCEAN.getId().getPath().equals("gt_deep_ocean"), "feature id");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTDeepOceanFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_deep_ocean.json is loaded");

        // Build a pylon on a prepared water column and check the shape.
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        Block dark = com.gregtech.gregtech.registry.GTBlocks.getStone(
                StoneType.PRISMARINE_DARK, com.gregtech.gregtech.block.stone.StoneVariant.STONE);
        helper.assertTrue(dark != null && dark != Blocks.AIR, "the dark prismarine stone block exists");
        int x = BASE_X, z = BASE_Z + 20, y = 34;
        clearPylon(level, x, y, z);
        helper.assertTrue(GTDeepOceanFeature.buildPylon(gen, new BlockPos(x, y, z),
                        StoneType.PRISMARINE_DARK, "Garnierite", RandomSource.create(7L)),
                "the pylon is built");
        helper.assertTrue(isPylonBlock(level.getBlockState(new BlockPos(x, y + 9, z)), dark),
                "the narrow top end sits 9 blocks up");
        helper.assertTrue(isPylonBlock(level.getBlockState(new BlockPos(x, y - 9, z)), dark),
                "the pylon is mirrored downwards");
        helper.assertTrue(isPylonBlock(level.getBlockState(new BlockPos(x + 3, y + 1, z + 3)), dark),
                "the widest ring has radius 3");
        helper.assertTrue(!isPylonBlock(level.getBlockState(new BlockPos(x + 4, y + 1, z)), dark),
                "nothing is placed outside radius 3");
        // The 1/8 ore sprinkles use the pylon's metal (garnierite for dark prismarine).
        int ore = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -10; dy <= 10; dy++) {
                    BlockState state = level.getBlockState(new BlockPos(x + dx, y + dy, z + dz));
                    ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (id != null && id.getPath().equals("ore_garnierite")) ore++;
                }
            }
        }
        helper.assertTrue(ore > 0, "the dark prismarine pylon carries garnierite ore");
        // §58/§103.B: GT6 sprinkles its normal-stone ore variant in, so the recorded host rock is
        // stone — read from the ore's block state property (it used to be the ore block entity).
        int checked = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -10; dy <= 10; dy++) {
                    BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
                    BlockState pylon = level.getBlockState(pos);
                    if (!(pylon.getBlock() instanceof com.gregtech.gregtech.block.OreBlock)) {
                        continue;
                    }
                    com.gregtech.gregtech.block.OreHostStone host =
                            com.gregtech.gregtech.block.OreBlock.stoneOf(pylon);
                    helper.assertTrue(host == com.gregtech.gregtech.block.OreHostStone.STONE,
                            "the pylon ore keeps GT6's normal-stone background, got " + host.id());
                    helper.assertTrue(level.getBlockEntity(pos) == null,
                            "§103.B: a pylon ore block has no block entity");
                    checked++;
                }
            }
        }
        helper.assertTrue(checked > 0, "at least one sprinkled ore was inspected");
        clearPylon(level, x, y, z);
        helper.succeed();
    }

    /**
     * A pylon block is either the prismarine stone or the ore the pylon sprinkles into it — every
     * prismarine block has a 1-in-8 chance of becoming an ore, so the shape checks must accept both.
     */
    private static boolean isPylonBlock(BlockState state, Block prismarine) {
        if (state.is(prismarine)) return true;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && id.getPath().equals("ore_garnierite");
    }

    private static void buildColumn(ServerLevel level, int x, int z, Block ground) {        int minHeight = level.getSeaLevel() - 1;
        int maxHeight = minHeight * 2 + 16;
        for (int y = minHeight; y <= maxHeight; y++) {
            BlockState state = y == minHeight ? ground.defaultBlockState() : Blocks.AIR.defaultBlockState();
            level.setBlock(new BlockPos(x, y, z), state, 2);
        }
    }

    private static void clearColumn(ServerLevel level, int x, int z, int minHeight, int maxHeight) {
        for (int y = minHeight; y <= maxHeight; y++) {
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void clearPylon(ServerLevel level, int x, int y, int z) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = -12; dy <= 12; dy++) {
                    level.setBlock(new BlockPos(x + dx, y + dy, z + dz),
                            Blocks.WATER.defaultBlockState(), 2);
                }
            }
        }
    }
}
