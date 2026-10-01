package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.EnergyNet;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity;
import com.gregtech.gregtech.loaders.Loader_OvenRecipes;
import com.gregtech.gregtech.registry.GTWires;
import com.gregtech.gregtech.world.GarbageData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Growth invariants for the three structures that could grow with play time instead of with the world's
 * content. Each test measures a size after driving the operation that used to grow it, and fails when
 * the size keeps climbing:
 *
 * <ul>
 *   <li>{@link #removedWireBlockEntityReleasesItsPosition()},
 *       {@link #wireGraphDoesNotGrowAcrossUnloadCycles()} and
 *       {@link #removedWireReleasesTheNeighbourEntryThatPointedAtIt()} measure {@link EnergyNet}: the
 *       per-dimension adjacency map is registered into on placement and used to be released only from
 *       {@code Block#onRemove}, so a chunk unload - which calls {@code onChunkUnloaded} and
 *       {@code setRemoved} ({@code LevelChunk#clearAllBlockEntities:574-577}) - left one entry per wire
 *       position of every chunk the player had ever visited.</li>
 *   <li>{@link #garbagePileDoesNotGrowWithoutBound()} measures {@link GarbageData}, which merges piles
 *       by {@code ItemStack.isSameItemSameTags}: a stack with per-instance NBT never merged, so every
 *       voided stack added a permanent pile.</li>
 *   <li>{@link #ovenMirrorReportIsPerPass()} measures {@code Loader_OvenRecipes.ADDED_TO_VANILLA},
 *       which used to accumulate one {@link ItemStack} per mirrored row of every world load.</li>
 * </ul>
 */
@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class MemoryGrowthTests {

    private static ElectricWireBlock wire() {
        return GTWires.all().get(0).get();
    }

    /** Places a bare wire (all connections off, so it adds no neighbour entries) and returns its position. */
    private static BlockPos placeWire(GameTestHelper h, BlockPos pos) {
        var state = wire().defaultBlockState();
        for (var property : ElectricWireBlock.CONNECTIONS) state = state.setValue(property, false);
        h.getLevel().setBlockAndUpdate(pos, state);
        return pos;
    }

    @GameTest(template = "test_blueprint_empty")
    public static void removedWireBlockEntityReleasesItsPosition(GameTestHelper h) {
        var level = h.getLevel();
        var pos = placeWire(h, h.absolutePos(new BlockPos(3, 3, 3)));
        var entity = level.getBlockEntity(pos);
        h.assertTrue(entity instanceof ElectricWireBlockEntity,
                "placing the wire block created its block entity, got " + entity);
        // ElectricWireBlockEntity#onLoad registers the position; called explicitly so the test does not
        // depend on when a game test level loads its block entities.
        EnergyNet.onPlace(level, pos);
        h.assertTrue(EnergyNet.tracks(level, pos), "a placed wire is registered with EnergyNet");

        // Vanilla's chunk unload calls exactly this pair on every block entity of the chunk before it
        // drops them from the chunk map (LevelChunk#clearAllBlockEntities:574-577).
        ((ElectricWireBlockEntity) entity).onChunkUnloaded();
        h.assertTrue(!EnergyNet.tracks(level, pos), "the unload hook must hand the position back, not"
                + " retain it for the lifetime of the level (tracked "
                + EnergyNet.trackedPositions(level) + ")");
        ((ElectricWireBlockEntity) entity).setRemoved();
        h.assertTrue(!EnergyNet.tracks(level, pos), "the removal hook must leave it released");

        // No wire is left in the test area, and the block path cannot mask the two hooks above: a
        // released block entity is gone from the chunk map, so ElectricWireBlock#onRemove finds none.
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        h.succeed();
    }

    /**
     * A chunk unload retires a wire for good, so no cycle may leave a position behind.
     *
     * <p>Vanilla unloads a chunk by calling {@code onChunkUnloaded} and then {@code setRemoved} on the
     * block entities of that chunk and dropping them from the chunk map
     * ({@code LevelChunk#clearAllBlockEntities:574-577}), after which nothing can put the wire back.
     * The test models exactly that: the block entity is released first, and only then is the wire's
     * block taken away. The order matters - {@code ElectricWireBlock#onRemove} reaches
     * {@code BlockEntity#getBlockEntity}, which drops a removed block entity from the chunk map and
     * returns null ({@code LevelChunk:299-301}), so once the block entity is released the block's own
     * removal path cannot release the position; only the block entity lifetimes under test can. Leaving
     * the wire block in place instead would let the chunk create a fresh block entity for it on the
     * next block change ({@code LevelChunk:261-265}), which is correct behaviour and not the case a
     * chunk unload produces.</p>
     */
    @GameTest(template = "test_blueprint_empty")
    public static void wireGraphDoesNotGrowAcrossUnloadCycles(GameTestHelper h) {
        var level = h.getLevel();
        int baseline = EnergyNet.trackedPositions(level);
        int cycles = 16;
        for (int i = 0; i < cycles; i++) {
            var pos = h.absolutePos(new BlockPos(1 + i % 4, 3, 1 + i / 4));
            placeWire(h, pos);
            var entity = level.getBlockEntity(pos);
            h.assertTrue(entity instanceof ElectricWireBlockEntity,
                    "cycle " + i + " placed a wire block entity, got " + entity);
            EnergyNet.onPlace(level, pos);
            h.assertTrue(EnergyNet.tracks(level, pos), "cycle " + i + ": the placed wire is tracked, so"
                    + " the release below is the only thing that can untrack it");

            ((ElectricWireBlockEntity) entity).setRemoved();
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

            h.assertTrue(!EnergyNet.tracks(level, pos), "cycle " + i + ": a released block entity must"
                    + " leave no position behind (tracked " + EnergyNet.trackedPositions(level) + ")");
            h.assertTrue(EnergyNet.trackedPositions(level) <= baseline, "cycle " + i + ": the graph must"
                    + " not grow across unload cycles (baseline " + baseline + ", now "
                    + EnergyNet.trackedPositions(level) + ")");
        }
        h.succeed();
    }

    /**
     * A neighbour entry that only existed because it pointed at the released wire goes with it.
     *
     * <p>{@code EnergyNet.onConnectionChange} is the call both wire block entities make when their
     * connections change; driving it directly records the two real edges A-B and B-A. Releasing A must
     * drop A's entry and B's entry, because B's only edge pointed at A and an entry without edges
     * carries no connectivity.</p>
     */
    @GameTest(template = "test_blueprint_empty")
    public static void removedWireReleasesTheNeighbourEntryThatPointedAtIt(GameTestHelper h) {
        var level = h.getLevel();
        var a = h.absolutePos(new BlockPos(2, 3, 2));
        var b = a.east();
        placeWire(h, a);
        placeWire(h, b);
        var connected = wire().defaultBlockState();
        for (var property : ElectricWireBlock.CONNECTIONS) connected = connected.setValue(property, true);
        level.setBlockAndUpdate(a, connected);
        level.setBlockAndUpdate(b, connected);
        EnergyNet.onPlace(level, a);
        EnergyNet.onPlace(level, b);
        EnergyNet.onConnectionChange(level, a, connected, ElectricWireBlock.CONNECTIONS);
        EnergyNet.onConnectionChange(level, b, connected, ElectricWireBlock.CONNECTIONS);
        h.assertTrue(EnergyNet.tracks(level, a) && EnergyNet.tracks(level, b),
                "both wires of the pair are tracked before the release");

        ((ElectricWireBlockEntity) level.getBlockEntity(a)).setRemoved();
        level.setBlockAndUpdate(a, Blocks.AIR.defaultBlockState());

        h.assertTrue(!EnergyNet.tracks(level, a), "the released wire's own position must be dropped");
        h.assertTrue(!EnergyNet.tracks(level, b), "the neighbour whose only edge pointed at the released"
                + " wire must be dropped with it, not left pointing at nothing");
        h.succeed();
    }

    @GameTest(template = "test_blueprint_empty")
    public static void garbagePileDoesNotGrowWithoutBound(GameTestHelper h) {
        GarbageData garbage = GarbageData.get(h.getLevel());
        // Per-instance NBT, so no two of these stacks merge and every one of them used to be a new
        // permanent pile in the dump.
        for (int i = 0; i < GarbageData.MAX_ENTRIES + 64; i++) {
            ItemStack stack = new ItemStack(Items.STONE);
            stack.getOrCreateTag().putInt("gt.growth.test", i);
            garbage.trash(stack);
        }
        int piles = garbage.entryCount();
        h.assertTrue(piles <= GarbageData.MAX_ENTRIES, "the dump must stay bounded by MAX_ENTRIES="
                + GarbageData.MAX_ENTRIES + ", but holds " + piles + " piles");
        h.succeed();
    }

    @GameTest(template = "test_blueprint_empty")
    public static void ovenMirrorReportIsPerPass(GameTestHelper h) {
        // An empty target list, so the pass mirrors every enabled GT furnace row again.
        List<Recipe<?>> first = new ArrayList<>();
        int rows = Loader_OvenRecipes.mirrorIntoFurnaceForTests(first);
        h.assertTrue(rows > 0, "the GT furnace table has rows to mirror, mirrored " + rows);
        int reported = Loader_OvenRecipes.addedToVanillaFurnace().size();
        h.assertTrue(reported == rows, "the report must describe this pass only: " + rows
                + " rows mirrored but " + reported + " reported");

        Loader_OvenRecipes.mirrorIntoFurnaceForTests(new ArrayList<>());
        int after = Loader_OvenRecipes.addedToVanillaFurnace().size();
        h.assertTrue(after == reported, "a repeated pass must replace the report instead of appending to"
                + " it (one pass = " + reported + ", after the second pass = " + after
                + "); this is the accumulator that grew once per world load");
        h.succeed();
    }
}
