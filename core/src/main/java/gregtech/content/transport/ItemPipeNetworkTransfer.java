package gregtech.content.transport;

import gregtech.content.transport.WeightedItemPipeRoutes.Exit;
import gregtech.content.transport.WeightedItemPipeRoutes.Routes;

/**
 * Delivers a source pipe's stored stacks along its cached {@link Routes}: straight into the nearest
 * exit that takes them, never resting in the pipes between.
 *
 * <p>Bandwidth is counted in stacks: every stack moved charges one transfer to each pipe on its
 * path, source and exit pipe included. An exit is skipped while any pipe on its path has spent its
 * allowance or a link on it is closed, and the next-nearest exit is tried instead. Only the cached
 * shortest path is considered; another path to the same exit is not searched for.</p>
 */
public final class ItemPipeNetworkTransfer {
    private ItemPipeNetworkTransfer() {}

    /** The platform side of one delivery pass from one source. */
    public interface Channel {
        boolean hasItems();

        /** Whether {@code pipe} may carry another stack in the current bandwidth window. */
        boolean hasCapacity(int pipe);

        void charge(int pipe);

        /** Gates that change without a topology rebuild (shutters, cover modes) on link {@code from} to {@code to}. */
        default boolean linkOpen(int from, int to) {
            return true;
        }

        /** Move one stack out through {@code exit}; per-stack rules such as filters apply here. */
        boolean send(Exit exit);
    }

    /** @return the number of stacks moved */
    public static int deliver(Routes routes, Channel channel) {
        int moved = 0;
        int[] previous = routes.previous();
        for (Exit exit : routes.exits()) {
            if (!channel.hasItems() || !channel.hasCapacity(routes.source())) break;
            while (channel.hasItems() && open(exit.pipe(), previous, channel) && channel.send(exit)) {
                moved++;
                for (int pipe = exit.pipe(); pipe >= 0; pipe = previous[pipe]) channel.charge(pipe);
            }
        }
        return moved;
    }

    private static boolean open(int pipe, int[] previous, Channel channel) {
        for (int at = pipe; ; ) {
            if (!channel.hasCapacity(at)) return false;
            int before = previous[at];
            if (before < 0) return true;
            if (!channel.linkOpen(before, at)) return false;
            at = before;
        }
    }

    /**
     * Whether a pipe that has carried {@code transferred} stacks this window may carry another: an
     * idle pipe always may, otherwise up to {@code max(1, allowance)} stacks.
     */
    public static boolean hasCapacity(long transferred, long allowance) {
        return transferred <= 0 || transferred < Math.max(1, allowance);
    }
}
