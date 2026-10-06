package com.gregtech.gregtech.content.logistics;

import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6 {@code MultiTileEntityLogisticsCore} structure ({@code :109-147}): a 5x5x5 cube whose every
 * cell is classified by its <em>squared</em> distance from the centre, exactly like the original's
 * {@code i*i + j*j + k*k} thresholds.
 *
 * <pre>
 * GT6:118   i*i + j*j + k*k &lt;  4  -&gt; the inner 3x3x3 core: processor units (or a wall)
 * GT6:137   i*i + j*j + k*k &gt;  6  -&gt; the 44 corner "walls", which are also the power input
 * GT6:139   otherwise            -&gt; ventilation units (53 of them: 54 face cells minus the controller)
 * </pre>
 *
 * <p>The cube is centred two blocks <em>behind</em> the controller ({@code getOffsetXN(mFacing, 2)},
 * {@code :110}), so the controller itself sits in the centre of one 5x5 face and faces outwards.
 * GT6 iterates world axes rather than rotating with the facing ({@code :117}); that is harmless
 * because a cube and the squared-distance classes are both rotation invariant, so the port
 * classifies in the controller's local frame instead and gets the same cells.</p>
 *
 * <p>CPU contributions are read verbatim from {@code :119-136}:</p>
 *
 * <ul>
 *   <li>{@code 18200} "Versatile Quadcore Processor Unit" adds <b>1 to all four</b> counters
 *       ({@code :120-123});</li>
 *   <li>{@code 18201-18204} (Logic/Control/Storage/Conversion) each add <b>4</b> to their own
 *       counter ({@code :125-131});</li>
 *   <li>{@code 18008} "Galvanized Steel Wall" is allowed in the core but contributes nothing -
 *       the original's "Well someone's a cheapstake. ;P" branch ({@code :132-133}) and its tooltip
 *       "{@code You can replace CPUs with Walls should you not be able to afford that many}".</li>
 * </ul>
 *
 * <p>A structure is valid when it is complete <em>and</em> all four counters are positive
 * ({@code :144}); the storage counter is capped at {@code MAX_STORAGE_CPU_COUNT = 108}
 * ({@code :64}, {@code :143}).</p>
 */
public final class LogisticsCoreStructure {
    /** GT6 part ids used by this structure. */
    public static final int WALL = 18008, VENT = 18299, VERSATILE = 18200,
            LOGIC = 18201, CONTROL = 18202, STORAGE = 18203, CONVERSION = 18204;
    /** GT6 {@code :64}: {@code MAX_STORAGE_CPU_COUNT}. */
    public static final int MAX_STORAGE_CPU_COUNT = 108;
    /** The original scans {@code tX+i, tY+j, tZ+k} for {@code i,j,k} in {@code [-2,2]} ({@code :117}). */
    public static final int RADIUS = 2;

    /** The three structural classes of {@code :118/:137/:139}. */
    public enum Kind { CPU, VENT, WALL }

    /**
     * One cell of the cube, in a frame perpendicular to the controller's facing. For horizontal
     * facings, {@code right} is clockwise and {@code up} is world Y; for vertical facings the
     * two tangent axes are world X/Z. {@code back=0} is the controller's own plane.
     */
    public record Cell(int right, int up, int back, Kind kind) {
        public BlockPos at(BlockPos controller, Direction front) {
            Direction rightAxis = front.getAxis().isHorizontal() ? front.getClockWise() : Direction.EAST;
            Direction upAxis = front == Direction.UP ? Direction.NORTH
                    : front == Direction.DOWN ? Direction.SOUTH : Direction.UP;
            return controller.relative(rightAxis, right).relative(upAxis, up)
                    .relative(front.getOpposite(), back);
        }
    }

    /** The four processor counters of GT6 {@code :68}. */
    public record Counts(int logic, int control, int storage, int conversion) {
        /** GT6 {@code :144}: every counter must be positive, and the structure must be complete. */
        public boolean valid() { return new LogisticsCoreLayout.Counts(logic,control,storage,conversion).valid(); }
        /** GT6 {@code :216}: minimum stored energy before a routing pass can start. */
        public long routingThreshold() { return new LogisticsCoreLayout.Counts(logic,control,storage,conversion).routingThreshold(); }
        /** GT6 {@code :699}: the nominal buffer ceiling (an incoming packet may exceed it). */
        public long energyCapacity() { return new LogisticsCoreLayout.Counts(logic,control,storage,conversion).energyCapacity(); }
        /** GT6 {@code :209,504}: fixed charge on every server tick. */
        public long fixedEnergyPerTick() { return new LogisticsCoreLayout.Counts(logic,control,storage,conversion).fixedEnergyPerTick(); }
    }

    private static final List<Cell> CELLS = create();

    private LogisticsCoreStructure() {}

    /** Every cube cell except the controller's own - GT6's scan never validates the main block. */
    public static List<Cell> cells() { return CELLS; }

    private static List<Cell> create() {
        return LogisticsCoreLayout.cells().stream().map(cell->new Cell(cell.right(),cell.up(),cell.back(),Kind.valueOf(cell.kind().name()))).toList();
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
        for(int id:new int[]{VERSATILE,LOGIC,CONTROL,STORAGE,CONVERSION}){if(block!=LargeMachineParts.block(id))continue;var contribution=LogisticsCoreLayout.contribution(id);return new Counts(contribution.logic(),contribution.control(),contribution.storage(),contribution.conversion());}
        return null;
    }

    /** The four counters of a complete structure, or {@code null} when a cell does not fit. */
    public static Counts counts(Level level, BlockPos controller, Direction front) {
        int logic = 0, control = 0, storage = 0, conversion = 0;
        for (var cell : CELLS) {
            var pos = cell.at(controller, front);
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

    // ── CPU economics (GT6 tooltips {@code :156-159}) ────────────────────────

    /** "Logic Processors increase Operation Count by 1" - operations per network pass. */
    public static int operations(Counts counts) { return counts.logic(); }

    /** GT6 {@code :443/:668}: maximum Chebyshev radius is two plus the control CPU count. */
    public static int range(Counts counts) { return counts.control() + 2; }

    /** "Storage Processors increase Buffer Size by 1 Stack or 16000L" - the port's own tank count. */
    public static int bufferTanks(Counts counts) { return counts.storage(); }

    /** "Conversion Processors increase Throughput by 1 Stack or 16000L" - per operation. */
    public static long throughput(Counts counts) { return counts.conversion(); }
}
