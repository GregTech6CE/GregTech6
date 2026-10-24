package com.gregtech.gregtech.api.addon;

/** SPDX-License-Identifier: LGPL-3.0-or-later. One-time bind, lock-free read. */
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
