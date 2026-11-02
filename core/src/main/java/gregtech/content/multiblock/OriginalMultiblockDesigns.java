package gregtech.content.multiblock;

import gregtech.api.machine.BlockFace;
import java.util.function.IntUnaryOperator;

/** GT6 part texture indices that depend on a recipe controller's facing or work state. */
public final class OriginalMultiblockDesigns {
    private OriginalMultiblockDesigns() {}

    public static int resolve(
            String machine,
            int part,
            int declared,
            int right,
            int back,
            BlockFace front,
            boolean running,
            boolean active,
            IntUnaryOperator random) {
        if (declared < 0) return declared;
        return switch (machine) {
            case "fusionreactor" -> part == 18003 && declared == 5 ? (active ? 6 : 5) : declared;
            case "largecrusher", "largeshredder" ->
                part == 18107 || part == 18108
                        ? (front == BlockFace.EAST || front == BlockFace.WEST ? 2 : 0) + (running ? 1 : 0)
                        : declared;
            case "largeelectrolyzer" ->
                part == 18105 && declared == 0 ? (active ? 2 + random.applyAsInt(6) : 0) : declared;
            case "largesluice" -> part == 18106 ? front.index() - 2 + (active ? 4 : 0) : declared;
            case "largecentrifuge" -> part == 18100 ? centrifuge(right, back - 1, front) : declared;
            default -> declared;
        };
    }

    /** The eight top/bottom tiles are indexed in world X/Z, not relative to the controller. */
    private static int centrifuge(int right, int back, BlockFace front) {
        int x = switch (front) {
            case SOUTH -> -right;
            case WEST -> back;
            case EAST -> -back;
            default -> right;
        };
        int z = switch (front) {
            case SOUTH -> -back;
            case WEST -> -right;
            case EAST -> right;
            default -> back;
        };
        int tile = (z + 1) * 3 + x + 1;
        return tile == 4 ? 0 : tile < 4 ? tile + 1 : tile;
    }
}
