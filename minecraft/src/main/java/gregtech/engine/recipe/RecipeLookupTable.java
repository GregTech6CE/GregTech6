package gregtech.engine.recipe;

import com.mojang.logging.LogUtils;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import org.slf4j.Logger;

/**
 * Ordered recipe candidates for the two static lookup stages.
 *
 * <p>The exact stage requires every required item of a row to be present, so indexing one required
 * input per row loses nothing; pure-fluid rows use a required fluid instead because
 * {@code possibleInputs} requires every non-empty required fluid to be present. The alias stage
 * uses the complete match-key set of one required item input; rows that cannot be keyed stay in an
 * ordered fallback. Candidates are merged back into snapshot order because the first matching row
 * wins.
 *
 * <p>One immutable {@link IndexState} is published through a volatile field and dropped by
 * {@link #invalidate()}; readers never mix a snapshot with another generation's buckets. The key
 * functions live in {@link RecipeInputs} next to the matcher they mirror.
 *
 * <p>{@code -Dgt6ce.recipeIndex=legacy} keeps the full scan for comparison,
 * {@code -Dgt6ce.recipeIndex=verify} runs both and logs ordered mismatches (rate-limited), and
 * {@code -Dgt6ce.recipeIndexStats=true} logs build sizes and query samples.
 */
final class RecipeLookupTable extends AbstractCollection<Recipe> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private enum Mode { INDEX, LEGACY, VERIFY }

    private static final Mode MODE = mode();
    private static final boolean STATS = flag("gt6ce.recipeIndexStats", "GT6CE_RECIPE_INDEX_STATS");
    private static final boolean AUDIT = flag("gt6ce.recipeIndexAudit", "GT6CE_RECIPE_INDEX_AUDIT");

    private static String setting(String property, String environment) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(environment);
        return value == null ? "" : value.trim();
    }

    private static boolean flag(String property, String environment) {
        String value = setting(property, environment);
        return value.isEmpty() ? Boolean.getBoolean(property) : !value.equalsIgnoreCase("false");
    }

    private static Mode mode() {
        return switch (setting("gt6ce.recipeIndex", "GT6CE_RECIPE_INDEX").toLowerCase(Locale.ROOT)) {
            case "legacy", "off", "full" -> Mode.LEGACY;
            case "verify" -> Mode.VERIFY;
            default -> Mode.INDEX;
        };
    }

    static boolean verifying() { return MODE == Mode.VERIFY; }

    private static final AtomicInteger MISMATCH_DETAILS = new AtomicInteger();
    private static final LongAdder MISMATCHES = new LongAdder();

    private final Collection<Recipe> rows;
    private final String name;
    private volatile IndexState state;
    private final AtomicLong revision = new AtomicLong();
    private volatile long observedComposition = -1;
    private final Deque<Candidates> recent = new ArrayDeque<>();

    private final LongAdder queries = new LongAdder();
    private final LongAdder exactScanned = new LongAdder();
    private final LongAdder alternativeScanned = new LongAdder();
    private final long[] samples = STATS ? new long[4096] : null;
    private long sampleCursor;

    static final class Candidates {
        final List<ItemStack> items;
        final List<FluidStack> fluids;
        private final List<Recipe> exact;
        private List<Recipe> alternatives;
        private java.util.function.Supplier<List<Recipe>> alternativeBuilder;

        Candidates(List<ItemStack> items, List<FluidStack> fluids, List<Recipe> exact,
                   java.util.function.Supplier<List<Recipe>> alternativeBuilder) {
            this.items = items;
            this.fluids = fluids;
            this.exact = exact;
            this.alternativeBuilder = alternativeBuilder;
        }

        List<Recipe> exact() { return exact; }

        List<Recipe> alternatives() {
            if (alternatives == null) {
                alternatives = alternativeBuilder.get();
                alternativeBuilder = null;
            }
            return alternatives;
        }
    }

    /** One published generation: the row snapshot and every bucket built against that snapshot. */
    private static final class IndexState {
        final Recipe[] snapshot;
        final long composition;
        final long revision;
        final boolean indexed;
        /** Any row whose lookup answer depends on a live item guard or predicate. */
        final boolean livePredicates;
        /** Any row that is skipped while the output slots are occupied. */
        final boolean emptyOutputRows;
        final Map<Item, int[]> exactItems;
        final Map<Fluid, int[]> exactFluids;
        final int[] exactFallback;
        final Map<Object, int[]> alias;
        final int[] aliasFallback;
        final Map<Object, int[]> admission;

        IndexState(Recipe[] snapshot, long composition, long revision, boolean indexed,
                   boolean livePredicates, boolean emptyOutputRows, Map<Item, int[]> exactItems,
                   Map<Fluid, int[]> exactFluids, int[] exactFallback, Map<Object, int[]> alias,
                   int[] aliasFallback, Map<Object, int[]> admission) {
            this.snapshot = snapshot;
            this.composition = composition;
            this.revision = revision;
            this.indexed = indexed;
            this.livePredicates = livePredicates;
            this.emptyOutputRows = emptyOutputRows;
            this.exactItems = exactItems;
            this.exactFluids = exactFluids;
            this.exactFallback = exactFallback;
            this.alias = alias;
            this.aliasFallback = aliasFallback;
            this.admission = admission;
        }

        long entries() {
            long total = exactFallback.length + aliasFallback.length;
            for (int[] bucket : exactItems.values()) total += bucket.length;
            for (int[] bucket : exactFluids.values()) total += bucket.length;
            for (int[] bucket : alias.values()) total += bucket.length;
            for (int[] bucket : admission.values()) total += bucket.length;
            return total;
        }

        long keys() {
            return exactItems.size() + exactFluids.size() + alias.size() + admission.size();
        }
    }

    RecipeLookupTable(Collection<Recipe> rows, String name) {
        this.rows = rows;
        this.name = name;
    }

    void invalidate() {
        revision.incrementAndGet();
        state = null;
        recent.clear();
    }

    /** Builds the index now, ignoring any cached generation; call only at a stable registration point. */
    synchronized void rebuildNow() {
        reportStats();
        invalidate();
        IndexState built = build(MODE != Mode.LEGACY);
        state = built;
        if (AUDIT && built.indexed) audit(built);
    }

    /**
     * Development check: every row must be reachable through its own declared inputs, for both the
     * exact entry and the alias anchor. This validates bucket construction for the whole table; the
     * cross-item key completeness still needs live verify queries.
     */
    private void audit(IndexState current) {
        long started = System.nanoTime();
        int exactMissing = 0, aliasMissing = 0;
        for (int row = 0; row < current.snapshot.length; row++) {
            Recipe recipe = current.snapshot[row];
            List<ItemStack> items = Arrays.asList(recipe.mInputs);
            if (!reachesRow(exactArrays(current, items, Arrays.asList(recipe.mFluidInputs)), row)) exactMissing++;
            if (recipe.mInputs.length > 0 && !reachesRow(aliasArrays(current, items), row)) aliasMissing++;
        }
        long millis = (System.nanoTime() - started) / 1_000_000L;
        if (exactMissing == 0 && aliasMissing == 0) {
            LOGGER.info("[{}] Recipe lookup audit: all {} rows reachable ({} ms)", name, current.snapshot.length, millis);
        } else {
            LOGGER.error("[{}] Recipe lookup audit: {} rows missing from the exact index, {} from the alias index",
                    name, exactMissing, aliasMissing);
        }
    }

    private static boolean reachesRow(int[][] arrays, int row) {
        var cursor = new Cursor(arrays);
        for (int index; (index = cursor.next()) >= 0; ) {
            if (index == row) return true;
            if (index > row) return false;
        }
        return false;
    }

    /**
     * Monotonic revision of everything the lookup depends on. Material compositions are folded in
     * when they are observed, because caches outside this class compare only this number.
     */
    long revision() {
        long composition = gregtech.engine.material.ItemMaterialRegistry.compositionRevision();
        if (composition != observedComposition) {
            observedComposition = composition;
            revision.incrementAndGet();
        }
        return revision.get();
    }

    @Override public int size() { return rows.size(); }
    @Override public boolean add(Recipe row) { invalidate(); return rows.add(row); }
    @Override public void clear() { invalidate(); rows.clear(); }
    @Override public boolean remove(Object row) { invalidate(); return rows.remove(row); }
    @Override public boolean removeAll(Collection<?> removed) { invalidate(); return rows.removeAll(removed); }
    @Override public boolean retainAll(Collection<?> kept) { invalidate(); return rows.retainAll(kept); }
    @Override public Iterator<Recipe> iterator() {
        var source = rows.iterator();
        return new Iterator<>() {
            public boolean hasNext() { return source.hasNext(); }
            public Recipe next() { return source.next(); }
            public void remove() { invalidate(); source.remove(); }
        };
    }

    private IndexState state() {
        IndexState current = state;
        long composition = gregtech.engine.material.ItemMaterialRegistry.compositionRevision();
        if (current != null && current.composition == composition && current.snapshot.length == rows.size())
            return current;
        return rebuild(composition);
    }

    private synchronized IndexState rebuild(long composition) {
        IndexState current = state;
        if (current != null && current.composition == composition && current.snapshot.length == rows.size())
            return current;
        reportStats();
        IndexState built = build(MODE != Mode.LEGACY);
        state = built;
        recent.clear();
        return built;
    }

    /**
     * Whether any published row answers through a live item guard or predicate. Unknown or stale
     * generations report {@code true}, so a caller never reuses a result it cannot prove static.
     */
    boolean hasLiveItemPredicates() {
        IndexState current = state;
        if (current == null) return true;
        if (current.composition != gregtech.engine.material.ItemMaterialRegistry.compositionRevision()) return true;
        if (current.snapshot.length != rows.size()) return true;
        return current.livePredicates;
    }

    /**
     * Whether any row is skipped while output slots are occupied. Unknown or stale generations
     * report {@code true}, so an output-dependent answer is never treated as a static negative.
     */
    boolean hasEmptyOutputRows() {
        IndexState current = state;
        if (current == null) return true;
        if (current.composition != gregtech.engine.material.ItemMaterialRegistry.compositionRevision()) return true;
        if (current.snapshot.length != rows.size()) return true;
        return current.emptyOutputRows;
    }

    /** The state a read-only caller may use without triggering a build; stale or absent means "no index". */
    private IndexState publishedState() {
        if (MODE == Mode.LEGACY) return null;
        IndexState current = state;
        if (current == null || !current.indexed) return null;
        if (current.composition != gregtech.engine.material.ItemMaterialRegistry.compositionRevision()) return null;
        if (current.snapshot.length != rows.size()) return null;
        return current;
    }

    private IndexState build(boolean indexes) {
        long started = System.nanoTime();
        Recipe[] snapshot = rows.toArray(Recipe[]::new);
        IndexState built;
        if (!indexes) {
            built = fullScanState(snapshot);
        } else {
            try {
                built = buildIndexes(snapshot);
            } catch (RuntimeException failure) {
                // A broken index must not take the recipe table with it: keep the full scan and
                // retry on the next invalidation instead.
                LOGGER.error("[{}] Recipe lookup index build failed; falling back to the full scan", name, failure);
                built = fullScanState(snapshot);
            }
        }
        logBuild(built, started, indexes);
        return built;
    }

    private IndexState fullScanState(Recipe[] snapshot) {
        int[] all = new int[snapshot.length];
        for (int i = 0; i < all.length; i++) all[i] = i;
        return new IndexState(snapshot, gregtech.engine.material.ItemMaterialRegistry.compositionRevision(),
                revision.get(), false, livePredicates(snapshot), emptyOutputRows(snapshot),
                Map.of(), Map.of(), all, Map.of(), all, Map.of());
    }

    /** Whether any row evaluates a live item guard or predicate; such maps never reuse a negative. */
    private static boolean livePredicates(Recipe[] snapshot) {
        for (Recipe row : snapshot)
            if (row.hasItemInputGuards() || row.hasSameItemInputPredicates()) return true;
        return false;
    }

    /** Whether any row is skipped while output slots are occupied; such rows answer from output state. */
    private static boolean emptyOutputRows(Recipe[] snapshot) {
        for (Recipe row : snapshot) if (row.mNeedsEmptyOutput) return true;
        return false;
    }

    private IndexState buildIndexes(Recipe[] snapshot) {
        int n = snapshot.length;
        // Occurrences over required inputs: only used to pick the least shared entry per row.
        Map<Item, Integer> itemFrequency = new IdentityHashMap<>();
        Map<Fluid, Integer> fluidFrequency = new IdentityHashMap<>();
        for (Recipe row : snapshot) {
            for (ItemStack required : row.mInputs) {
                if (empty(required)) continue;
                itemFrequency.merge(required.getItem(), 1, Integer::sum);
            }
            for (FluidStack required : row.mFluidInputs) {
                if (required == null || required.isEmpty()) continue;
                fluidFrequency.merge(required.getFluid(), 1, Integer::sum);
            }
        }

        Map<Item, IntList> itemBuckets = new IdentityHashMap<>();
        Map<Fluid, IntList> fluidBuckets = new IdentityHashMap<>();
        IntList exactFallback = new IntList();
        for (int i = 0; i < n; i++) {
            Recipe row = snapshot[i];
            Object anchor = null;
            int best = Integer.MAX_VALUE;
            for (ItemStack required : row.mInputs) {
                if (empty(required)) continue;
                int count = itemFrequency.get(required.getItem());
                if (count < best) { best = count; anchor = required.getItem(); }
            }
            if (anchor == null) {
                for (FluidStack required : row.mFluidInputs) {
                    if (required == null || required.isEmpty()) continue;
                    int count = fluidFrequency.get(required.getFluid());
                    if (count < best) { best = count; anchor = required.getFluid(); }
                }
            }
            if (anchor instanceof Item item) itemBuckets.computeIfAbsent(item, k -> new IntList()).add(i);
            else if (anchor instanceof Fluid fluid) fluidBuckets.computeIfAbsent(fluid, k -> new IntList()).add(i);
            else exactFallback.add(i);
        }

        var keys = new KeyProvider();
        // Key frequency over every required item input of every row: an anchor is only as cheap as
        // the rows its expanded keys can pull in, not as rare as its raw item identity.
        Map<Object, Integer> keyFrequency = new HashMap<>();
        var rowKeys = new HashSet<Object>();
        for (Recipe row : snapshot) {
            rowKeys.clear();
            for (ItemStack required : row.mInputs) {
                if (empty(required)) continue;
                for (Object key : keys.require(required)) rowKeys.add(key);
            }
            for (Object key : rowKeys) keyFrequency.merge(key, 1, Integer::sum);
        }

        Map<Object, IntList> aliasBuckets = new HashMap<>();
        IntList aliasFallback = new IntList();
        for (int i = 0; i < n; i++) {
            Recipe row = snapshot[i];
            Object[] anchorKeys = null;
            long bestCost = Long.MAX_VALUE;
            for (ItemStack required : row.mInputs) {
                if (empty(required)) continue;
                Object[] candidate = keys.require(required);
                long cost = 0;
                for (Object key : candidate) cost += keyFrequency.getOrDefault(key, 0);
                if (cost < bestCost) { bestCost = cost; anchorKeys = candidate; }
            }
            if (anchorKeys == null) {
                // A row without item slots can only fail the alias stage's !exactItems test through
                // empty declared inputs; pure-fluid rows never reach it and are left out.
                if (row.mInputs.length > 0) aliasFallback.add(i);
                continue;
            }
            for (Object key : anchorKeys) aliasBuckets.computeIfAbsent(key, k -> new IntList()).add(i);
        }

        Map<Object, IntList> admissionBuckets = new HashMap<>();
        for (int i = 0; i < n; i++) {
            for (ItemStack required : snapshot[i].mInputs) {
                if (empty(required)) continue;
                for (Object key : keys.require(required))
                    admissionBuckets.computeIfAbsent(key, k -> new IntList()).add(i);
            }
        }

        return new IndexState(snapshot, gregtech.engine.material.ItemMaterialRegistry.compositionRevision(),
                revision.get(), true, livePredicates(snapshot), emptyOutputRows(snapshot),
                freeze(itemBuckets), freeze(fluidBuckets), exactFallback.toArray(), freeze(aliasBuckets),
                aliasFallback.toArray(), freeze(admissionBuckets));
    }

    /** Exact and alias keys of one required stack; cached per plain item during one build. */
    private static final class KeyProvider {
        private final IdentityHashMap<Item, Object[]> plain = new IdentityHashMap<>();

        Object[] require(ItemStack stack) {
            if (!plain(stack)) return compute(stack);
            Object[] cached = plain.get(stack.getItem());
            if (cached == null) plain.put(stack.getItem(), cached = compute(stack));
            return cached;
        }

        private static Object[] compute(ItemStack stack) {
            var out = new LinkedHashSet<Object>();
            RecipeInputs.requireKeys(stack, out);
            return out.toArray();
        }
    }

    private static boolean plain(ItemStack stack) {
//? if >=1.21.1 {
        /*return stack.getComponentsPatch().isEmpty() && !stack.isDamaged();
*///?} else {
        return !stack.hasTag() && !stack.isDamaged();
//?}
    }

    private static boolean empty(ItemStack stack) { return stack == null || stack.isEmpty(); }

    private static <K> Map<K, int[]> freeze(Map<K, IntList> buckets) {
        var frozen = new HashMap<K, int[]>(buckets.size() * 2);
        for (var entry : buckets.entrySet()) frozen.put(entry.getKey(), entry.getValue().toArray());
        return Map.copyOf(frozen);
    }

    private static final class IntList {
        private int[] values = new int[4];
        private int size;

        void add(int value) {
            if (size == values.length) values = Arrays.copyOf(values, size * 2);
            values[size++] = value;
        }

        int[] toArray() { return Arrays.copyOf(values, size); }
    }

    /** Ascending merge over row-index arrays; equal heads from several buckets are consumed once. */
    private static final class Cursor {
        private final int[][] arrays;
        private final int[] positions;

        Cursor(int[][] arrays) {
            this.arrays = arrays;
            this.positions = new int[arrays.length];
        }

        int next() {
            int best = Integer.MAX_VALUE;
            for (int a = 0; a < arrays.length; a++) {
                int p = positions[a];
                if (p < arrays[a].length && arrays[a][p] < best) best = arrays[a][p];
            }
            if (best == Integer.MAX_VALUE) return -1;
            for (int a = 0; a < arrays.length; a++) {
                int[] array = arrays[a];
                while (positions[a] < array.length && array[positions[a]] == best) positions[a]++;
            }
            return best;
        }
    }

    /** Cache candidate identities, never the match result: counts, enabled flags and outputs stay live. */
    Candidates candidates(List<ItemStack> items, List<FluidStack> fluids) {
        long started = STATS ? System.nanoTime() : 0;
        IndexState current = state();
        for (var cached : recent) if (sameInputs(cached, items, fluids)) return cached;

        List<Recipe> exact = MODE == Mode.VERIFY ? verifyExact(current, items, fluids) : exactList(current, items, fluids);
        var itemKey = items.stream().map(s -> s.isEmpty() ? ItemStack.EMPTY : s.copyWithCount(1)).toList();
        var fluidKey = fluids.stream().map(s -> {
            var copy = s.copy();
            if (!copy.isEmpty()) copy.setAmount(1);
            return copy;
        }).toList();
        // RecipeMap exhausts the live exact-match checks before asking for alternatives. Most
        // native inputs therefore never need the expensive full alias pass. Retain original
        // counts and components, not the count-normalized cache keys, for the deferred predicate.
        var ordered = current;
        var originalItems = items.stream().map(ItemStack::copy).toList();
        var originalFluids = fluids.stream().map(FluidStack::copy).toList();
        java.util.function.Supplier<List<Recipe>> alternativeBuilder = () -> MODE == Mode.VERIFY
                ? verifyAlternatives(ordered, originalItems, originalFluids)
                : alternativeList(ordered, originalItems, originalFluids);
        var result = new Candidates(itemKey, fluidKey, List.copyOf(exact), alternativeBuilder);
        if (recent.size() == 16) recent.removeLast();
        recent.addFirst(result);
        if (STATS) {
            // Periodic flush so a long development run reports without needing a reload.
            if ((queries.sum() & 0xFFFFL) == 0) reportStats();
            queries.increment();
            samples[(int) (sampleCursor++ & (samples.length - 1))] = System.nanoTime() - started;
        }
        return result;
    }

    private static int[][] exactArrays(IndexState current, List<ItemStack> items, List<FluidStack> fluids) {
        var arrays = new ArrayList<int[]>();
        arrays.add(current.exactFallback);
        Item[] seenItems = new Item[items.size()];
        int seenItemCount = 0;
        for (var input : items) {
            if (empty(input)) continue;
            Item item = input.getItem();
            boolean known = false;
            for (int i = 0; i < seenItemCount; i++) if (seenItems[i] == item) { known = true; break; }
            if (known) continue;
            seenItems[seenItemCount++] = item;
            int[] bucket = current.exactItems.get(item);
            if (bucket != null) arrays.add(bucket);
        }
        Fluid[] seenFluids = new Fluid[fluids.size()];
        int seenFluidCount = 0;
        for (var input : fluids) {
            if (input == null || input.isEmpty()) continue;
            Fluid fluid = input.getFluid();
            boolean known = false;
            for (int i = 0; i < seenFluidCount; i++) if (seenFluids[i] == fluid) { known = true; break; }
            if (known) continue;
            seenFluids[seenFluidCount++] = fluid;
            int[] bucket = current.exactFluids.get(fluid);
            if (bucket != null) arrays.add(bucket);
        }
        return arrays.toArray(new int[0][]);
    }

    private static int[][] aliasArrays(IndexState current, List<ItemStack> items) {
        var arrays = new ArrayList<int[]>();
        arrays.add(current.aliasFallback);
        var keys = new LinkedHashSet<Object>();
        for (var input : items) if (!empty(input)) RecipeInputs.offerKeys(input, keys);
        for (Object key : keys) {
            int[] bucket = current.alias.get(key);
            if (bucket != null) arrays.add(bucket);
        }
        return arrays.toArray(new int[0][]);
    }

    private List<Recipe> exactList(IndexState current, List<ItemStack> items, List<FluidStack> fluids) {
        var cursor = new Cursor(exactArrays(current, items, fluids));
        var result = new ArrayList<Recipe>();
        int scanned = 0;
        for (int index; (index = cursor.next()) >= 0; ) {
            scanned++;
            Recipe row = current.snapshot[index];
            if (RecipeMap.exactItems(row, items) && possibleInputs(row, items, fluids)) result.add(row);
        }
        if (STATS) exactScanned.add(scanned);
        return result;
    }

    private List<Recipe> alternativeList(IndexState current, List<ItemStack> items, List<FluidStack> fluids) {
        var cursor = new Cursor(aliasArrays(current, items));
        var result = new ArrayList<Recipe>();
        int scanned = 0;
        for (int index; (index = cursor.next()) >= 0; ) {
            scanned++;
            Recipe row = current.snapshot[index];
            if (!RecipeMap.exactItems(row, items) && possibleInputs(row, items, fluids)) result.add(row);
        }
        if (STATS) alternativeScanned.add(scanned);
        return result;
    }

    private List<Recipe> verifyExact(IndexState current, List<ItemStack> items, List<FluidStack> fluids) {
        var indexed = exactList(current, items, fluids);
        var reference = fullScan(current.snapshot, items, fluids, true);
        compare("exact", indexed, reference);
        return reference;
    }

    private List<Recipe> verifyAlternatives(IndexState current, List<ItemStack> items, List<FluidStack> fluids) {
        var indexed = alternativeList(current, items, fluids);
        var reference = fullScan(current.snapshot, items, fluids, false);
        compare("alternatives", indexed, reference);
        return reference;
    }

    private static List<Recipe> fullScan(Recipe[] snapshot, List<ItemStack> items, List<FluidStack> fluids,
                                         boolean exactStage) {
        var result = new ArrayList<Recipe>();
        for (Recipe row : snapshot) {
            if (exactStage ? RecipeMap.exactItems(row, items) && possibleInputs(row, items, fluids)
                    : !RecipeMap.exactItems(row, items) && possibleInputs(row, items, fluids)) result.add(row);
        }
        return result;
    }

    /** Ordered identity comparison; the first matching row wins, so a reordering is a real difference. */
    private static void compare(String stage, List<Recipe> indexed, List<Recipe> reference) {
        boolean same = indexed.size() == reference.size();
        if (same) for (int i = 0; i < indexed.size(); i++) if (indexed.get(i) != reference.get(i)) { same = false; break; }
        if (same) return;
        MISMATCHES.increment();
        int detail = MISMATCH_DETAILS.incrementAndGet();
        if (detail > 8) return;
        int size = Math.min(indexed.size(), reference.size());
        int first = -1;
        for (int i = 0; i < size; i++) if (indexed.get(i) != reference.get(i)) { first = i; break; }
        if (first < 0) first = size;
        LOGGER.warn("[gregtech] Recipe lookup verify mismatch ({}): indexed {} rows, full scan {} rows, first difference at {}",
                stage, indexed.size(), reference.size(), first);
    }

    /** Records a mismatch between the admission index and the full scan; callers keep the full-scan answer. */
    static void admissionMismatch(String map, boolean expected, boolean indexed) {
        MISMATCHES.increment();
        if (MISMATCH_DETAILS.incrementAndGet() > 8) return;
        LOGGER.warn("[{}] Recipe admission verify mismatch: full scan {}, admission index {}", map, expected, indexed);
    }

    private static boolean possibleInputs(Recipe row, List<ItemStack> items, List<FluidStack> fluids) {
        for (var required : row.mInputs) {
            if (required == null || required.isEmpty()) continue;
            boolean found = false;
            // Same-item container capabilities may depend on stack count. Keep these candidates;
            // the live matcher checks contents/components rather than caching that admission result.
            for (var available : items) {
                if (!available.isEmpty() && (required.is(available.getItem()) || RecipeInputs.matches(required, available))) {
                    found = true; break;
                }
            }
            if (!found) return false;
        }
        for (var required : row.mFluidInputs) {
            if (required == null || required.isEmpty()) continue;
            boolean found = false;
            for (var available : fluids) {
                if (!available.isEmpty() && RecipeInputs.sameFluid(available, required)) { found = true; break; }
            }
            if (!found) return false;
        }
        return true;
    }

    /**
     * Ordered rows whose item inputs may accept {@code stack}, or null when no usable index is
     * published. Admission callers must still run their original per-row test; the index only
     * narrows the candidate set and never triggers a build on the caller's thread.
     */
    @javax.annotation.Nullable
    List<Recipe> admissionCandidates(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        IndexState current = publishedState();
        if (current == null) return null;
        var keys = new LinkedHashSet<Object>();
        RecipeInputs.offerKeys(stack, keys);
        var arrays = new ArrayList<int[]>(keys.size());
        for (Object key : keys) {
            int[] bucket = current.admission.get(key);
            if (bucket != null) arrays.add(bucket);
        }
        var cursor = new Cursor(arrays.toArray(new int[0][]));
        var result = new ArrayList<Recipe>();
        for (int index; (index = cursor.next()) >= 0; ) result.add(current.snapshot[index]);
        return result;
    }

    private static boolean sameInputs(Candidates cached, List<ItemStack> items, List<FluidStack> fluids) {
        if (cached.items.size() != items.size() || cached.fluids.size() != fluids.size()) return false;
        for (int i = 0; i < items.size(); i++) {
            var a = cached.items.get(i); var b = items.get(i);
            if (a.isEmpty() != b.isEmpty()) return false;
//? if >=1.21.1 {
            /*if (!ItemStack.isSameItemSameComponents(a, b)) return false;
*///?} else {
            if (!ItemStack.isSameItemSameTags(a, b)) return false;
//?}
        }
        for (int i = 0; i < fluids.size(); i++) {
            var a = cached.fluids.get(i); var b = fluids.get(i);
            if (a.isEmpty() != b.isEmpty()) return false;
            if (!a.isEmpty() && !RecipeInputs.sameFluid(a, b)) return false;
        }
        return true;
    }

    private void logBuild(IndexState built, long started, boolean indexes) {
        long millis = (System.nanoTime() - started) / 1_000_000L;
        if (STATS) {
            LOGGER.info("[{}] Recipe lookup index built in {} ms (revision {}): {} rows, {} buckets, {} entries, about {} KiB",
                    name, millis, built.revision, built.snapshot.length, built.keys(), built.entries(),
                    built.entries() * 4L / 1024L);
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] Recipe lookup {} built in {} ms: {} rows, {} keys, {} entries", name,
                    indexes ? "index" : "full-scan state", millis, built.snapshot.length, built.keys(), built.entries());
        }
        if (MODE == Mode.VERIFY && MISMATCHES.sum() > 0)
            LOGGER.warn("[gregtech] Recipe lookup verify mismatches so far: {}", MISMATCHES.sum());
    }

    private void reportStats() {
        if (!STATS) return;
        long queryCount = queries.sumThenReset();
        long exactCount = exactScanned.sumThenReset();
        long alternativeCount = alternativeScanned.sumThenReset();
        if (queryCount == 0) return;
        int filled = (int) Math.min(sampleCursor, samples.length);
        if (filled == 0) return;
        long[] copy = Arrays.copyOf(samples, filled);
        Arrays.sort(copy);
        LOGGER.info("[{}] Recipe lookup stats: {} queries, {} exact candidates, {} alias candidates, "
                        + "p50/p95/p99 {} / {} / {} us",
                name, queryCount, exactCount, alternativeCount,
                copy[filled / 2] / 1_000L, copy[Math.min(filled - 1, filled * 95 / 100)] / 1_000L,
                copy[Math.min(filled - 1, filled * 99 / 100)] / 1_000L);
    }

    static {
        if (MODE != Mode.INDEX) LOGGER.warn("[gregtech] Recipe lookup index mode: {}", MODE);
    }
}
