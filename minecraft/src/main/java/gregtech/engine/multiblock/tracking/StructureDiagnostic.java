package gregtech.engine.multiblock.tracking;

import gregtech.content.multiblock.LargeMachineLayouts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Diagnostic scan derived from the same compiled definition the runtime uses. Unlike the runtime
 * evaluator it never stops at the first failure, so the magnifier can list every wrong cell.
 */
public final class StructureDiagnostic {
    private StructureDiagnostic() {}

    public record Problem(BlockPos pos, Block expected, Block found, String reason) {
        public String describe() {
            String at = pos.getX() + "," + pos.getY() + "," + pos.getZ();
            if ("unloaded".equals(reason)) return "Chunk not loaded at " + at;
            if ("aggregate".equals(reason)) return "Part counts do not satisfy the structure at " + at;
            if ("layers".equals(reason)) return "Not enough repeat layers at " + at;
            String want = "air".equals(reason) ? "air" : expected == null ? "another block" : name(expected);
            String got = found == null ? "unloaded" : name(found);
            return "Wrong block at " + at + ": expected " + want + ", found " + got;
        }

        private static String name(Block block) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            return id == null ? block.toString() : id.toString();
        }
    }

    /** Every cell the structure expects but the world does not satisfy, in definition order. */
    public static List<Problem> inspect(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        Level level = entity.getLevel();
        if (level == null || entity.isRemoved()) return List.of();
        Direction front = LayoutStructures.facing(entity);
        StructureDefinition definition = machine.structureDefinition();
        if (definition.formsImmediately()) return List.of();
        if (!(definition instanceof StructureDefinition.Declarative declarative)) return List.of();
        CompiledPattern pattern = declarative.compile(front);
        if (pattern == null) return List.of();
        BlockPos origin = entity.getBlockPos();
        List<Problem> problems = new ArrayList<>();
        fixed(level, origin, pattern, problems);
        repeats(level, origin, pattern, level.getHeight(), problems);
        aggregates(level, origin, pattern, machine, declarative, problems);
        return List.copyOf(problems);
    }

    private static void fixed(Level level, BlockPos origin, CompiledPattern pattern, List<Problem> problems) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int index = 0; index < pattern.fixedCount(); index++) {
            cursor.setWithOffset(origin, pattern.dx(index), pattern.dy(index), pattern.dz(index));
            BlockPos pos = cursor.immutable();
            if (!level.hasChunkAt(pos)) {
                problems.add(new Problem(pos, null, null, "unloaded"));
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (pattern.air(index)) {
                if (!state.isAir()) problems.add(new Problem(pos, null, state.getBlock(), "air"));
                continue;
            }
            if (pattern.matcher(index).match(level, pos, state) == CompiledPattern.FAILED)
                problems.add(new Problem(pos, expected(pattern.cell(index, 0)), state.getBlock(), "block"));
        }
    }

    private static void repeats(
            Level level, BlockPos origin, CompiledPattern pattern, int worldHeight, List<Problem> problems) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int group = 0; group < pattern.groupCount(); group++) {
            int limit = pattern.groupLimit(group, worldHeight);
            int first = pattern.groupFirstCell(group);
            int count = pattern.groupCellCount(group);
            if (count == 0) continue;
            int copies = 0;
            boolean reportedLayer = false;
            for (int copy = 0; copy < limit; copy++) {
                boolean layerComplete = true;
                var layerProblems = new ArrayList<Problem>();
                for (int cell = first; cell < first + count; cell++) {
                    cursor.setWithOffset(
                            origin, pattern.repeatDx(cell), pattern.repeatDy(cell, copy), pattern.repeatDz(cell));
                    BlockPos pos = cursor.immutable();
                    if (!level.hasChunkAt(pos)) {
                        layerProblems.add(new Problem(pos, null, null, "unloaded"));
                        layerComplete = false;
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (pattern.repeatAir(cell)) {
                        if (!state.isAir()) {
                            layerProblems.add(new Problem(pos, null, state.getBlock(), "air"));
                            layerComplete = false;
                        }
                    } else if (pattern.repeatMatcher(cell).match(level, pos, state) == CompiledPattern.FAILED) {
                        layerProblems.add(
                                new Problem(pos, expected(pattern.repeatCell(cell, 0)), state.getBlock(), "block"));
                        layerComplete = false;
                    }
                }
                if (!layerComplete) {
                    // A failing copy only blocks formation when the group is still below its minimum.
                    if (copies < pattern.groupMinimum(group)) {
                        problems.addAll(layerProblems);
                        reportedLayer = true;
                    }
                    break;
                }
                copies++;
            }
            if (copies < pattern.groupMinimum(group) && !reportedLayer)
                problems.add(new Problem(
                        origin.offset(pattern.repeatDx(first), pattern.repeatDy(first, 0), pattern.repeatDz(first)),
                        null,
                        null,
                        "layers"));
        }
    }

    private static void aggregates(
            Level level,
            BlockPos origin,
            CompiledPattern pattern,
            TrackedMultiblock machine,
            StructureDefinition.Declarative definition,
            List<Problem> problems) {
        if (problems.stream().anyMatch(problem -> !"aggregate".equals(problem.reason()))) return;
        if (definition.aggregates().isEmpty()) return;
        PatternState state = new PatternState(pattern, level.getHeight());
        state.evaluate(level, origin);
        if (!state.complete() || !machine.structurePartsValid())
            problems.add(new Problem(origin, null, null, "aggregate"));
    }

    private static Block expected(LargeMachineLayouts.Cell cell) {
        if (cell.plain() != null || cell.part() <= 0) return null;
        return gregtech.content.multiblock.LargeMachineParts.block(cell.part());
    }
}
