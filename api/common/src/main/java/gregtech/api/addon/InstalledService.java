/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

/**
 * Internal: not part of the addon contract; may change without notice.
 * One-time bind, lock-free read. The first bind owns the implementation; a later bind fails
 * with the first binder's class and a cause carrying its call stack.
 * GregTech installs the platform services; addons must not call {@code bind}.
 */
public final class InstalledService<T> {
    private volatile T value;
    private Throwable firstBind;

    public void bind(T implementation, String missingArgument, String alreadyInstalled) {
        if (implementation == null) throw new IllegalArgumentException(missingArgument);
        synchronized (this) {
            if (value != null) {
                Throwable first = firstBind;
                throw new IllegalStateException(alreadyInstalled + " (first bound by " + describe(first) + ")", first);
            }
            firstBind = new Throwable("first bind");
            value = implementation;
        }
    }

    public T get(String missing) {
        T installed = value;
        if (installed == null) throw new IllegalStateException(missing);
        return installed;
    }

    private static String describe(Throwable first) {
        StackTraceElement fallback = null;
        StackTraceElement[] trace = first == null ? new StackTraceElement[0] : first.getStackTrace();
        for (StackTraceElement frame : trace) {
            String type = frame.getClassName();
            if (type.equals(InstalledService.class.getName())) continue;
            if (fallback == null) fallback = frame;
            if (type.startsWith("gregtech.api.") || type.startsWith("java.") || type.startsWith("jdk.")
                    || type.startsWith("sun.") || type.startsWith("com.sun.")) continue;
            return format(frame);
        }
        return fallback == null ? "unknown" : format(fallback);
    }

    private static String format(StackTraceElement frame) {
        return frame.getClassName() + "." + frame.getMethodName()
                + "(" + frame.getFileName() + ":" + frame.getLineNumber() + ")";
    }
}
