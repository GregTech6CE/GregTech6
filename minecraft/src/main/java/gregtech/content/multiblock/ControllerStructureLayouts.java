package gregtech.content.multiblock;

import gregtech.content.registry.MultiblockRegistration;
import gregtech.engine.multiblock.tracking.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;

/** Detached, world-free projections of the real controller definitions. No second geometry table. */
public final class ControllerStructureLayouts {
    private ControllerStructureLayouts() {}

    private static int epoch = -1;
    private static Map<Block, List<StructurePlan.Cell>> plans = Map.of();

    public static synchronized void invalidate() {
        epoch = -1;
    }

    public static synchronized Map<Block, List<StructurePlan.Cell>> plans() {
        int current = StructureEpoch.get();
        if (epoch == current) return plans;
        var next = new LinkedHashMap<Block, List<StructurePlan.Cell>>();
        for (var entry : MultiblockRegistration.controllers().entrySet()) {
            Block block = entry.getKey();
            var tracked = entry.getValue();
            var definition = tracked.structureDefinition();
            if (definition.formsImmediately() || !(definition instanceof StructureDefinition.Declarative declarative))
                continue;
            var pattern = declarative.compile(Direction.NORTH);
            if (pattern == null) continue;
            // Do not retain detached controllers/inventories through the matchers in a client cache.
            var cells = StructurePlan.preview(pattern).stream()
                    .map(cell -> new StructurePlan.Cell(cell.offset(), cell.specification(), null))
                    .toList();
            if (!cells.isEmpty()) next.put(block, cells);
        }
        if (!next.equals(plans)) plans = Collections.unmodifiableMap(next);
        epoch = current;
        return plans;
    }

    public static Map<BlockPos, Block> cells(Block controller) {
        var plan = plans().get(controller);
        if (plan == null) return null;
        var cells = new LinkedHashMap<BlockPos, Block>();
        for (var cell : plan) cells.put(cell.offset(), cell.specification().block());
        return Collections.unmodifiableMap(cells);
    }
}
