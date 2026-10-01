package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.ConcreteBlock;
import com.gregtech.gregtech.block.misc.ColoredGlassBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;

/** Dungeon colour metadata in GT6 is black=0, white=15, the reverse of vanilla's old block metadata. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DungeonColorPaletteTests {
    private static final DyeColor[] GT6_ORDER = {
            DyeColor.BLACK, DyeColor.RED, DyeColor.GREEN, DyeColor.BROWN,
            DyeColor.BLUE, DyeColor.PURPLE, DyeColor.CYAN, DyeColor.LIGHT_GRAY,
            DyeColor.GRAY, DyeColor.PINK, DyeColor.LIME, DyeColor.YELLOW,
            DyeColor.LIGHT_BLUE, DyeColor.MAGENTA, DyeColor.ORANGE, DyeColor.WHITE};

    private static GTDungeonData data(GameTestHelper helper, int color) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return new GTDungeonData(helper.getLevel(), origin.getX(), origin.getY(), origin.getZ(),
                StoneType.LIMESTONE, StoneType.SLATE, color, new byte[5][5], 2, 2, 0,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                RandomSource.create(61));
    }

    @GameTest(template = "test_empty")
    public static void dungeonConcreteUsesGt6ColoredBlockAndMetadata(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int i = 0; i < GT6_ORDER.length; i++) {
            helper.assertTrue(data(helper, i).colored(1, 1, 1), "concrete placed for GT6 dye " + i);
            var state = helper.getLevel().getBlockState(pos);
            helper.assertTrue(state.is(GTDecorBlocks.CONCRETE.get())
                            && state.getValue(ConcreteBlock.COLOR) == GT6_ORDER[i],
                    "GT6 concrete metadata " + i + " is " + GT6_ORDER[i]);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void dungeonGlassUsesGt6DyeOrder(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int i = 0; i < GT6_ORDER.length; i++) {
            helper.assertTrue(data(helper, i).glass(1, 1, 1), "glass placed for GT6 dye " + i);
            var state = helper.getLevel().getBlockState(pos);
            helper.assertTrue(state.is(GTDecorBlocks.GLASS_CLEAR.get())
                            && state.getValue(ColoredGlassBlock.COLOR) == GT6_ORDER[i],
                    "GT6 clear glass metadata " + i + " is " + GT6_ORDER[i]);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void dungeonGlowingGlassKeepsColorAndFullBrightness(GameTestHelper helper) {
        GTDungeonData red = data(helper, 1);
        helper.assertTrue(red.glassglow(1, 1, 1), "glowing glass placed");
        var state = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(1, 1, 1)));
        helper.assertTrue(state.is(GTDecorBlocks.GLASS_GLOW.get())
                        && state.getValue(ColoredGlassBlock.COLOR) == DyeColor.RED
                        && state.getLightEmission() == 15,
                "GT6 glowing glass colour and light are both preserved");
        helper.succeed();
    }
}
