package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.bootstrap.registration.OriginCreativeContents;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;

/** Exact registry/placement contracts behind issues #64 and #79; not a client visual test. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class OpenIssueRegressionTests {
    @GameTest(template="test_empty")
    public static void extruderShapesDoNotSplitTheMachineCreativeFamily(GameTestHelper h) {
        int shapes = 0;
        for (var item : ForgeRegistries.ITEMS.getValues()) {
            var id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null || !id.getNamespace().equals("gt6ce")) continue;
            if (id.getPath().startsWith("extruder_shape_") || id.getPath().startsWith("low_heat_extruder_shape_")) {
                h.assertTrue(OriginCreativeContents.family(item).equals("molds"), "shape belongs to molds: " + id);
                shapes++;
            }
        }
        h.assertTrue(shapes > 0, "actual shape registrations were checked");
        var invar = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gt6ce", "extruder_invar"));
        h.assertTrue(invar != null && OriginCreativeContents.family(invar).equals("basic_machines"),
                "invar extruder stays with machines rather than its shapes");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void dungeonWaterPlacementSchedulesFlowOnlyAfterSuccessfulWrite(GameTestHelper h) {
        boolean[] success = {true};
        var ticks = new ArrayList<Object[]>();
        WorldGenLevel recorder = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("setBlock")) return success[0];
                    if (method.getName().equals("scheduleTick")) { ticks.add(args.clone()); return null; }
                    throw new AssertionError("unexpected world access: " + method.getName());
                });
        var data = new GTDungeonData(recorder, 32, 40, 48, StoneType.LIMESTONE, StoneType.SLATE, 3,
                new byte[5][5], 2, 2, 0, new long[GTDungeonFeature.KEY_COUNT],
                new ItemStack[GTDungeonFeature.KEY_COUNT], new boolean[GTDungeonFeature.KEY_COUNT],
                new HashSet<>(), new HashSet<>(), RandomSource.create(79));
        h.assertTrue(data.set(1, 10, 1, Blocks.WATER.defaultBlockState()), "source placement succeeds");
        h.assertTrue(ticks.size() == 1 && ticks.get(0)[0].equals(new BlockPos(33, 50, 49))
                        && ticks.get(0)[1] == Fluids.WATER && ((Integer) ticks.get(0)[2]) > 0,
                "source gets a deferred water tick at the exact world position");
        data.set(2, 10, 1, Blocks.STONE.defaultBlockState());
        h.assertTrue(ticks.size() == 1, "dry blocks do not schedule fluid ticks");
        success[0] = false;
        h.assertTrue(!data.set(3, 10, 1, Blocks.WATER.defaultBlockState()), "failed placement is preserved");
        h.assertTrue(ticks.size() == 1, "failed water writes do not schedule phantom ticks");
        h.succeed();
    }
}
