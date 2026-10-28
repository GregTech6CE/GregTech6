package gregtech.content.multiblock;

import gregtech.content.block.machine.OriginalLightningRodControllerBlock;
import gregtech.content.blockentity.machine.*;
import gregtech.content.logistics.LogisticsCoreStructure;
import gregtech.content.registry.GTMultiblocks;
import gregtech.engine.multiblock.MultiblockLayout.Role;
import gregtech.engine.multiblock.tracking.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/** World predicates and material variants over shared character layers; watches use the compiled cells. */
public final class SpecialStructures {
    public static final String BEDROCK_FLOOR = "bedrockfloor";
    public static final String FIXED_BLOCK = "fixedblock#";

    private SpecialStructures() {}

    private static LargeMachineLayouts.Cell part(int id, Role role) {
        return new LargeMachineLayouts.Cell(0, 0, 0, id, role);
    }

    private static LargeMachineLayouts.Cell literal(Block block, Role role) {
        return new LargeMachineLayouts.Cell(0, 0, 0, 0, role, FIXED_BLOCK + BuiltInRegistries.BLOCK.getKey(block));
    }

    private static void add(List<CompiledPattern.FixedCell> cells, BlockPos offset, LargeMachineLayouts.Cell spec) {
        cells.add(new CompiledPattern.FixedCell(offset.getX(), offset.getY(), offset.getZ(), spec));
    }

    /** Shared by the tank definition and the JEI/EMI preview. */
    public static List<CompiledPattern.FixedCell> tankCells(int size, Block wall, Direction front) {
        var cells = new ArrayList<CompiledPattern.FixedCell>();
        for (var cell : SharedTankStructure.layout(size).cells()) {
            var role = Role.valueOf(cell.role().name());
            var offset = AxialStructureTransform.at(BlockPos.ZERO, front, cell.right(), cell.up(), cell.back());
            add(cells, offset, role == Role.AIR ? part(0, Role.AIR) : literal(wall, role));
        }
        return cells;
    }

    public static StructureDefinition.Declarative tank(MultiblockTankControllerBlockEntity tank) {
        return new Fixed(tank, StructureDefinition.ClaimPolicy.ATOMIC) {
            @Override
            List<CompiledPattern.FixedCell> cells(Direction front) {
                int radius = tank.trackedSize() / 2;
                Block wall = tank.valveSpec() == null
                        ? (radius == 2 ? GTMultiblocks.TANK_WALL_DENSE.get() : GTMultiblocks.TANK_WALL.get())
                        : LargeMachineParts.block(tank.valveSpec().wallId());
                return tankCells(tank.trackedSize(), wall, front);
            }
        };
    }

    /** Shared by the logistics definition and the JEI/EMI preview. */
    public static List<CompiledPattern.FixedCell> logisticsCells(Direction front) {
        var cells = new ArrayList<CompiledPattern.FixedCell>();
        for (var cell : LogisticsCoreStructure.cells()) {
            var spec = switch (cell.kind()) {
                case WALL -> part(LogisticsCoreStructure.WALL, Role.ENERGY_INPUT);
                case VENT -> part(LogisticsCoreStructure.VENT, Role.LOGISTICS);
                case CPU ->
                    new LargeMachineLayouts.Cell(
                            0,
                            0,
                            0,
                            LogisticsCoreStructure.VERSATILE,
                            Role.CASING,
                            null,
                            -1,
                            List.of(
                                    new SharedLargeMachineLayouts.Choice(LogisticsCoreStructure.LOGIC, null),
                                    new SharedLargeMachineLayouts.Choice(LogisticsCoreStructure.CONTROL, null),
                                    new SharedLargeMachineLayouts.Choice(LogisticsCoreStructure.STORAGE, null),
                                    new SharedLargeMachineLayouts.Choice(LogisticsCoreStructure.CONVERSION, null),
                                    new SharedLargeMachineLayouts.Choice(LogisticsCoreStructure.WALL, null)));
            };
            add(cells, cell.at(BlockPos.ZERO, front), spec);
        }
        return cells;
    }

    public static StructureDefinition.Declarative logistics(LogisticsCoreControllerBlockEntity machine) {
        return new Fixed(machine, StructureDefinition.ClaimPolicy.PER_CELL) {
            @Override
            List<CompiledPattern.FixedCell> cells(Direction front) {
                return logisticsCells(front);
            }

            @Override
            public List<Aggregate> aggregates() {
                var constraints = new ArrayList<Aggregate>();
                for (int id : new int[] {
                    LogisticsCoreStructure.LOGIC,
                    LogisticsCoreStructure.CONTROL,
                    LogisticsCoreStructure.STORAGE,
                    LogisticsCoreStructure.CONVERSION
                }) {
                    constraints.add(Aggregate.Count.atLeast(
                            (candidate, plain, role) -> role == Role.CASING
                                    && (candidate == LogisticsCoreStructure.VERSATILE || candidate == id),
                            1));
                }
                return List.copyOf(constraints);
            }
        };
    }

    /** Shared by the drill definition and the JEI/EMI preview. */
    public static List<CompiledPattern.FixedCell> drillCells() {
        var cells = new ArrayList<CompiledPattern.FixedCell>();
        for (var cell : SharedDrillStructure.LAYOUT.cells()) {
            var role = Role.valueOf(cell.role().name());
            var spec = cell.up() == -5
                    ? new LargeMachineLayouts.Cell(0, 0, 0, 0, role, BEDROCK_FLOOR)
                    : part(cell.up() == -4 ? 18103 : 18026, role);
            add(cells, new BlockPos(cell.right(), cell.up(), cell.back()), spec);
        }
        return cells;
    }

    public static StructureDefinition.Declarative drill(BedrockDrillControllerBlockEntity machine) {
        return new Fixed(machine, StructureDefinition.ClaimPolicy.ATOMIC) {
            @Override
            List<CompiledPattern.FixedCell> cells(Direction front) {
                return drillCells();
            }
        };
    }

    /** Shared by the lightning-rod definition and the JEI/EMI preview. */
    public static LargeMachineLayouts.Layout lightningLayout(boolean original) {
        var layout = SharedLightningStructure.LAYOUT;
        var fixed =
                layout.cells().stream().map(SpecialStructures::lightningCell).toList();
        var repeats = layout.repeats().stream()
                .map(repeat -> new LargeMachineLayouts.Repeat(
                        repeat.key(),
                        repeat.up(),
                        repeat.min(),
                        repeat.max(),
                        repeat.step(),
                        repeat.height(),
                        repeat.cells().stream()
                                .map(SpecialStructures::lightningCell)
                                .toList()))
                .toList();
        return new LargeMachineLayouts.Layout(fixed, repeats);
    }

    private static LargeMachineLayouts.Cell lightningCell(SharedLargeMachineLayouts.Cell cell) {
        // Lightning rods accept the block itself, without requiring a structural-port entity.
        return new LargeMachineLayouts.Cell(
                cell.right(),
                cell.up(),
                cell.back(),
                0,
                Role.valueOf(cell.role().name()),
                FIXED_BLOCK + BuiltInRegistries.BLOCK.getKey(LargeMachineParts.block(cell.part())));
    }

    public static StructureDefinition.Declarative lightning(LightningRodControllerBlockEntity machine) {
        return new StructureDefinition.Declarative() {
            private LargeMachineLayouts.Layout layout() {
                return lightningLayout(
                        machine.getBlockState().getBlock() instanceof OriginalLightningRodControllerBlock);
            }

            @Override
            public StructureDefinition.ClaimPolicy claimPolicy() {
                return StructureDefinition.ClaimPolicy.ATOMIC;
            }

            @Override
            public boolean tightenWatch() {
                return true;
            }

            @Override
            public CompiledPattern compile(Direction front) {
                return CompiledPattern.compile(machine, layout(), front, List.of());
            }

            @Override
            public Footprint watchFootprint(TrackedMultiblock ignored) {
                var tracker = StructureTracking.tracker(machine);
                return LayoutStructures.of(
                        layout(),
                        machine.getBlockPos(),
                        LayoutStructures.facing(machine),
                        tracker == null ? null : tracker.matchedLayers(),
                        true);
            }

            @Override
            public Footprint declaredFootprint(TrackedMultiblock ignored) {
                return LayoutStructures.declared(layout(), machine);
            }
        };
    }

    /** The block a compiled cell shows in a preview; the definition's own matcher input. */
    public static Block previewBlock(LargeMachineLayouts.Cell cell) {
        String plain = cell.plain();
        if (plain != null) {
            if (BEDROCK_FLOOR.equals(plain)) return net.minecraft.world.level.block.Blocks.BEDROCK;
            if (plain.startsWith(FIXED_BLOCK))
                return BuiltInRegistries.BLOCK
                        .getOptional(net.minecraft.resources.ResourceLocation.tryParse(
                                plain.substring(FIXED_BLOCK.length())))
                        .orElse(net.minecraft.world.level.block.Blocks.AIR);
        }
        if (cell.role() == Role.AIR || cell.part() <= 0) return net.minecraft.world.level.block.Blocks.AIR;
        return LargeMachineParts.block(cell.part());
    }

    /** Wrap the existing layout footprint while enabling oven candidates and their same-block constraint. */
    public static StructureDefinition.Declarative oven(LargeRecipeMachineBlockEntity machine) {
        var delegate = LayoutStructures.recipe(machine);
        return new StructureDefinition.Declarative() {
            @Override
            public StructureDefinition.ClaimPolicy claimPolicy() {
                return delegate.claimPolicy();
            }

            @Override
            public List<Aggregate> aggregates() {
                return List.of(new Aggregate.SameBlock((part, plain, role) -> part == 18042));
            }

            @Override
            public CompiledPattern compile(Direction front) {
                return CompiledPattern.compile(machine, machine.trackedLayout(), front, aggregates());
            }

            @Override
            public boolean tightenWatch() {
                return delegate.tightenWatch();
            }

            @Override
            public boolean usesTags() {
                return delegate.usesTags();
            }

            @Override
            public String overlayMachine() {
                return delegate.overlayMachine();
            }

            @Override
            public Footprint watchFootprint(TrackedMultiblock ignored) {
                return delegate.watchFootprint(machine);
            }

            @Override
            public Footprint declaredFootprint(TrackedMultiblock ignored) {
                return delegate.declaredFootprint(machine);
            }
        };
    }

    private abstract static class Fixed implements StructureDefinition.Declarative {
        final TrackedMultiblock machine;
        private final StructureDefinition.ClaimPolicy policy;

        Fixed(TrackedMultiblock machine, StructureDefinition.ClaimPolicy policy) {
            this.machine = machine;
            this.policy = policy;
        }

        abstract List<CompiledPattern.FixedCell> cells(Direction front);

        @Override
        public StructureDefinition.ClaimPolicy claimPolicy() {
            return policy;
        }

        @Override
        public CompiledPattern compile(Direction front) {
            return CompiledPattern.compileFixed(machine, front, cells(front), aggregates());
        }

        @Override
        public Footprint watchFootprint(TrackedMultiblock ignored) {
            BlockPos origin = machine.trackedEntity().getBlockPos();
            var builder = new Footprint.Builder().addController(origin);
            for (var cell : cells(LayoutStructures.facing(machine.trackedEntity()))) {
                var spec = cell.cell();
                Projection projection = spec.role() == Role.AIR
                        ? Projection.AIR
                        : BEDROCK_FLOOR.equals(spec.plain()) ? Projection.bedrockFloor() : Projection.BLOCK;
                builder.add(origin.offset(cell.dx(), cell.dy(), cell.dz()), projection);
            }
            return builder.build();
        }

        @Override
        public Footprint declaredFootprint(TrackedMultiblock ignored) {
            return watchFootprint(machine);
        }
    }
}
