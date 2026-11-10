package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.FluidCatalog;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;
import java.util.function.BiFunction;
import static gregtech.content.recipe.SourceThermalNativeAdapter.*;

/** Audited source recyclable single-component input forms, not a complete recycling listener.
 * The earlier one-unit producer and reviewed/imported rows remain independent. There is no
 * alias scan, generation gate, multi-component inference, or legacy fallback for new inputs.
 */
public final class SharedThermalInputRows {
    private SharedThermalInputRows() {}
    private static final Map<RecipeMap, Set<Recipe>> OWNED = new IdentityHashMap<>();
    private static final List<Deferred> DEFERRED = new ArrayList<>();
    private static final List<Admission> ADMISSIONS = new ArrayList<>();
    private static List<GTMaterial> materials = List.of();
    private static boolean initialized;
    public record Deferred(String material, MaterialPrefix prefix, Action action, Reason reason) {}
    public record Admission(String material, MaterialPrefix prefix, String map, boolean accepted) {}
    public static Set<Recipe> owned() {
        var result = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        OWNED.values().forEach(result::addAll); return Collections.unmodifiableSet(result);
    }
    public static List<Deferred> deferred() { return List.copyOf(DEFERRED); }
    public static List<Admission> admissions() { return List.copyOf(ADMISSIONS); }
    public static void initialize() {
        if (initialized) throw new IllegalStateException("Thermal input rows initialized twice");
        initialized = true;
        materials = unique(GTMaterialRegistry.allMaterials());
        // First admission waits for the overlay, after fixed Ice/Snow and other static recipes.
    }
    private static List<GTMaterial> unique(Collection<GTMaterial> selected) {
        Set<GTMaterial> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        var out = new ArrayList<GTMaterial>();
        for (var value : selected) if (value != null && seen.add(value.resolve())) out.add(value.resolve());
        out.sort(Comparator.comparingInt(GTMaterial::getId)); return List.copyOf(out);
    }
    private static void clearOwned() {
        OWNED.forEach((map, rows) -> rows.forEach(map::unlink)); OWNED.clear();
    }
    /** Startup identities survive server stop; accepted objects and diagnostics do not. */
    public static void clear() { clearOwned(); DEFERRED.clear(); ADMISSIONS.clear(); }
    public static void rebuild() {
        if (!initialized) return;
        var snapshot = new Snapshot();
        var planned = definitions(materials, snapshot);
        var prepared = new ArrayList<Prepared>();
        for (var definition : planned) {
            var recipe = definition.recipe();
            if (recipe != null) prepared.add(new Prepared(definition, recipe));
        }
        clearOwned(); DEFERRED.clear(); DEFERRED.addAll(snapshot.deferred); ADMISSIONS.clear();
        try {
            for (var candidate : prepared) {
                var definition = candidate.definition();
                RecipeMap map = definition.row().map();
                Recipe accepted;
                if (map == gregtech.content.data.MachineRecipeMaps.Smelter && MoltenMetalUnitRecipes.collect(
                        candidate.recipe().mInputs[0], candidate.recipe().mFluidOutputs[0],
                        candidate.recipe().mDuration, candidate.recipe().mEUt, definition.row().inputProof()::matches)) {
                    accepted = null;
                } else {
                    accepted = RecoveryRecipeConflicts.publish(map, candidate.recipe());
                }
                ADMISSIONS.add(new Admission(definition.material().getName(), definition.prefix(), map.mNameInternal, accepted != null));
                if (accepted != null) OWNED.computeIfAbsent(map, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(accepted);
            }
        } catch (RuntimeException | Error failure) { clear(); throw failure; }
    }
    static void smeltingInputs(java.util.function.BiConsumer<ItemStack, FluidStack> visitor) {
        if (!initialized) return;
        for (var definition : definitions(materials, new Snapshot())) {
            var row = definition.row();
            if (row.map() == gregtech.content.data.MachineRecipeMaps.Smelter) visitor.accept(row.input(), row.output());
        }
    }
    record Prepared(Definition definition, Recipe recipe) {}

    record Definition(GTMaterial material, MaterialPrefix prefix, NativeRow row) {
        Recipe recipe() {
            if (row.input().isEmpty() || row.output().isEmpty()) return null;
            var recipe = RecipeMap.make(true, new ItemStack[]{row.input().copy()}, new ItemStack[0], null, null,
                    new FluidStack[0], new FluidStack[]{row.output().copy()}, row.ticks(), row.power(), 0);
            // Retain the registration proof at lookup and consumption: no damaged or filled input.
            // This existing guard preserves equivalent material aliases, unlike exact-item matching.
            return recipe == null
                    ? null
                    : recipe.withMaterialRecovery()
                            .withItemInputGuard(0, row.inputProof()::matches)
                            .withSource("recovery:thermal-input");
        }
    }
    static List<Definition> definitions(Collection<GTMaterial> selected, Snapshot snapshot) {
        var out = new ArrayList<Definition>();
        for (var material : unique(selected)) for (var form : SourceThermalInputRules.forms()) {
            if (AddonMaterials.owns(material)) continue;
            MaterialPrefix prefix = form.prefix();
            if (snapshot.reviewedOwner(prefix, material)) continue;
            ItemStack input = snapshot.selected(prefix, material);
            var result = inspectForm(prefix, material, input, snapshot);
            for (var row : result.rows()) out.add(new Definition(material, prefix, row));
            if (result.action() == Action.DEFERRED_NO_ROW || result.action() == Action.LEGACY_UNSUPPORTED)
                snapshot.deferred.add(new Deferred(material.getName(), prefix, result.action(), result.reason()));
        }
        return List.copyOf(out);
    }
    static class Snapshot extends SourceThermalNativeAdapter.Snapshot {
        final List<Deferred> deferred = new ArrayList<>();
        Snapshot() { super(); }
        Snapshot(Map<String, FluidCatalog.FluidEntry> definitions, BiFunction<String, Integer, FluidStack> fluids) {
            super(definitions, fluids);
        }
        ItemStack selected(MaterialPrefix prefix, GTMaterial material) { return GTItems.getStack(prefix, material, 1); }
        boolean reviewedOwner(MaterialPrefix prefix, GTMaterial material) {
            return material == GTMaterialRegistry.get(material.getName()).resolve()
                    && ThermalRecoveryOwnership.reviewedSpecialOwner(prefix, material);
        }
    }
}
