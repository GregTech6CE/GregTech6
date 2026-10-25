package gregtech.content.multiblock;

import gregtech.internal.multiblock.MultiblockLayout.Role;
import gregtech.internal.multiblock.tracking.*;
import gregtech.content.block.machine.OriginalLightningRodControllerBlock;
import gregtech.content.blockentity.machine.*;
import gregtech.content.logistics.LogisticsCoreStructure;
import gregtech.content.registry.GTMultiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** Card 55 geometry. Compiled cells are also the sole source of each family's watch footprint. */
public final class SpecialStructures {
    public static final String BEDROCK_FLOOR = "bedrockfloor";
    public static final String FIXED_BLOCK = "fixedblock#";
    private SpecialStructures() {}

    private static LargeMachineLayouts.Cell part(int id, Role role) {
        return new LargeMachineLayouts.Cell(0, 0, 0, id, role);
    }

    private static LargeMachineLayouts.Cell literal(Block block, Role role) {
        return new LargeMachineLayouts.Cell(0, 0, 0, 0, role,
                FIXED_BLOCK + BuiltInRegistries.BLOCK.getKey(block));
    }

    private static void add(List<CompiledPattern.FixedCell> cells, BlockPos offset, LargeMachineLayouts.Cell spec) {
        cells.add(new CompiledPattern.FixedCell(offset.getX(), offset.getY(), offset.getZ(), spec));
    }

    public static StructureDefinition.Declarative tank(MultiblockTankControllerBlockEntity tank) {
        return new Fixed(tank, StructureDefinition.ClaimPolicy.ATOMIC) {
            @Override List<CompiledPattern.FixedCell> cells(Direction front) {
                int radius = tank.trackedSize() / 2;
                Block wall = tank.valveSpec() == null
                        ? (radius == 2 ? GTMultiblocks.TANK_WALL_DENSE.get() : GTMultiblocks.TANK_WALL.get())
                        : LargeMachineParts.block(tank.valveSpec().wallId());
                BlockPos centre = BlockPos.ZERO.relative(front.getOpposite(), radius);
                var cells = new ArrayList<CompiledPattern.FixedCell>();
                for (int x = -radius; x <= radius; x++)
                    for (int y = -radius; y <= radius; y++)
                        for (int z = -radius; z <= radius; z++) {
                            BlockPos offset = centre.offset(x, y, z);
                            if (offset.equals(BlockPos.ZERO)) continue;
                            boolean shell = Math.abs(x) == radius || Math.abs(y) == radius || Math.abs(z) == radius;
                            add(cells, offset, shell ? literal(wall, Role.FLUID_IO) : part(0, Role.AIR));
                        }
                return cells;
            }
        };
    }

    public static StructureDefinition.Declarative logistics(LogisticsCoreControllerBlockEntity machine) {
        return new Fixed(machine, StructureDefinition.ClaimPolicy.PER_CELL) {
            @Override List<CompiledPattern.FixedCell> cells(Direction front) {
                var cells = new ArrayList<CompiledPattern.FixedCell>();
                for (var cell : LogisticsCoreStructure.cells()) {
                    var spec = switch (cell.kind()) {
                        case WALL -> part(LogisticsCoreStructure.WALL, Role.ENERGY_INPUT);
                        case VENT -> part(LogisticsCoreStructure.VENT, Role.LOGISTICS);
                        case CPU -> new LargeMachineLayouts.Cell(0, 0, 0, LogisticsCoreStructure.VERSATILE,
                                Role.CASING, null, -1, List.of(
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

            @Override public List<Aggregate> aggregates() {
                var constraints = new ArrayList<Aggregate>();
                for (int id : new int[]{LogisticsCoreStructure.LOGIC, LogisticsCoreStructure.CONTROL,
                        LogisticsCoreStructure.STORAGE, LogisticsCoreStructure.CONVERSION}) {
                    constraints.add(Aggregate.Count.atLeast((candidate, plain, role) -> role == Role.CASING
                            && (candidate == LogisticsCoreStructure.VERSATILE || candidate == id), 1));
                }
                return List.copyOf(constraints);
            }
        };
    }

    public static StructureDefinition.Declarative drill(BedrockDrillControllerBlockEntity machine) {
        return new Fixed(machine, StructureDefinition.ClaimPolicy.ATOMIC) {
            @Override List<CompiledPattern.FixedCell> cells(Direction front) {
                var cells = new ArrayList<CompiledPattern.FixedCell>();
                for (int x = -1; x <= 1; x++)
                    for (int z = -1; z <= 1; z++) {
                        add(cells, new BlockPos(x, -5, z),
                                new LargeMachineLayouts.Cell(0, 0, 0, 0, Role.CASING, BEDROCK_FLOOR));
                        for (int y = -4; y <= 0; y++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            // Part matchers additionally require the native port entity, just like the full scan.
                            add(cells, new BlockPos(x, y, z), part(y == -4 ? 18103 : 18026,
                                    BedrockDrillControllerBlockEntity.role(x, y, z)));
                        }
                    }
                return cells;
            }
        };
    }

    public static StructureDefinition.Declarative lightning(LightningRodControllerBlockEntity machine) {
        return new StructureDefinition.Declarative() {
            private LargeMachineLayouts.Layout layout() {
                boolean original = machine.getBlockState().getBlock() instanceof OriginalLightningRodControllerBlock;
                var cells = new ArrayList<LargeMachineLayouts.Cell>();
                for (int y = 0; y < 5; y++)
                    for (int x = -1; x <= 1; x++)
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            Block block = original ? LargeMachineParts.block(y % 2 == 0 ? 18004 : 18041)
                                    : y % 2 == 0 ? GTMultiblocks.TUNGSTEN_WALL.get()
                                    : GTMultiblocks.LARGE_NIOBIUM_TITANIUM_COIL.get();
                            // Symmetric base: local right/back coordinates give the same world cube.
                            cells.add(new LargeMachineLayouts.Cell(x, y, z, 0, Role.CASING,
                                    FIXED_BLOCK + BuiltInRegistries.BLOCK.getKey(block)));
                        }
                Block rod = original ? LargeMachineParts.block(18104) : GTMultiblocks.LIGHTNING_ROD.get();
                return new LargeMachineLayouts.Layout(List.copyOf(cells), List.of(
                        new LargeMachineLayouts.Repeat("rod", 5, 0, -1, 1, 1, List.of(literal(rod, Role.CASING)))));
            }
            @Override public StructureDefinition.ClaimPolicy claimPolicy() { return StructureDefinition.ClaimPolicy.ATOMIC; }
            @Override public boolean tightenWatch() { return true; }
            @Override public CompiledPattern compile(Direction front) {
                return CompiledPattern.compile(machine, layout(), front, List.of());
            }
            @Override public Footprint watchFootprint(TrackedMultiblock ignored) {
                var tracker = StructureTracking.tracker(machine);
                return LayoutStructures.of(layout(), machine.getBlockPos(), LayoutStructures.facing(machine),
                        tracker == null ? null : tracker.matchedLayers(), true);
            }
            @Override public Footprint declaredFootprint(TrackedMultiblock ignored) {
                return LayoutStructures.declared(layout(), machine);
            }
        };
    }

    /** Wrap the existing layout footprint while enabling oven candidates and their same-block constraint. */
    public static StructureDefinition.Declarative oven(LargeRecipeMachineBlockEntity machine) {
        var delegate = LayoutStructures.recipe(machine);
        return new StructureDefinition.Declarative() {
            @Override public StructureDefinition.ClaimPolicy claimPolicy() { return delegate.claimPolicy(); }
            @Override public List<Aggregate> aggregates() {
                return List.of(new Aggregate.SameBlock((part, plain, role) -> part == 18042));
            }
            @Override public CompiledPattern compile(Direction front) {
                return CompiledPattern.compile(machine, machine.trackedLayout(), front, aggregates());
            }
            @Override public boolean tightenWatch() { return delegate.tightenWatch(); }
            @Override public boolean usesTags() { return delegate.usesTags(); }
            @Override public String overlayMachine() { return delegate.overlayMachine(); }
            @Override public Footprint watchFootprint(TrackedMultiblock ignored) { return delegate.watchFootprint(machine); }
            @Override public Footprint declaredFootprint(TrackedMultiblock ignored) { return delegate.declaredFootprint(machine); }
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
        @Override public StructureDefinition.ClaimPolicy claimPolicy() { return policy; }
        @Override public CompiledPattern compile(Direction front) {
            return CompiledPattern.compileFixed(machine, front, cells(front), aggregates());
        }
        @Override public Footprint watchFootprint(TrackedMultiblock ignored) {
            BlockPos origin = machine.trackedEntity().getBlockPos();
            var builder = new Footprint.Builder().addController(origin);
            for (var cell : cells(LayoutStructures.facing(machine.trackedEntity()))) {
                var spec = cell.cell();
                Projection projection = spec.role() == Role.AIR ? Projection.AIR
                        : BEDROCK_FLOOR.equals(spec.plain()) ? Projection.bedrockFloor() : Projection.BLOCK;
                builder.add(origin.offset(cell.dx(), cell.dy(), cell.dz()), projection);
            }
            return builder.build();
        }
        @Override public Footprint declaredFootprint(TrackedMultiblock ignored) { return watchFootprint(machine); }
    }
}
