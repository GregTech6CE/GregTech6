package gregtech.engine.machine;

import java.util.ArrayList;
import java.util.List;

/** Pipe connection bounds in model pixels; axis indices are X=0, Y=1, Z=2. */
public final class PipeConnectionGeometry {
    public static final double NONE = -1;

    private PipeConnectionGeometry() {}

    public static double virtual(double half) { return -2 - half; }
    public static boolean isVirtual(double neighbor) { return neighbor <= -2; }
    public static double virtualSize(double neighbor) { return -2 - neighbor; }

    /** The thin pipe owns its rendered extension, preserving its material and overlays. */
    public static double[] renderArm(int axis, boolean positive, double half, double neighbor) {
        double reach = 0;
        if (isVirtual(neighbor)) {
            if (virtualSize(neighbor) < half) return null;
        } else if (neighbor != NONE) {
            if (neighbor < half) return null;
            if (neighbor > half) reach = Math.max(0, 8 - neighbor);
        }
        return box(axis, half, positive ? 8 + half : -reach, positive ? 16 + reach : 8 - half);
    }

    /** Physical/selection ownership stops at the cell boundary, including incoming thin arms. */
    public static double[] localArm(int axis, boolean positive, double half, double neighbor) {
        double width = neighbor == NONE ? half : isVirtual(neighbor) ? virtualSize(neighbor) : neighbor;
        return box(axis, Math.min(half, width), positive ? 8 + half : 0, positive ? 16 : 8 - half);
    }

    /** A machine socket has no pipe model of its own, so the adjoining pipe renders its stub. */
    public static double[] virtualStub(int axis, boolean positive, double half, double neighbor) {
        if (!isVirtual(neighbor) || virtualSize(neighbor) >= half) return null;
        return localArm(axis, positive, half, neighbor);
    }

    private static double[] box(int axis, double half, double from, double to) {
        if (from >= to || half <= 0) return null;
        double[] box = {8 - half, 8 - half, 8 - half, 8 + half, 8 + half, 8 + half};
        box[axis] = from;
        box[axis + 3] = to;
        return box;
    }

    /** Split extended tubes at texture tile boundaries instead of stretching/clamping edge texels. */
    public static List<double[]> textureSegments(double[] box, int axis) {
        double from = box[axis], to = box[axis + 3];
        double boundary = (Math.floor(from / 16) + 1) * 16;
        if (boundary >= to) return List.<double[]>of(box);
        List<double[]> segments = new ArrayList<>();
        while (from < to) {
            double end = Math.min(boundary, to);
            double[] segment = box.clone();
            segment[axis] = from;
            segment[axis + 3] = end;
            segments.add(segment);
            from = end;
            boundary += 16;
        }
        return segments;
    }

    /** A segment ending exactly at a tile edge keeps UV=16, rather than wrapping that vertex to 0. */
    public static double textureOrigin(double min, double max) {
        return Math.floor((min + max) / 32) * 16;
    }

    public static float textureCoordinate(double position, double origin) {
        // Only the tiny overlay offsets may fall outside a segment's texture tile.
        return (float) Math.max(0, Math.min(16, position - origin));
    }
}
