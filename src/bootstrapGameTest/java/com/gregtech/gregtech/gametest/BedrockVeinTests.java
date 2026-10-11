package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.worldgen.GTBedrockOreFeature;
import net.minecraft.core.BlockPos;
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

/**
 * §56: GT6's bedrock vein shape ({@code WorldgenOresBedrock.generateVein}) — a chunk-anchored
 * deposit: bedrock core in the 6x6 area around the chunk centre, the "muffin" blob of layers y=1..6
 * whose bounds come from GT6's own {@code tD1}/{@code tD2} arrays, and the random-walk sprinkle up
 * to the water level.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BedrockVeinTests {
    private static final int BASE_X = 58000;
    private static final int BASE_Z = 58000;

    /** GT6's layer bounds, verbatim. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void muffinBoundsMatchGt6(GameTestHelper helper) {
        helper.assertTrue(GTBedrockOreFeature.MUFFIN_INNER.length == 7
                        && GTBedrockOreFeature.MUFFIN_OUTER.length == 7,
                "GT6's tD1/tD2 arrays have seven entries (index 0 unused)");
        int[] inner = {5, 4, 2, 1, 0, 2, 5};
        int[] outer = {11, 12, 14, 15, 16, 14, 11};
        for (int i = 0; i < inner.length; i++) {
            helper.assertTrue(GTBedrockOreFeature.MUFFIN_INNER[i] == inner[i]
                            && GTBedrockOreFeature.MUFFIN_OUTER[i] == outer[i],
                    "layer " + i + " must be " + inner[i] + ".." + outer[i]);
        }
        // The widest layer is the middle one: the full 16x16 chunk at y = 4.
        helper.assertTrue(GTBedrockOreFeature.MUFFIN_INNER[4] == 0
                        && GTBedrockOreFeature.MUFFIN_OUTER[4] == 16,
                "the vein is a full chunk wide in its middle layer");
        helper.assertTrue(GTBedrockOreFeature.MUFFIN_OUTER[6] - GTBedrockOreFeature.MUFFIN_INNER[6] == 6,
                "the top layer is 6x6 like the bedrock core");
        helper.succeed();
    }

    /** The vein really is placed: bedrock core, muffin layers, and small ores above. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void veinFollowsGt6Shape(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        int floor = level.getMinBuildHeight();
        int minX = BASE_X, minZ = BASE_Z;

        // Prepare a chunk column area: bedrock floor, deepslate above it, clear air higher up.
        // The four layers the "sprinkle stops at the water level" check looks at are cleared too: on a
        // world that was never generated before, the chunk carries its own natural GT ore veins up
        // there, and counting those would make this test depend on whether an earlier run had already
        // touched the chunk (it used to pass only on a reused world).
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                level.setBlock(new BlockPos(minX + x, floor, minZ + z), Blocks.BEDROCK.defaultBlockState(), 2);
                for (int y = floor + 1; y <= level.getSeaLevel(); y++) {
                    level.setBlock(new BlockPos(minX + x, y, minZ + z),
                            Blocks.DEEPSLATE.defaultBlockState(), 2);
                }
                for (int y = level.getSeaLevel() + 1; y <= level.getSeaLevel() + 4; y++) {
                    level.setBlock(new BlockPos(minX + x, y, minZ + z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        int seaLevel = level.getSeaLevel();
        boolean placed = GTBedrockOreFeature.placeVein(gen, RandomSource.create(1234L), minX, minZ, Materials.Gold);
        helper.assertTrue(placed, "the vein places ore");

        // The bedrock core: at least one gold ore in the 6x6 area, with the bedrock background.
        int coreOres = 0;
        for (int x = 5; x < 11; x++) {
            for (int z = 5; z < 11; z++) {
                BlockPos pos = new BlockPos(minX + x, floor, minZ + z);
                BlockState core = level.getBlockState(pos);
                if (isOre(core)) {
                    coreOres++;
                    // §103.B: the bedrock background is the ore's `stone` block-state property now.
                    helper.assertTrue(com.gregtech.gregtech.block.OreBlock.isBedrockOre(core),
                            "the core ore keeps the bedrock background");
                    helper.assertTrue(level.getBlockEntity(pos) == null,
                            "§103.B: a bedrock ore has no block entity");
                }
            }
        }
        helper.assertTrue(coreOres >= 1, "the bedrock core has ore, got " + coreOres);

        // The muffin: the middle layer reaches the chunk edges, the layers above it do not.
        helper.assertTrue(isOre(level.getBlockState(new BlockPos(minX, floor + 4, minZ)))
                        || isOre(level.getBlockState(new BlockPos(minX + 15, floor + 4, minZ))),
                "the middle layer spans the whole chunk");
        helper.assertTrue(!isOre(level.getBlockState(new BlockPos(minX, floor + 1, minZ))),
                "layer 1 stays inside its 4..11 bounds");
        helper.assertTrue(!isOre(level.getBlockState(new BlockPos(minX, floor + 6, minZ))),
                "the top layer stays inside 5..10");

        // Nothing above the water level (GT6's sprinkle stops there).
        int above = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = seaLevel; y <= seaLevel + 4; y++) {
                    if (isOre(level.getBlockState(new BlockPos(minX + x, y, minZ + z)))) above++;
                }
            }
        }
        helper.assertTrue(above == 0, "no ore above the water level, found " + above);

        // A chunk without bedrock gets no vein at all.
        int bx = BASE_X + 32;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                level.setBlock(new BlockPos(bx + x, floor, minZ + z), Blocks.DEEPSLATE.defaultBlockState(), 2);
            }
        }
        helper.assertTrue(!GTBedrockOreFeature.placeVein(gen, RandomSource.create(1234L), bx, minZ, Materials.Gold),
                "GT6 needs existing bedrock at the vein's centre");

        // Clear the site so repeated runs start clean.
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                level.setBlock(new BlockPos(minX + x, floor, minZ + z), Blocks.DEEPSLATE.defaultBlockState(), 2);
                for (int y = floor + 1; y <= seaLevel + 4; y++) {
                    level.setBlock(new BlockPos(minX + x, y, minZ + z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        helper.succeed();
    }

    private static boolean isOre(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && id.getNamespace().equals("gregtech") && id.getPath().startsWith("ore");
    }
}
