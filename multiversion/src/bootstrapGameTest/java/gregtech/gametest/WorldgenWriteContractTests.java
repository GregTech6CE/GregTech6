package gregtech.gametest;

import gregtech.worldgen.GTPitFeature;
import gregtech.worldgen.GTWaterBodyFeature;
import gregtech.worldgen.center.NativeOriginWorld;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class WorldgenWriteContractTests {
    /** Native block states, tags and adapter; no classification double. */
    @GameTest(template = "test_empty", timeoutTicks = 30)
    public static void nativeOriginWoodClassification(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 3, 2));
        var world = new NativeOriginWorld(h.getLevel(), new ChunkPos(pos), pos.getY());
        var blocks = new net.minecraft.world.level.block.Block[] {
                Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.OAK_FENCE, Blocks.OAK_SLAB,
                Blocks.OAK_STAIRS, Blocks.STONE, Blocks.OAK_LEAVES, Blocks.NETHER_BRICK_FENCE};
        for (int i = 0; i < blocks.length; i++) {
            h.getLevel().setBlock(pos, blocks[i].defaultBlockState(), 2);
            var actual = world.getBlock(pos.getX(), pos.getY(), pos.getZ());
            h.assertTrue(actual.wood() == (i < 5), "native road wood classification: " + blocks[i]);
            h.assertTrue(actual.leaves() == (blocks[i] == Blocks.OAK_LEAVES), "leaf identity remains separate");
        }
        h.assertTrue(!Blocks.OAK_PLANKS.defaultBlockState().is(BlockTags.LOGS),
                "wooden material classification does not redefine log tags");
        h.succeed();
    }

    /** A fault-injection writer: all reads and accepted writes use the actual ServerLevel.
     * A rejection is controlled here, NOT evidence that an ordinary generated chunk refused it.
     */
    private static WorldGenLevel rejectingWriter(GameTestHelper h, Predicate<BlockPos> reject) {
        return (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[] {WorldGenLevel.class}, (proxy, method, args) -> {
                    // Match the setBlock overload signature rather than its mapped method name.
                    if (method.getReturnType() == boolean.class && args != null && args.length >= 3
                            && args[0] instanceof BlockPos pos
                            && args[1] instanceof net.minecraft.world.level.block.state.BlockState
                            && args[2] instanceof Integer && reject.test(pos)) return false;
                    try {
                        return method.invoke(h.getLevel(), args);
                    } catch (InvocationTargetException error) {
                        throw error.getCause();
                    }
                });
    }

    @GameTest(template = "test_empty", timeoutTicks = 30)
    public static void waterFaultInjectionPreservesSurfaceLevels(GameTestHelper h) {
        var top = h.absolutePos(new BlockPos(2, 5, 2));
        var fill = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "seawater"))
                .defaultBlockState();
        h.assertTrue(fill.getBlock() instanceof LiquidBlock, "registered seawater liquid block");
        for (int i = 0; i < 3; i++) h.getLevel().setBlock(top.below(i),
                Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 7 - i * 2), 2);
        h.getLevel().setBlock(top.below(3), Blocks.STONE.defaultBlockState(), 2);
        var rejectAll = rejectingWriter(h, pos -> true);
        h.assertTrue(!GTWaterBodyFeature.convertColumn(rejectAll, top.getX(), top.getZ(), top.getY(), fill, false),
                "fault injection: no accepted writes means false");
        h.assertTrue(h.getLevel().getBlockState(top).is(Blocks.WATER), "rejected source unchanged");
        var rejectTop = rejectingWriter(h, top::equals);
        h.assertTrue(GTWaterBodyFeature.convertColumn(rejectTop, top.getX(), top.getZ(), top.getY(), fill, false),
                "fault injection: later accepted writes mean true");
        h.assertTrue(h.getLevel().getBlockState(top).is(Blocks.WATER), "top rejection does not mutate native state");
        h.assertTrue(h.getLevel().getBlockState(top.below()).getValue(LiquidBlock.LEVEL) == 5,
                "failed surface write does not promote deeper flowing water to source");
        h.assertTrue(h.getLevel().getBlockState(top.below(2)).getValue(LiquidBlock.LEVEL) == 3,
                "later accepted conversion preserves original flowing level");
        h.assertTrue(GTWaterBodyFeature.convertColumn(h.getLevel(), top.getX(), top.getZ(), top.getY(), fill, false),
                "native writer converts the remaining top block");
        h.assertTrue(!GTWaterBodyFeature.convertColumn(h.getLevel(), top.getX(), top.getZ(), top.getY(), fill, false),
                "already converted native column is idempotent");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 30)
    public static void pitFaultInjectionPreservesAttemptDepth(GameTestHelper h) {
        var top = h.absolutePos(new BlockPos(2, 10, 2));
        h.getLevel().setBlock(top.above(), Blocks.AIR.defaultBlockState(), 2);
        h.getLevel().setBlock(top, Blocks.SAND.defaultBlockState(), 2);
        for (int i = 1; i < 8; i++) h.getLevel().setBlock(top.below(i), Blocks.STONE.defaultBlockState(), 2);
        var fill = Blocks.CLAY.defaultBlockState();
        var attempted = new java.util.ArrayList<BlockPos>();
        var rejectAll = rejectingWriter(h, pos -> { attempted.add(pos.immutable()); return true; });
        h.assertTrue(!GTPitFeature.carveColumn(rejectAll, top.getX(), top.getZ(), top.getY(), top.getY() - 7, fill),
                "fault injection: all writes rejected means false");
        h.assertTrue(attempted.size() == 7 && !attempted.contains(top.below(7)),
                "failed writes still consume original seven-layer attempt budget");
        var rejectTop = rejectingWriter(h, top::equals);
        h.assertTrue(GTPitFeature.carveColumn(rejectTop, top.getX(), top.getZ(), top.getY(), top.getY() - 7, fill),
                "fault injection: accepted lower writes report success without changing host policy");
        h.assertTrue(h.getLevel().getBlockState(top).is(Blocks.SAND), "rejected top stays sand");
        for (int i = 1; i < 7; i++) h.assertTrue(h.getLevel().getBlockState(top.below(i)).is(Blocks.CLAY),
                "baseline eligible-attempt policy continues into lower stone");
        h.assertTrue(h.getLevel().getBlockState(top.below(7)).is(Blocks.STONE),
                "rejected first write does not extend the depth budget");
        h.assertTrue(GTPitFeature.carveColumn(h.getLevel(), top.getX(), top.getZ(), top.getY(), top.getY() - 7, fill),
                "native writer fills the remaining top position");
        h.assertTrue(!GTPitFeature.carveColumn(h.getLevel(), top.getX(), top.getZ(), top.getY(), top.getY() - 7, fill),
                "already filled depth budget has no new successful write");
        h.succeed();
    }
}
