package gregtech.engine.multiblock.tracking;

import gregtech.content.blockentity.machine.BehaviorMachineBlockEntity;
import gregtech.content.blockentity.machine.LargeRecipeMachineBlockEntity;
import gregtech.content.multiblock.LargeMachineLayouts;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Footprints and cached matching compiled from the same declared layout. */
public final class LayoutStructures {
    private LayoutStructures() {}

    @FunctionalInterface
    public interface PartMatch {
        boolean matches(LargeMachineLayouts.Cell cell, Block expected, Block actual);
    }

    public static StructureDefinition.Declarative recipe(LargeRecipeMachineBlockEntity machine) {
        return new LayoutDefinition(machine, machine::trackedLayout);
    }

    public static StructureDefinition.Declarative behavior(BehaviorMachineBlockEntity machine) {
        return new LayoutDefinition(machine, machine::trackedLayout);
    }

    public static StructureDefinition.Declarative fuel(
            gregtech.content.blockentity.machine.FuelTurbineBlockEntity machine) {
        return new LayoutDefinition(machine, () -> gregtech.content.registry.MultiblockRegistration.layout(machine));
    }

    public static Footprint of(
            LargeMachineLayouts.Layout layout, BlockPos origin, Direction front, int layers, boolean frontier) {
        if (layout == null) return new Footprint.Builder().addController(origin).build();
        int[] all = new int[layout.repeats().size()];
        java.util.Arrays.fill(all, layers);
        return of(layout, origin, front, all, frontier);
    }

    /** Watches each repeat group up to its matched layer count, plus the next layer when {@code frontier}. */
    public static Footprint of(
            LargeMachineLayouts.Layout layout, BlockPos origin, Direction front, int[] matched, boolean frontier) {
        Footprint.Builder builder = new Footprint.Builder().addController(origin);
        if (layout == null || front == null) return builder.build();
        boolean identity = layout.constraints().stream()
                .anyMatch(rule -> rule instanceof gregtech.api.multiblock.StructureConstraint.SameBlock);
        for (LargeMachineLayouts.Cell cell : layout.cells()) {
            builder.add(cell.at(origin, front), projection(cell, identity));
        }
        for (int group = 0; group < layout.repeats().size(); group++) {
            LargeMachineLayouts.Repeat repeat = layout.repeats().get(group);
            int found = matched != null && group < matched.length ? matched[group] : repeat.min();
            int limit = frontier ? found + 1 : found;
            int cap = repeat.max() < 0 ? LargeMachineLayouts.MAX_REPEATS : repeat.max();
            int count = Math.max(0, Math.min(limit, cap));
            for (int copy = 0; copy < count; copy++) {
                for (LargeMachineLayouts.Cell cell : repeat.cells())
                    builder.add(cell.at(origin, front, repeat.absoluteUp(cell, copy)), projection(cell, identity));
            }
        }
        return builder.build();
    }

    /** Every cell a full scan is allowed to read, including the failed frontier layer. */
    public static Footprint declared(LargeMachineLayouts.Layout layout, BlockEntity entity) {
        Direction front = facing(entity);
        int height = entity.getLevel() == null
                ? LargeMachineLayouts.MAX_REPEATS
                : entity.getLevel().getHeight();
        return of(layout, entity.getBlockPos(), front, height, false);
    }

    private static Projection projection(LargeMachineLayouts.Cell cell, boolean identity) {
        var membership = projection(cell);
        // Equal tag/predicate membership does not imply equal block identity for a SameBlock rule.
        return identity ? Projection.and(Projection.BLOCK, membership) : membership;
    }

    static Projection projection(LargeMachineLayouts.Cell cell) {
        if (cell.role() == gregtech.engine.multiblock.MultiblockLayout.Role.AIR)
            return (level, pos, previous, next) -> previous.isAir() == next.isAir();
        return (level, pos, previous, next) -> {
            int left = accepted(cell, level, pos, previous);
            int right = accepted(cell, level, pos, next);
            if (left != right) return false;
            if (left < 0) return true;
            String plain = plainAt(cell, left);
            if (plain != null) return Projection.plain(plain).equivalent(level, pos, previous, next);
            return previous.getBlock() == next.getBlock();
        };
    }

    /** Primary choice is 0, then each alternative. -1 is not accepted. Coil blocks stay distinct. */
    private static int accepted(
            LargeMachineLayouts.Cell cell,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state) {
        if (accepts(cell.part(), cell.plain(), level, pos, state)) return 0;
        var alternatives = cell.alternatives();
        for (int index = 0; index < alternatives.size(); index++) {
            var choice = alternatives.get(index);
            if (accepts(choice.part(), choice.plain(), level, pos, state)) return index + 1;
        }
        return -1;
    }

    private static String plainAt(LargeMachineLayouts.Cell cell, int index) {
        if (index == 0) return cell.plain();
        return cell.alternatives().get(index - 1).plain();
    }

    private static boolean accepts(
            int part,
            String plain,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state) {
        if (plain != null) return gregtech.content.multiblock.PlainStructureBlocks.matches(level, pos, plain, state);
        net.minecraft.world.level.block.Block block = state.getBlock();
        net.minecraft.world.level.block.Block expected = gregtech.content.multiblock.LargeMachineParts.block(part);
        return block == expected;
    }

    public static Direction facing(BlockEntity entity) {
        BlockState state = entity.getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (state.hasProperty(BlockStateProperties.FACING)) return state.getValue(BlockStateProperties.FACING);
        return Direction.NORTH;
    }

    private static final class LayoutDefinition implements StructureDefinition.Declarative {
        private final TrackedMultiblock machine;
        private final Supplier<LargeMachineLayouts.Layout> layout;

        private LayoutDefinition(TrackedMultiblock machine, Supplier<LargeMachineLayouts.Layout> layout) {
            this.machine = machine;
            this.layout = layout;
        }

        @Override
        public StructureDefinition.ClaimPolicy claimPolicy() {
            return StructureDefinition.ClaimPolicy.ATOMIC;
        }

        @Override
        public CompiledPattern compile(net.minecraft.core.Direction front) {
            return CompiledPattern.compile(machine, layout.get(), front, aggregates());
        }

        @Override
        public java.util.List<Aggregate> aggregates() {
            var value = layout.get();
            return value == null ? java.util.List.of() : StructureConstraints.compile(value.constraints());
        }

        @Override
        public boolean tightenWatch() {
            LargeMachineLayouts.Layout value = layout.get();
            return value != null && !value.repeats().isEmpty();
        }

        @Override
        public boolean usesTags() {
            LargeMachineLayouts.Layout value = layout.get();
            if (value == null) return false;
            if (tags(value.cells())) return true;
            for (LargeMachineLayouts.Repeat repeat : value.repeats()) if (tags(repeat.cells())) return true;
            return false;
        }

        @Override
        public String overlayMachine() {
            if (machine instanceof gregtech.content.blockentity.machine.LargeRecipeMachineBlockEntity recipe)
                return recipe.spec() == null ? null : recipe.spec().machineName();
            if (machine instanceof gregtech.content.blockentity.machine.BehaviorMachineBlockEntity behavior)
                return behavior.declaration().casing().machineType();
            if (machine instanceof gregtech.content.blockentity.machine.FuelTurbineBlockEntity fuel)
                return fuel.declaration().casing().machineType();
            return null;
        }

        private static boolean tags(java.util.List<LargeMachineLayouts.Cell> cells) {
            for (LargeMachineLayouts.Cell cell : cells) {
                for (var choice : cell.alternatives())
                    if (choice.plain() != null && choice.plain().contains("#")) return true;
                String plain = cell.plain();
                if (plain == null) continue;
                if (plain.startsWith("#")) return true;
                if (plain.startsWith("anyfluid") && plain.indexOf('#') >= 0) return true;
            }
            return false;
        }

        @Override
        public Footprint watchFootprint(TrackedMultiblock ignored) {
            LargeMachineLayouts.Layout value = layout.get();
            BlockEntity entity = machine.trackedEntity();
            if (value == null)
                return new Footprint.Builder()
                        .addController(entity.getBlockPos())
                        .build();
            StructureTracker tracker = StructureTracking.tracker(entity);
            int[] matched = tracker == null ? null : tracker.matchedLayers();
            return of(value, entity.getBlockPos(), facing(entity), matched, true);
        }

        @Override
        public Footprint declaredFootprint(TrackedMultiblock ignored) {
            return declared(layout.get(), machine.trackedEntity());
        }
    }
}
