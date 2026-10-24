package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.HeatHandler;
import com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerSpecs;
import com.gregtech.gregtech.registry.GTHeatExchangers;
import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Public heat facade contract only: no changes to ticking, pressure hazards or cooling policy. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class HeatSignedExtractionTests {
    private static Block registered(String name) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", name));
    }

    /** Preserve only explicitly touched disposable-test cells, even on failed assertions. */
    private static final class Fixture implements AutoCloseable {
        private record Saved(BlockState state, CompoundTag data) {}
        private final GameTestHelper h;
        private final java.util.Map<BlockPos, Saved> cells = new java.util.LinkedHashMap<>();
        Fixture(GameTestHelper h) { this.h = h; }
        void set(BlockPos pos, BlockState state) {
            if (!cells.containsKey(pos)) {
                var entity = h.getLevel().getBlockEntity(pos);
//? if >=1.21.1 {
/*                var data = entity == null ? null : entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
                var data = entity == null ? null : entity.saveWithoutMetadata();
//?}
                cells.put(pos.immutable(), new Saved(h.getLevel().getBlockState(pos), data));
            }
            h.getLevel().removeBlockEntity(pos);
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            h.getLevel().setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        @Override public void close() {
            for (var pos : cells.keySet()) h.getLevel().removeBlockEntity(pos);
            for (var entry : cells.entrySet()) {
                var pos = entry.getKey(); var saved = entry.getValue();
                h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                h.getLevel().setBlock(pos, saved.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                BlockEntity restored = h.getLevel().getBlockEntity(pos);
                if (restored != null && saved.data() != null) {
//? if >=1.21.1 {
/*                    restored.loadWithComponents(saved.data(), h.getLevel().registryAccess());
*///?} else {
                    restored.load(saved.data());
//?}
                }
            }
        }
    }

    /** Assert each returned quantity against the actual buffer delta, not a mirrored implementation. */
    private static void operation(GameTestHelper h, HeatHandler heat, boolean insert, long request, long expected) {
        long before = heat.heatStored();
        long simulated = insert ? heat.insertHeat(request, true) : heat.extractHeat(request, true);
        h.assertTrue(simulated == expected && heat.heatStored() == before,
                "signed simulation preserves heat: insert=" + insert + " request=" + request);
        long committed = insert ? heat.insertHeat(request, false) : heat.extractHeat(request, false);
        long after = insert ? Math.addExact(before, committed) : Math.subtractExact(before, committed);
        h.assertTrue(committed == expected && heat.heatStored() == after,
                "signed return equals committed buffer delta: insert=" + insert + " request=" + request);
    }

    private static void matrix(GameTestHelper h, HeatHandler heat) {
        h.assertTrue(heat.heatStored() == 0, "new native adapter starts empty");
        operation(h, heat, false, -10, -10); // Regression: previously returned +10 while adding 10 HU.
        operation(h, heat, true, 20, 20);
        operation(h, heat, false, 7, 7);
        operation(h, heat, true, -5, -5);
        operation(h, heat, true, 0, 0);
        operation(h, heat, false, 0, 0);
        operation(h, heat, true, Long.MIN_VALUE, 0);
        operation(h, heat, false, Long.MIN_VALUE, 0);
        operation(h, heat, false, Long.MAX_VALUE, 18);
        operation(h, heat, true, -10, 0); // Existing clipping policy: empty is not negative storage.
        operation(h, heat, true, Long.MAX_VALUE - 2, Long.MAX_VALUE - 2);
        operation(h, heat, false, -5, -2); // Only two positive HU fit; returned removal is negative two.
        operation(h, heat, false, -1, 0);
        operation(h, heat, true, 1, 0);
        operation(h, heat, true, -Long.MAX_VALUE, -Long.MAX_VALUE);
        operation(h, heat, false, -Long.MAX_VALUE, -Long.MAX_VALUE);
        operation(h, heat, false, Long.MAX_VALUE, Long.MAX_VALUE);
        h.assertTrue(heat.heatStored() == 0, "matrix drains all fixture heat before any native tick");
    }

    @GameTest(template="test_empty")
    public static void signedExtractionConservesHeatAcrossNativeAdapters(GameTestHelper h) {
        var blocks = java.util.List.of(registered("steam_boiler_bronze"), registered("smelting_crucible_steel"),
                GTHeatExchangers.all().iterator().next().get(), GTMultiblocks.HEAT_EXCHANGER_MAIN.get());
        var pos = h.absolutePos(new BlockPos(4, 4, 4));
        try (var fixture = new Fixture(h)) {
            for (var block : blocks) {
                fixture.set(pos, block.defaultBlockState());
                var entity = h.getLevel().getBlockEntity(pos);
                h.assertTrue(entity instanceof HeatHandler, "registered block exposes native heat adapter: " + block);
                matrix(h, (HeatHandler) entity);
            }
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void signedLargeBoilerExtractionPreservesStructureGate(GameTestHelper h) {
        var variant = OriginalLargeBoilerSpecs.all().get(0);
        var pos = h.absolutePos(new BlockPos(6, 5, 6));
        try (var fixture = new Fixture(h)) {
            // Clear the entire real shell before asserting the unformed input gate.
            for (var cell : OriginalLargeBoilerSpecs.LAYOUT.cells())
                fixture.set(cell.at(pos, Direction.NORTH), Blocks.AIR.defaultBlockState());
            fixture.set(pos, LargeMachineParts.block(variant.originalId()).defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            var boiler = (OriginalLargeBoilerControllerBlockEntity) h.getLevel().getBlockEntity(pos);
            h.assertTrue(!StructureTestSupport.ok(boiler), "unassembled registered boiler is unformed");
            operation(h, boiler, false, -10, 0);
            operation(h, boiler, true, 10, 0);
            for (var cell : OriginalLargeBoilerSpecs.LAYOUT.cells()) {
                var cellPos = cell.at(pos, Direction.NORTH);
                if (cellPos.equals(pos)) continue;
                var part = switch (cell.role()) {
                    case HEAT_INPUT -> GTMultiblocks.HEAT_TRANSMITTER.get();
                    case AIR -> Blocks.AIR;
                    default -> variant.wall();
                };
                fixture.set(cellPos, part.defaultBlockState());
            }
            h.assertTrue(StructureTestSupport.ok(boiler), "native transmitter and wall shell forms without overriding validation");
            matrix(h, boiler);
            operation(h, boiler, true, 10, 10);
            var requiredWall = OriginalLargeBoilerSpecs.LAYOUT.cells().stream()
                    .filter(cell -> cell.role() != com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.AIR
                            && cell.role() != com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.HEAT_INPUT
                            && !cell.at(pos, Direction.NORTH).equals(pos))
                    .findFirst().orElseThrow().at(pos, Direction.NORTH);
            fixture.set(requiredWall, Blocks.AIR.defaultBlockState());
            h.assertTrue(!StructureTestSupport.ok(boiler), "removing a required loaded wall invalidates the shell");
            operation(h, boiler, false, -10, 0);
            operation(h, boiler, true, 10, 0);
            operation(h, boiler, false, 10, 10); // Existing extraction remains a plain drain when broken.
        }
        h.succeed();
    }
}
