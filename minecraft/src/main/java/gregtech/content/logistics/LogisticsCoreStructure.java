package gregtech.content.logistics;

import static gregtech.content.logistics.LogisticsCoreLayout.CONTROL;
import static gregtech.content.logistics.LogisticsCoreLayout.CONVERSION;
import static gregtech.content.logistics.LogisticsCoreLayout.LOGIC;
import static gregtech.content.logistics.LogisticsCoreLayout.MAX_STORAGE_CPU_COUNT;
import static gregtech.content.logistics.LogisticsCoreLayout.STORAGE;
import static gregtech.content.logistics.LogisticsCoreLayout.VENT;
import static gregtech.content.logistics.LogisticsCoreLayout.VERSATILE;
import static gregtech.content.logistics.LogisticsCoreLayout.WALL;

import gregtech.content.logistics.LogisticsCoreLayout.Cell;
import gregtech.content.logistics.LogisticsCoreLayout.Counts;
import gregtech.content.logistics.LogisticsCoreLayout.Kind;
import gregtech.content.multiblock.LargeMachineParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * GT6 {@code MultiTileEntityLogisticsCore} structure: a 5x5x5 cube whose every
 * cell is classified by its <em>squared</em> distance from the centre, exactly like the original's
 * {@code i*i + j*j + k*k} thresholds.
 *
 * <pre>
 * GT6 i*i + j*j + k*k &lt; 4 -&gt; the inner 3x3x3 core: processor units (or a wall)
 * GT6 i*i + j*j + k*k &gt; 6 -&gt; the 44 corner "walls", which are also the power input
 * GT6 otherwise -&gt; ventilation units (53 of them: 54 face cells minus the controller)
 * </pre>
 *
 * <p>The cube is centred two blocks <em>behind</em> the controller ({@code getOffsetXN(mFacing, 2)},
 * ), so the controller itself sits in the centre of one 5x5 face and faces outwards.
 * GT6 iterates world axes rather than rotating with the facing; that is harmless
 * because a cube and the squared-distance classes are both rotation invariant, so the port
 * classifies in the controller's local frame instead and gets the same cells.</p>
 *
 * <p>CPU contributions are read verbatim from:</p>
 *
 * <ul>
 *   <li>{@code 18200} "Versatile Quadcore Processor Unit" adds <b>1 to all four</b> counters;
 *       </li>
 *   <li>{@code 18201-18204} (Logic/Control/Storage/Conversion) each add <b>4</b> to their own
 *       counter;</li>
 *   <li>{@code 18008} "Galvanized Steel Wall" is allowed in the core but contributes nothing -
 *       the original's "Well someone's a cheapstake.;P" branch and its tooltip
 *       "{@code You can replace CPUs with Walls should you not be able to afford that many}".</li>
 * </ul>
 *
 * <p>A structure is valid when it is complete <em>and</em> all four counters are positive;
 * the storage counter is capped at {@code MAX_STORAGE_CPU_COUNT = 108}.</p>
 */
public final class LogisticsCoreStructure {
    private LogisticsCoreStructure() {}

    /**
     * Projects a core cell in the frame perpendicular to the controller's facing. For horizontal
     * facings, {@code right} is clockwise and {@code up} is world Y; for vertical facings the
     * two tangent axes are world X/Z. {@code back=0} is the controller's own plane.
     */
    public static BlockPos at(Cell cell, BlockPos controller, Direction front) {
        Direction rightAxis = front.getAxis().isHorizontal() ? front.getClockWise() : Direction.EAST;
        Direction upAxis =
                front == Direction.UP ? Direction.NORTH : front == Direction.DOWN ? Direction.SOUTH : Direction.UP;
        return controller
                .relative(rightAxis, cell.right())
                .relative(upAxis, cell.up())
                .relative(front.getOpposite(), cell.back());
    }

    /** Block a cell of the given class must hold, for the diagnostic messages and for JEI. */
    public static Block block(Kind kind) {
        return switch (kind) {
            case WALL -> LargeMachineParts.block(WALL);
            case VENT -> LargeMachineParts.block(VENT);
            case CPU -> LargeMachineParts.block(VERSATILE);
        };
    }

    /** The counters a processor part contributes, or {@code null} when the block is not a core CPU. */
    public static Counts contribution(Block block) {
        for (int id : new int[] {VERSATILE, LOGIC, CONTROL, STORAGE, CONVERSION}) {
            if (block != LargeMachineParts.block(id)) continue;
            return LogisticsCoreLayout.contribution(id);
        }
        return null;
    }

    /** The four counters of a complete structure, or {@code null} when a cell does not fit. */
    public static Counts counts(Level level, BlockPos controller, Direction front) {
        int logic = 0, control = 0, storage = 0, conversion = 0;
        for (var cell : LogisticsCoreLayout.cells()) {
            var pos = at(cell, controller, front);
            if (!level.hasChunkAt(pos)) return null;
            var block = level.getBlockState(pos).getBlock();
            switch (cell.kind()) {
                case WALL, VENT -> {
                    int expected = cell.kind() == Kind.WALL ? WALL : VENT;
                    if (block != LargeMachineParts.block(expected)) return null;
                }
                case CPU -> {
                    if (block == LargeMachineParts.block(WALL)) continue; // cheapstake branch, no CPU
                    var contribution = contribution(block);
                    if (contribution == null) return null;
                    logic += contribution.logic();
                    control += contribution.control();
                    storage += contribution.storage();
                    conversion += contribution.conversion();
                }
            }
        }
        return new Counts(logic, control, Math.min(MAX_STORAGE_CPU_COUNT, storage), conversion);
    }

    // ── CPU economics (GT6 tooltips) ────────────────────────

    /** "Logic Processors increase Operation Count by 1" - operations per network pass. */
    public static int operations(Counts counts) {
        return counts.logic();
    }

    /** GT6's: maximum Chebyshev radius is two plus the control CPU count. */
    public static int range(Counts counts) {
        return counts.control() + 2;
    }

    /** "Storage Processors increase Buffer Size by 1 Stack or 16000L" - the port's own tank count. */
    public static int bufferTanks(Counts counts) {
        return counts.storage();
    }

    /** "Conversion Processors increase Throughput by 1 Stack or 16000L" - per operation. */
    public static long throughput(Counts counts) {
        return counts.conversion();
    }
}
