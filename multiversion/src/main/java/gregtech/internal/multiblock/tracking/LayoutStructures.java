package gregtech.internal.multiblock.tracking;

import gregtech.blockentity.machine.BehaviorMachineBlockEntity;
import gregtech.blockentity.machine.LargeRecipeMachineBlockEntity;
import gregtech.content.multiblock.LargeMachineLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.Supplier;

/** Footprints derived from a {@link LargeMachineLayouts.Layout}. The scan itself stays in the controller. */
public final class LayoutStructures {
    private LayoutStructures() {}

    @FunctionalInterface
    public interface PartMatch {
        boolean matches(LargeMachineLayouts.Cell cell, Block expected, Block actual);
    }

    public static StructureDefinition recipe(LargeRecipeMachineBlockEntity machine) {
        return new LayoutDefinition(machine, machine::trackedLayout, false);
    }

    public static StructureDefinition behavior(BehaviorMachineBlockEntity machine) {
        return new LayoutDefinition(machine, machine::trackedLayout, true);
    }

    public static Footprint of(LargeMachineLayouts.Layout layout, BlockPos origin, Direction front, int layers, boolean frontier) {
        Footprint.Builder builder = new Footprint.Builder().addController(origin);
        if (layout == null || front == null) return builder.build();
        for (LargeMachineLayouts.Cell cell : layout.cells()) {
            builder.add(cell.at(origin, front), projection(cell));
        }
        int limit = frontier ? layers + 1 : layers;
        for (LargeMachineLayouts.Repeat repeat : layout.repeats()) {
            int cap = repeat.max() < 0 ? LargeMachineLayouts.MAX_REPEATS : repeat.max();
            int count = Math.max(0, Math.min(limit, cap));
            for (int copy = 0; copy < count; copy++) {
                for (LargeMachineLayouts.Cell cell : repeat.cells())
                    builder.add(cell.at(origin, front, repeat.absoluteUp(cell, copy)), projection(cell));
            }
        }
        return builder.build();
    }

    /** Every cell a full scan is allowed to read, including the failed frontier layer. */
    public static Footprint declared(LargeMachineLayouts.Layout layout, BlockEntity entity) {
        Direction front = facing(entity);
        int height = entity.getLevel() == null ? LargeMachineLayouts.MAX_REPEATS : entity.getLevel().getHeight();
        return of(layout, entity.getBlockPos(), front, height, false);
    }

    static Projection projection(LargeMachineLayouts.Cell cell) {
        if (cell.role() == gregtech.internal.multiblock.MultiblockLayout.Role.AIR)
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
    private static int accepted(LargeMachineLayouts.Cell cell, net.minecraft.world.level.Level level,
                                net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
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

    private static boolean accepts(int part, String plain, net.minecraft.world.level.Level level,
                                   net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (plain != null) return gregtech.content.multiblock.PlainStructureBlocks.matches(level, pos, plain, state);
        net.minecraft.world.level.block.Block block = state.getBlock();
        net.minecraft.world.level.block.Block expected = gregtech.content.multiblock.LargeMachineParts.block(part);
        if (part == 18042) {
            return block == expected || block == gregtech.content.multiblock.LargeMachineParts.block(18043);
        }
        return block == expected;
    }

    public static Direction facing(BlockEntity entity) {
        BlockState state = entity.getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (state.hasProperty(BlockStateProperties.FACING)) return state.getValue(BlockStateProperties.FACING);
        return Direction.NORTH;
    }

    private static final class LayoutDefinition implements StructureDefinition.Declarative {
        private final TrackedMultiblock machine;
        private final Supplier<LargeMachineLayouts.Layout> layout;
        private final boolean formWithoutLayout;

        private LayoutDefinition(TrackedMultiblock machine, Supplier<LargeMachineLayouts.Layout> layout, boolean formWithoutLayout) {
            this.machine = machine;
            this.layout = layout;
            this.formWithoutLayout = formWithoutLayout;
        }

        @Override public StructureDefinition.ClaimPolicy claimPolicy() { return StructureDefinition.ClaimPolicy.ATOMIC; }

        @Override public boolean formsImmediately() {
            return formWithoutLayout && layout.get() == null;
        }

        @Override public boolean tightenWatch() {
            LargeMachineLayouts.Layout value = layout.get();
            return value != null && !value.repeats().isEmpty();
        }

        @Override public Footprint watchFootprint(TrackedMultiblock ignored) {
            LargeMachineLayouts.Layout value = layout.get();
            BlockEntity entity = machine.trackedEntity();
            if (value == null) return new Footprint.Builder().addController(entity.getBlockPos()).build();
            int layers = 0;
            for (LargeMachineLayouts.Repeat repeat : value.repeats()) layers = Math.max(layers, repeat.min());
            return of(value, entity.getBlockPos(), facing(entity), layers, true);
        }

        @Override public Footprint declaredFootprint(TrackedMultiblock ignored) {
            return declared(layout.get(), machine.trackedEntity());
        }
    }
}
