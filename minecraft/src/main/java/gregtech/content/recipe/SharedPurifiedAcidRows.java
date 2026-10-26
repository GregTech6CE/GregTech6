package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.FluidCatalog;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTFluids;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;
import java.util.function.BiFunction;
import static gregtech.content.recipe.SharedOreWashingRows.NativeForm;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourcePurifiedAcidData.*;
import static gregtech.content.recipe.SourcePurifiedAcidRules.*;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;
import static gregtech.content.recipe.SourceRecyclingFacts.Identity;

/** Source Ores108-159 purified acid Bath calls, independent of ordinary ore washing gates.
 * First admission is deliberately after existing static/imported producers. Ownership is only
 * the object returned by ordinary addRecipe; no legacy signatures or unrelated rows are removed.
 */
public final class SharedPurifiedAcidRows {
    private SharedPurifiedAcidRows() {}
    private static final Set<Recipe> OWNED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final List<Deferred> DEFERRED = new ArrayList<>();
    private static final List<Admission> ADMISSIONS = new ArrayList<>();
    private static List<GTMaterial> materials = List.of();
    private static boolean initialized;

    public record Deferred(String material, String producer, String reason) {}
    public record Admission(String material, String producer, Shape shape, boolean accepted) {}
    public static Set<Recipe> owned() {
        var result = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        result.addAll(OWNED); return Collections.unmodifiableSet(result);
    }
    public static List<Deferred> deferred() { return List.copyOf(DEFERRED); }
    public static List<Admission> admissions() { return List.copyOf(ADMISSIONS); }

    /** Capture only the bounded source identity scope, not dynamic byproduct membership or forms. */
    public static void initialize() {
        if (initialized) throw new IllegalStateException("Purified acid rows initialized twice");
        initialized = true;
        Set<GTMaterial> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        var selected = new ArrayList<GTMaterial>();
        for (var value : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = value.resolve();
            if (sourceIdentity(material) && seen.add(material)) selected.add(material);
        }
        selected.sort(Comparator.comparingInt(GTMaterial::getId));
        materials = List.copyOf(selected);
        // No early rebuild: the existing ten imported acid calls and foreign producers retain
        // their normal priority before the first overlay admits this new producer.
    }
    private static void clearOwned() {
        OWNED.forEach(MachineRecipeMaps.Bath::unlink); OWNED.clear();
    }
    /** Keep initialized identities for a subsequent server in the same JVM. */
    public static void clear() { clearOwned(); DEFERRED.clear(); ADMISSIONS.clear(); }
    public static void rebuild() {
        if (!initialized) return;
        var snapshot = new Snapshot();
        var planned = definitions(materials, snapshot);
        clearOwned(); DEFERRED.clear(); DEFERRED.addAll(snapshot.deferred); ADMISSIONS.clear();
        try {
            for (var definition : planned) {
                Recipe accepted = definition.admit();
                ADMISSIONS.add(new Admission(definition.material().name(), definition.producer(),
                        definition.shape(), accepted != null));
                if (accepted != null) OWNED.add(accepted);
            }
        } catch (RuntimeException | Error failure) { clear(); throw failure; }
    }

    record Definition(Identity material, String producer, Shape shape, Result result, ItemStack input,
                      ItemStack[] outputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs) {
        Recipe admit() {
            // RecipeMap removes EMPTY fluids; unresolved source-positive phases must never reach it.
            if (input.isEmpty() || fluidInputs.length != 1) return null;
            for (var fluid : fluidInputs) if (fluid == null || fluid.isEmpty()) return null;
            for (var fluid : fluidOutputs) if (fluid == null || fluid.isEmpty()) return null;
            long[] chances = result.outputs().stream().mapToLong(Slot::chance).toArray();
            return MachineRecipeMaps.Bath.addRecipe(result.optimize(), new ItemStack[]{input.copy()},
                    Arrays.stream(outputs).map(ItemStack::copy).toArray(ItemStack[]::new), null, chances,
                    Arrays.stream(fluidInputs).map(FluidStack::copy).toArray(FluidStack[]::new),
                    Arrays.stream(fluidOutputs).map(FluidStack::copy).toArray(FluidStack[]::new),
                    result.ticks(), result.power(), 0);
        }
    }

    static List<Definition> definitions(Collection<GTMaterial> selected, Snapshot snapshot) {
        var out = new ArrayList<Definition>();
        var candidates = new ArrayList<GTMaterial>();
        Set<GTMaterial> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var material : selected) if (material != null && seen.add(material)) candidates.add(material);
        // Literal family/member/shape order is significant; fixed families precede the dynamic loop.
        for (var candidate : fixedCandidates()) for (var material : candidates) {
            if (!candidate.material().matches(material.getId(), material.getName())) continue;
            String producer = "fixed/" + candidate.family().sourceLine();
            if (!snapshot.context(material)) {
                snapshot.note(material, producer, "unverified-source-material/" + candidate.shape()); continue;
            }
            var resolution = new Resolution(snapshot);
            var result = fixedPlan(candidate.family(), candidate.material(), candidate.shape(), resolution);
            add(material, producer, candidate.shape(), result, resolution, snapshot, out);
        }
        candidates.sort(Comparator.comparingInt(GTMaterial::getId));
        for (var material : candidates) {
            Decision selector = snapshot.dynamicDecision(material);
            if (selector == Decision.NO) continue;
            for (var shape : Shape.values()) {
                var resolution = new Resolution(snapshot);
                var result = manganesePlan(identity(material), shape, selector, resolution);
                add(material, "manganese", shape, result, resolution, snapshot, out);
            }
        }
        return List.copyOf(out);
    }
    private static void add(GTMaterial material, String producer, Shape shape, Result result,
                            Resolution resolution, Snapshot snapshot, List<Definition> out) {
        if (result.status() != Status.ROW) {
            if (result.status() == Status.UNSUPPORTED)
                snapshot.note(material, producer, shape + ":" + result.reason() + resolution.problem);
            return;
        }
        ItemStack[] outputs = result.outputs().stream().map(slot -> slot.item() == null
                ? ItemStack.EMPTY : resolution.items.get(slot.item()).stack().copy()).toArray(ItemStack[]::new);
        FluidStack[] inputs = result.fluidInputs().stream().map(fluid -> resolution.fluids.get(fluid).stack().copy())
                .toArray(FluidStack[]::new);
        FluidStack[] fluids = result.fluidOutputs().stream().map(fluid -> resolution.fluids.get(fluid).stack().copy())
                .toArray(FluidStack[]::new);
        out.add(new Definition(identity(material), producer, shape, result,
                resolution.items.get(result.input()).stack().copy(), outputs, inputs, fluids));
    }
    /** Cache exactly the objects proved during one plan; do not re-resolve an output after planning. */
    private static final class Resolution implements Resolver {
        final Snapshot snapshot;
        final Map<SourcePurifiedAcidRules.Item, NativeForm> items = new HashMap<>();
        final Map<SourcePurifiedAcidData.Fluid, NativeFluid> fluids = new HashMap<>();
        String problem = "";
        Resolution(Snapshot snapshot) { this.snapshot = snapshot; }
        @Override public Availability item(MaterialPrefix prefix, Identity material, int count) {
            var key = new SourcePurifiedAcidRules.Item(prefix, material, count);
            var result = items.computeIfAbsent(key, ignored -> snapshot.item(prefix, material, count));
            if (result.availability() == Availability.UNKNOWN) problem = "/form:" + material.name() + "/" + prefix.getName();
            return result.availability();
        }
        @Override public Availability fluid(Phase phase, long amount) {
            var key = new SourcePurifiedAcidData.Fluid(phase, amount);
            var result = fluids.computeIfAbsent(key, ignored -> snapshot.fluid(phase, amount));
            if (result.availability() == Availability.UNKNOWN) problem = "/phase:" + phase.nativeField();
            return result.availability();
        }
    }
    private static Identity identity(GTMaterial material) { return new Identity(material.getId(), material.getName()); }
    private static boolean canonical(GTMaterial material) {
        return material != null && material.isValid() && material.resolve() == material
                && GTMaterialRegistry.get(material.getName()).resolve() == material
                && GTMaterialRegistry.isCoreMaterial(material) && !AddonMaterials.owns(material);
    }
    private static boolean sourceIdentity(GTMaterial material) {
        // This table supplies identity evidence only, never thermal target/flag permissions.
        return canonical(material) && (SourceRecyclingFacts.find(material.getId(), material.getName()) != null
                || reviewedIdentity(identity(material)));
    }
    record NativeFluid(Availability availability, FluidStack stack) {
        static NativeFluid unknown() { return new NativeFluid(Availability.UNKNOWN, FluidStack.EMPTY); }
        static NativeFluid present(FluidStack stack) { return new NativeFluid(Availability.PRESENT, stack.copy()); }
    }
    static class Snapshot {
        final List<Deferred> deferred = new ArrayList<>();
        private final SharedOreWashingRows.Snapshot forms = new SharedOreWashingRows.Snapshot();
        private final Map<String, FluidCatalog.FluidEntry> fluidDefinitions;
        private final BiFunction<String, Integer, FluidStack> lookup;
        Snapshot() { this(FluidCatalog.all(), GTFluids::stack); }
        Snapshot(Map<String, FluidCatalog.FluidEntry> definitions, BiFunction<String, Integer, FluidStack> lookup) {
            this.fluidDefinitions = Map.copyOf(definitions); this.lookup = lookup;
        }
        void note(GTMaterial material, String producer, String reason) {
            deferred.add(new Deferred(material.getName(), producer, reason));
        }
        boolean context(GTMaterial material) { return sourceIdentity(material); }
        boolean anti(GTMaterial material) { return material.has(MaterialProperty.ANTIMATTER); }
        List<Identity> directByproducts(GTMaterial material) {
            var result = new ArrayList<Identity>();
            for (var byproduct : material.getByProducts()) {
                GTMaterial value = byproduct == null ? null : byproduct.resolve();
                result.add(sourceIdentity(value) ? identity(value) : null);
            }
            return Collections.unmodifiableList(result);
        }
        Decision dynamicDecision(GTMaterial material) {
            if (!context(material)) return Decision.UNKNOWN;
            if (anti(material)) return Decision.NO;
            return manganeseSelector(material.isValid(), false, directManganeseMembership(directByproducts(material)));
        }
        NativeForm item(MaterialPrefix prefix, Identity identity, int count) {
            GTMaterial material = material(identity);
            if (material == null) return NativeForm.unknown();
            // Direct forms only: no ORES/generation gate, continuation policy, or OM main fallback.
            return forms.direct(prefix, material, count);
        }
        GTMaterial material(Identity identity) {
            if (identity == null) return null;
            GTMaterial material = GTMaterialRegistry.get(identity.name()).resolve();
            return canonical(material) && identity.matches(material.getId(), material.getName()) ? material : null;
        }
        NativeFluid fluid(Phase phase, long amount) {
            if (phase == null || amount <= 0 || amount > Integer.MAX_VALUE || material(phase.material()) == null)
                return NativeFluid.unknown();
            String field = phase.nativeField();
            var entry = fluidDefinitions.get(field);
            if (entry == null || !FluidCatalog.canonicalField(field).equals(field) || FluidCatalog.get(field) != entry
                    || entry.gas() != (phase.state() == State.GAS)) return NativeFluid.unknown();
            if (phase == Phase.WATER || phase == Phase.HYDROGEN) {
                var expected = phase == Phase.WATER ? FluidCatalog.Water : FluidCatalog.Hydrogen;
                if (entry != expected || !FluidCatalog.isBuiltInDefinition(entry)) return NativeFluid.unknown();
                if (phase == Phase.HYDROGEN && !phase.material().name().equals(FluidCatalog.boundMaterial(entry.registryName())))
                    return NativeFluid.unknown();
            } else if (!FluidCatalog.isGeneratedGasLiquidDefinition(field, entry)
                    || !phase.material().name().equals(entry.materialKey())) return NativeFluid.unknown();
            String expectedPath = phase == Phase.WATER ? "minecraft:water"
                    : "gt6ce:" + FluidCatalog.sanitizePath(entry.registryName());
            if (!phase.registryId().equals(expectedPath)) return NativeFluid.unknown();
            var stack = lookup.apply(field, (int)amount);
            if (stack == null || stack.isEmpty() || stack.getAmount() != amount
                    || !BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString().equals(phase.registryId()))
                return NativeFluid.unknown();
            if (phase == Phase.WATER ? stack.getFluid() != Fluids.WATER : GTFluids.entryForFluid(stack.getFluid()) != entry)
                return NativeFluid.unknown();
            // All eighteen source phases are positively evidenced. A missing native output holder
            // is UNKNOWN too, never source ABSENT that could silently discard a vitriol/gas product.
            return NativeFluid.present(stack);
        }
    }
}
