package gregtech.internal.multiblock.tracking;

import gregtech.internal.multiblock.MultiblockLayout;
import gregtech.content.blockentity.machine.AxialGeneratorBlockEntity;
import gregtech.content.blockentity.machine.FuelTurbineBlockEntity;
import gregtech.content.blockentity.machine.LargeGasTurbineControllerBlockEntity;
import gregtech.content.blockentity.machine.LightningRodControllerBlockEntity;
import gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity;
import gregtech.content.logistics.LogisticsCoreStructure;
import gregtech.content.multiblock.AxialStructureTransform;
import gregtech.content.multiblock.FusionStructure;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.content.multiblock.OriginalLargeBoilerSpecs;
import gregtech.content.multiblock.TurbineStructure;
import gregtech.content.multiblock.VonDaGraaggRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Function;

/** Hand-written footprints for controllers that do not scan a {@link LargeMachineLayouts.Layout}. */
public final class CustomStructures {
    private CustomStructures() {}

    public static StructureDefinition fusion(BlockEntity entity) {
        return fixed(entity, CustomStructures::fusionFootprint);
    }

    public static StructureDefinition implosion(BlockEntity entity) {
        return fixed(entity, CustomStructures::implosionFootprint);
    }

    public static StructureDefinition boiler(BlockEntity entity) {
        return fixed(entity, CustomStructures::boilerFootprint);
    }

    public static StructureDefinition gasTurbine(LargeGasTurbineControllerBlockEntity entity) {
        return fixed(entity, machine -> axialShell(entity, TurbineStructure.LAYOUT.cells()));
    }

    public static StructureDefinition axial(AxialGeneratorBlockEntity entity) {
        return fixed(entity, machine -> axialPositions(entity, entity.trackedCells()));
    }

    public static StructureDefinition tank(MultiblockTankControllerBlockEntity entity) {
        return fixed(entity, machine -> tankFootprint(entity));
    }

    public static StructureDefinition logistics(BlockEntity entity) {
        return fixed(entity, CustomStructures::logisticsFootprint);
    }

    public static StructureDefinition exchanger(BlockEntity entity) {
        return fixed(entity, CustomStructures::exchangerFootprint);
    }

    public static StructureDefinition drill(BlockEntity entity) {
        return fixed(entity, CustomStructures::drillFootprint);
    }

    public static StructureDefinition crucible(BlockEntity entity) {
        return fixed(entity, CustomStructures::crucibleFootprint);
    }

    public static StructureDefinition tower(BlockEntity entity) {
        return fixed(entity, CustomStructures::towerFootprint);
    }

    public static StructureDefinition fuel(FuelTurbineBlockEntity entity) {
        return fixed(entity, machine -> fuelFootprint(entity));
    }

    public static StructureDefinition lightning(LightningRodControllerBlockEntity entity) {
        return new Lightning(entity);
    }

    public static StructureDefinition unformed(BlockEntity entity) {
        return fixed(entity, machine -> new Footprint.Builder().addController(machine.trackedEntity().getBlockPos()).build());
    }

    private static StructureDefinition fixed(BlockEntity entity, Function<TrackedMultiblock, Footprint> footprint) {
        return new StructureDefinition.Custom() {
            @Override public StructureDefinition.ClaimPolicy claimPolicy() { return StructureDefinition.ClaimPolicy.ATOMIC; }
            @Override public Footprint watchFootprint(TrackedMultiblock machine) { return footprint.apply(machine); }
            @Override public Footprint declaredFootprint(TrackedMultiblock machine) { return footprint.apply(machine); }
        };
    }

    private static Footprint fusionFootprint(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        Direction front = LayoutStructures.facing(entity);
        Footprint.Builder builder = new Footprint.Builder().addController(entity.getBlockPos());
        for (var cell : FusionStructure.CELLS) builder.add(cell.at(entity.getBlockPos(), front), Projection.BLOCK);
        return builder.build();
    }

    private static Footprint implosionFootprint(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        Direction front = LayoutStructures.facing(entity);
        Footprint.Builder builder = new Footprint.Builder().addController(entity.getBlockPos());
        for (var cell : gregtech.content.blockentity.machine.ImplosionCompressorControllerBlockEntity.LAYOUT.cells()) {
            builder.add(cell.at(entity.getBlockPos(), front),
                    cell.role() == MultiblockLayout.Role.AIR ? Projection.AIR : Projection.BLOCK);
        }
        return builder.build();
    }

    private static Footprint boilerFootprint(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        Direction front = LayoutStructures.facing(entity);
        BlockPos centre = entity.getBlockPos().relative(front.getOpposite());
        Footprint.Builder builder = new Footprint.Builder().addController(entity.getBlockPos());
        for (var cell : gregtech.content.multiblock.OriginalLargeBoilerParameters.CHECK_ORDER) {
            Projection projection = cell.role() == gregtech.internal.multiblock.StructureGrid.Role.AIR
                    ? Projection.AIR : Projection.BLOCK;
            builder.add(centre.offset(cell.x(), cell.y(), cell.z()), projection);
        }
        for (var cell : OriginalLargeBoilerSpecs.LAYOUT.cells()) builder.add(cell.at(entity.getBlockPos(), front), Projection.BLOCK);
        return builder.build();
    }

    private static Footprint logisticsFootprint(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        Direction front = LayoutStructures.facing(entity);
        Footprint.Builder builder = new Footprint.Builder().addController(entity.getBlockPos());
        for (var cell : LogisticsCoreStructure.cells()) builder.add(cell.at(entity.getBlockPos(), front), Projection.BLOCK);
        return builder.build();
    }

    private static Footprint exchangerFootprint(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        Footprint.Builder builder = new Footprint.Builder().addController(entity.getBlockPos());
        for (var cell : gregtech.content.multiblock.SharedHeatExchangerStructure.CELLS)
            builder.add(entity.getBlockPos().offset(cell.right(), cell.up(), cell.back()), Projection.BLOCK);
        return builder.build();
    }

    private static Footprint drillFootprint(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        BlockPos origin = entity.getBlockPos();
        Footprint.Builder builder = new Footprint.Builder().addController(origin);
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) {
                builder.add(origin.offset(x, -5, z), Projection.bedrockFloor());
                for (int y = -4; y <= 0; y++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    builder.add(origin.offset(x, y, z), Projection.BLOCK);
                }
            }
        return builder.build();
    }

    private static Footprint crucibleFootprint(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        Footprint.Builder builder = new Footprint.Builder().addController(entity.getBlockPos());
        for (var cell : gregtech.content.multiblock.SharedLargeCrucibleStructure.CELLS) {
            builder.add(entity.getBlockPos().offset(cell.x(), cell.y(), cell.z()),
                    cell.air() ? Projection.AIR : Projection.BLOCK);
        }
        return builder.build();
    }

    private static Footprint towerFootprint(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        Footprint.Builder builder = new Footprint.Builder().addController(entity.getBlockPos());
        for (var cell : VonDaGraaggRules.layout())
            builder.add(entity.getBlockPos().offset(cell.x(), cell.y(), cell.z()), Projection.BLOCK);
        return builder.build();
    }

    private static Footprint fuelFootprint(FuelTurbineBlockEntity entity) {
        var cells = entity.trackedCells();
        Footprint.Builder builder = new Footprint.Builder().addController(entity.getBlockPos());
        if (cells == null) return builder.build();
        Direction front = LayoutStructures.facing(entity);
        for (var cell : cells) builder.add(cell.at(entity.getBlockPos(), front), LayoutStructures.projection(cell));
        return builder.build();
    }

    private static Footprint tankFootprint(MultiblockTankControllerBlockEntity entity) {
        int size = entity.trackedSize();
        Direction front = LayoutStructures.facing(entity);
        BlockPos origin = entity.getBlockPos();
        int radius = size / 2;
        BlockPos centre = origin.relative(front.getOpposite(), radius);
        Footprint.Builder builder = new Footprint.Builder().addController(origin);
        for (int x = -radius; x <= radius; x++)
            for (int y = -radius; y <= radius; y++)
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = centre.offset(x, y, z);
                    if (pos.equals(origin)) continue;
                    boolean shell = Math.abs(x) == radius || Math.abs(y) == radius || Math.abs(z) == radius;
                    builder.add(pos, shell ? Projection.BLOCK : Projection.AIR);
                }
        return builder.build();
    }

    private static Footprint axialShell(BlockEntity entity, Iterable<MultiblockLayout.Cell> cells) {
        Direction front = LayoutStructures.facing(entity);
        Footprint.Builder builder = new Footprint.Builder().addController(entity.getBlockPos());
        for (var cell : cells)
            builder.add(AxialStructureTransform.at(entity.getBlockPos(), front, cell.right(), cell.up(), cell.back()), Projection.BLOCK);
        return builder.build();
    }

    private static Footprint axialPositions(BlockEntity entity, Iterable<BlockPos> cells) {
        Direction front = LayoutStructures.facing(entity);
        Footprint.Builder builder = new Footprint.Builder().addController(entity.getBlockPos());
        for (BlockPos cell : cells)
            builder.add(AxialStructureTransform.at(entity.getBlockPos(), front, cell.getX(), cell.getY(), cell.getZ()), Projection.BLOCK);
        return builder.build();
    }

    private static final class Lightning implements StructureDefinition.Custom {
        private final LightningRodControllerBlockEntity entity;

        private Lightning(LightningRodControllerBlockEntity entity) { this.entity = entity; }

        @Override public StructureDefinition.ClaimPolicy claimPolicy() { return StructureDefinition.ClaimPolicy.ATOMIC; }
        @Override public boolean tightenWatch() { return true; }

        @Override public Footprint watchFootprint(TrackedMultiblock machine) {
            return rod(entity, 1);
        }

        @Override public Footprint declaredFootprint(TrackedMultiblock machine) {
            int span = entity.getLevel() == null ? 384 : entity.getLevel().getMaxBuildHeight() - entity.getBlockPos().getY();
            return rod(entity, Math.max(1, span));
        }

        private static Footprint rod(LightningRodControllerBlockEntity entity, int segments) {
            BlockPos origin = entity.getBlockPos();
            Footprint.Builder builder = new Footprint.Builder().addController(origin);
            for (int y = 0; y < 5; y++)
                for (int x = -1; x <= 1; x++)
                    for (int z = -1; z <= 1; z++)
                        builder.add(origin.offset(x, y, z), Projection.BLOCK);
            int limit = entity.getLevel() == null ? segments : Math.min(segments,
                    Math.max(0, entity.getLevel().getMaxBuildHeight() - origin.getY() - 5));
            for (int i = 0; i < limit; i++) builder.add(origin.above(5 + i), Projection.BLOCK);
            return builder.build();
        }
    }
}
