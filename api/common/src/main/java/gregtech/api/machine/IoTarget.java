/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

/**
 * One large-recipe input or output cell. The origin is rejected and each component
 * is from -16 to 16. Structure cells do not use this limit.
 *
 * @param right offset to the right
 * @param up offset upward
 * @param back offset backward
 * @param receiver the receiving face
 */
public record IoTarget(int right, int up, int back, MachineFace receiver) {
    public IoTarget {
        if (receiver == null) throw new IllegalArgumentException("Missing receiver");
        if (right == 0 && up == 0 && back == 0)
            throw new IllegalArgumentException("IO target cannot be the origin");
        if (right < -16 || right > 16 || up < -16 || up > 16 || back < -16 || back > 16)
            throw new IllegalArgumentException("IO target is out of range: " + right + "," + up + "," + back);
    }

    public static IoTarget at(int right, int up, int back, MachineFace receiver) {
        return new IoTarget(right, up, back, receiver);
    }
}
