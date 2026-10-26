package gregtech.engine.machine;

/** Whole-family connection ownership and UV-density regressions, without a game renderer. */
public final class PipeGeometryContracts {
    private static final double[] SIZES = {1, 2, 3, 4, 5, 6, 7, 8};
    private static int connections;

    public static void main(String[] args) {
        for (int axis = 0; axis < 3; axis++) for (boolean positive : new boolean[]{false, true}) {
            for (double half : SIZES) {
                checkConnection(axis, positive, half, PipeConnectionGeometry.NONE);
                for (double neighbor : SIZES) {
                    checkConnection(axis, positive, half, neighbor);
                    checkConnection(axis, positive, half, PipeConnectionGeometry.virtual(neighbor));
                    if (half < neighbor) checkIncomingArm(axis, positive, half, neighbor);
                }
            }
        }
        System.out.println("Pipe geometry contracts passed: " + connections + " size/direction/socket combinations.");
    }

    private static void checkConnection(int axis, boolean positive, double half, double neighbor) {
        connections++;
        double[] local = PipeConnectionGeometry.localArm(axis, positive, half, neighbor);
        if (half == 8) {
            check(local == null, "full core needs no additional local arm");
            return;
        }
        check(local != null, "connected sub-block core must reach its cell boundary");
        for (int i = 0; i < 3; i++) {
            check(local[i] >= 0 && local[i + 3] <= 16 && local[i] < local[i + 3], "local bounds within cell");
            if (i != axis) {
                double size = neighbor == PipeConnectionGeometry.NONE ? half
                        : PipeConnectionGeometry.isVirtual(neighbor) ? PipeConnectionGeometry.virtualSize(neighbor) : neighbor;
                check(local[i + 3] - local[i] == 2 * Math.min(half, size), "no widening of incoming thin arm");
            }
        }
        check(local[axis] == (positive ? 8 + half : 0), "local arm start");
        check(local[axis + 3] == (positive ? 16 : 8 - half), "local arm end");
        double[] rendered = PipeConnectionGeometry.renderArm(axis, positive, half, neighbor);
        if (rendered == null) rendered = PipeConnectionGeometry.virtualStub(axis, positive, half, neighbor);
        if (rendered != null) checkTexture(rendered, axis);
    }

    private static void checkIncomingArm(int axis, boolean positive, double thin, double thick) {
        double[] rendered = PipeConnectionGeometry.renderArm(axis, positive, thin, thick);
        double[] outgoing = PipeConnectionGeometry.localArm(axis, positive, thin, thick);
        double[] incoming = PipeConnectionGeometry.localArm(axis, !positive, thick, thin);
        if (thick == 8) {
            check(incoming == null, "full-size neighbor needs no stub");
            for (int i = 0; i < rendered.length; i++)
                check(rendered[i] == outgoing[i], "full-size neighbor stops extension at boundary");
            return;
        }
        double shift = positive ? 16 : -16;
        double start = incoming[axis] + shift, end = incoming[axis + 3] + shift;
        check(positive ? outgoing[axis + 3] == start : end == outgoing[axis], "two local pieces meet without gap");
        check(rendered[axis] == Math.min(outgoing[axis], start)
                && rendered[axis + 3] == Math.max(outgoing[axis + 3], end), "render extent equals both owned pieces");
        check(positive ? rendered[axis + 3] > 16 : rendered[axis] < 0, "thin visual still extends into thick cell");
    }

    private static void checkTexture(double[] box, int axis) {
        double cursor = box[axis];
        for (double[] segment : PipeConnectionGeometry.textureSegments(box, axis)) {
            check(segment[axis] == cursor, "texture pieces meet without gap or overlap");
            double origin = PipeConnectionGeometry.textureOrigin(segment[axis], segment[axis + 3]);
            double u0 = PipeConnectionGeometry.textureCoordinate(segment[axis], origin);
            double u1 = PipeConnectionGeometry.textureCoordinate(segment[axis + 3], origin);
            check(u0 >= 0 && u1 <= 16, "UV inside sprite");
            check(u1 - u0 == segment[axis + 3] - segment[axis], "one texture pixel per model pixel; no stretching");
            for (int i = 0; i < 3; i++) if (i != axis)
                check(segment[i] == box[i] && segment[i + 3] == box[i + 3], "segment thickness unchanged");
            cursor = segment[axis + 3];
        }
        check(cursor == box[axis + 3], "texture covers the complete extension");
    }

    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
