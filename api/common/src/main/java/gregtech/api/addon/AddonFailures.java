/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

/**
 * Internal: not part of the addon contract; may change without notice.
 * Addon callback failures keep {@link Error} unwrapped.
 */
public final class AddonFailures {
    private AddonFailures() {}

    /**
     * Returns an {@link IllegalStateException} for a {@link RuntimeException}.
     * An {@link Error} is rethrown as-is so the JVM can handle it (OOM, linkage).
     */
    public static IllegalStateException of(String message, Throwable failure) {
        if (failure instanceof Error error) throw error;
        return new IllegalStateException(message, failure);
    }
}
