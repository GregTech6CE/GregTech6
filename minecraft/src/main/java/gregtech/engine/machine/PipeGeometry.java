package gregtech.engine.machine;


//? if >=1.21.1 {
//?} else {
import gregtech.content.block.energy.ElectricWireBlock;
//?}
import gregtech.content.block.machine.FluidPipeBlock;

//? if >=1.21.1 {
//?} else {
import gregtech.content.block.machine.ItemPipeBlock;
//?}
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Shared connection-arm geometry for pipes/wires (GT6 rules), in 1/16 px units.
 * Rendered extensions retain the thin pipe's material. Physical and selection
 * shapes divide the same connection at the cell boundary via {@link #localArmBox}.
 *
 * <p>GT6 sizing rules per connected side:</p>
 * <ul>
 *   <li>neighbor pipe thinner — we draw nothing; the thin pipe reaches into us</li>
 *   <li>neighbor pipe equal — arm to the block boundary (both halves meet)</li>
 *   <li>neighbor pipe thicker — arm extends past the boundary up to the
 *       neighbor's core surface</li>
 *   <li>anything else (machine, chest, …) — arm to the boundary with an end cap</li>
 * </ul>
 */
public final class PipeGeometry {

    /** Sentinel for "connected to something that is not a pipe/wire". */
    public static final double NONE = PipeConnectionGeometry.NONE;

    private PipeGeometry() {}

    /** Inventory connectors are a straight north/south segment with two exposed ends. */
    public static boolean itemConnected(Direction side) { return side.getAxis() == Direction.Axis.Z; }

    // ── Virtual pipe neighbors ───────────────────────────────────────────────
    // Machines with GT6 pipe connectors (steam engines: tiny on the water
    // sides, medium on the steam input) act as virtual pipes of that size.
    // Encoded below NONE so the value still travels through one double/float.

    /** Encode a virtual connector size (px half-thickness). */
    public static double virtual(double half) { return PipeConnectionGeometry.virtual(half); }

    public static boolean isVirtual(double n) { return PipeConnectionGeometry.isVirtual(n); }

    /** Decode {@link #virtual}. */
    public static double virtualSize(double n) { return PipeConnectionGeometry.virtualSize(n); }

    /**
     * Arm cuboid toward {@code dir}: {x0, y0, z0, x1, y1, z1} in px (may extend
     * past 0/16 into the neighbor), or null when the thinner-neighbor rule
     * suppresses the arm.
     */
    @Nullable
    public static double[] armBox(Direction dir, double half, double neighborHalf) {
        return PipeConnectionGeometry.renderArm(dir.getAxis().ordinal(),
                dir.getAxisDirection() == Direction.AxisDirection.POSITIVE, half, neighborHalf);
    }

    /** One cell's part of a connection; used by both collision and selection queries. */
    @Nullable
    public static double[] localArmBox(Direction dir, double half, double neighborHalf) {
        return PipeConnectionGeometry.localArm(dir.getAxis().ordinal(),
                dir.getAxisDirection() == Direction.AxisDirection.POSITIVE, half, neighborHalf);
    }

    /** Whether the arm toward this neighbor carries an end cap at the boundary. */
    public static boolean hasEndCap(double neighborHalf) {
        return neighborHalf == NONE || isVirtual(neighborHalf);
    }

    /**
     * Thin stub toward a virtual connector smaller than this pipe: the machine's
     * virtual pipe (e.g. steam engine tiny/medium socket) reaches from the block
     * boundary to this pipe's core surface, rendered by this pipe.
     * @return {x0, y0, z0, x1, y1, z1} in px, or null when no stub applies
     */
    @Nullable
    public static double[] stubBox(Direction dir, double half, double neighborHalf) {
        return PipeConnectionGeometry.virtualStub(dir.getAxis().ordinal(),
                dir.getAxisDirection() == Direction.AxisDirection.POSITIVE, half, neighborHalf);
    }

    /**
     * Neighbor half-thickness as seen by a fluid pipe: real pipes report their
     * size; GT6 steam engines act as virtual pipes — tiny (2 px half) on the
     * four distilled-water sides, medium (4 px half) on the steam input face,
     * nothing on the kinetic output front.
     * @param direction direction from the pipe toward the neighbor
     */
    public static double fluidNeighborHalf(BlockState state, Direction direction) {
        double half = pipeHalfOf(state);
        if (half != NONE) return half;
        return state.getBlock() instanceof FluidConnectorGeometry connector
                ? connector.fluidConnectorHalf(state, direction.getOpposite()) : NONE;
    }

    /** A machine socket exposes its original attachment width to adjoining pipe geometry. */
    public interface FluidConnectorGeometry {
        double fluidConnectorHalf(BlockState state, Direction face);
    }

    /** px box from {@link #armBox} → block-unit VoxelShape. */
    public static net.minecraft.world.phys.shapes.VoxelShape boxShape(double[] b) {
        return net.minecraft.world.phys.shapes.Shapes.box(
                b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0, b[5] / 16.0);
    }

    /** Half-thickness of a pipe-family neighbor (fluid/item pipes connect to each other). */
//? if >=1.21.1 {
    /*public static double pipeHalfOf(BlockState state){
        if(state.getBlock() instanceof FluidPipeBlock pipe)return Math.max(1,pipe.spec().diameter()*8);
        if(state.getBlock() instanceof gregtech.engine.inventory.PipeFormLike pipe){
            String size=pipe.pipeSizeName().replace("RESTRICTIVE_","");
            for(var candidate:PipeSpec.PipeSize.values())if(candidate.name().equals(size))return Math.max(1,candidate.diameter()*8);
*///?} else {
    public static double pipeHalfOf(BlockState state) {
        if (state.getBlock() instanceof FluidPipeBlock pipe) {
            return Math.max(1.0, pipe.spec().diameter() * 8.0);
        }
        if (state.getBlock() instanceof ItemPipeBlock pipe) {
            return Math.max(1.0, pipe.spec().diameter() * 8.0);
//?}
        }
        return NONE;
    }

    /** Half-thickness of a wire-family neighbor. */
//? if >=1.21.1 {
    /*public static double wireHalfOf(BlockState state){if(state.getBlock() instanceof gregtech.content.block.energy.SignalWireBlock wire)return wire.halfThickness();return state.getBlock() instanceof gregtech.engine.energy.WireMaterialLike wire?wire.spec().halfThickness():NONE;}
*///?} else {
    public static double wireHalfOf(BlockState state) {
        if (state.getBlock() instanceof ElectricWireBlock wire) {
            return wire.spec().halfThickness();
        }
        return NONE;
    }
//?}

    /** Half-thickness of an axle-family neighbor. */
//? if >=1.21.1 {
    /*public static double axleHalfOf(BlockState state){return state.getBlock() instanceof AxleConnectorGeometry axle?axle.axleHalfThickness():NONE;}
    public interface AxleConnectorGeometry { double axleHalfThickness(); }
*///?} else {
    public static double axleHalfOf(BlockState state) {
        if (state.getBlock() instanceof gregtech.content.block.energy.AxleBlock axle) {
            return axle.spec().halfThickness();
        }
        return NONE;
    }
//?}
}
