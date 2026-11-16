package gregtech.content.recipe;

import com.mojang.logging.LogUtils;
import gregtech.api.addon.material.DataKey;
import gregtech.api.addon.recipe.RecipeOutputChances;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.OreBlock;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTFluids;
import gregtech.content.worldgen.MaterialWorldgenFacts;
import gregtech.engine.fluid.FluidVisualPolicy;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.material.MaterialProcessGraph;
import gregtech.engine.material.MaterialProcessGraph.Amount;
import gregtech.engine.material.MaterialProcessGraph.AmountUnit;
import gregtech.engine.material.MaterialProcessGraph.Budgets;
import gregtech.engine.material.MaterialProcessGraph.Direction;
import gregtech.engine.material.MaterialProcessGraph.Notice;
import gregtech.engine.material.MaterialProcessGraph.Port;
import gregtech.engine.material.MaterialProcessGraph.Query;
import gregtech.engine.material.MaterialProcessGraph.Resource;
import gregtech.engine.material.MaterialProcessGraph.Role;
import gregtech.engine.material.MaterialProcessGraph.Step;
import gregtech.engine.material.MaterialPublication;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import org.slf4j.Logger;

/**
 * One complete material process generation projected from the final machine maps and active
 * world generation facts. Native recipe objects stay behind opaque step references.
 */
public final class MaterialProcessSnapshot {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final AtomicLong RESOURCE_GENERATIONS = new AtomicLong();

    public record NativeRecipe(RecipeMap map, Recipe recipe, int index) {
        public NativeRecipe {
            Objects.requireNonNull(map);
            Objects.requireNonNull(recipe);
            if (index < 0) throw new IllegalArgumentException("Invalid native recipe index");
        }

        /** Publication fallback ownership also covers ordinary processing such as anvil crushing. */
        public boolean genericRecovery() {
            return RecoveryRecipePublisher.generic(recipe)
                    && (map == gregtech.content.data.MachineRecipeMaps.Shredder
                            || map == gregtech.content.data.MachineRecipeMaps.Melter
                            || map == gregtech.content.data.MachineRecipeMaps.Smelter);
        }
    }

    public record NativeWorldgen(MaterialWorldgenFacts.Fact fact) {
        public NativeWorldgen {
            Objects.requireNonNull(fact);
        }
    }

    public record Root(String material, String form, DataKey resource) {
        public Root {
            if (material == null || material.isBlank() || form == null || form.isBlank())
                throw new IllegalArgumentException("Invalid material process root");
            Objects.requireNonNull(resource);
        }
    }

    private static final List<Notice> COVERAGE = List.of(
            new Notice("coverage_final_recipes", 0),
            new Notice("coverage_active_worldgen", 0),
            new Notice("partial_dynamic_selectors", 0),
            new Notice("partial_unknown_roles", 0));
    private static final MaterialProcessSnapshot EMPTY = empty();

    private record Bucket(List<Step> steps, Map<String, Resource> roots) {}

    private final MaterialProcessGraph.Snapshot snapshot;
    private final long resourceGeneration;
    private final Map<DataKey, Step> steps;
    private final Map<DataKey, Resource> resources;
    private final MaterialProcessGraph graph;
    private final List<Root> roots;
    private final Map<DataKey, NativeRecipe> nativeRecipes;
    private final Map<DataKey, NativeWorldgen> worldgen;
    private final Map<DataKey, List<ItemStack>> viewerIcons;
    private record ViewerRecipe(String process, ItemStack machine) {}

    private final Map<DataKey, ViewerRecipe> viewerRecipes;

    private MaterialProcessSnapshot(
            MaterialProcessGraph.Snapshot snapshot,
            long resourceGeneration,
            Map<String, Bucket> buckets,
            List<Root> roots,
            Map<DataKey, NativeRecipe> nativeRecipes,
            Map<DataKey, NativeWorldgen> worldgen) {
        this(snapshot, resourceGeneration, buckets, roots, nativeRecipes, worldgen, Map.of(), Map.of(), COVERAGE);
    }

    private MaterialProcessSnapshot(
            MaterialProcessGraph.Snapshot snapshot,
            long resourceGeneration,
            Map<String, Bucket> buckets,
            List<Root> roots,
            Map<DataKey, NativeRecipe> nativeRecipes,
            Map<DataKey, NativeWorldgen> worldgen,
            Map<DataKey, List<ItemStack>> viewerIcons,
            Map<DataKey, ViewerRecipe> viewerRecipes,
            List<Notice> coverage) {
        this.snapshot = snapshot;
        this.resourceGeneration = resourceGeneration;
        var resources = new LinkedHashMap<DataKey, Resource>();
        var steps = new LinkedHashMap<DataKey, Step>();
        buckets.values().forEach(bucket -> bucket.steps.forEach(step -> steps.putIfAbsent(step.reference(), step)));
        var builder = MaterialProcessGraph.builder(snapshot);
        coverage.forEach(notice -> builder.notice(notice.code(), notice.omitted()));
        for (Step step : steps.values()) {
            NativeRecipe nativeRecipe = nativeRecipes.get(step.reference());
            builder.add(step, nativeRecipe != null && nativeRecipe.genericRecovery());
        }
        this.graph = builder.build();
        int suppliedSteps = steps.size();
        steps.entrySet().removeIf(entry -> graph.step(entry.getKey()).isEmpty());
        for (Step step : steps.values()) {
            step.inputs().forEach(port -> resources.putIfAbsent(port.resource().key(), port.resource()));
            step.outputs().forEach(port -> resources.putIfAbsent(port.resource().key(), port.resource()));
        }
        this.resources = Map.copyOf(resources);
        this.steps = Map.copyOf(steps);
        this.roots = suppliedSteps == steps.size() ? List.copyOf(roots) : roots.stream().map(root -> {
            var candidates = new LinkedHashMap<String, Resource>();
            for (Resource resource : buckets.get(root.material()).roots().values()) {
                Resource retained = resources.get(resource.key());
                if (retained != null && retained.material().equals(root.material()))
                    candidates.put(retained.key().toString(), retained);
            }
            return candidates.isEmpty() ? null : bestRoot(root.material(), candidates);
        }).filter(Objects::nonNull).toList();
        var retainedNative = new LinkedHashMap<DataKey, NativeRecipe>();
        nativeRecipes.forEach((key, value) -> {
            if (steps.containsKey(key)) retainedNative.put(key, value);
        });
        this.nativeRecipes = Map.copyOf(retainedNative);
        var retainedWorldgen = new LinkedHashMap<DataKey, NativeWorldgen>();
        worldgen.forEach((key, value) -> {
            if (steps.containsKey(key)) retainedWorldgen.put(key, value);
        });
        this.worldgen = Map.copyOf(retainedWorldgen);
        var retainedIcons = new LinkedHashMap<DataKey, List<ItemStack>>();
        viewerIcons.forEach((key, value) -> {
            if (resources.containsKey(key)) retainedIcons.put(key, value);
        });
        this.viewerIcons = Map.copyOf(retainedIcons);
        var retainedRecipes = new LinkedHashMap<DataKey, ViewerRecipe>();
        viewerRecipes.forEach((key, value) -> {
            if (steps.containsKey(key)) retainedRecipes.put(key, value);
        });
        this.viewerRecipes = Map.copyOf(retainedRecipes);
    }

    public List<ItemStack> viewerIcons(DataKey resource) {
        return viewerIcons.getOrDefault(resource, List.of()).stream().map(ItemStack::copy).toList();
    }

    public ItemStack viewerMachine(DataKey step) {
        var recipe = viewerRecipes.get(step);
        return recipe == null ? ItemStack.EMPTY : recipe.machine().copy();
    }

    public String process(DataKey step) {
        var recipe = nativeRecipes.get(step);
        if (recipe != null) return recipe.map().mNameInternal;
        var viewer = viewerRecipes.get(step);
        var value = steps.get(step);
        return viewer != null ? viewer.process() : value == null ? "" : value.name();
    }

    /** Starts a client-local view whose processing rows come exclusively from the viewer. */
    public ViewerBuilder viewerBuilder() {
        return new ViewerBuilder(this);
    }

    public MaterialProcessGraph.Snapshot snapshot() {
        return snapshot;
    }

    public List<Root> roots() {
        return roots;
    }

    public Optional<Resource> resource(DataKey key) {
        return Optional.ofNullable(resources.get(key));
    }

    public Query query(DataKey root, Direction direction, Budgets budgets) {
        Optional<Resource> resource = resource(root);
        if (resource.isEmpty()) return new Query(snapshot, root, direction, List.of(), List.of(), COVERAGE, false);
        return graph.query(root, direction, budgets);
    }

    public Query queryMaterial(DataKey root, Budgets budgets) {
        if (resource(root).isEmpty())
            return new Query(snapshot, root, Direction.BOTH, List.of(), List.of(), COVERAGE, false);
        return graph.queryMaterial(root, budgets);
    }

    public MaterialProcessGraph.MaterialQuery queryMaterial(DataKey root) {
        return graph.queryMaterial(root);
    }

    public List<Step> materialSteps(DataKey root) {
        return graph.materialSteps(root);
    }

    public Optional<NativeRecipe> recipe(DataKey reference) {
        return Optional.ofNullable(nativeRecipes.get(reference));
    }

    public Optional<NativeWorldgen> worldgen(DataKey reference) {
        return Optional.ofNullable(worldgen.get(reference));
    }

    public boolean samePublication(MaterialPublication.Stamp publication) {
        return snapshot.publication().equals(publication);
    }

    private static MaterialProcessSnapshot empty() {
        var stamp = MaterialPublication.current();
        return new MaterialProcessSnapshot(
                new MaterialProcessGraph.Snapshot(stamp, 1, 0, 0, 0),
                0,
                Map.of(),
                List.of(),
                Map.of(),
                Map.of());
    }

    static MaterialProcessSnapshot build(
            MaterialPublication.Stamp publication,
            long sessionGeneration,
            MaterialWorldgenFacts.Snapshot worldgenFacts) {
        return build(publication, sessionGeneration, worldgenFacts, RecipeMap.RECIPE_MAP_LIST);
    }

    static MaterialProcessSnapshot build(
            MaterialPublication.Stamp publication,
            long sessionGeneration,
            MaterialWorldgenFacts.Snapshot worldgenFacts,
            Iterable<RecipeMap> maps) {
        Objects.requireNonNull(publication);
        Objects.requireNonNull(worldgenFacts);
        long resourceGeneration = RESOURCE_GENERATIONS.incrementAndGet();
        var graphSnapshot = new MaterialProcessGraph.Snapshot(
                publication,
                sessionGeneration,
                gregtech.engine.material.MaterialViews.contentRevision(),
                ItemMaterialRegistry.compositionState().tagGeneration(),
                worldgenFacts.revision());
        var mutable = new LinkedHashMap<String, MutableBucket>();
        var nativeRecipes = new LinkedHashMap<DataKey, NativeRecipe>();
        var nativeWorldgen = new LinkedHashMap<DataKey, NativeWorldgen>();
        int invalid = 0;
        for (RecipeMap map : maps) {
            var rows = new ArrayList<Recipe>(map.mRecipeList);
            for (int index = 0; index < rows.size(); index++) {
                Recipe recipe = rows.get(index);
                if (recipe == null) {
                    LOGGER.warn("Discarded null material process recipe in {} at index {}", map.mNameInternal, index);
                    invalid++;
                    continue;
                }
                if (!recipe.mEnabled || recipe.mHidden) continue;
                if (recipe.mFakeRecipe && !hasMaterialUnitInput(recipe)) continue;
                BuiltStep built;
                try {
                    built = recipeStep(
                            map, recipe, index, publication.revision(), sessionGeneration, resourceGeneration);
                } catch (RuntimeException failure) {
                    LOGGER.warn("Discarded invalid material process recipe in {} at index {}", map.mNameInternal, index, failure);
                    invalid++;
                    continue;
                }
                if (built == null) continue;
                built = scoped(built, publication.revision(), resourceGeneration);
                addStep(mutable, built.step, built.materials);
                nativeRecipes.put(built.step.reference(), new NativeRecipe(map, recipe, index));
            }
        }
        for (MaterialWorldgenFacts.Fact fact : worldgenFacts.facts()) {
            var built = worldgenStep(fact, publication.revision(), sessionGeneration);
            if (built == null) continue;
            built = scoped(built, publication.revision(), resourceGeneration);
            addStep(mutable, built.step, built.materials);
            nativeWorldgen.put(built.step.reference(), new NativeWorldgen(fact));
        }
        var buckets = new LinkedHashMap<String, Bucket>();
        mutable.forEach((material, bucket) -> buckets.put(material, bucket.freeze()));
        var roots = buckets.entrySet().stream()
                .map(entry -> bestRoot(entry.getKey(), entry.getValue().roots))
                .sorted(Comparator.comparing(Root::material, String.CASE_INSENSITIVE_ORDER))
                .toList();
        var coverage = new ArrayList<>(COVERAGE);
        if (invalid > 0) coverage.add(new Notice("invalid_process_data", invalid));
        return new MaterialProcessSnapshot(
                graphSnapshot, resourceGeneration, buckets, roots, nativeRecipes, nativeWorldgen, Map.of(), Map.of(), coverage);
    }

    private static BuiltStep recipeStep(
            RecipeMap map,
            Recipe recipe,
            int index,
            long publication,
            long session,
            long resourceGeneration) {
        var inputs = new ArrayList<Port>();
        var outputs = new ArrayList<Port>();
        var materials = new LinkedHashSet<String>();
        var auxiliaryInputs = new ArrayList<String>();
        String nativeStep = path(map.mNameInternal) + "/" + index;
        boolean mainInput = false;
        for (int input = 0; input < recipe.mInputs.length; input++) {
            ItemStack stack = recipe.mInputs[input];
            if (stack == null || stack.isEmpty()) continue;
            if (recipe.isCatalystInput(input)) {
                Port port = itemPort(stack, recipe, input, false, false, nativeStep);
                if (port != null) inputs.add(port);
                else auxiliaryInputs.add("unresolved catalyst: " + BuiltInRegistries.ITEM.getKey(stack.getItem())
                        + itemStateSuffix(stack) + " x" + stack.getCount());
                continue;
            }
            Port port = itemPort(stack, recipe, input, false, !mainInput, nativeStep);
            if (port == null) continue;
            inputs.add(port);
            addMaterial(materials, port);
            mainInput |= port.main();
        }
        for (int input = 0; input < recipe.mFluidInputs.length; input++) {
            FluidStack stack = recipe.mFluidInputs[input];
            if (stack == null || stack.isEmpty()) continue;
            Port port = fluidPort(stack, recipe, input, false, !mainInput);
            if (port == null) continue;
            inputs.add(port);
            if (port.role() == Role.CONSUMED) addMaterial(materials, port);
            mainInput |= port.main();
        }
        for (ItemStack catalyst : gregtech.engine.recipe.RecipeSpecialItems.display(recipe.mSpecialItems)) {
            Port port = catalystPort(catalyst, publication, resourceGeneration);
            if (port != null) inputs.add(port);
        }
        boolean mainOutput = false;
        for (int output = 0; output < recipe.mOutputs.length; output++) {
            ItemStack stack = recipe.mOutputs[output];
            if (stack == null || stack.isEmpty()) continue;
            Port port = itemPort(stack, recipe, output, true, !mainOutput, nativeStep);
            if (port == null) continue;
            outputs.add(port);
            addMaterial(materials, port);
            mainOutput |= port.main();
        }
        for (int output = 0; output < recipe.mFluidOutputs.length; output++) {
            FluidStack stack = recipe.mFluidOutputs[output];
            if (stack == null || stack.isEmpty()) continue;
            Port port = fluidPort(stack, recipe, output, true, !mainOutput);
            if (port == null) continue;
            outputs.add(port);
            addMaterial(materials, port);
            mainOutput |= port.main();
        }
        if (materials.isEmpty()) return null;
        String mapPath = path(map.mNameInternal);
        DataKey reference = new DataKey(
                "gt6ce",
                "process/machine/" + mapPath + "/" + index + "/" + Long.toUnsignedString(publication, 36)
                        + "/" + Long.toUnsignedString(session, 36));
        var details = new ArrayList<String>();
        details.addAll(auxiliaryInputs);
        details.add("map: " + map.mNameInternal);
        details.add("duration: " + recipe.mDuration + " ticks");
        details.add("energy: " + recipe.mEUt + " EU/t");
        if (gregtech.engine.addon.ExclusiveOutputMaps.enabled(map))
            details.add("exclusive output group: the first chance selects one item output");
        if (!recipe.mSource.isBlank()) details.add("source: " + recipe.mSource);
        if (recipe.mFakeRecipe) details.add("symbolic unit template");
        if (recipe.mExactItemInputs) details.add("exact item-state matching");
        if (recipe.mExplicitCatalystsOnly) details.add("only explicit catalysts are retained");
        if (recipe.mNeedsEmptyOutput) details.add("empty output slots required");
        if (recipe.mRequiresEmptyContainerInputs) details.add("empty container inputs required");
        var special = gregtech.engine.recipe.RecipeSpecialItems.display(recipe.mSpecialItems);
        if (!special.isEmpty())
            details.add("special catalysts: " + special.stream()
                    .limit(8)
                    .map(ItemStack::getItem)
                    .map(BuiltInRegistries.ITEM::getKey)
                    .map(ResourceLocation::toString)
                    .toList());
        return new BuiltStep(
                new Step(
                        reference,
                        map.mNameInternal,
                        inputs,
                        outputs,
                        details,
                        Optional.ofNullable(recipe.specialProcess()),
                        true),
                materials);
    }

    private static Port catalystPort(ItemStack stack, long publication, long resourceGeneration) {
        if (stack == null || stack.isEmpty()) return null;
        var form = MaterialEquivalence.form(stack);
        GTMaterial material = form == null ? material(stack) : form.material().resolve();
        String state = itemDisplayState(stack);
        String prefix = form == null ? "item" : form.prefix().getName();
        Resource resource = material == null
                ? new Resource(nativeResourceKey("item", state), "unknown", "item", "solid", state)
                : itemResource(material, prefix, state);
        return scoped(
                new Port(resource, Role.CATALYST, new Amount(stack.getCount(), AmountUnit.ITEM),
                        OptionalInt.empty(), false, List.of("native special catalyst")),
                publication, resourceGeneration);
    }

    private static Port itemPort(
            ItemStack stack, Recipe recipe, int index, boolean output, boolean mainCandidate, String nativeStep) {
        String prefix;
        GTMaterial material;
        String state;
        Amount amount;
        Recipe.MaterialInputDisplay display = recipe.materialInputDisplay(index);
        TagKey<net.minecraft.world.item.Item> tag = output ? null : recipe.itemTag(index);
        String nativeSuffix = itemStateSuffix(stack);
        if (!output && display != null) {
            var representative = material(stack);
            if (representative == null) return nativeItemPort(stack, recipe, index, output, mainCandidate, nativeStep);
            material = representative;
            prefix = "unit";
            var alternatives = display.alternatives().stream()
                    .filter(candidate -> !candidate.isEmpty())
                    .map(ItemStack::getItem)
                    .map(BuiltInRegistries.ITEM::getKey)
                    .distinct()
                    .map(ResourceLocation::toString)
                    .limit(8)
                    .sorted()
                    .toList();
            tag = display.tag() == null ? tag : display.tag();
            state = "unit:" + "rounding=" + display.rounding().name() + "|"
                    + (tag == null ? "" : "tag=" + tag.location() + "|") + String.join(",", alternatives);
            amount = new Amount(display.amount(), AmountUnit.MATERIAL_UNIT);
        } else {
            var equivalent = MaterialEquivalence.form(stack);
            if (equivalent == null) return nativeItemPort(stack, recipe, index, output, mainCandidate, nativeStep);
            material = equivalent.material().resolve();
            if (!material.isValid()) return null;
            prefix = equivalent.prefix().getName();
            state = tag == null
                    ? "item:" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + nativeSuffix
                    : "tag:" + tag.location();
            amount = new Amount(stack.getCount(), AmountUnit.ITEM);
        }
        OptionalInt chance;
        if (output) {
            chance = OptionalInt.of(recipe.getOutputChance(index));
        } else {
            chance = OptionalInt.empty();
        }
        Role role;
        if (output) role = Role.UNKNOWN;
        else if (recipe.isCatalystInput(index)) role = Role.CATALYST;
        else role = Role.CONSUMED;
        var details = new ArrayList<String>();
        if (tag != null) details.add("item tag: " + tag.location());
        if (tag == null && !nativeSuffix.isEmpty()) details.add("native stack data is present");
        if (!output && recipe.hasItemInputGuard(index)) details.add("native item-input guard");
        if (!output && recipe.hasSameItemInputPredicate(index)) details.add("native item-state predicate");
        if (!output && recipe.viewerInputNote(index).length() > 0)
            details.add(recipe.viewerInputNote(index));
        return new Port(
                itemSelector(itemResource(material, prefix, state), recipe, index, output, nativeStep),
                role, amount, chance, mainCandidate, details);
    }

    private static Port fluidPort(
            FluidStack stack, Recipe recipe, int index, boolean output, boolean mainCandidate) {
        RegisteredFluids.FluidEntry entry = GTFluids.entryForFluid(stack.getFluid());
        GTMaterial material = entry == null ? null : FluidVisualPolicy.material(entry).resolve();
        if (material != null && !material.isValid()) material = null;
        TagKey<net.minecraft.world.level.material.Fluid> tag = output ? null : recipe.fluidTag(index);
        String nativeSuffix = fluidStateSuffix(stack);
        String state = tag == null
                ? "fluid:" + BuiltInRegistries.FLUID.getKey(stack.getFluid()) + nativeSuffix
                : "tag:" + tag.location();
        String phase = entry != null && entry.gas() ? "gas" : "liquid";
        Role role;
        if (output) role = Role.UNKNOWN;
        else if (recipe.isCatalystFluidInput(index)) role = Role.CATALYST;
        else if (recipe.isPresenceFluidInput(index)) role = Role.PRESENCE;
        else role = Role.CONSUMED;
        var details = new ArrayList<String>();
        if (tag != null) details.add("fluid tag: " + tag.location());
        if (tag == null && !nativeSuffix.isEmpty()) details.add("native fluid data is present");
        return new Port(
                new Resource(
                        material == null ? nativeResourceKey("fluid", state) : resourceKey("fluid", material, "fluid", phase, state),
                        material == null ? "unknown" : material.getName(),
                        "fluid",
                        phase,
                        state),
                role,
                new Amount(stack.getAmount(), AmountUnit.MILLIBUCKET),
                output ? OptionalInt.of(RecipeOutputChances.CERTAIN) : OptionalInt.empty(),
                mainCandidate,
                details);
    }

    private static BuiltStep worldgenStep(
            MaterialWorldgenFacts.Fact fact, long publication, long session) {
        var merged = new LinkedHashMap<DataKey, Port>();
        var materials = new LinkedHashSet<String>();
        for (MaterialWorldgenFacts.Ore ore : fact.ores()) {
            Resource resource = worldgenResource(ore);
            var details = new ArrayList<>(ore.details());
            details.add("dimension: " + fact.dimension());
            details.add("height: " + ore.minY() + ".." + ore.maxY());
            if (ore.chanceDivisor() > 0) details.add("native roll divisor: " + ore.chanceDivisor());
            Port port = new Port(
                    resource,
                    Role.UNKNOWN,
                    new Amount(Math.max(1, ore.amount()), AmountUnit.ITEM),
                    OptionalInt.empty(),
                    merged.isEmpty(),
                    details);
            Port previous = merged.putIfAbsent(resource.key(), port);
            if (previous != null) {
                var combined = new ArrayList<>(previous.details());
                combined.addAll(port.details());
                merged.put(
                        resource.key(),
                        new Port(
                                resource,
                                previous.role(),
                                new Amount(
                                        Math.max(previous.amount().value(), port.amount().value()),
                                        previous.amount().unit()),
                                previous.chance(),
                                previous.main(),
                                combined));
            }
            materials.add(ore.material().getName());
        }
        if (materials.isEmpty()) return null;
        DataKey reference = new DataKey(
                "gt6ce",
                "process/worldgen/" + path(fact.id()) + "/" + Long.toUnsignedString(publication, 36) + "/"
                        + Long.toUnsignedString(session, 36));
        var details = new ArrayList<String>();
        details.add("worldgen: " + fact.name());
        details.add("kind: " + fact.kind().name().toLowerCase(Locale.ROOT));
        details.add("dimension: " + fact.dimension());
        details.add("height: " + fact.minY() + ".." + fact.maxY());
        if (fact.weight() > 0) {
            details.add("selection weight: " + fact.weight());
            if (fact.totalWeight() > 0) details.add("total selection weight: " + fact.totalWeight());
        }
        return new BuiltStep(
                new Step(reference, fact.name(), List.of(), List.copyOf(merged.values()), details, true),
                materials);
    }

    private static Resource itemResource(GTMaterial material, String prefix, String state) {
        return new Resource(
                resourceKey("item", material, prefix, "solid", state),
                material.resolve().getName(),
                prefix,
                "solid",
                state);
    }

    private static void addMaterial(Set<String> materials, Port port) {
        if (!port.resource().material().equals("unknown")) materials.add(port.resource().material());
    }

    private static Port nativeItemPort(
            ItemStack stack, Recipe recipe, int index, boolean output, boolean main, String nativeStep) {
        GTMaterial material = material(stack);
        String state = "item:" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + itemStateSuffix(stack);
        String form = "item";
        if (stack.getItem() instanceof BlockItem block
                && block.getBlock() instanceof OreBlock ore) {
            material = ore.material().resolve();
            form = ore.isSmall() ? "oreSmall" : "oreVanillastone";
        }
        var tag = output ? null : recipe.itemTag(index);
        if (tag != null) state = "tag:" + tag.location();
        Resource resource = material == null
                ? new Resource(
                        nativeResourceKey("item", state),
                        "unknown", "item", "solid", state)
                : itemResource(material, form, state);
        var details = new ArrayList<String>();
        details.add("native item: " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        details.add("material form is not classified; see the original recipe for matching conditions");
        if (!output && recipe.hasItemInputGuard(index)) details.add("native item-input guard");
        if (!output && recipe.hasSameItemInputPredicate(index)) details.add("native item-state predicate");
        if (!output && !recipe.viewerInputNote(index).isEmpty()) details.add(recipe.viewerInputNote(index));
        return new Port(itemSelector(resource, recipe, index, output, nativeStep), output ? Role.UNKNOWN : Role.CONSUMED,
                new Amount(stack.getCount(), AmountUnit.ITEM),
                output ? OptionalInt.of(recipe.getOutputChance(index)) : OptionalInt.empty(), main, details);
    }

    private static Resource itemSelector(Resource resource, Recipe recipe, int index, boolean output, String nativeStep) {
        if (output || recipe.materialInputDisplay(index) == null
                && !recipe.hasItemInputGuard(index) && !recipe.hasSameItemInputPredicate(index)) return resource;
        var key = new DataKey(resource.key().namespace(), resource.key().path() + "/selector/" + nativeStep + "/" + index);
        return new Resource(key, resource.material(), resource.form(), resource.phase(), resource.state());
    }

    private static DataKey nativeResourceKey(String kind, String state) {
        return new DataKey("gt6ce", "resource/native/" + kind + "/" + path(state) + "/"
                + Integer.toUnsignedString(state.hashCode(), 36));
    }

    private static Resource worldgenResource(MaterialWorldgenFacts.Ore ore) {
        String state = ore.itemState();
        if (state.startsWith("item:")) {
            ResourceLocation id = ResourceLocation.tryParse(state.substring("item:".length()));
            if (id != null) {
                var form = MaterialEquivalence.form(new ItemStack(BuiltInRegistries.ITEM.get(id)));
                if (form != null && form.material().resolve() == ore.material().resolve())
                    return itemResource(ore.material(), form.prefix().getName(), state);
                var item = BuiltInRegistries.ITEM.get(id);
                if (item instanceof BlockItem block
                        && block.getBlock() instanceof OreBlock nativeOre
                        && nativeOre.material().resolve() == ore.material().resolve())
                    return itemResource(ore.material(), nativeOre.isSmall() ? "oreSmall" : "oreVanillastone", state);
            }
        }
        return itemResource(ore.material(), "worldgen", state);
    }

    private static BuiltStep scoped(BuiltStep built, long publication, long session) {
        Step step = built.step;
        return new BuiltStep(new Step(step.reference(), step.name(),
                step.inputs().stream().map(port -> scoped(port, publication, session)).toList(),
                step.outputs().stream().map(port -> scoped(port, publication, session)).toList(),
                step.details(), step.specialProcess(), step.resolved()), built.materials);
    }

    private static Port scoped(Port port, long publication, long session) {
        Resource resource = port.resource();
        var key = new DataKey(resource.key().namespace(), resource.key().path() + "/generation/"
                + Long.toUnsignedString(publication, 36) + "/" + Long.toUnsignedString(session, 36));
        return new Port(new Resource(key, resource.material(), resource.form(), resource.phase(), resource.state()),
                port.role(), port.amount(), port.chance(), port.main(), port.details());
    }

    private static DataKey resourceKey(
            String kind, GTMaterial material, String form, String phase, String state) {
        return new DataKey(
                "gt6ce",
                "resource/" + kind + "/" + path(material.resolve().getName()) + "/" + path(form) + "/"
                        + path(phase) + "/" + path(state) + "/" + Integer.toUnsignedString(state.hashCode(), 36));
    }

    private static GTMaterial material(ItemStack stack) {
        Optional<ItemComposition> composition = ItemMaterialRegistry.get(stack);
        if (composition.isEmpty()) return null;
        var row = composition.get();
        if (row.components().size() != 1) return null;
        return row.components().get(0).material().resolve();
    }

    private static boolean hasMaterialUnitInput(Recipe recipe) {
        for (int index = 0; index < recipe.mInputs.length; index++)
            if (recipe.materialInputDisplay(index) != null) return true;
        return false;
    }

    private static void addStep(
            Map<String, MutableBucket> buckets, Step step, Set<String> materials) {
        for (String material : materials) {
            var bucket = buckets.computeIfAbsent(material, ignored -> new MutableBucket());
            bucket.steps.putIfAbsent(step.reference(), step);
            for (Port port : step.inputs())
                if (port.resource().material().equals(material))
                    bucket.resource(port.resource());
            for (Port port : step.outputs())
                if (port.resource().material().equals(material))
                    bucket.resource(port.resource());
        }
    }

    private static Root bestRoot(String material, Map<String, Resource> resources) {
        Resource best = null;
        int bestRank = Integer.MAX_VALUE;
        for (Resource resource : resources.values()) {
            int rank = rootRank(resource.form());
            if (best == null
                    || rank < bestRank
                    || (rank == bestRank
                            && resource.key().toString().compareTo(best.key().toString()) < 0)) {
                best = resource;
                bestRank = rank;
            }
        }
        return new Root(material, best.form(), best.key());
    }

    private static int rootRank(String form) {
        return switch (form) {
            case "ore", "oreVanillastone" -> 0;
            case "oreRaw", "oreDense" -> 1;
            case "crushed" -> 2;
            case "dust" -> 3;
            case "oreSmall" -> 5;
            case "fluid" -> 6;
            default -> 4;
        };
    }

    private static String path(String value) {
        String path = value.toLowerCase(Locale.ROOT)
                .replace(':', '/')
                .replace(',', '/')
                .replaceAll("[^a-z0-9/._-]", "_");
        return path.isBlank() ? "unknown" : path;
    }

    /** State-sensitive display identity, independent of query scopes and per-recipe selectors. */
    public static String itemDisplayState(ItemStack stack) {
        return "item:" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + itemStateSuffix(stack);
    }

    public static String fluidDisplayState(FluidStack stack) {
        return "fluid:" + BuiltInRegistries.FLUID.getKey(stack.getFluid()) + fluidStateSuffix(stack);
    }

    private static String itemStateSuffix(ItemStack stack) {
        var suffix = new StringBuilder();
        if (stack.isDamaged()) suffix.append("#damaged").append(stack.getDamageValue());
//? if >=1.21.1 {
        /*if (!stack.getComponentsPatch().isEmpty())
            suffix.append("#components").append(fingerprint(stack.getComponentsPatch().toString()));
*///?} else {
        if (stack.hasTag()) suffix.append("#tag").append(fingerprint(stack.getTag().toString()));
//?}
        return suffix.toString();
    }

    private static String fluidStateSuffix(FluidStack stack) {
//? if >=1.21.1 {
        /*return stack.getComponentsPatch().isEmpty()
                ? ""
                : "#components" + fingerprint(stack.getComponentsPatch().toString());
*///?} else {
        return stack.hasTag() ? "#tag" + fingerprint(stack.getTag().toString()) : "";
//?}
    }

    private static String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder(16);
            for (int index = 0; index < 8; index++) {
                int bits = digest[index] & 0xff;
                hex.append(HEX[bits >>> 4]).append(HEX[bits & 0x0f]);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private record BuiltStep(Step step, Set<String> materials) {
        private BuiltStep {
            materials = Set.copyOf(materials);
        }
    }

    private static final class MutableBucket {
        private final LinkedHashMap<DataKey, Step> steps = new LinkedHashMap<>();
        private final LinkedHashMap<String, Resource> roots = new LinkedHashMap<>();

        private void resource(Resource resource) {
            roots.putIfAbsent(resource.key().toString(), resource);
        }

        private Bucket freeze() {
            return new Bucket(List.copyOf(steps.values()), Map.copyOf(roots));
        }
    }

    /** Native stacks and semantic rows for one viewer reload; never published as server recipes. */
    public static final class ViewerBuilder {
        private final MaterialProcessSnapshot source;
        private final Map<String, MutableBucket> buckets = new LinkedHashMap<>();
        private final Map<DataKey, NativeRecipe> recipes = new LinkedHashMap<>();
        private final Map<DataKey, List<ItemStack>> icons = new LinkedHashMap<>();
        private final Map<DataKey, ViewerRecipe> viewerRecipes = new LinkedHashMap<>();
        private final Map<RecipeMap, Set<Recipe>> selected = new java.util.IdentityHashMap<>();
        private final Map<RecipeMap, Map<Recipe, DataKey>> nativeReferences = new java.util.IdentityHashMap<>();
        private int omitted;

        private ViewerBuilder(MaterialProcessSnapshot source) {
            this.source = source;
            source.nativeRecipes.forEach((reference, row) -> nativeReferences
                    .computeIfAbsent(row.map(), ignored -> new java.util.IdentityHashMap<>()).put(row.recipe(), reference));
            source.steps.values().stream()
                    .filter(step -> source.worldgen.containsKey(step.reference())
                            || Optional.ofNullable(source.nativeRecipes.get(step.reference()))
                                    .map(NativeRecipe::map)
                                    .map(map -> !map.mViewerAllowed)
                                    .orElse(false))
                    .sorted(Comparator.comparing(step -> step.reference().toString()))
                    .forEach(step -> {
                        add(step);
                        Optional.ofNullable(source.nativeRecipes.get(step.reference()))
                                .ifPresent(recipe -> recipes.put(step.reference(), recipe));
                    });
        }

        public void omitted() {
            omitted++;
        }

        public long publicationRevision() {
            return source.snapshot.publication().revision();
        }

        public long sessionGeneration() {
            return source.snapshot.sessionGeneration();
        }

        /** Reuses GT matching, unit quantities and recovery ownership, once per visible native row. */
        public boolean machine(RecipeMap map, Recipe recipe) {
            if (!recipe.mEnabled || recipe.mHidden || recipe.mFakeRecipe && !hasMaterialUnitInput(recipe)) return false;
            var rows = selected.computeIfAbsent(map, ignored -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));
            if (rows.contains(recipe)) return true;
            DataKey reference = nativeReferences.getOrDefault(map, Map.of()).get(recipe);
            if (reference != null) {
                add(source.steps.get(reference));
                recipes.put(reference, source.nativeRecipes.get(reference));
                rows.add(recipe);
                return true;
            }
            int index = map.mRecipeList.size() + recipes.size();
            BuiltStep built = recipeStep(
                    map,
                    recipe,
                    index,
                    source.snapshot.publication().revision(),
                    source.snapshot.sessionGeneration(),
                    source.resourceGeneration);
            if (built == null) return false;
            built = scoped(built, source.snapshot.publication().revision(), source.resourceGeneration);
            addStep(buckets, built.step, built.materials);
            recipes.put(built.step.reference(), new NativeRecipe(map, recipe, index));
            rows.add(recipe);
            return true;
        }

        public Port item(ItemStack stack, Role role, long amount, OptionalInt chance) {
            String state = itemDisplayState(stack);
            var form = MaterialEquivalence.form(stack);
            GTMaterial material = form == null ? material(stack) : form.material().resolve();
            String prefix = form == null ? "item" : form.prefix().getName();
            if (stack.getItem() instanceof BlockItem block && block.getBlock() instanceof OreBlock ore) {
                material = ore.material().resolve();
                prefix = ore.isSmall() ? "oreSmall" : "oreVanillastone";
            }
            Resource resource = material == null
                    ? new Resource(nativeResourceKey("item", state), "unknown", "item", "solid", state)
                    : itemResource(material, prefix, state);
            return port(resource, role, new Amount(amount, AmountUnit.ITEM), chance, stack.copyWithCount(1));
        }

        public Port fluid(FluidStack stack, Role role, long amount, OptionalInt chance) {
            var entry = GTFluids.entryForFluid(stack.getFluid());
            GTMaterial material = entry == null ? null : FluidVisualPolicy.material(entry).resolve();
            if (material != null && !material.isValid()) material = null;
            String state = fluidDisplayState(stack);
            String phase = entry != null && entry.gas() ? "gas" : "liquid";
            var resource = new Resource(
                    material == null ? nativeResourceKey("fluid", state) : resourceKey("fluid", material, "fluid", phase, state),
                    material == null ? "unknown" : material.getName(), "fluid", phase, state);
//? if >=1.21.1 {
            /*var lookup = gregtech.engine.fluid.FluidDisplayBinding.currentLookup();
            var icon = lookup == null ? ItemStack.EMPTY : gregtech.engine.fluid.FluidDisplayBinding.display(lookup, stack);
*///?} else {
            var icon = gregtech.engine.fluid.FluidDisplayBinding.display(stack);
//?}
            return port(resource, role, new Amount(amount, AmountUnit.MILLIBUCKET), chance, icon);
        }

        private Port port(Resource resource, Role role, Amount amount, OptionalInt chance, ItemStack icon) {
            var port = scoped(new Port(resource, role, amount, chance, false, List.of()),
                    source.snapshot.publication().revision(), source.resourceGeneration);
            if (!icon.isEmpty()) icons.putIfAbsent(port.resource().key(), List.of(icon.copyWithCount(1)));
            return port;
        }

        /** Alternatives remain one selector, never a list of simultaneously consumed inputs. */
        public Port alternatives(List<Port> candidates) {
            if (candidates.isEmpty()) throw new IllegalArgumentException("Empty viewer selector");
            Port first = candidates.get(0);
            if (candidates.size() == 1) return first;
            Resource representative = first.resource();
            boolean sameMaterial = candidates.stream().allMatch(port -> port.resource().material().equals(representative.material()));
            boolean sameForm = candidates.stream().allMatch(port -> port.resource().form().equals(representative.form()));
            boolean samePhase = candidates.stream().allMatch(port -> port.resource().phase().equals(representative.phase()));
            String state = "selector:" + fingerprint(candidates.stream()
                    .map(port -> port.resource().state()).distinct().sorted().collect(java.util.stream.Collectors.joining("|")));
            Resource resource = new Resource(nativeResourceKey("selector", state),
                    sameMaterial ? representative.material() : "unknown",
                    sameForm ? representative.form() : "selector", samePhase ? representative.phase() : "unknown", state);
            Amount amount = candidates.stream().allMatch(port -> port.amount().equals(first.amount()))
                    ? first.amount() : Amount.unknown();
            OptionalInt chance = !first.output() && candidates.stream().allMatch(port -> port.chance().equals(first.chance()))
                    ? first.chance() : OptionalInt.empty();
            var result = scoped(new Port(resource, first.role(), amount, chance, false,
                    List.of("viewer alternatives: " + candidates.size(), "matching constraints: see original recipe")),
                    source.snapshot.publication().revision(), source.resourceGeneration);
            var representatives = new LinkedHashMap<String, ItemStack>();
            for (Port candidate : candidates)
                for (ItemStack icon : icons.getOrDefault(candidate.resource().key(), List.of())) {
                    if (representatives.size() >= 16) break;
                    representatives.putIfAbsent(itemDisplayState(icon), icon.copyWithCount(1));
                }
            icons.put(result.resource().key(), List.copyOf(representatives.values()));
            return result;
        }

        public void recipe(DataKey reference, String name, ItemStack machine,
                List<Port> inputs, List<Port> outputs, List<String> details) {
            recipe(reference, name, machine, inputs, outputs, details, true);
        }

        public void recipe(DataKey reference, String name, ItemStack machine,
                List<Port> inputs, List<Port> outputs, List<String> details, boolean resolved) {
            recipe(reference, name, name, machine, inputs, outputs, details, resolved);
        }

        public void recipe(DataKey reference, String process, String name, ItemStack machine,
                List<Port> inputs, List<Port> outputs, List<String> details, boolean resolved) {
            resolved &= java.util.stream.Stream.concat(inputs.stream(), outputs.stream())
                    .noneMatch(port -> port.resource().state().startsWith("selector:"));
            add(new Step(reference, name, inputs, outputs, details, resolved));
            viewerRecipes.put(reference, new ViewerRecipe(process, machine.copyWithCount(1)));
        }

        private void add(Step step) {
            var materials = new LinkedHashSet<String>();
            step.inputs().forEach(port -> addMaterial(materials, port));
            step.outputs().forEach(port -> addMaterial(materials, port));
            addStep(buckets, step, materials);
        }

        public MaterialProcessSnapshot build() {
            var frozen = new LinkedHashMap<String, Bucket>();
            buckets.forEach((material, bucket) -> frozen.put(material, bucket.freeze()));
            var roots = frozen.entrySet().stream().map(entry -> bestRoot(entry.getKey(), entry.getValue().roots))
                    .sorted(Comparator.comparing(Root::material, String.CASE_INSENSITIVE_ORDER)).toList();
            var coverage = new ArrayList<>(COVERAGE);
            coverage.add(new Notice("coverage_viewer_recipes", 0));
            coverage.add(new Notice("partial_viewer_recipes", omitted));
            return new MaterialProcessSnapshot(source.snapshot, source.resourceGeneration, frozen, roots, recipes,
                    source.worldgen, icons, viewerRecipes, coverage);
        }
    }

    /** Current publication retained for lifecycle-bound viewer callbacks. */
    public static final class Store {
        private static volatile MaterialProcessSnapshot current = EMPTY;

        private Store() {}

        public static MaterialProcessSnapshot current() {
            return current;
        }

        public static boolean current(MaterialProcessSnapshot snapshot) {
            return current == snapshot;
        }

        public static synchronized MaterialProcessSnapshot publish(
                MaterialPublication.Stamp publication, long sessionGeneration) {
            if (sessionGeneration <= 0 || MachineRecipeOverlay.lastResult().failed()) return current;
            try {
                long started = System.nanoTime();
                var worldgen = MaterialWorldgenFacts.capture();
                if (current.samePublication(publication)
                        && current.snapshot.sessionGeneration() == sessionGeneration
                        && current.snapshot.formRevision() == gregtech.engine.material.MaterialViews.contentRevision()
                        && current.snapshot.tagGeneration() == ItemMaterialRegistry.compositionState().tagGeneration()
                        && current.snapshot.worldgenRevision() == worldgen.revision()) {
                    gregtech.engine.material.MaterialPublication.commit(publication);
                    return current;
                }
                MaterialProcessSnapshot built =
                        MaterialProcessSnapshot.build(publication, sessionGeneration, worldgen);
                current = built;
                gregtech.engine.material.MaterialPublication.commit(publication);
                LOGGER.info(
                        "[gregtech] Material process snapshot {} published with {} material roots, "
                                + "{} recipe refs in {} ms",
                        publication.revision(),
                        built.roots.size(),
                        built.nativeRecipes.size(),
                        (System.nanoTime() - started) / 1_000_000L);
                return built;
            } catch (RuntimeException failure) {
                LOGGER.error(
                        "[gregtech] Material process snapshot {} rebuild failed; retaining revision {}",
                        publication.revision(),
                        current.snapshot.publication().revision(),
                        failure);
                return current;
            }
        }

        public static synchronized void clear(MaterialProcessSnapshot expected) {
            if (current != expected) return;
            current = EMPTY;
        }
    }
}
