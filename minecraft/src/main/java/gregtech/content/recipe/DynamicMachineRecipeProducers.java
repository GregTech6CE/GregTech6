package gregtech.content.recipe;

import gregtech.compat.common.CompatRecipes;
import gregtech.engine.recipe.Recipe;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Ordered producers whose rows are rebuilt outside the static machine-recipe baseline. */
final class DynamicMachineRecipeProducers {
    private record Producer(
            Runnable rebuild,
            Supplier<? extends Collection<Recipe>> baselineExclusions,
            Runnable clear,
            Runnable recover) {
        Producer(Runnable rebuild, Supplier<? extends Collection<Recipe>> baselineExclusions, Runnable clear) {
            this(rebuild, baselineExclusions, clear, clear);
        }
    }

    private static final List<Producer> PRODUCERS = List.of(
            new Producer(
                    DecompositionRecipeLoader::rebuild,
                    DecompositionRecipeLoader::owned,
                    DecompositionRecipeLoader::clear),
            new Producer(
                    ExternalOreProcessing::rebuild,
                    ExternalOreProcessing::owned,
                    ExternalOreProcessing::clearOwnedRows),
            new Producer(NonOreMacerationRows::rebuild, NonOreMacerationRows::owned, NonOreMacerationRows::clear),
            new Producer(SourceOreOutputRows::rebuild, SourceOreOutputRows::owned, SourceOreOutputRows::clear),
            new Producer(
                    SharedPurifiedCentrifugeRows::rebuild,
                    SharedPurifiedCentrifugeRows::owned,
                    SharedPurifiedCentrifugeRows::clear),
            new Producer(
                    SharedMineralSteamCrackingRows::rebuild,
                    SharedMineralSteamCrackingRows::owned,
                    SharedMineralSteamCrackingRows::clear),
            new Producer(SharedOreWashingRows::rebuild, SharedOreWashingRows::owned, SharedOreWashingRows::clear),
            new Producer(SharedOreMagneticRows::rebuild, SharedOreMagneticRows::owned, SharedOreMagneticRows::clear),
            new Producer(SharedOreSiftingRows::rebuild, SharedOreSiftingRows::owned, SharedOreSiftingRows::clear),
            new Producer(CompatRecipes::rebuild, CompatRecipes::owned, CompatRecipes::clear),
            // Captured OPL rows yield to independent static, addon and tag-owned rows.
            new Producer(
                    SharedOreCrushingRows::rebuild,
                    SharedOreCrushingRows::baselineExcluded,
                    SharedOreCrushingRows::clear),
            new Producer(SharedOilSandRows::rebuild, SharedOilSandRows::baselineExcluded, SharedOilSandRows::clear),
            // Purified acid rows yield to the earlier Bath producers.
            new Producer(
                    SharedSpecialProcessRows::rebuild,
                    SharedSpecialProcessRows::owned,
                    SharedSpecialProcessRows::clear),
            // The brewing probe is staged independently; restore it even after overlay failure.
            new Producer(
                    PotionBrewingRecipes::rebuildStaged,
                    PotionBrewingRecipes::owned,
                    PotionBrewingRecipes::clear,
                    PotionBrewingRecipes::rebuildStaged));

    private static final List<Producer> RECOVERY = List.of(
            new Producer(
                    ShredderRecyclingRecipes::rebuildTagRows,
                    ShredderRecyclingRecipes::owned,
                    ShredderRecyclingRecipes::clearTagRows),
            new Producer(
                    VanillaRecoveryRecipes::rebuildCraftingRows,
                    VanillaRecoveryRecipes::craftingRows,
                    VanillaRecoveryRecipes::clearCraftingRows),
            thermal());

    private static final List<Producer> ALL = java.util.stream.Stream.concat(PRODUCERS.stream(), RECOVERY.stream())
            .toList();

    private DynamicMachineRecipeProducers() {}

    static void rebuild() {
        for (var producer : PRODUCERS) producer.rebuild().run();
    }

    static void rebuildRecovery() {
        for (var producer : RECOVERY) producer.rebuild().run();
    }

    static Set<Recipe> baselineExclusions() {
        var rows = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        for (var producer : ALL) rows.addAll(producer.baselineExclusions().get());
        return rows;
    }

    static void recover() {
        for (var producer : ALL) producer.recover().run();
    }

    static void clear() {
        for (var producer : ALL) producer.clear().run();
    }

    private static Producer thermal() {
        return new Producer(
                () -> {
                    MoltenMetalUnitRecipes.begin();
                    SharedThermalInputRows.rebuild();
                    SharedThermalRecyclingRows.rebuild();
                    MoltenMetalUnitRecipes.finish();
                },
                () -> {
                    var rows = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
                    rows.addAll(SharedThermalInputRows.owned());
                    rows.addAll(SharedThermalRecyclingRows.owned());
                    rows.addAll(MoltenMetalUnitRecipes.owned());
                    return rows;
                },
                () -> {
                    SharedThermalInputRows.clear();
                    SharedThermalRecyclingRows.clear();
                    MoltenMetalUnitRecipes.clear();
                });
    }
}
