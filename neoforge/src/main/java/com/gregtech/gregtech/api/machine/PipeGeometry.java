package com.gregtech.gregtech.api.machine;


import com.gregtech.gregtech.block.machine.FluidPipeBlock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Shared connection-arm geometry for pipes/wires (GT6 rules), in 1/16 px units.
 * Drives both the VoxelShapes and the dynamic baked model so the selection box
 * always matches the rendered geometry.
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
    public static final double NONE = -1.0;

    private PipeGeometry() {}

    /** Inventory connectors are a straight north/south segment with two exposed ends. */
    public static boolean itemConnected(Direction side) { return side.getAxis() == Direction.Axis.Z; }

    // ── Virtual pipe neighbors ───────────────────────────────────────────────
    // Machines with GT6 pipe connectors (steam engines: tiny on the water
    // sides, medium on the steam input) act as virtual pipes of that size.
    // Encoded below NONE so the value still travels through one double/float.

    /** Encode a virtual connector size (px half-thickness). */
    public static double virtual(double half) { return -2.0 - half; }

    public static boolean isVirtual(double n) { return n <= -2.0; }

    /** Decode {@link #virtual}. */
    public static double virtualSize(double n) { return -2.0 - n; }

    /**
     * Arm cuboid toward {@code dir}: {x0, y0, z0, x1, y1, z1} in px (may extend
     * past 0/16 into the neighbor), or null when the thinner-neighbor rule
     * suppresses the arm.
     */
    @Nullable
    public static double[] armBox(Direction dir, double half, double neighborHalf) {
        double reach; // px past the block boundary
        if (neighborHalf == NONE) {
            reach = 0.0;
        } else if (isVirtual(neighborHalf)) {
            // virtual connector: thinner → stub instead (see stubBox); else flush arm
            if (virtualSize(neighborHalf) < half) return null;
            reach = 0.0;
        } else if (neighborHalf < half) {
            return null;
        } else if (neighborHalf > half) {
            reach = Math.max(0.0, 8.0 - neighborHalf);
        } else {
            reach = 0.0;
        }
        double lo = 8.0 - half, hi = 8.0 + half;
        boolean positive = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        double from = positive ? hi : -reach;
        double to = positive ? 16.0 + reach : lo;
        if (from >= to) return null;
        return switch (dir.getAxis()) {
            case X -> new double[]{from, lo, lo, to, hi, hi};
            case Y -> new double[]{lo, from, lo, hi, to, hi};
            case Z -> new double[]{lo, lo, from, hi, hi, to};
        };
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
        if (!isVirtual(neighborHalf)) return null;
        double v = virtualSize(neighborHalf);
        if (v >= half) return null;
        double lo = 8.0 - v, hi = 8.0 + v;
        boolean positive = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        double from = positive ? 8.0 + half : 0.0;
        double to = positive ? 16.0 : 8.0 - half;
        if (from >= to) return null;
        return switch (dir.getAxis()) {
            case X -> new double[]{from, lo, lo, to, hi, hi};
            case Y -> new double[]{lo, from, lo, hi, to, hi};
            case Z -> new double[]{lo, lo, from, hi, hi, to};
        };
    }

    /**
     * Neighbor half-thickness as seen by a fluid pipe: real pipes report their
     * size; GT6 steam engines act as virtual pipes — tiny (2 px half) on the
     * four distilled-water sides, medium (4 px half) on the steam input face,
     * nothing on the kinetic output front.
     * @param dirFromPipe direction from the pipe toward the neighbor
     */
    public static double fluidNeighborHalf(BlockState state,Direction direction){
        double half=pipeHalfOf(state);
        if(half!=NONE)return half;
        return state.getBlock() instanceof FluidConnectorGeometry connector?connector.fluidConnectorHalf(state,direction.getOpposite()):NONE;
    }
    /** Thin world boundary for future original steam-engine tiny/medium sockets. */
    public interface FluidConnectorGeometry { double fluidConnectorHalf(BlockState state,Direction face); }

    /** px box from {@link #armBox} → block-unit VoxelShape. */
    public static net.minecraft.world.phys.shapes.VoxelShape boxShape(double[] b) {
        return net.minecraft.world.phys.shapes.Shapes.box(
                b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0, b[5] / 16.0);
    }

    /** Half-thickness of a pipe-family neighbor (fluid/item pipes connect to each other). */
    public static double pipeHalfOf(BlockState state){
        if(state.getBlock() instanceof FluidPipeBlock pipe)return Math.max(1,pipe.spec().diameter()*8);
        if(state.getBlock() instanceof com.gregtech.gregtech.api.inventory.PipeFormLike pipe){
            String size=pipe.pipeSizeName().replace("RESTRICTIVE_","");
            for(var candidate:PipeSpec.PipeSize.values())if(candidate.name().equals(size))return Math.max(1,candidate.diameter()*8);
        }
        return NONE;
    }

    /** Half-thickness of a wire-family neighbor. */
    public static double wireHalfOf(BlockState state){if(state.getBlock() instanceof com.gregtech.gregtech.block.energy.SignalWireBlock wire)return wire.halfThickness();return state.getBlock() instanceof com.gregtech.gregtech.api.energy.WireMaterialLike wire?wire.spec().halfThickness():NONE;}

    /** Half-thickness of an axle-family neighbor. */
    public static double axleHalfOf(BlockState state){return state.getBlock() instanceof AxleConnectorGeometry axle?axle.axleHalfThickness():NONE;}
    public interface AxleConnectorGeometry { double axleHalfThickness(); }
}
