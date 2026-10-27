package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.OreBlock;
import gregtech.content.block.OreHostStone;
import gregtech.content.data.FluidCatalog;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTFluids;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import static gregtech.content.recipe.SourceOreCrushingAdapter.Candidate;
import static gregtech.content.recipe.SourceOreCrushingAdapter.NativeKind;
import static gregtech.content.recipe.SourceOreCrushingAdapter.Domain;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;
import static gregtech.content.recipe.SourceOilSandRules.*;

/** The OilSand ore-registration Centrifuge event, independent of the Crusher/Hammer handlers.
 * Captured OPL rows are replaced only after static/addon recipes can retain their ownership.
 */
public final class SharedOilSandRows {
    private SharedOilSandRows() {}
    private static final RecipeMap MAP = MachineRecipeMaps.Centrifuge;
    private static final class Group {
        final Candidate candidate;
        final Recipe original, template;
        Group(Candidate candidate, Recipe original) {
            this.candidate = candidate; this.original = original;
            this.template = original == null ? null : copy(original, original.mInputs[0]);
        }
    }
    private static final Map<Item, Group> GROUPS = new LinkedHashMap<>();
    private static final Set<Recipe> OWNED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Recipe> ORIGINALS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final List<Deferred> DEFERRED = new ArrayList<>();
    private static Snapshot startup;
    private static boolean sealed;
    public record Deferred(String kind, String reason) {}
    enum Action { SUPPORTED, SOURCE_NO_ROW, DEFERRED_NO_ROW, LEGACY_UNSUPPORTED }
    record Outcome(Action action, List<Recipe> rows, boolean unknownCompatibility, String reason) {
        Outcome { rows = List.copyOf(rows); }
    }
    record HostSelection(Domain domain, String host, boolean broken) {}
    private static Outcome none(Action action, String reason) { return new Outcome(action, List.of(), false, reason); }

    static void begin() {
        if (startup != null) throw new IllegalStateException("OilSand capture initialized twice");
        startup = new Snapshot();
    }
    static Candidate candidate(ItemStack input) {
        if (startup == null || sealed) throw new IllegalStateException("OilSand capture not open");
        GTMaterial material = GTMaterialRegistry.get("OilSand");
        boolean raw = SourceFurnaceNativeAdapter.nativeForm(MaterialPrefix.oreRaw, material, input);
        return candidate(material, input, raw, startup);
    }
    static Candidate candidate(GTMaterial material, ItemStack input, boolean raw, Snapshot snapshot) {
        return material != null && isOilSand(material.getId(), material.getName())
                ? SourceOreCrushingAdapter.candidate(material, input, raw, snapshot) : null;
    }
    static void capture(Candidate candidate, Recipe accepted) {
        if (candidate == null) return;
        if (sealed) throw new IllegalStateException("OilSand definitions sealed");
        // Only the exact old single-input/one-oil-output OPL call is captured, never arbitrary rows.
        if (accepted != null && !ordinary(accepted)) return;
        Item item = candidate.input().getItem();
        Group previous = GROUPS.get(item);
        if (previous != null) {
            if (accepted != null && accepted != previous.original) throw new IllegalStateException("Duplicate OilSand capture");
            return;
        }
        GROUPS.put(item, new Group(candidate, accepted));
        if (accepted != null) { ORIGINALS.add(accepted); OWNED.add(accepted); }
    }
    static void seal() {
        if (startup == null || sealed) throw new IllegalStateException("Invalid OilSand seal");
        GTMaterial material = GTMaterialRegistry.get("OilSand");
        Candidate block = candidate(material, GTBlocks.getStack(BlockMaterialPrefix.blockRaw, material), false, startup);
        if (block != null && block.kind() == NativeKind.RAW_BLOCK) capture(block, null);
        sealed = true;
    }
    private static boolean ordinary(Recipe row) {
        return row.mInputs.length == 1 && !row.mInputs[0].isEmpty() && row.mInputs[0].getCount() == 1
                && row.mFluidInputs.length == 0 && row.mFluidOutputs.length == 1
                && !row.mFluidOutputs[0].isEmpty() && row.mSpecialItems == null && row.mSpecialValue == 0
                && row.mEUt == 16 && row.mEnabled && row.mCanBeBuffered && !row.mNeedsEmptyOutput
                && !row.mHidden && !row.mFakeRecipe && !row.mMaterialRecovery && !row.mRequiresEmptyContainerInputs
                && !row.mExactItemInputs && !row.mExplicitCatalystsOnly
                && row.explicitCatalystInputs().length == 0 && row.explicitCatalystFluidInputs().length == 0
                && row.explicitPresenceFluidInputs().length == 0 && !row.hasSameItemInputPredicate(0)
                && row.viewerInputNote(0).isEmpty()
                && Arrays.stream(row.mChances).allMatch(c -> c == 10000)
                && Arrays.stream(row.mMaxChances).allMatch(c -> c == 10000);
    }
    public static Set<Recipe> owned() {
        Set<Recipe> result = Collections.newSetFromMap(new IdentityHashMap<>());
        result.addAll(OWNED); return Collections.unmodifiableSet(result);
    }
    public static Set<Recipe> baselineExcluded() {
        Set<Recipe> result = Collections.newSetFromMap(new IdentityHashMap<>());
        result.addAll(ORIGINALS); result.addAll(OWNED); return Collections.unmodifiableSet(result);
    }
    public static List<Deferred> deferred() { return List.copyOf(DEFERRED); }
    private static void clearOwned() { OWNED.forEach(MAP::unlink); OWNED.clear(); }
    public static void clear() { clearOwned(); DEFERRED.clear(); }
    public static void rebuild() { rebuild(new Snapshot()); }
    static void rebuild(Snapshot snapshot) {
        if (!sealed) return;
        clearOwned(); DEFERRED.clear();
        Set<Item> independent = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Recipe row : MAP.mRecipeList) if (!row.mFakeRecipe)
            for (ItemStack input : row.mInputs) if (input != null && !input.isEmpty()) independent.add(input.getItem());
        for (Group group : GROUPS.values()) {
            if (independent.contains(group.candidate.input().getItem())) {
                restore(group); defer(group, "independent-same-item-row"); continue;
            }
            if (group.original != null && (!ordinary(group.original) || !sameFields(group.original, group.template))) {
                restore(group); defer(group, "captured-recipe-overridden"); continue;
            }
            Outcome outcome;
            try { outcome = definitions(group.candidate, snapshot); }
            catch (RuntimeException | Error failure) { restore(group); throw failure; }
            if (outcome.action() == Action.LEGACY_UNSUPPORTED) {
                restore(group); defer(group, outcome.reason()); continue;
            }
            var planned = new ArrayList<>(outcome.rows());
            // The already-correct raw event keeps its exact accepted identity on ordinary reloads.
            if (group.candidate.kind() == NativeKind.RAW_ITEM && group.original != null
                    && planned.size() == 1 && sameFields(group.original, planned.get(0))) planned.set(0, group.original);
            if (outcome.unknownCompatibility() && group.original != null) {
                Recipe fallback = compatibility(group.candidate, group.template);
                if (fallback == null) { restore(group); defer(group, "no-authentic-unknown-representative"); continue; }
                planned.add(fallback);
            }
            if (!admitGroup(planned, group.original)) defer(group, "normal-admission-rollback");
            else if (outcome.action() == Action.DEFERRED_NO_ROW) defer(group, outcome.reason());
        }
    }
    private static void defer(Group group, String reason) { DEFERRED.add(new Deferred(group.candidate.kind().name(), reason)); }
    private static void restore(Group group) {
        if (group.original == null) return;
        Recipe accepted = MAP.addRecipe(group.original, true, group.original.mFakeRecipe, group.original.mHidden);
        if (accepted != null) OWNED.add(accepted); else defer(group, "legacy-readmission-rejected");
    }
    private static boolean admitGroup(List<Recipe> planned, Recipe legacy) {
        var accepted = new ArrayList<Recipe>();
        try {
            for (Recipe row : planned) {
                Recipe result = MAP.addRecipe(row);
                if (result == null) {
                    accepted.forEach(MAP::unlink);
                    Recipe restored = legacy == null ? null : MAP.addRecipe(legacy, true, legacy.mFakeRecipe, legacy.mHidden);
                    if (restored != null) OWNED.add(restored);
                    return false;
                }
                accepted.add(result);
            }
            OWNED.addAll(accepted); return true;
        } catch (RuntimeException | Error failure) {
            accepted.forEach(MAP::unlink);
            if (legacy != null) try {
                Recipe restored = MAP.addRecipe(legacy, true, legacy.mFakeRecipe, legacy.mHidden); if (restored != null) OWNED.add(restored);
            } catch (RuntimeException | Error restoration) { failure.addSuppressed(restoration); }
            throw failure;
        }
    }

    static Outcome definitions(Candidate candidate, Snapshot snapshot) {
        GTMaterial material = candidate.material();
        if (!isOilSand(material.getId(), material.getName()) || !snapshot.context(material)
                || snapshot.kind(material, candidate.input(), candidate.kind() == NativeKind.RAW_ITEM) != candidate.kind())
            return none(Action.LEGACY_UNSUPPORTED, "native-context-changed");
        // This event has no material ANTIMATTER/quality/crushing-target gate. Its exact prefix gate
        // is independent of the Crusher: sediment DUST_ORE prefixes ARE included here.
        var rows = new ArrayList<Recipe>();
        if (candidate.kind() == NativeKind.HOST_ORE) {
            OreBlock block = (OreBlock)((BlockItem)candidate.input().getItem()).getBlock();
            for (OreHostStone host : OreHostStone.values()) {
                HostProfile profile = hostProfile(host.id()); if (profile == null) continue;
                Result result = plan(new Request(profile.prefixWeight(), profile.byproducts()), snapshot::available);
                if (result.status() == Status.UNSUPPORTED) return none(Action.LEGACY_UNSUPPORTED, result.reason());
                FluidStack oil = snapshot.oil(Math.toIntExact(result.oilMb()));
                if (oil.isEmpty()) return none(Action.DEFERRED_NO_ROW, "required-oil-binding-absent");
                ItemStack representative = OreBlock.withStone(candidate.input(), block.stateFor(host));
                Recipe row = make(representative, result, oil, stack -> {
                    HostSelection actual = classifyHost(stack);
                    return actual.domain() == Domain.SOURCE_PROFILE && actual.host().equals(host.id());
                }, snapshot);
                if (row == null) return none(Action.DEFERRED_NO_ROW, "native-normalization");
                if (hasBrokenSourcePair(host.id())) row.withViewerInputAlternatives(0,
                        List.of(representative, OreBlock.withStone(candidate.input(), block.brokenStateFor(host))));
                rows.add(row);
            }
            return new Outcome(Action.SUPPORTED, rows, true, "verified-host-domains");
        }
        long weight = candidate.kind() == NativeKind.RAW_BLOCK ? WEIGHT_RAW_BLOCK
                : candidate.kind() == NativeKind.DENSE ? WEIGHT_DENSE : WEIGHT_STANDARD;
        List<Byproduct> byproducts = candidate.kind() == NativeKind.FIXED_STONE
                ? hostProfile("stone").byproducts() : List.of();
        Result result = plan(new Request(weight, byproducts), snapshot::available);
        if (result.status() == Status.UNSUPPORTED) return none(Action.LEGACY_UNSUPPORTED, result.reason());
        FluidStack oil = snapshot.oil(Math.toIntExact(result.oilMb()));
        if (oil.isEmpty()) return none(Action.DEFERRED_NO_ROW, "required-oil-binding-absent");
        Recipe row = make(candidate.input(), result, oil, stack -> true, snapshot);
        return row == null ? none(Action.DEFERRED_NO_ROW, "native-normalization")
                : new Outcome(Action.SUPPORTED, List.of(row), false, "verified-fixed-kind");
    }
    private static Recipe make(ItemStack input, Result result, FluidStack oil, Predicate<ItemStack> predicate, Snapshot snapshot) {
        ItemStack[] outputs = new ItemStack[result.slots().size()]; long[] chances = new long[outputs.length];
        for (int index = 0; index < outputs.length; index++) {
            var slot = result.slots().get(index); chances[index] = slot.chance();
            outputs[index] = slot.stack() == null ? ItemStack.EMPTY : snapshot.stack(slot.stack());
        }
        Recipe row = RecipeMap.make(result.optimize(), new ItemStack[]{input}, outputs, null, chances,
                new FluidStack[0], new FluidStack[]{oil}, result.ticks(), result.power(), 0);
        return row == null ? null : row.withSameItemInputPredicate(0, predicate);
    }
    static HostSelection classifyHost(ItemStack stack) {
        var data = SourceOreCrushingAdapter.hostData(stack);
        Decision decision = hostDecision(data.host());
        if (decision == Decision.NO) return new HostSelection(Domain.SOURCE_NO, data.host(), data.broken());
        if (decision == Decision.UNKNOWN || !data.knownMode() || data.broken() && !hasBrokenSourcePair(data.host()))
            return new HostSelection(Domain.UNKNOWN_HOST, data.host(), data.broken());
        return new HostSelection(Domain.SOURCE_PROFILE, data.host(), data.broken());
    }
    private static Recipe compatibility(Candidate candidate, Recipe legacy) {
        OreBlock block = (OreBlock)((BlockItem)candidate.input().getItem()).getBlock();
        ItemStack representative = OreBlock.withStone(candidate.input(), block.stateFor(OreHostStone.TUFF));
        if (classifyHost(representative).domain() != Domain.UNKNOWN_HOST) return null;
        return copy(legacy, representative).withSameItemInputPredicate(0,
                stack -> classifyHost(stack).domain() == Domain.UNKNOWN_HOST)
                .withViewerInputNote(0, "gt6ce.jei.ore_host_legacy");
    }
    private static Recipe copy(Recipe recipe, ItemStack input) {
        Recipe copy = new Recipe(new ItemStack[]{input.copy()}, Arrays.stream(recipe.mOutputs).map(ItemStack::copy).toArray(ItemStack[]::new),
                recipe.mSpecialItems, recipe.mChances.clone(), Arrays.stream(recipe.mFluidInputs).map(FluidStack::copy).toArray(FluidStack[]::new),
                Arrays.stream(recipe.mFluidOutputs).map(FluidStack::copy).toArray(FluidStack[]::new),
                recipe.mDuration, recipe.mEUt, recipe.mSpecialValue);
        System.arraycopy(recipe.mMaxChances, 0, copy.mMaxChances, 0, recipe.mMaxChances.length);
        copy.mEnabled = recipe.mEnabled; copy.mCanBeBuffered = recipe.mCanBeBuffered; copy.mNeedsEmptyOutput = recipe.mNeedsEmptyOutput;
        return copy;
    }
    private static boolean sameFields(Recipe left, Recipe right) {
        if (left.mInputs.length != right.mInputs.length || left.mOutputs.length != right.mOutputs.length
                || left.mFluidInputs.length != right.mFluidInputs.length || left.mFluidOutputs.length != right.mFluidOutputs.length
                || left.mDuration != right.mDuration || left.mEUt != right.mEUt || left.mSpecialValue != right.mSpecialValue
                || !Arrays.equals(left.mChances, right.mChances) || !Arrays.equals(left.mMaxChances, right.mMaxChances)) return false;
        for (int i = 0; i < left.mInputs.length; i++) if (!ItemStack.matches(left.mInputs[i], right.mInputs[i])) return false;
        for (int i = 0; i < left.mOutputs.length; i++) if (!ItemStack.matches(left.mOutputs[i], right.mOutputs[i])) return false;
        for (int i = 0; i < left.mFluidOutputs.length; i++) if (left.mFluidOutputs[i].getFluid() != right.mFluidOutputs[i].getFluid()
                || left.mFluidOutputs[i].getAmount() != right.mFluidOutputs[i].getAmount()) return false;
        return true;
    }
    static class Snapshot extends SourceOreCrushingAdapter.Snapshot {
        @Override boolean context(GTMaterial material) {
            // This independent event does not depend on crushed/refined forms, ore multipliers,
            // crushing targets, or the non-ore continuation family. kind() proves its actual input.
            return identity(material) && isOilSand(material.getId(), material.getName());
        }
        @Override NativeKind kind(GTMaterial material, ItemStack input, boolean raw) {
            if (raw && overridden(material, List.of(MaterialPrefix.oreRaw))) return null;
            return super.kind(material, input, raw);
        }
        private final Map<String, FluidCatalog.FluidEntry> fluids;
        private final BiFunction<String, Integer, FluidStack> fluidLookup;
        Snapshot() { this(FluidCatalog.all(), GTFluids::stack); }
        Snapshot(Map<String, FluidCatalog.FluidEntry> fluids, BiFunction<String, Integer, FluidStack> lookup) {
            this.fluids = Map.copyOf(fluids); this.fluidLookup = lookup;
        }
        FluidStack oil(int amount) {
            var entry = fluids.get("Oil_Normal");
            if (amount <= 0 || entry != FluidCatalog.Oil_Normal || !FluidCatalog.isBuiltInDefinition(entry)) return FluidStack.EMPTY;
            FluidStack stack = fluidLookup.apply("Oil_Normal", amount);
            if (stack == null || stack.isEmpty() || stack.getAmount() != amount
                    || GTFluids.entryForFluid(stack.getFluid()) != entry
                    || !BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString().equals("gt6ce:" + FluidCatalog.sanitizePath(entry.registryName())))
                return FluidStack.EMPTY;
            return stack.copy();
        }
    }
}
