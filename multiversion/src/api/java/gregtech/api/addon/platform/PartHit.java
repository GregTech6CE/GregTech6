/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.core.Direction;

/**
 * Where a player clicked a part. {@code x}, {@code y} and {@code z} are fractions of the block,
 * each measured from the low corner of that axis.
 */
public final class PartHit {
    private final Direction face;
    private final double x;
    private final double y;
    private final double z;

    private PartHit(Direction face, double x, double y, double z) {
        this.face = face;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static PartHit at(Direction face, double x, double y, double z) {
        if (face == null) throw new IllegalArgumentException("Missing face");
        return new PartHit(face, x, y, z);
    }

    public Direction face() { return face; }

    public double x() { return x; }

    public double y() { return y; }

    public double z() { return z; }
}
