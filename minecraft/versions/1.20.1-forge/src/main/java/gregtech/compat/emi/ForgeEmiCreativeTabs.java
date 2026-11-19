package gregtech.compat.emi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Isolate the temporary tab contents EMI builds with its own display parameters. */
public final class ForgeEmiCreativeTabs {
    private static final ThreadLocal<List<Runnable>> RESTORATIONS = new ThreadLocal<>();

    private ForgeEmiCreativeTabs() {}

    public static <T> T collect(Supplier<T> collection) {
        var previous = RESTORATIONS.get();
        var restorations = new ArrayList<Runnable>();
        RESTORATIONS.set(restorations);
        try {
            return collection.get();
        } finally {
            if (previous == null) RESTORATIONS.remove();
            else RESTORATIONS.set(previous);
            // The combined search tab needs all category contents until collection finishes.
            for (int i = restorations.size() - 1; i >= 0; i--)
                restorations.get(i).run();
        }
    }

    public static boolean isCollecting() {
        return RESTORATIONS.get() != null;
    }

    public static void restoreAfterCollection(Runnable restoration) {
        RESTORATIONS.get().add(restoration);
    }
}
