package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.worldgen.GTBedrockOres;
import com.gregtech.gregtech.worldgen.GTBedrockOreDimensions;
import com.gregtech.gregtech.worldgen.GTNetherDepositFeature;
import com.gregtech.gregtech.worldgen.GTOreBlockResolver;
import com.gregtech.gregtech.worldgen.GTOreVeins;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * §50: the parts of GT6's nether worldgen the port still approximated — the nether bedrock ore
 * table ({@code WorldgenOresBedrock}, {@code Loader_Worldgen:758-764}), the exact
 * {@code WorldgenNetherCrystals} ceiling rules, plus the one small ore GT6 had and the port did not
 * ({@code ore.small.eudialyte}, {@code Loader_Worldgen:829}).
 *
 * <p>The last test is the important one: it proves every entry of GT6's bedrock and small-ore tables
 * resolves to a real {@code ore_*} / {@code ore_small_*} block in the port, so nothing in those
 * tables is silently skipped at runtime.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class NetherWorldgenTests {
    private static final int BASE_X = 52000;
    private static final int BASE_Z = 52000;

    /** GT6's nether bedrock ore table, and the dimension switch that selects it. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void netherBedrockOresMatchGt6(GameTestHelper helper) {
        helper.assertTrue(GTBedrockOres.NETHER.size() == 7,
                "Loader_Worldgen:758-764 registers 7 nether bedrock ores, got " + GTBedrockOres.NETHER.size());
        String[][] expected = {
                {"voidquartz", "4000"}, {"glowstone", "4000"}, {"gloomstone", "4000"},
                {"efrine", "2000"}, {"netherquartz", "2000"}, {"firestone", "8000"},
                {"ancientdebris", "4000"}};
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < expected.length; i++) {
            GTBedrockOres.BedrockOre ore = GTBedrockOres.NETHER.get(i);
            if (!ore.name().equals(expected[i][0])) {
                problems.add("entry " + i + ": expected " + expected[i][0] + ", got " + ore.name());
            } else if (ore.chance() != Integer.parseInt(expected[i][1])) {
                problems.add(ore.name() + ": GT6 uses " + expected[i][1] + ", got " + ore.chance());
            } else if (ore.flowerId() != null) {
                problems.add(ore.name() + ": GT6 registers the nether deposits without a flower");
            }
        }
        helper.assertTrue(problems.isEmpty(), "nether bedrock table: " + problems);

        // The feature must pick the table of the level's dimension.
        helper.assertTrue(GTBedrockOreDimensions.forDimension(Level.NETHER) == GTBedrockOres.NETHER,
                "the nether uses the nether table");
        helper.assertTrue(GTBedrockOreDimensions.forDimension(Level.OVERWORLD) == GTBedrockOres.OVERWORLD,
                "the overworld keeps its 28 entries");
        helper.assertTrue(GTBedrockOreDimensions.forDimension(Level.END).isEmpty(), "GT6 has no end bedrock ores");
        helper.assertTrue(GTBedrockOres.OVERWORLD.size() == 28, "the overworld table is unchanged");
        // GT6's nether entries use the same rarity divisors as the overworld ones: bigger = rarer.
        helper.assertTrue(GTBedrockOres.NETHER.get(5).chance() > GTBedrockOres.NETHER.get(3).chance(),
                "firestone (1/8000) is rarer than efrine (1/2000)");
        // The ancient debris vein is GT6's only nether material that also appears on Mars.
        helper.assertTrue(GTBedrockOres.NETHER.stream()
                        .anyMatch(o -> o.material() == Materials.AncientDebris),
                "ancient debris is in the nether table");
        helper.succeed();
    }

    /** GT6's crystal ceiling rules: rock only, no nether bricks, and the ceiling block is replaced. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void netherCrystalsReplaceTheCeiling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        Block crystal = ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", "crystal_ore_cinnabar"));
        helper.assertTrue(crystal != null && crystal != Blocks.AIR, "gregtech:crystal_ore_cinnabar exists");
        BlockState crystalState = crystal.defaultBlockState();

        // A prepared cave: stone floor at the lava sea, air, then a netherrack ceiling 20 blocks up.
        int x = BASE_X, z = BASE_Z;
        int ceilingY = GTNetherDepositFeature.NETHER_WATER_LEVEL + 20;
        buildCave(level, x, z, ceilingY, Blocks.NETHERRACK);
        helper.assertTrue(GTNetherDepositFeature.placeCrystalAt(gen, x, z, crystalState,
                        RandomSource.create(11L)),
                "a netherrack ceiling 20 blocks above the lava sea gets a crystal");
        helper.assertTrue(level.getBlockState(new BlockPos(x, ceilingY, z)).is(crystal),
                "GT6 replaces the ceiling block itself, not the air below it");
        helper.assertTrue(!level.getBlockState(new BlockPos(x, ceilingY, z)).is(Blocks.NETHERRACK),
                "the netherrack ceiling block is gone (the old port code seeded the air below it)");
        // The cluster hangs from the seed, so a crystal sits somewhere below the ceiling.
        boolean grewDown = false;
        for (int y = ceilingY - 1; y > GTNetherDepositFeature.NETHER_WATER_LEVEL; y--) {
            if (level.getBlockState(new BlockPos(x, y, z)).is(crystal)) {
                grewDown = true;
                break;
            }
        }
        helper.assertTrue(grewDown, "the cluster grows down from the ceiling crystal");

        // Nether bricks are rejected by GT6 even though they are rock.
        int bx = BASE_X + 3;
        buildCave(level, bx, z, ceilingY, Blocks.NETHER_BRICKS);
        helper.assertTrue(!GTNetherDepositFeature.placeCrystalAt(gen, bx, z, crystalState, RandomSource.create(11L)),
                "GT6 rejects a nether brick ceiling");
        helper.assertTrue(level.getBlockState(new BlockPos(bx, ceilingY, z)).is(Blocks.NETHER_BRICKS),
                "the nether brick ceiling is untouched");

        // Glass-like ceilings are not rock: glowstone and soul sand are both rejected.
        for (int i = 0; i < 2; i++) {
            int gx = BASE_X + 6 + i * 3;
            Block ceilingBlock = i == 0 ? Blocks.GLOWSTONE : Blocks.SOUL_SAND;
            buildCave(level, gx, z, ceilingY, ceilingBlock);
            helper.assertTrue(!GTNetherDepositFeature.placeCrystalAt(gen, gx, z, crystalState,
                            RandomSource.create(11L)),
                    "a " + ceilingBlock.getName().getString() + " ceiling is not Material.rock");
        }
        helper.assertTrue(!GTNetherDepositFeature.isRockMaterial(Blocks.GLOWSTONE.defaultBlockState())
                        && !GTNetherDepositFeature.isRockMaterial(Blocks.NETHER_BRICKS.defaultBlockState())
                        && GTNetherDepositFeature.isRockMaterial(Blocks.NETHERRACK.defaultBlockState())
                        && GTNetherDepositFeature.isRockMaterial(Blocks.BLACKSTONE.defaultBlockState()),
                "the rock test matches GT6's Material.rock (netherrack/blackstone yes, bricks/glowstone no)");

        // A ceiling too close to the lava sea fails GT6's height gate.
        int sx = BASE_X + 12;
        buildCave(level, sx, z, GTNetherDepositFeature.NETHER_WATER_LEVEL + 5, Blocks.NETHERRACK);
        helper.assertTrue(!GTNetherDepositFeature.placeCrystalAt(gen, sx, z, crystalState, RandomSource.create(11L)),
                "GT6 needs at least 11 blocks of air above the lava sea");

        for (int cx : new int[] {x, bx, BASE_X + 6, BASE_X + 9, sx}) clearCave(level, cx, z, ceilingY);
        helper.succeed();
    }

    /** The cluster grows only into air that already touches a crystal (GT6's 1500 picks). */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void crystalClusterGrowsFromTheSeed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        Block crystal = ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", "crystal_ore_pyrite"));
        BlockState crystalState = crystal.defaultBlockState();
        int x = BASE_X + 20, z = BASE_Z;
        int ceilingY = GTNetherDepositFeature.NETHER_WATER_LEVEL + 20;
        buildCave(level, x, z, ceilingY, Blocks.NETHERRACK);
        helper.assertTrue(GTNetherDepositFeature.placeCrystalAt(gen, x, z, crystalState, RandomSource.create(5L)),
                "the seed crystal is placed");

        int count = 0;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                for (int y = ceilingY - 12; y <= ceilingY + 1; y++) {
                    if (level.getBlockState(new BlockPos(x + dx, y, z + dz)).is(crystal)) count++;
                }
            }
        }
        helper.assertTrue(count > 1, "the cluster grew beyond the seed block, found " + count);
        // Everything the cluster added must hang from the seed: no crystal floats on its own.
        int floating = 0;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                for (int y = ceilingY - 12; y <= ceilingY; y++) {
                    BlockPos pos = new BlockPos(x + dx, y, z + dz);
                    if (!level.getBlockState(pos).is(crystal)) continue;
                    boolean attached = false;
                    for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                        if (level.getBlockState(pos.relative(dir)).is(crystal)) {
                            attached = true;
                            break;
                        }
                    }
                    if (!attached) floating++;
                }
            }
        }
        helper.assertTrue(floating == 0, "every crystal must touch another crystal, " + floating + " did not");
        clearCave(level, x, z, ceilingY);
        helper.succeed();
    }

    /**
     * Every entry of GT6's tables must resolve to an ore block — this is the guard against entries
     * that generate nothing because the port has no {@code ore_<material>} block for them.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void everyWorldgenTableEntryResolves(GameTestHelper helper) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        List<String> missing = new ArrayList<>();
        for (GTBedrockOres.BedrockOre ore : GTBedrockOres.OVERWORLD) {
            if (GTOreBlockResolver.resolve(stone, ore.material(), false) == null) {
                missing.add("bedrock/" + ore.name());
            }
        }
        for (GTBedrockOres.BedrockOre ore : GTBedrockOres.NETHER) {
            if (GTOreBlockResolver.resolve(stone, ore.material(), false) == null) {
                missing.add("nether-bedrock/" + ore.name());
            }
        }
        checkSmall(missing, stone, GTOreVeins.OVERWORLD_SMALL_ORES, "overworld");
        checkSmall(missing, stone, GTOreVeins.NETHER_SMALL_ORES, "nether");
        checkSmall(missing, stone, GTOreVeins.END_SMALL_ORES, "end");
        helper.assertTrue(missing.isEmpty(), "worldgen entries without an ore block: " + missing);

        // GT6's ore.small.eudialyte (Loader_Worldgen:829), which the port used to skip.
        boolean eudialyte = GTOreVeins.OVERWORLD_SMALL_ORES.stream()
                .anyMatch(o -> o.name().equals("eudialyte") && o.minY() == 20 && o.maxY() == 40 && o.amount() == 4);
        helper.assertTrue(eudialyte, "the overworld table carries GT6's eudialyte entry (20/40, amount 4)");
        helper.assertTrue(GTOreVeins.OVERWORLD_SMALL_ORES.size() == 36,
                "the overworld small ore table now has 36 always-on entries, got "
                        + GTOreVeins.OVERWORLD_SMALL_ORES.size());
        // The small-ore counts match GT6's per-dimension registration counts (minus other-mod gates).
        helper.assertTrue(GTOreVeins.NETHER_SMALL_ORES.size() == 18, "the nether table matches GT6's 18");
        helper.assertTrue(GTOreVeins.END_SMALL_ORES.size() == 32, "the end table matches GT6's 32");
        helper.succeed();
    }

    private static void checkSmall(List<String> missing, BlockState stone,
                                   List<GTOreVeins.SmallOre> ores, String table) {
        for (GTOreVeins.SmallOre ore : ores) {
            if (GTOreBlockResolver.resolve(stone, ore.material(), true) == null) {
                missing.add(table + "-small/" + ore.name());
            }
        }
    }

    /** Cuts a 19x19 cave: stone up to the lava sea, air above it, and the given ceiling block. */
    private static void buildCave(ServerLevel level, int x, int z, int ceilingY, Block ceilingBlock) {
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                for (int y = GTNetherDepositFeature.NETHER_WATER_LEVEL - 1;
                     y <= Math.max(ceilingY, GTNetherDepositFeature.NETHER_WATER_LEVEL + 25); y++) {
                    BlockState state = y == ceilingY
                            ? ceilingBlock.defaultBlockState()
                            : (y < GTNetherDepositFeature.NETHER_WATER_LEVEL
                                    ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                    level.setBlock(new BlockPos(x + dx, y, z + dz), state, 2);
                }
            }
        }
    }

    private static void clearCave(ServerLevel level, int x, int z, int ceilingY) {
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                for (int y = GTNetherDepositFeature.NETHER_WATER_LEVEL - 1;
                     y <= Math.max(ceilingY, GTNetherDepositFeature.NETHER_WATER_LEVEL + 25) + 1; y++) {
                    level.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }
}
