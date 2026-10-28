package gregtech.engine.multiblock.tracking;

import gregtech.content.multiblock.FusionStructure;
import gregtech.content.multiblock.LargeMachineLayouts;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Hand-written footprints for controllers that do not scan a {@link LargeMachineLayouts.Layout}. */
public final class CustomStructures {
    private CustomStructures() {}

    public static StructureDefinition fusion(BlockEntity entity) {
        return fixed(entity, CustomStructures::fusionFootprint);
    }

    public static StructureDefinition unformed(BlockEntity entity) {
        return fixed(
                entity,
                machine -> new Footprint.Builder()
                        .addController(machine.trackedEntity().getBlockPos())
                        .build());
    }

    private static StructureDefinition fixed(BlockEntity entity, Function<TrackedMultiblock, Footprint> footprint) {
        return new StructureDefinition.Custom() {
            @Override
            public StructureDefinition.ClaimPolicy claimPolicy() {
                return StructureDefinition.ClaimPolicy.ATOMIC;
            }

            @Override
            public Footprint watchFootprint(TrackedMultiblock machine) {
                return footprint.apply(machine);
            }

            @Override
            public Footprint declaredFootprint(TrackedMultiblock machine) {
                return footprint.apply(machine);
            }
        };
    }

    private static Footprint fusionFootprint(TrackedMultiblock machine) {
        BlockEntity entity = machine.trackedEntity();
        Direction front = LayoutStructures.facing(entity);
        Footprint.Builder builder = new Footprint.Builder().addController(entity.getBlockPos());
        for (var cell : FusionStructure.CELLS) builder.add(cell.at(entity.getBlockPos(), front), Projection.BLOCK);
        return builder.build();
    }
}
