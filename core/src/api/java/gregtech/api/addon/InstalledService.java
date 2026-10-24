/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

/**
 * Internal: not part of the addon contract; may change without notice.
 * One-time bind, lock-free read. The first bind owns the implementation; a later bind fails.
 * GregTech installs the platform services; addons must not call {@code bind}.
 */
public final class InstalledService<T> {
    private volatile T value;

    public void bind(T implementation, String missingArgument, String alreadyInstalled) {
        if (implementation == null) throw new IllegalArgumentException(missingArgument);
        synchronized (this) {
            if (value != null) throw new IllegalStateException(alreadyInstalled);
            value = implementation;
        }
    }

    public T get(String missing) {
        T installed = value;
        if (installed == null) throw new IllegalStateException(missing);
        return installed;
    }
}
