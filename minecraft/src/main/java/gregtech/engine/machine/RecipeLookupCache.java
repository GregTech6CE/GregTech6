package gregtech.engine.machine;

import com.mojang.logging.LogUtils;
import gregtech.engine.recipe.RecipeMap;
import java.util.Locale;
import java.util.concurrent.atomic.LongAdder;
import org.slf4j.Logger;

/**
 * Cross-tick reuse of one machine's static "no recipe" answer.
 *
 * <p>An entry is valid only while the map identity, the machine's input-state version and the
 * map's lookup revision are unchanged and the recheck timer has not expired. Rechecks land on a
 * per-machine phase of the {@link #RECHECK_TICKS} period, so idle machines do not rescan in the
 * same tick, and never more than {@code RECHECK_TICKS} later. Only a null static lookup may be
 * recorded; a positive match, a cost or an output-capacity refusal is never reused this way.</p>
 *
 * <p>{@code -Dgt6ce.recipeNegativeCache=off} (or {@code GT6CE_RECIPE_NEGATIVE_CACHE=off}) disables
 * the reuse. {@code -Dgt6ce.recipeNegativeCacheStats=true} logs real lookups, hits, timed rechecks
 * and active invalidations so a save can be compared with and without the cache.</p>
 */
final class RecipeLookupCache {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Upper bound of a cached negative's lifetime; per-machine phases only shorten the first one. */
    static final int RECHECK_TICKS = 100;

    static final boolean ENABLED = !off(setting("gt6ce.recipeNegativeCache", "GT6CE_RECIPE_NEGATIVE_CACHE"));
    private static final boolean STATS =
            truthy(setting("gt6ce.recipeNegativeCacheStats", "GT6CE_RECIPE_NEGATIVE_CACHE_STATS"));

    private static final LongAdder LOOKUPS = new LongAdder();
    private static final LongAdder HITS = new LongAdder();
    private static final LongAdder RECHECKS = new LongAdder();
    private static final LongAdder INVALIDATIONS = new LongAdder();

    private boolean cached;
    private RecipeMap map;
    private long revision;
    private long inputVersion;
    private long recheckAt;

    private static String setting(String property, String environment) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(environment);
        return value == null ? "" : value.trim();
    }

    private static boolean off(String value) {
        return value.equalsIgnoreCase("off") || value.equalsIgnoreCase("legacy") || value.equalsIgnoreCase("false");
    }

    private static boolean truthy(String value) {
        return !value.isEmpty() && !off(value) && !value.equals("0");
    }

    /** Counts one lookup that the cache did not answer, and periodically reports the totals. */
    static void countLookup() {
        if (!STATS) return;
        LOOKUPS.increment();
        if ((LOOKUPS.sum() & 0xFFFFL) == 0) report();
    }

    /**
     * Whether the entry still answers {@code map} for these inputs at {@code now}. An expired
     * entry is dropped and counted as a timed recheck; a different map, version or revision is
     * dropped as an active invalidation (recipe, tag or composition changes reach us this way).
     */
    boolean hit(RecipeMap map, long now, long inputVersion) {
        if (!cached) return false;
        if (now == Long.MIN_VALUE
                || map != this.map
                || inputVersion != this.inputVersion
                || map.lookupRevision() != revision) {
            if (STATS) INVALIDATIONS.increment();
            clear();
            return false;
        }
        if (now >= recheckAt) {
            clear();
            if (STATS) RECHECKS.increment();
            return false;
        }
        if (STATS) HITS.increment();
        return true;
    }

    /**
     * Remembers a static "no recipe" answer until the timer expires. {@code eligible} must come
     * from a check that the map has no dynamic, contextual, USB or live-predicate path; without a
     * world object the answer is never reused across ticks.
     */
    void record(RecipeMap map, long now, long inputVersion, boolean eligible, long seed) {
        clear();
        if (!ENABLED || !eligible || now == Long.MIN_VALUE) return;
        cached = true;
        this.map = map;
        this.revision = map.lookupRevision();
        this.inputVersion = inputVersion;
        this.recheckAt = nextRecheck(now, seed);
    }

    /**
     * The smallest tick after {@code now} on this machine's phase of the recheck period. The first
     * entry is spread across the whole period; later entries follow exactly one period later.
     */
    private static long nextRecheck(long now, long seed) {
        long phase = Math.floorMod(seed, RECHECK_TICKS);
        long candidate = now + 1;
        return candidate + Math.floorMod(phase - candidate, RECHECK_TICKS);
    }

    /** Drops a live entry because inputs or machine settings changed. */
    void invalidate() {
        if (!cached) return;
        if (STATS) INVALIDATIONS.increment();
        clear();
    }

    private void clear() {
        cached = false;
        map = null;
    }

    private static void report() {
        LOGGER.info(
                "[gregtech] Recipe negative cache stats: {} lookups, {} hits, {} timed rechecks, "
                        + "{} active invalidations",
                LOOKUPS.sumThenReset(),
                HITS.sumThenReset(),
                RECHECKS.sumThenReset(),
                INVALIDATIONS.sumThenReset());
    }

    static {
        if (!ENABLED) LOGGER.warn("[gregtech] Recipe negative cache disabled ({})", nameOfOff());
    }

    private static String nameOfOff() {
        return setting("gt6ce.recipeNegativeCache", "GT6CE_RECIPE_NEGATIVE_CACHE")
                .toLowerCase(Locale.ROOT);
    }
}
