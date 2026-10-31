package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.recipe.Recipe;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Mineral wax cracking from original Loader_Recipes_Ores:385-387,473-481.
 * Selected registered forms determine admission, not a named ore or generation-permission list.
 * Ambient byproduct phases reuse the source-backed centrifuge resolver; unknown is not solid.
 */
public final class SharedMineralSteamCrackingRows {
    private static final Set<Recipe> OWNED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<GTMaterial, String> STATUS = new IdentityHashMap<>();
    private static boolean initialized;

    private SharedMineralSteamCrackingRows() {}

    public static Set<Recipe> owned() {
        var copy = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        copy.addAll(OWNED);
        return Collections.unmodifiableSet(copy);
    }

    public static Map<GTMaterial, String> statuses() {
        return Collections.unmodifiableMap(new IdentityHashMap<>(STATUS));
    }

    public static void initialize() {
        if (initialized) throw new IllegalStateException("Mineral steam cracking initialized twice");
        initialized = true;
        rebuild();
    }

    public static void clear() {
        MachineRecipeMaps.SteamCracking.removeAll(OWNED);
        OWNED.clear();
        STATUS.clear();
    }

    public static void rebuild() {
        if (!initialized) return;
        var definitions = new ArrayList<Recipe>();
        var statuses = new IdentityHashMap<GTMaterial, String>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<GTMaterial, Boolean>());
        var snapshot = new Snapshot();
        // These are source ANY.Wax factory members, not a mineral admission whitelist.
        // The original source binds each to 144 mB/U; unavailable wax fluids are skipped.
        var waxes = ReviewedCinnabarFacts.sourceWaxes();
        for (GTMaterial alias : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = alias.resolve();
            if (!seen.add(material)) continue;
            if (!material.isValid() || material.has(MaterialProperty.ANTIMATTER)
                    || material == GTMaterialRegistry.get("Bedrock")) continue;
            if (snapshot.item(MaterialPrefix.dust, material, 1).isEmpty()) continue;
            ItemStack whole = snapshot.item(MaterialPrefix.crushedCentrifuged, material, 1);
            ItemStack tiny = snapshot.item(MaterialPrefix.crushedCentrifugedTiny, material, 1);
            if (whole.isEmpty() || tiny.isEmpty()) continue;

            GTMaterial[] byproducts = new GTMaterial[2];
            SourcePurifiedCentrifugePlan.Environment[] environments = new SourcePurifiedCentrifugePlan.Environment[2];
            int[] millibuckets = new int[2];
            boolean resolved = true;
            for (int index = 0; index < 2; index++) {
                byproducts[index] = index < material.getByProducts().size()
                        ? material.getByProducts().get(index).resolve() : material;
                var environment = snapshot.phases.environment(byproducts[index], SourcePurifiedCentrifugePlan.AMBIENT_K);
                environments[index] = environment;
                if (environment.kind() == SourcePurifiedCentrifugePlan.EnvironmentKind.UNKNOWN) {
                    statuses.put(material, "UNRESOLVED:byproduct-" + index + ":" + environment.evidence());
                    resolved = false;
                    break;
                }
                if (environment.kind() == SourcePurifiedCentrifugePlan.EnvironmentKind.FLUID) {
                    var binding = environment.binding();
                    // Original first rounds U/18 UP, then multiplies that result by nine.
                    millibuckets[index] = OriginalMaterialFluidUnits.millibuckets(
                            OriginalMaterialFluidUnits.SOURCE_UNIT / 18,
                            binding.referenceSourceUnits(), binding.referenceMillibuckets(), true);
                    if (millibuckets[index] < 0) {
                        statuses.put(material, "UNRESOLVED:byproduct-volume-" + index);
                        resolved = false;
                        break;
                    }
                }
            }
            if (!resolved) continue;
            int count = 0;
            for (GTMaterial wax : waxes) for (int scale : new int[]{9, 1}) {
                FluidStack waxFluid = OreFluidBindingAdapter.liquid(wax,
                        scale == 9 ? OriginalMaterialFluidUnits.SOURCE_UNIT
                                : OriginalMaterialFluidUnits.SOURCE_UNIT / 9, true);
                if (waxFluid.isEmpty()) continue;
                FluidStack steam = GTFluids.stack("Steam", 160 * scale);
                FluidStack water = GTFluids.stack("Water", scale);
                if (steam == null || steam.isEmpty() || water == null || water.isEmpty()) continue;
                ItemStack[] outputs = {snapshot.item(MaterialPrefix.dustTiny, material, scale),
                        ItemStack.EMPTY, ItemStack.EMPTY};
                var fluids = new ArrayList<FluidStack>();
                fluids.add(water);
                for (int index = 0; index < 2; index++) {
                    if (millibuckets[index] > 0) {
                        FluidStack fluid = snapshot.phases.fluids.get(environments[index].binding().bindingKey()).copy();
                        fluid.setAmount(Math.multiplyExact(millibuckets[index], scale));
                        fluids.add(fluid);
                    } else outputs[index + 1] = snapshot.item(MaterialPrefix.dustTiny, byproducts[index], scale);
                }
                // Preserve source output slots and aligned 100/40/40 chances, including absent
                // dust slots and repeated byproducts. Fluid outputs are not chance-rolled.
                definitions.add(new Recipe(new ItemStack[]{scale == 9 ? whole.copy() : tiny.copy()},
                        outputs, null, new long[]{10000, 4000, 4000},
                        new FluidStack[]{steam, waxFluid}, fluids.toArray(FluidStack[]::new), 16L * scale, 16, 0));
                count++;
            }
            statuses.put(material, "PLANNED:" + count);
        }
        clear();
        STATUS.putAll(statuses);
        try {
            for (Recipe definition : definitions) {
                Recipe accepted = MachineRecipeMaps.SteamCracking.addRecipe(definition);
                if (accepted == definition) OWNED.add(accepted);
            }
        } catch (RuntimeException | Error failure) {
            clear();
            throw failure;
        }
    }

    private static final class Snapshot {
        final SharedPurifiedCentrifugeRows.Snapshot phases = new SharedPurifiedCentrifugeRows.Snapshot();
        final Map<GTMaterial, Map<MaterialPrefix, List<ItemStack>>> explicit = new IdentityHashMap<>();

        Snapshot() {
            for (var item : ItemMaterialRegistry.entries().keySet()) {
                ItemStack stack = new ItemStack(item);
                var form = MaterialEquivalence.form(stack);
                if (form != null) explicit.computeIfAbsent(form.material().resolve(), key -> new IdentityHashMap<>())
                        .computeIfAbsent(form.prefix(), key -> new ArrayList<>()).add(stack);
            }
        }

        ItemStack item(MaterialPrefix prefix, GTMaterial material, int count) {
            ItemStack selected = GTItems.getStack(prefix, material, count);
            if (!selected.isEmpty()) return selected;
            var candidates = explicit.getOrDefault(material, Map.of()).getOrDefault(prefix, List.of());
            // No arbitrary choice between ambiguous foreign registrations.
            return candidates.size() == 1 ? candidates.get(0).copyWithCount(count) : ItemStack.EMPTY;
        }
    }
}
