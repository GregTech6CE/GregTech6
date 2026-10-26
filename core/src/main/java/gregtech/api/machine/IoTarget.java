/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import java.util.Objects;

/**
 * One large-recipe input or output cell. The origin is rejected and each component
 * is from -16 to 16. Structure cells do not use this limit.
 */
public final class IoTarget {
    private final int right;
    private final int up;
    private final int back;
    private final MachineFace receiver;

    private IoTarget(int right, int up, int back, MachineFace receiver) {
        this.right = right;
        this.up = up;
        this.back = back;
        this.receiver = receiver;
    }

    public int right() { return right; }

    public int up() { return up; }

    public int back() { return back; }

    public MachineFace receiver() { return receiver; }

    public static IoTarget at(int right, int up, int back, MachineFace receiver) {
        if (receiver == null) throw new IllegalArgumentException("Missing receiver");
        if (right == 0 && up == 0 && back == 0)
            throw new IllegalArgumentException("IO target cannot be the origin");
        if (right < -16 || right > 16 || up < -16 || up > 16 || back < -16 || back > 16)
            throw new IllegalArgumentException("IO target is out of range: " + right + "," + up + "," + back);
        return new IoTarget(right, up, back, receiver);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof IoTarget that)) return false;
        return right == that.right && up == that.up && back == that.back && receiver == that.receiver;
    }

    @Override public String toString() {
        return "IoTarget[at=" + right + "/" + up + "/" + back + ", receiver=" + receiver + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(right, up, back, receiver);
    }
}
