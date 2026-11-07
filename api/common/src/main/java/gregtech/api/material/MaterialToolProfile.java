/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

/** Immutable material tool parameters. */
public record MaterialToolProfile(int types, float speed, long durability, int quality) {
    public MaterialToolProfile {
        if (types < 0 || !Float.isFinite(speed) || speed < 0 || durability < 0 || quality < 0)
            throw new IllegalArgumentException("Invalid tool profile");
    }
}
