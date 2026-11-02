package gregtech.engine.multiblock.tracking;

import gregtech.api.addon.platform.StructureProblem;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.content.multiblock.LargeMachineParts;
import gregtech.content.multiblock.SpecialStructures;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/** An on-demand scan of the runtime definition. Never evaluates the tracker or changes claims. */
public final class StructureDiagnostic {
    private StructureDiagnostic() {}

    private record Matched(BlockPos pos, LargeMachineLayouts.Cell cell, Block block) {}

    public static List<StructureProblem> inspect(TrackedMultiblock machine) {
        var entity = machine.trackedEntity();
        BlockPos origin = entity.getBlockPos();
        if (!(entity.getLevel() instanceof ServerLevel level) || entity.isRemoved())
            return List.of(new StructureProblem.Unavailable(origin));
        var definition = machine.structureDefinition();
        if (definition.formsImmediately()) return List.of();
        if (!(definition instanceof StructureDefinition.Declarative))
            return List.of(new StructureProblem.Unavailable(origin));
        var pattern = StructurePlan.compile(machine);
        if (pattern == null) return List.of(new StructureProblem.Unavailable(origin));
        List<StructureProblem> problems = new ArrayList<>();
        List<Matched> matched = new ArrayList<>();
        for (int i = 0; i < pattern.fixedCount(); i++) {
            var pos = origin.offset(pattern.dx(i), pattern.dy(i), pattern.dz(i));
            Matched cell = scan(level, origin, pos, pattern.declaredCell(i), pattern.matcher(i), problems);
            if (cell != null) matched.add(cell);
        }
        for (int group = 0; group < pattern.groupCount(); group++) {
            int first = pattern.groupFirstCell(group), count = pattern.groupCellCount(group);
            int copies = 0;
            for (; copies < pattern.groupLimit(group, level.getHeight()); copies++) {
                var layerProblems = new ArrayList<StructureProblem>();
                var layer = new ArrayList<Matched>();
                for (int i = first; i < first + count; i++) {
                    var pos = origin.offset(pattern.repeatDx(i), pattern.repeatDy(i, copies), pattern.repeatDz(i));
                    Matched cell = scan(
                            level, origin, pos, pattern.declaredRepeatCell(i), pattern.repeatMatcher(i), layerProblems);
                    if (cell != null) layer.add(cell);
                }
                if (layer.size() != count) {
                    // The first incomplete optional copy is outside the formed structure.
                    if (copies < pattern.groupMinimum(group)) problems.addAll(layerProblems);
                    break;
                }
                problems.addAll(layerProblems);
                matched.addAll(layer);
            }
            if (copies < pattern.groupMinimum(group))
                problems.add(new StructureProblem.Layers(
                        origin, pattern.groupKey(group), copies, pattern.groupMinimum(group)));
        }
        // An unloaded cell might satisfy a count. Do not report its unknown contents as a shortage.
        if (problems.stream().noneMatch(StructureProblem.Unloaded.class::isInstance)) {
            aggregates(origin, pattern.aggregates(), matched, problems);
            if (!machine.structurePartsValid()) problems.add(new StructureProblem.Rejected(origin));
        }
        return List.copyOf(problems);
    }

    private static Matched scan(
            ServerLevel level,
            BlockPos origin,
            BlockPos pos,
            LargeMachineLayouts.Cell declared,
            CompiledPattern.Matcher matcher,
            List<StructureProblem> problems) {
        if (!level.hasChunkAt(pos)) {
            problems.add(new StructureProblem.Unloaded(pos));
            return null;
        }
        var state = level.getBlockState(pos);
        int choice = matcher == null ? (state.isAir() ? 0 : CompiledPattern.FAILED) : matcher.match(level, pos, state);
        if (choice == CompiledPattern.FAILED) {
            var expected = expected(declared);
            var found = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            problems.add(
                    expected.contains(found.toString())
                            ? new StructureProblem.Rejected(pos)
                            : new StructureProblem.WrongBlock(pos, expected, found));
            return null;
        }
        var selected = declared;
        if (choice > 0) {
            var alternative = declared.alternatives().get(choice - 1);
            selected = new LargeMachineLayouts.Cell(
                    declared.right(),
                    declared.up(),
                    declared.back(),
                    alternative.part(),
                    declared.role(),
                    alternative.plain(),
                    declared.design(),
                    List.of(),
                    declared.ports());
        }
        // Read both live and reserved ownership; canClaim would register waiters and is not read-only.
        var claims = WorldStructureClaims.table(level);
        Long owner = claims.owner(pos.asLong());
        if (owner == null) owner = claims.reservedOwner(pos.asLong());
        if (owner != null && owner != origin.asLong())
            problems.add(new StructureProblem.Occupied(pos, BlockPos.of(owner)));
        return new Matched(pos, selected, state.getBlock());
    }

    private static List<String> expected(LargeMachineLayouts.Cell cell) {
        if (cell.role() == gregtech.engine.multiblock.MultiblockLayout.Role.AIR) return List.of("air");
        List<String> result = new ArrayList<>();
        result.add(expected(cell.part(), cell.plain()));
        for (var alternative : cell.alternatives()) result.add(expected(alternative.part(), alternative.plain()));
        return result.stream().distinct().toList();
    }

    private static String expected(int part, String plain) {
        if (plain != null)
            return plain.startsWith(SpecialStructures.FIXED_BLOCK)
                    ? plain.substring(SpecialStructures.FIXED_BLOCK.length())
                    : plain;
        var block = LargeMachineParts.block(part);
        return block == null
                ? "part:" + part
                : BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static void aggregates(
            BlockPos origin, List<Aggregate> rules, List<Matched> cells, List<StructureProblem> problems) {
        for (var rule : rules) {
            if (rule instanceof Aggregate.Count count) {
                int actual = (int) cells.stream()
                        .filter(cell -> selects(count.selector(), cell))
                        .count();
                if (actual < count.min() || count.max() >= 0 && actual > count.max())
                    problems.add(
                            new StructureProblem.Count(origin, count.description(), actual, count.min(), count.max()));
            } else if (rule instanceof Aggregate.SameBlock same) {
                Matched first = null;
                for (var cell : cells) {
                    if (!selects(same.selector(), cell)) continue;
                    if (first == null) first = cell;
                    else if (cell.block() != first.block())
                        problems.add(new StructureProblem.DifferentBlocks(
                                cell.pos(),
                                same.description(),
                                first.pos(),
                                BuiltInRegistries.BLOCK.getKey(first.block()),
                                BuiltInRegistries.BLOCK.getKey(cell.block())));
                }
            }
        }
    }

    private static boolean selects(Aggregate.CellSelector selector, Matched matched) {
        var cell = matched.cell();
        return selector.selects(cell.part(), cell.plain(), cell.role());
    }

    /** Translates the same detached results consumed by addon diagnostic tools. */
    public static Component describe(StructureProblem problem) {
        String at = coordinates(problem.position());
        if (problem instanceof StructureProblem.WrongBlock wrong) {
            var expected = Component.empty();
            for (String candidate : wrong.expected()) {
                if (!expected.getSiblings().isEmpty()) expected.append(" / ");
                expected.append(name(candidate));
            }
            return text("block", at, expected, name(wrong.found().toString()));
        }
        if (problem instanceof StructureProblem.Unloaded) return text("unloaded", at);
        if (problem instanceof StructureProblem.Layers layers)
            return text(
                    "layers", layers.group(), layers.actual(), layers.minimum(), layers.minimum() - layers.actual());
        if (problem instanceof StructureProblem.Count count) {
            String selection = count.selection().isBlank() ? "*" : count.selection();
            return count.actual() < count.minimum()
                    ? text("count_short", selection, count.actual(), count.minimum(), count.minimum() - count.actual())
                    : text(
                            "count_excess",
                            selection,
                            count.actual(),
                            count.maximum(),
                            count.actual() - count.maximum());
        }
        if (problem instanceof StructureProblem.DifferentBlocks different)
            return text(
                    "same_block",
                    at,
                    name(different.found().toString()),
                    coordinates(different.firstPosition()),
                    name(different.first().toString()));
        if (problem instanceof StructureProblem.Occupied occupied)
            return text("occupied", at, coordinates(occupied.controller()));
        if (problem instanceof StructureProblem.Rejected) return text("rejected", at);
        return text("unavailable", at);
    }

    private static Component text(String key, Object... arguments) {
        return Component.translatable("gt6ce.structure.diagnostic." + key, arguments);
    }

    private static Component name(String expected) {
        if (expected.equals("air")
                || expected.equals("anyopaque")
                || expected.equals("anyfluid")
                || expected.equals(SpecialStructures.BEDROCK_FLOOR))
            return Component.translatable("gt6ce.structure.diagnostic.predicate." + expected);
        ResourceLocation id = ResourceLocation.tryParse(expected);
        if (id != null && BuiltInRegistries.BLOCK.containsKey(id))
            return BuiltInRegistries.BLOCK.get(id).getName();
        return Component.literal(expected);
    }

    private static String coordinates(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
