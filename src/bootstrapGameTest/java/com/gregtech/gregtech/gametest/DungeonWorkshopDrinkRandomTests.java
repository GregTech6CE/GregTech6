package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomWorkshop;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

/** Regression for GT6's global-RNG drink selection in the workshop cellar. */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class DungeonWorkshopDrinkRandomTests {
    private static final Set<String> DRINKS = Set.of("Purple_Drink", "Vodka", "Mead",
            "Whiskey_GlenMcKenner", "Wine_Grape_Purple");

    /**
     * GT6 {@code DungeonChunkRoomWorkshop:180} uses {@code UT.Code.select(NF, tDrinks)}. That
     * choice is made by the shared RNG, not the room's {@code aData.next(7)}. Generate actual rooms,
     * require a cellar drink to be chosen, and reject any room-stream seven-way draw. This would
     * catch a return to the old {@code DRINKS[data.next(DRINKS.length)]} implementation.
     */
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void workshopDrinksDoNotAdvanceTheRoomStream(GameTestHelper helper) {
        WorldGenLevel level = helper.getLevel();
        int y = GTDungeonLayout.y(level, GTDungeonLayout.MIN_Y);
        int barrels = 0;
        for (int seed = 1; seed <= 4; seed++) {
            byte[][] layout = new byte[5][5];
            int x = (2350 + seed * 2) * 16;
            int z = 2350 * 16;
            CountingRandomSource roomRandom = new CountingRandomSource(seed);
            GTDungeonData data = new GTDungeonData(level, x, y, z,
                    StoneType.LIMESTONE, StoneType.SLATE, 3, layout, 2, 2, 0,
                    new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                    new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<BlockPos>(),
                    new HashSet<String>(), roomRandom);
            helper.assertTrue(new GTDungeonChunkRoomWorkshop().generate(data),
                    "GT6 workshop generates with seed " + seed);
            helper.assertTrue(roomRandom.sevenWayDraws == 0,
                    "workshop drink selection must not consume the room's next(7), seed " + seed);
            for (int i = 1; i <= 2; i++) for (int j = 12; j <= 13; j++) for (int k = 1; k <= 3; k++) {
                BlockPos pos = new BlockPos(x + i, y + k, z + j);
                String name = String.valueOf(ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()));
                if (!name.equals("gregtech:wood_barrel_treated")
                        && !name.equals("gregtech:wood_barrel_ironwood")) continue;
                barrels++;
                helper.assertTrue(level.getBlockEntity(pos) instanceof TankBlockEntity,
                        "a GT6 drink barrel has its tank entity");
                var fluid = ((TankBlockEntity) level.getBlockEntity(pos)).getFluidTank().getFluid();
                int expectedAmount = name.equals("gregtech:wood_barrel_ironwood") ? 32000 : 16000;
                helper.assertTrue(fluid.getAmount() == expectedAmount,
                        "GT6 cellar barrel holds " + expectedAmount + " mB");
                boolean allowed = false;
                for (String drink : DRINKS) {
                    if (fluid.getFluid() == GTFluids.stack(drink, 1).getFluid()) allowed = true;
                }
                helper.assertTrue(allowed, "cellar barrel holds a GT6 drink");
            }
        }
        helper.assertTrue(barrels > 0, "the generated cellar exercised GT6's global drink selection");
        helper.succeed();
    }

    private static final class CountingRandomSource implements RandomSource {
        private final RandomSource delegate;
        int sevenWayDraws;

        CountingRandomSource(long seed) { delegate = RandomSource.create(seed); }
        @Override public RandomSource fork() { return delegate.fork(); }
        @Override public PositionalRandomFactory forkPositional() { return delegate.forkPositional(); }
        @Override public void setSeed(long seed) { delegate.setSeed(seed); }
        @Override public int nextInt() { return delegate.nextInt(); }
        @Override public int nextInt(int bound) {
            if (bound == 7) sevenWayDraws++;
            return delegate.nextInt(bound);
        }
        @Override public long nextLong() { return delegate.nextLong(); }
        @Override public boolean nextBoolean() { return delegate.nextBoolean(); }
        @Override public float nextFloat() { return delegate.nextFloat(); }
        @Override public double nextDouble() { return delegate.nextDouble(); }
        @Override public double nextGaussian() { return delegate.nextGaussian(); }
    }
}
