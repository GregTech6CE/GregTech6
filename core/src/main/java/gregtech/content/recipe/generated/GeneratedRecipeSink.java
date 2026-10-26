package gregtech.content.recipe.generated;
/** Scoped native consumer for the single original recipe-row catalog. No platform objects live here. */
public final class GeneratedRecipeSink {
    @FunctionalInterface
    public interface Sink {
        void accept(String map, long eut, long ticks, long[] chances, String[] itemsIn,
                    String[] fluidsIn, String[] fluidsOut, String[] itemsOut);
    }

    @FunctionalInterface
    public interface SourceSink {
        void accept(boolean optimize, String map, long eut, long ticks, long[] chances,
                    String[] itemsIn, String[] fluidsIn, String[] fluidsOut, String[] itemsOut);
    }
    private static final ThreadLocal<SourceSink> SOURCE = new ThreadLocal<>();

    public static void emitSource(SourceSink sink, Runnable rows) {
        var previous = SOURCE.get();
        SOURCE.set(java.util.Objects.requireNonNull(sink));
        var previousLegacy = CURRENT.get();
        CURRENT.set((m, p, t, c, i, fi, fo, o) -> sink.accept(false, m, p, t, c, i, fi, fo, o));
        try { rows.run(); }
        finally {
            if (previous == null) SOURCE.remove(); else SOURCE.set(previous);
            if (previousLegacy == null) CURRENT.remove(); else CURRENT.set(previousLegacy);
        }
    }

    static void registerSource(boolean optimize, String map, long eut, long ticks, long[] chances,
            String[] itemsIn, String[] fluidsIn, String[] fluidsOut, String[] itemsOut) {
        var sink = SOURCE.get();
        if (sink != null) sink.accept(optimize, map, eut, ticks, chances, itemsIn, fluidsIn, fluidsOut, itemsOut);
        else register(map, eut, ticks, chances, itemsIn, fluidsIn, fluidsOut, itemsOut);
    }

    private static final ThreadLocal<Sink> CURRENT = new ThreadLocal<>();

    private GeneratedRecipeSink() {}

    public static void emit(Sink sink, Runnable rows) {
        java.util.Objects.requireNonNull(sink);
        var previous = CURRENT.get();
        var previousSource = SOURCE.get();
        SOURCE.remove();
        CURRENT.set(java.util.Objects.requireNonNull(sink));
        try {
            rows.run();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
            if (previousSource == null) SOURCE.remove(); else SOURCE.set(previousSource);
        }
    }

    static void register(String map, long eut, long ticks, long[] chances, String[] itemsIn,
                         String[] fluidsIn, String[] fluidsOut, String[] itemsOut) {
        var sink = CURRENT.get();
        if (sink == null) throw new IllegalStateException("Recipe rows need a platform resolver");
        sink.accept(map, eut, ticks, chances, itemsIn, fluidsIn, fluidsOut, itemsOut);
    }
}
