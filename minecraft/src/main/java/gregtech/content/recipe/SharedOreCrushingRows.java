package gregtech.content.recipe;

import static gregtech.content.recipe.SourceOreCrushingAdapter.*;
import static gregtech.content.recipe.SourceOreCrushingRules.Machine;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.registry.GTBlocks;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import java.util.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Captures only OPL's accepted objects, then takes over after static/addon/external producers.
 * No recipe signature grants ownership. Unknown/independent groups retain their exact legacy object.
 */
public final class SharedOreCrushingRows {
    private SharedOreCrushingRows() {}

    private record Key(RecipeMap map, Item item) {}

    private static final class Group {
        final Candidate candidate;
        final RecipeMap map;
        Recipe original, template;

        Group(Candidate candidate, RecipeMap map) {
            this.candidate = candidate;
            this.map = map;
        }
    }

    private static final Map<Key, Group> GROUPS = new LinkedHashMap<>();
    private static final IdentityHashMap<RecipeMap, Set<Recipe>> OWNED = new IdentityHashMap<>();
    private static final Set<Recipe> ORIGINALS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final List<Deferred> DEFERRED = new ArrayList<>();
    private static Snapshot startup;
    private static boolean sealed;

    public record Deferred(String material, String kind, String map, String reason) {}

    static void begin() {
        if (startup != null) throw new IllegalStateException("Crushing capture initialized twice");
        startup = new Snapshot();
    }

    static Candidate candidate(GTMaterial material, ItemStack input, boolean raw) {
        if (startup == null || sealed) throw new IllegalStateException("Crushing capture is not open");
        // Preserve the already reviewed raw Cinnabar helper and its baseline Recipe identity.
        if (raw && ReviewedCinnabarFacts.supports(material)) return null;
        return SourceOreCrushingAdapter.candidate(material, input, raw, startup);
    }

    static void capture(Candidate candidate, RecipeMap map, Recipe accepted) {
        if (candidate == null) return;
        if (sealed) throw new IllegalStateException("Crushing definitions sealed");
        if (accepted != null && !capturable(map, accepted)) return;
        Key key = new Key(map, candidate.input().getItem());
        Group group = GROUPS.computeIfAbsent(key, ignored -> new Group(candidate, map));
        if (accepted != null) {
            if (group.original != null && group.original != accepted)
                throw new IllegalStateException("Duplicate accepted OPL crushing input");
            group.original = accepted;
            // These captured producers are ordinary single-input/no-fluid rows. Freeze their normalized
            // fields now, rather than recomputing legacy values with the new source planner on reload.
            group.template = new Recipe(
                    copies(accepted.mInputs),
                    copies(accepted.mOutputs),
                    accepted.mSpecialItems,
                    accepted.mChances.clone(),
                    accepted.mFluidInputs.clone(),
                    accepted.mFluidOutputs.clone(),
                    accepted.mDuration,
                    accepted.mEUt,
                    accepted.mSpecialValue);
            System.arraycopy(accepted.mMaxChances, 0, group.template.mMaxChances, 0, accepted.mMaxChances.length);
            group.template.mEnabled = accepted.mEnabled;
            group.template.mCanBeBuffered = accepted.mCanBeBuffered;
            group.template.mNeedsEmptyOutput = accepted.mNeedsEmptyOutput;
            ORIGINALS.add(accepted);
            remember(map, accepted);
        }
    }
    /** These exact OPL calls never set special matching/output flags; do not clone arbitrary rows. */
    private static boolean capturable(RecipeMap map, Recipe row) {
        int outputs = map == MachineRecipeMaps.Crusher ? 2 : 1;
        return row.mInputs.length == 1
                && !row.mInputs[0].isEmpty()
                && row.mInputs[0].getCount() == 1
                && row.mOutputs.length == outputs
                && Arrays.stream(row.mOutputs).noneMatch(ItemStack::isEmpty)
                && row.mFluidInputs.length == 0
                && row.mFluidOutputs.length == 0
                && row.mSpecialItems == null
                && row.mSpecialValue == 0
                && row.mEUt == 16
                && row.mChances.length == outputs
                && row.mMaxChances.length == outputs
                && Arrays.stream(row.mChances).allMatch(chance -> chance == 10000)
                && Arrays.stream(row.mMaxChances).allMatch(chance -> chance == 10000)
                && row.mEnabled
                && row.mCanBeBuffered
                && !row.mNeedsEmptyOutput
                && !row.mHidden
                && !row.mFakeRecipe
                && !row.mMaterialRecovery
                && !row.mRequiresEmptyContainerInputs
                && !row.mExactItemInputs
                && !row.mExplicitCatalystsOnly
                && row.explicitCatalystInputs().length == 0
                && !row.hasSameItemInputPredicate(0)
                && row.viewerInputNote(0).isEmpty();
    }

    static void seal() {
        if (startup == null || sealed) throw new IllegalStateException("Invalid crushing seal");
        Set<GTMaterial> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            GTMaterial m = material.resolve();
            if (!seen.add(m)) continue;
            ItemStack block = GTBlocks.getStack(BlockMaterialPrefix.blockRaw, m);
            Candidate candidate = SourceOreCrushingAdapter.candidate(m, block, false, startup);
            if (candidate == null || candidate.kind() != NativeKind.RAW_BLOCK) continue;
            // Newly supplied physical blockRaw input, not retirement of a nonexistent OPL row.
            capture(candidate, MachineRecipeMaps.Crusher, null);
            capture(candidate, MachineRecipeMaps.Hammer, null);
        }
        sealed = true;
    }

    private static ItemStack[] copies(ItemStack[] stacks) {
        return Arrays.stream(stacks)
                .map(s -> s == null ? ItemStack.EMPTY : s.copy())
                .toArray(ItemStack[]::new);
    }

    private static void remember(RecipeMap map, Recipe row) {
        OWNED.computeIfAbsent(map, ignored -> Collections.newSetFromMap(new IdentityHashMap<>()))
                .add(row);
    }

    public static Set<Recipe> owned() {
        Set<Recipe> result = Collections.newSetFromMap(new IdentityHashMap<>());
        OWNED.values().forEach(result::addAll);
        return Collections.unmodifiableSet(result);
    }
    /** Captured originals remain excluded even while their source replacements are currently live. */
    public static Set<Recipe> baselineExcluded() {
        Set<Recipe> result = Collections.newSetFromMap(new IdentityHashMap<>());
        result.addAll(ORIGINALS);
        result.addAll(owned());
        return Collections.unmodifiableSet(result);
    }

    public static List<Deferred> deferred() {
        return List.copyOf(DEFERRED);
    }

    private static void clearOwned() {
        OWNED.forEach((map, rows) -> rows.forEach(map::unlink));
        OWNED.clear();
    }
    /** Keep sealed startup definitions for the next server in this JVM. */
    public static void clear() {
        clearOwned();
        DEFERRED.clear();
    }

    public static void rebuild() {
        rebuild(new Snapshot());
    }

    static void rebuild(Snapshot snapshot) {
        if (!sealed) return;
        clearOwned();
        DEFERRED.clear();
        var independent = independentInputs();
        for (Group group : GROUPS.values()) {
            Key key = new Key(group.map, group.candidate.input().getItem());
            if (independent.contains(key)) {
                restore(group);
                defer(group, "independent-same-item-row");
                continue;
            }
            if (group.original != null && !unchanged(group.original, group.template)) {
                restore(group);
                defer(group, "captured-recipe-overridden");
                continue;
            }
            Machine machine = group.map == MachineRecipeMaps.Crusher ? Machine.CRUSHER : Machine.HAMMER;
            Outcome outcome;
            try {
                outcome = definitions(group.candidate, machine, snapshot);
            } catch (RuntimeException | Error failure) {
                restore(group);
                throw failure;
            }
            if (outcome.action() == Action.LEGACY_UNSUPPORTED) {
                restore(group);
                defer(group, outcome.reason());
                continue;
            }
            var planned = new ArrayList<>(outcome.rows());
            if (outcome.unknownCompatibility() && group.original != null) {
                Recipe compatibility = compatibility(group.candidate, group.template);
                if (compatibility == null) {
                    restore(group);
                    defer(group, "no-authentic-unknown-representative");
                    continue;
                }
                planned.add(compatibility);
            }
            Admission admission = admitGroup(group.map, planned, group.original);
            for (Recipe row : admission.accepted()) remember(group.map, row);
            if (!admission.committed()) defer(group, "normal-admission-rollback");
            else if (outcome.action() == Action.DEFERRED_NO_ROW) defer(group, outcome.reason());
        }
    }

    private static boolean unchanged(Recipe original, Recipe template) {
        return stacksEqual(original.mInputs, template.mInputs)
                && stacksEqual(original.mOutputs, template.mOutputs)
                && Arrays.equals(original.mChances, template.mChances)
                && Arrays.equals(original.mMaxChances, template.mMaxChances)
                && original.mEnabled == template.mEnabled
                && !original.mHidden
                && !original.mFakeRecipe
                && original.mCanBeBuffered == template.mCanBeBuffered
                && original.mNeedsEmptyOutput == template.mNeedsEmptyOutput
                && !original.mMaterialRecovery
                && !original.mRequiresEmptyContainerInputs
                && !original.mExactItemInputs
                && !original.mExplicitCatalystsOnly
                && original.explicitCatalystInputs().length == 0
                && !original.hasSameItemInputPredicate(0);
    }

    private static boolean stacksEqual(ItemStack[] left, ItemStack[] right) {
        if (left.length != right.length) return false;
        for (int i = 0; i < left.length; i++) if (!ItemStack.matches(left[i], right[i])) return false;
        return true;
    }

    private static Set<Key> independentInputs() {
        Set<Key> result = new HashSet<>();
        for (RecipeMap map : List.of(MachineRecipeMaps.Crusher, MachineRecipeMaps.Hammer))
            for (Recipe row : map.mRecipeList)
                if (!row.mFakeRecipe)
                    for (ItemStack input : row.mInputs)
                        if (input != null && !input.isEmpty()) result.add(new Key(map, input.getItem()));
        return result;
    }

    private static void defer(Group group, String reason) {
        DEFERRED.add(new Deferred(
                group.candidate.material().getName(), group.candidate.kind().name(), group.map.mNameInternal, reason));
    }

    private static void restore(Group group) {
        if (group.original == null) return;
        Recipe accepted = group.map.addRecipe(group.original, true, group.original.mFakeRecipe, group.original.mHidden);
        if (accepted != null) remember(group.map, accepted);
        else defer(group, "legacy-readmission-rejected");
    }

    record Admission(boolean committed, List<Recipe> accepted) {
        Admission {
            accepted = List.copyOf(accepted);
        }
    }
    /** Atomic for this producer's group only; ordinary validation/collision is never bypassed. */
    static Admission admitGroup(RecipeMap map, List<Recipe> planned, Recipe legacy) {
        var accepted = new ArrayList<Recipe>();
        try {
            for (Recipe row : planned) {
                Recipe admitted = map.addRecipe(row);
                if (admitted == null) {
                    accepted.forEach(map::unlink);
                    accepted.clear();
                    Recipe restored =
                            legacy == null ? null : map.addRecipe(legacy, true, legacy.mFakeRecipe, legacy.mHidden);
                    if (restored != null) accepted.add(restored);
                    return new Admission(false, accepted);
                }
                accepted.add(admitted);
            }
            return new Admission(true, accepted);
        } catch (RuntimeException | Error failure) {
            accepted.forEach(map::unlink);
            // Exceptional admission still cannot leave a partial source group or lose its old row.
            // Keep any restored accepted identity owned, then propagate the real failure.
            if (legacy != null)
                try {
                    Recipe restored = map.addRecipe(legacy, true, legacy.mFakeRecipe, legacy.mHidden);
                    if (restored != null) remember(map, restored);
                } catch (RuntimeException | Error restoration) {
                    failure.addSuppressed(restoration);
                }
            throw failure;
        }
    }
}
