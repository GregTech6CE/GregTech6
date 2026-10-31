package gregtech.engine.machine;

import gregtech.engine.recipe.MachineWorkCost;
import gregtech.engine.recipe.MachineWorkOutputs;
import gregtech.engine.recipe.ParallelFit;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeInputs;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.stack.StackCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import org.jetbrains.annotations.Nullable;

/**
 * Recipe job state and transitions for a basic machine.
 *
 * <p>Owns the current job's cost, progress and rolled outputs, the parallel search and the cached
 * evaluation that {@code refreshWorkPossible} and the job start share. Host behaviour arrives
 * through three narrow interfaces — {@link Config}, {@link IO} and {@link Gate} — so the processor
 * does not reach into the block entity's fields. Pure arithmetic stays in
 * {@link MachineWorkCost} and the other {@code core} helpers.</p>
 */
public final class RecipeProcessor {
    /** Static machine settings the processor reads instead of the host's spec. */
    public interface Config {
        @Nullable RecipeMap recipeMap();

        boolean searchOutputTanks();

        boolean exclusiveOutputs();

        default boolean requireEmptyOutputs() { return false; }

        int parallelLimit();

        boolean parallelScalesDuration();

        boolean cheapOverclocking();

        boolean usesTimeEnergy();

        int efficiency();

        long inputMinimum();

        long inputMaximum();

        /** Nominal rating the parallel cap derives from; {@code spec.energyIn()}. */
        long nominalEnergyInput();

        long startupEnergyFor(Recipe recipe);
    }

    /** Inventory, world and job side effects. */
    public interface IO {
        @Nullable MachineInventory inventory();

        @Nullable Level level();

        ItemStack autocraftingProgram();

        void setStartupEnergy(long value);

        void clearStartupEnergy();

        void markJobStarted();

        default BlockEntity owner() {
            return (BlockEntity) this;
        }
    }

    /** Start gates, asked only where the original machine asked them. */
    public interface Gate {
        boolean structureComplete();

        boolean switchesAllowRunning();

        boolean recipeStartAllowed();

        boolean lightAllowsStart();
    }

    /**
     * A looked-up recipe with its fluid side, parallel batch and cost. A null {@code cost} mirrors
     * the original early return after a refused cost calculation; a null evaluation means no recipe
     * matched at all, which abandons the current job.
     */
    public record Evaluation(Recipe recipe, boolean outputTanks, int parallel, @Nullable MachineWorkCost.Cost cost) {}

    /** Display-only source values of the consumed job; never used to advance or price it. */
    public record RecipeDetails(long power, long duration) {}

    private static final String NBT_PROGRESS = "gt.progress";
    private static final String NBT_MAX_PROGRESS = "gt.max_progress";
    private static final String NBT_PARALLEL = "gt.parallel";
    private static final String NBT_MIN_ENERGY = "gt.min_energy";
    private static final String NBT_PENDING_OUTPUTS = "gt.pending_outputs";
    private static final String NBT_RECIPE_DETAILS = "gt.recipe_details";
    private static final long MAX_SAVED_WORK = Long.MAX_VALUE / 2;

    private final Config config;
    private final IO io;
    private final Gate gate;
    private long minimumEnergy;
    private long totalWork;
    private int parallel = 1;
    private long progress;
    private MachineWorkOutputs pendingOutputs;
    private @Nullable RecipeDetails recipeDetails;
    private @Nullable Evaluation cached;
    private long cachedAt = Long.MIN_VALUE;
    private boolean dirty = true;
    private final RecipeLookupCache lookupCache = new RecipeLookupCache();
    /** Bumped by {@link #invalidate()}; a negative lookup is only reused under the same version. */
    private long stateVersion;

    public RecipeProcessor(Config config, IO io, Gate gate) {
        this.config = config;
        this.io = io;
        this.gate = gate;
    }

    public long progress() {
        return progress;
    }

    public long totalWork() {
        return totalWork;
    }

    public long minimumEnergy() {
        return minimumEnergy;
    }

    public int parallelCount() {
        return parallel;
    }

    @Nullable
    public RecipeDetails recipeDetails() {
        return totalWork > 0 ? recipeDetails : null;
    }

    @Nullable
    public MachineWorkOutputs pendingOutputs() {
        return pendingOutputs;
    }

    public boolean hasPendingOutputs() {
        return pendingOutputs != null;
    }

    public boolean hasJob() {
        return totalWork > 0;
    }

    /** Drops the cached evaluation; call whenever inputs or machine settings change. */
    public void invalidate() {
        stateVersion++;
        dirty = true;
        cached = null;
        cachedAt = Long.MIN_VALUE;
        lookupCache.invalidate();
    }

    /** The evaluation used by both the work-possible probe and the job start; cached per tick. */
    @Nullable
    public Evaluation evaluation() {
        Level level = io.level();
        long now = level == null ? Long.MIN_VALUE : level.getGameTime();
        if (dirty || cachedAt != now) {
            long version = stateVersion;
            cached = computeEvaluation(now);
            cachedAt = now;
            // An invalidation during the computation must leave the result dirty, not clear it.
            if (version == stateVersion) dirty = false;
        }
        return cached;
    }

    @Nullable
    private Evaluation computeEvaluation(long now) {
        RecipeMap map = config.recipeMap();
        MachineInventory inventory = io.inventory();
        if (map == null || inventory == null) return null;
        if (lookupCache.hit(map, now, stateVersion)) return null;
        long version = stateVersion;
        RecipeLookupCache.countLookup();
        var match = RecipeLookup.find(io.owner(), map, inventory, io.autocraftingProgram(), config.searchOutputTanks());
        if (match == null) {
            // Only a static no-answer may be reused. A found row refused by cost, parallel or
            // output capacity returns null too, and a row that needs empty outputs is skipped
            // while outputs are occupied: both must be re-evaluated at their usual cadence.
            boolean stable = version == stateVersion;
            boolean outputRefusal = map.mNeedsOutputs && map.hasEmptyOutputRows() && inventory.hasItemOutput();
            lookupCache.record(map, now, version,
                    stable && !outputRefusal && RecipeLookup.staticAnswerFinal(map), recheckSeed());
            return null;
        }
        if (!exclusivePairOrPlain(match.recipe())) return null;
        var fluids = match.outputTanks() ? inventory.outputTankFluids() : inventory.inputFluids();
        int parallel = computeParallel(match.recipe(), fluids);
        var cost = MachineWorkCost.calculate(match.recipe().mEUt, match.recipe().mDuration, parallel,
                config.parallelScalesDuration(), config.efficiency(), config.inputMinimum(), config.inputMaximum(),
                config.cheapOverclocking(), config.usesTimeEnergy());
        return new Evaluation(match.recipe(), match.outputTanks(), parallel, cost);
    }

    /** Stable per-machine offset so idle machines do not all recheck the same tick. */
    private long recheckSeed() {
        BlockEntity owner = io.owner();
        return owner == null ? 0L : owner.getBlockPos().asLong();
    }

    public enum StartResult { REFUSED, NO_RECIPE, NO_COST, STARTED }

    /**
     * The original {@code checkRecipe} sequence: gates first, then lookup, cost and consumption.
     * {@code NO_RECIPE} abandons the job; {@code NO_COST} leaves the state untouched; a refused
     * consumption clears the job like the original did.
     */
    public StartResult startJob() {
        if (!gate.recipeStartAllowed()) return StartResult.REFUSED;
        if (!gate.lightAllowsStart()) return StartResult.REFUSED;
        if (config.recipeMap() == null) return StartResult.REFUSED;
        if (!gate.switchesAllowRunning()) return StartResult.REFUSED;
        if (!gate.structureComplete()) return StartResult.REFUSED;
        Evaluation evaluation = evaluation();
        if (evaluation == null) {
            abandon();
            return StartResult.NO_RECIPE;
        }
        if (evaluation.cost() == null) return StartResult.NO_COST;
        return start(evaluation) ? StartResult.STARTED : StartResult.NO_RECIPE;
    }

    /**
     * Consumes the evaluation's inputs, rolls its outputs and installs the job. A refused
     * consumption clears the job exactly like the original path did.
     */
    public boolean start(Evaluation evaluation) {
        MachineInventory inventory = io.inventory();
        RecipeMap map = config.recipeMap();
        if (inventory == null || map == null || evaluation.cost() == null) return false;
        minimumEnergy = evaluation.cost().minimumPower();
        totalWork = evaluation.cost().totalWork();

        var fluids = evaluation.outputTanks() ? inventory.outputTankFluids() : inventory.inputFluids();
        var remaining = RecipeInputs.consume(evaluation.recipe(), inventory.inputItems(), fluids, evaluation.parallel());
        if (remaining == null || evaluation.parallel() < 1) {
            totalWork = 0;
            recipeDetails = null;
            io.clearStartupEnergy();
            invalidate();
            return false;
        }
        pendingOutputs = MachineWorkOutputs.roll(evaluation.recipe(), evaluation.parallel(),
                io.level().random::nextInt, config.exclusiveOutputs());
        for (int i = 0; i < remaining.items().size(); i++) inventory.items().setStackInSlot(i, remaining.items().get(i));
        var drained = evaluation.outputTanks() ? inventory.tanksOutput() : inventory.tanksInput();
        for (int i = 0; i < drained.length; i++) drained[i].setFluid(remaining.fluids().get(i));
        io.markJobStarted();

        parallel = evaluation.parallel();
        recipeDetails = new RecipeDetails(evaluation.recipe().mEUt, evaluation.recipe().mDuration);
        progress = 0;
        io.setStartupEnergy(config.startupEnergyFor(evaluation.recipe()));
        invalidate();
        return true;
    }

    /**
     * No recipe matched: the original path cleared the work total and let the host clear startup.
     * The static negative answer for unchanged inputs stays valid; only the job is dropped.
     */
    public void abandon() {
        totalWork = 0;
        recipeDetails = null;
        cached = null;
        cachedAt = Long.MIN_VALUE;
        dirty = true;
    }

    /** Advances the job by the supplied energy; true when it reached its total work. */
    public boolean advance(long supplied) {
        progress = MachineWorkCost.advance(progress, totalWork, supplied);
        return progress >= totalWork;
    }

    /** Holds a completed job at its total while its outputs cannot be delivered. */
    public void holdAtTotal() {
        progress = totalWork;
    }

    /** Constant-energy machines lose accumulated progress; the rolled outputs stay pending. */
    public void loseProgress() {
        progress = 0;
    }

    /** Delivers rolled outputs; false while any output still lacks space. */
    public boolean flushOutputs() {
        if (pendingOutputs == null) return true;
        MachineInventory inventory = io.inventory();
        RecipeMap map = config.recipeMap();
        if (inventory == null || map == null) return false;
        boolean finished = pendingOutputs.flush(inventory.items(), map.mInputItemsCount, inventory.tanksOutput());
        if (finished) {
            pendingOutputs = null;
            parallel = 1;
        }
        return finished;
    }

    /** Clears the finished job's cost and progress; the host clears startup energy itself. */
    /** Soft-hammer reset discards the current job, including its rolled but unflushed products. */
    public void cancelJob() {
        pendingOutputs = null;
        parallel = 1;
        reset();
        io.clearStartupEnergy();
    }

    public void reset() {
        progress = 0;
        totalWork = 0;
        minimumEnergy = 0;
        recipeDetails = null;
        invalidate();
    }

    /**
     * Writes the job under its unchanged flat keys. {@code lookup} is the 1.21 registry provider
     * (typed as {@link Object} because that type does not exist on 1.20.1) and may be null.
     */
    public void save(CompoundTag tag, @Nullable Object lookup) {
        tag.putLong(NBT_PROGRESS, progress);
        tag.putLong(NBT_MAX_PROGRESS, totalWork);
        tag.putInt(NBT_PARALLEL, parallel);
        tag.putLong(NBT_MIN_ENERGY, minimumEnergy);
        if (recipeDetails() != null) {
            var details = new CompoundTag();
            details.putLong("power", recipeDetails.power());
            details.putLong("duration", recipeDetails.duration());
            tag.put(NBT_RECIPE_DETAILS, details);
        } else {
            tag.remove(NBT_RECIPE_DETAILS);
        }
        if (pendingOutputs != null) {
//? if >=1.21.1 {
            /*tag.put(NBT_PENDING_OUTPUTS, pendingOutputs.save((net.minecraft.core.HolderLookup.Provider) lookup));
*///?} else {
            tag.put(NBT_PENDING_OUTPUTS, pendingOutputs.save());
//?}
        }
    }

    /** Reads the job; the caller clears startup energy after this, exactly like the original order. */
    public void load(CompoundTag tag, @Nullable Object lookup) {
        if (tag.contains(NBT_PROGRESS)) progress = tag.getLong(NBT_PROGRESS);
        if (tag.contains(NBT_MAX_PROGRESS)) totalWork = tag.getLong(NBT_MAX_PROGRESS);
        if (tag.contains(NBT_PARALLEL)) parallel = Math.max(1, tag.getInt(NBT_PARALLEL));
        minimumEnergy = tag.getLong(NBT_MIN_ENERGY);
        // Older jobs have no source snapshot. Leave the new details absent until their next job.
        recipeDetails = null;
        if (tag.contains(NBT_RECIPE_DETAILS)) {
            var details = tag.getCompound(NBT_RECIPE_DETAILS);
            long power = details.getLong("power"), duration = details.getLong("duration");
            if (power >= 0 && duration > 0)
                recipeDetails = new RecipeDetails(power, duration);
        }
//? if >=1.21.1 {
        /*if (tag.contains(NBT_PENDING_OUTPUTS)) pendingOutputs = MachineWorkOutputs.load(tag.getCompound(NBT_PENDING_OUTPUTS), (net.minecraft.core.HolderLookup.Provider) lookup);
*///?} else {
        if (tag.contains(NBT_PENDING_OUTPUTS)) pendingOutputs = MachineWorkOutputs.load(tag.getCompound(NBT_PENDING_OUTPUTS));
//?}
        // Old saves never recorded their consumed job; do not invent outputs.
        if (totalWork > 0 && pendingOutputs == null) {
            progress = 0;
            totalWork = 0;
        }
        invalidate();
    }

    /** Re-derive or bound every saved value that a save or {@code /data merge block} could forge. */
    public void sanitize() {
        parallel = (int) clamp(parallel, 1, Math.max(1, config.parallelLimit()));
        if (pendingOutputs == null) {
            progress = 0;
            totalWork = 0;
            minimumEnergy = 0;
            recipeDetails = null;
            return;
        }
        var savedCost = savedJobCost();
        if (savedCost != null) {
            minimumEnergy = savedCost.minimumPower();
            totalWork = savedCost.totalWork();
        } else {
            // The inputs of an in-progress job are already consumed, so its recipe may not be
            // resolvable. Keep the saved cost bounded instead of zeroing it: a zero cost would
            // let a reloaded job finish for free.
            minimumEnergy = clamp(minimumEnergy, 0, config.inputMaximum());
            totalWork = clamp(totalWork, 0, MAX_SAVED_WORK);
        }
        progress = clamp(progress, 0, totalWork);
        if (recipeDetails != null) {
            var cost = MachineWorkCost.calculate(recipeDetails.power(), recipeDetails.duration(), parallel,
                    config.parallelScalesDuration(), config.efficiency(), config.inputMinimum(), config.inputMaximum(),
                    config.cheapOverclocking(), config.usesTimeEnergy());
            if (cost == null || cost.totalWork() != totalWork || cost.minimumPower() != minimumEnergy)
                recipeDetails = null;
        }
    }

    /** Recomputes the current job cost without consuming inputs or rolling outputs. */
    @Nullable
    private MachineWorkCost.Cost savedJobCost() {
        RecipeMap map = config.recipeMap();
        MachineInventory inventory = io.inventory();
        if (map == null || inventory == null || io.level() == null) return null;
        var match = RecipeLookup.find(io.owner(), map, inventory, io.autocraftingProgram(), config.searchOutputTanks());
        if (match == null || !exclusivePairOrPlain(match.recipe())) return null;
        // The saved batch is the one the job actually rolled; remaining inputs may already be
        // partly consumed, so re-running the parallel search would understate the cost.
        return MachineWorkCost.calculate(match.recipe().mEUt, match.recipe().mDuration, Math.max(1, parallel),
                config.parallelScalesDuration(), config.efficiency(), config.inputMinimum(), config.inputMaximum(),
                config.cheapOverclocking(), config.usesTimeEnergy());
    }

    /** A plain machine accepts any row. An exclusive machine accepts only a pair, and skips the rest. */
    private boolean exclusivePairOrPlain(Recipe recipe) {
        if (!config.exclusiveOutputs()) return true;
        int declared = recipe.mOutputs == null ? 0 : recipe.mOutputs.length;
        return gregtech.engine.addon.ExclusiveOutputRules.isPair(declared,
                gregtech.engine.addon.ExclusiveOutputMaps.nonEmptyItems(recipe.mOutputs));
    }

    private int computeParallel(Recipe recipe, java.util.List<FluidStack> fluids) {
        int low = 0;
        // GT6's {@code MultiTileEntityBasicMachine:730} and {@code MultiTileEntityBasicMachine:743} cap
        // power-scaled parallel batches at the nominal input.
        int high = ParallelFit.powerCap(config.parallelLimit(), config.nominalEnergyInput(), recipe.mEUt,
                config.parallelScalesDuration(), config.usesTimeEnergy());
        for (var fluid : recipe.mFluidOutputs) if (fluid != null && !fluid.isEmpty()) high = ParallelFit.amountCap(high, fluid.getAmount());
        var items = io.inventory().inputItems();
        while (low < high) {
            int mid = low + (high - low + 1) / 2;
            if (RecipeInputs.consume(recipe, items, fluids, mid) != null) low = mid; else high = mid - 1;
        }
        return Math.min(low, outputLimitedParallel(recipe, low));
    }

    /**
     * GT6 {@code MultiTileEntityBasicMachine.canOutput}: a batch may not exceed the free space in
     * the output slots and output tanks.
     *
     * <p>Without this cap a batch whose products cannot be delivered is started anyway; the machine
     * then retries the flush every tick and stays at 100 % progress forever. That is reachable
     * with the {@code large*} machines, whose parallel limit reaches 256 while most recipe maps
     * declare a single item output slot.</p>
     */
    private int outputLimitedParallel(Recipe recipe, int maxParallel) {
        MachineInventory inventory = io.inventory();
        RecipeMap map = config.recipeMap();
        if (maxParallel <= 0 || map == null || inventory == null) return maxParallel;
        if (config.requireEmptyOutputs() && (inventory.hasItemOutput() || inventory.hasFluidOutput())) return 0;

        // Items: simulate the output section using the same slot rules as MachineWorkOutputs#flush.
        int firstOutput = map.mInputItemsCount;
        if (recipe.mOutputs.length > 0 && firstOutput < inventory.items().getSlots()) {
            var present = new ItemStack[inventory.items().getSlots()];
            for (int i = firstOutput; i < inventory.items().getSlots(); i++) present[i] = inventory.items().getStackInSlot(i).copy();
            int limit = maxParallel;
            while (limit > 0 && !itemsFit(recipe, limit, firstOutput, present)) limit--;
            maxParallel = Math.min(maxParallel, limit);
        }

        // Fluids: free capacity of the output tanks, filled the same way flush() does.
        if (recipe.mFluidOutputs.length > 0 && inventory.tanksOutput().length > 0) {
            int limit = maxParallel;
            while (limit > 0 && !fluidsFit(recipe, limit)) limit--;
            maxParallel = Math.min(maxParallel, limit);
        }
        return maxParallel;
    }

    /** Whether {@code parallel} runs of the recipe's item outputs fit into the output section. */
    private static boolean itemsFit(Recipe recipe, int parallel, int firstOutput, ItemStack[] present) {
        var outputs = new java.util.ArrayList<ItemStack>();
        for (var output : recipe.mOutputs) if (output != null && !output.isEmpty()) outputs.add(output);
        int[] group = itemGroups(outputs);
        int groups = groupCount(group);
        int[] slotKeys = new int[present.length - firstOutput];
        int[] slotCounts = new int[slotKeys.length];
        for (int i = 0; i < slotKeys.length; i++) {
            ItemStack slot = present[firstOutput + i];
            if (slot == null || slot.isEmpty()) {
                slotKeys[i] = ParallelFit.EMPTY;
                continue;
            }
            slotKeys[i] = groupOf(outputs, group, slot);
            slotCounts[i] = slot.getCount();
        }
        int[] capacities = new int[groups];
        long[] counts = new long[groups];
        for (int output = 0; output < outputs.size(); output++) {
            int key = group[output];
            capacities[key] = Math.min(64, outputs.get(output).getMaxStackSize());
            counts[key] += outputs.get(output).getCount();
        }
        return ParallelFit.itemsFit(slotKeys, slotCounts, capacities, counts, parallel);
    }

    /** Whether {@code parallel} runs of the recipe's fluid outputs fit into the output tanks. */
    private boolean fluidsFit(Recipe recipe, int parallel) {
        var tanksOutput = io.inventory().tanksOutput();
        var outputs = new java.util.ArrayList<FluidStack>();
        for (var output : recipe.mFluidOutputs) if (output != null && !output.isEmpty()) outputs.add(output);
        int[] group = fluidGroups(outputs);
        int groups = groupCount(group);
        int[] tankKeys = new int[tanksOutput.length];
        long[] free = new long[tanksOutput.length];
        for (int i = 0; i < tanksOutput.length; i++) {
            free[i] = Math.max(0, tanksOutput[i].capacity() - tanksOutput[i].getAmount());
            if (tanksOutput[i].isEmpty()) {
                tankKeys[i] = ParallelFit.EMPTY;
                continue;
            }
            tankKeys[i] = groupOfFluid(outputs, group, tanksOutput[i].getFluid());
        }
        long[] amounts = new long[groups];
        for (int output = 0; output < outputs.size(); output++) amounts[group[output]] += outputs.get(output).getAmount();
        return ParallelFit.fluidsFit(tankKeys, free, amounts, parallel);
    }

    /**
     * Identical outputs share slots and tanks, so each output is assigned the group id of the first
     * output equal to it; that id is the key {@link ParallelFit} compares.
     */
    private static int[] itemGroups(java.util.List<ItemStack> outputs) {
        int[] group = new int[outputs.size()];
        int next = 0;
        for (int i = 0; i < outputs.size(); i++) {
            group[i] = -1;
            for (int j = 0; j < i; j++)
                if (StackCompat.sameItem(outputs.get(i), outputs.get(j))) {
                    group[i] = group[j];
                    break;
                }
            if (group[i] < 0) group[i] = next++;
        }
        return group;
    }

    private static int[] fluidGroups(java.util.List<FluidStack> outputs) {
        int[] group = new int[outputs.size()];
        int next = 0;
        for (int i = 0; i < outputs.size(); i++) {
            group[i] = -1;
            for (int j = 0; j < i; j++)
                if (StackCompat.sameFluid(outputs.get(i), outputs.get(j))) {
                    group[i] = group[j];
                    break;
                }
            if (group[i] < 0) group[i] = next++;
        }
        return group;
    }

    private static int groupCount(int[] group) {
        int groups = 0;
        for (int key : group) groups = Math.max(groups, key + 1);
        return groups;
    }

    private static int groupOf(java.util.List<ItemStack> outputs, int[] group, ItemStack present) {
        for (int output = 0; output < outputs.size(); output++)
            if (StackCompat.sameItem(present, outputs.get(output))) return group[output];
        return ParallelFit.NO_MATCH;
    }

    private static int groupOfFluid(java.util.List<FluidStack> outputs, int[] group, FluidStack present) {
        for (int output = 0; output < outputs.size(); output++)
            if (StackCompat.sameFluid(present, outputs.get(output))) return group[output];
        return ParallelFit.NO_MATCH;
    }

    private static long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
