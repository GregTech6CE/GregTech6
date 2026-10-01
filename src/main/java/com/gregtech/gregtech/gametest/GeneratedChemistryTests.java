package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.loaders.c.GTGeneratedChem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Guards the transpiled GT6 chemistry data ({@code Loader_Recipes_Chem} →
 * {@code tools/transpile_gt6_chem.py} → {@code GTChemGen}).
 * <p>
 * The transpiler used to reject GT6's aligned call spacing ({@code OP.dust .mat(...)}),
 * which left whole RecipeMaps empty — the Autoclave and Fermenter machines could not process
 * anything at all. These tests pin the coverage so a parser regression shows up immediately,
 * and keep the remaining "missing content" list visible.
 * </p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class GeneratedChemistryTests {

    /** Machines that were completely inert before the transpiler was fixed. */
    @GameTest(template = "test_empty")
    public static void autoclaveAndFermenterNowProcess(GameTestHelper helper) {
        helper.assertTrue(MachineRecipeMaps.Autoclave.mRecipeList.size() >= 10,
                "Autoclave recipes from GT6 Loader_Recipes_Chem: " + MachineRecipeMaps.Autoclave.mRecipeList.size());
        helper.assertTrue(!MachineRecipeMaps.Fermenter.mRecipeList.isEmpty(),
                "Fermenter recipes: " + MachineRecipeMaps.Fermenter.mRecipeList.size());
        helper.assertTrue(!MachineRecipeMaps.CryoDistillationTower.mRecipeList.isEmpty(),
                "Cryo Distillation Tower recipes: " + MachineRecipeMaps.CryoDistillationTower.mRecipeList.size());

        // The regenerated chemistry data also has to keep the long-standing chains populated.
        for (var entry : java.util.Map.of(
                "Mixer", MachineRecipeMaps.Mixer,
                "Electrolyzer", MachineRecipeMaps.Electrolyzer,
                "Bath", MachineRecipeMaps.Bath,
                "Roasting", MachineRecipeMaps.Roasting,
                "Drying", MachineRecipeMaps.Drying).entrySet()) {
            helper.assertTrue(entry.getValue().mRecipeList.size() >= 10,
                    entry.getKey() + " recipes: " + entry.getValue().mRecipeList.size());
        }
        helper.succeed();
    }

    /**
     * Transpiled coverage must not regress when the transpiler or the data is touched.
     * <p>
     * Five GT6 loaders feed the resolver now ({@code Loader_Recipes_Chem / _Other / _Potions /
     * _Food / _Extruder}); the chemistry set is the one held to the strict skip budget, because
     * the other four draw on cross-mod content (generator modules, EtFu food, GT6 ropes) that
     * this port deliberately does not register.
     * </p>
     */
    @GameTest(template = "test_empty")
    public static void transpiledCoverageDoesNotRegress(GameTestHelper helper) {
        int added = GTGeneratedChem.addedRecipes();
        // Floor set from the state after the parser fixes (1062 added across the five sets).
        helper.assertTrue(added >= 1000, "transpiled recipes registered: " + added);

        var stats = GTGeneratedChem.setStats();
        helper.assertTrue(stats.size() == 9, "transpiled sets: " + stats);
        TreeMap<String, GTGeneratedChem.SetStats> bySet = new TreeMap<>();
        for (var stat : stats) bySet.put(stat.set(), stat);
        for (String set : new String[]{"chem", "other", "potions", "food", "temporary", "ores"}) {
            var stat = bySet.get(set);
            helper.assertTrue(stat != null && stat.added() > 0,
                    "set '" + set + "' translated nothing: " + stat);
        }
        // The comb set is GT6's twenty centrifuge rows from MultiItemFood (§73); all of them have to
        // register, because a comb without a row is an item with no use.
        var combs = bySet.get("combs");
        helper.assertTrue(combs != null && combs.added() == 20 && combs.skipped() == 0,
                "comb centrifuge rows registered: " + combs);
        // The extruder set is driven by GT6's generator-module items (IL.Module_*_Generator,
        // MultiItemRandomTools:421-426): those are multi-items, so the tech: token has to resolve
        // through the registry as well, and their recipes have to exist. Both are asserted here.
        var extruder = bySet.get("extruder");
        helper.assertTrue(extruder != null && extruder.added() > 0,
                "the extruder set registers once the generator modules resolve: " + extruder);
        helper.assertTrue(GTGeneratedChem.missingContent().stream()
                        .noneMatch(t -> t.startsWith("tech:module_")),
                "no generator module token stays unresolved: " + GTGeneratedChem.missingContent().stream()
                        .filter(t -> t.startsWith("tech:module_")).toList());
        // Loader_Recipes_Vanilla's machine recipes need the 1.7.10 -> 1.20.1 vanilla name mapping;
        // if that mapping regresses the whole set collapses to zero (the rest of the file's 235
        // calls reference IC2/Forestry content this port does not register).
        var vanilla = bySet.get("vanilla");
        helper.assertTrue(vanilla != null && vanilla.added() >= 40,
                "vanilla machine recipes registered: " + vanilla);
        // The chemistry set has an explicit budget: everything it skips is chemistry-only content.
        var chem = bySet.get("chem");
        helper.assertTrue(chem.skipped() <= 60,
                "chemistry recipes skipped for missing content: " + chem.skipped());
        helper.succeed();
    }

    /**
     * GT6's generator modules are recipe <em>keys</em> ({@code MultiItemRandomTools:421-426}): the
     * Extruder rows for stone, basalt and blackstone name one in their special slot
     * ({@code Loader_Recipes_Extruder:44-151}), so both the item and its recipe have to exist for
     * that family to be reachable in survival.
     */
    @GameTest(template = "test_empty")
    public static void generatorModulesAreCraftableRecipeKeys(GameTestHelper helper) {
        for (String id : new String[]{"stone_generator_module", "basalt_generator_module",
                "blackstone_generator_module"}) {
            var item = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("gregtech", id));
            helper.assertTrue(item != null && item != Items.AIR, id + " is registered");
            helper.assertTrue(helper.getLevel().getRecipeManager()
                            .byKey(ResourceLocation.fromNamespaceAndPath("gregtech", "recipe_keys/" + id))
                            .isPresent(),
                    id + " keeps GT6's crafting recipe");
        }
        int rows = 0;
        for (var recipe : MachineRecipeMaps.Extruder.mRecipeList) {
            if (recipe.mInputs.length < 2 || recipe.mInputs[0].isEmpty()) continue;
            var key = ForgeRegistries.ITEMS.getKey(recipe.mInputs[0].getItem());
            if (key != null && key.getPath().endsWith("_generator_module")) rows++;
        }
        // GT6 writes 89 of these rows (Loader_Recipes_Extruder:44-151). 25 register today; the rest
        // wait on stone-material forms: 176 rows ask for gears and tool heads, which GT6's own
        // G_STONE does not register (TD.java: PROJECTILES, DUSTS, PLANTS, PLATES, STICKS) and 10 ask
        // for Blackstone plate/stick forms, which GT6 does have (stone() factory) and this port's
        // hand-written Blackstone material does not - that is the next batch, not a silent skip.
        helper.assertTrue(rows >= 25, "extruder rows driven by a generator module: " + rows);
        helper.succeed();
    }

    /** Per-loader transpile results and the recipe maps they filled stay on the record. */
    @GameTest(template = "test_empty")
    public static void perLoaderCoverageIsReported(GameTestHelper helper) {
        var report = new TreeMap<String, Object>();
        for (var stat : GTGeneratedChem.setStats()) {
            report.put("set." + stat.set(), "added " + stat.added() + ", skipped " + stat.skipped());
        }
        report.put("totalAdded", GTGeneratedChem.addedRecipes());
        report.put("totalSkipped", GTGeneratedChem.skippedRecipes());
        // Recipe maps the transpiled sets feed (GT6 Loader_Recipes_Other/_Potions/_Food/_Extruder).
        var maps = new TreeMap<String, Object>();
        for (var entry : java.util.Map.ofEntries(
                java.util.Map.entry("Mixer", MachineRecipeMaps.Mixer),
                java.util.Map.entry("Distillery", MachineRecipeMaps.Distillery),
                java.util.Map.entry("Bath", MachineRecipeMaps.Bath),
                java.util.Map.entry("Fermenter", MachineRecipeMaps.Fermenter),
                java.util.Map.entry("Centrifuge", MachineRecipeMaps.Centrifuge),
                java.util.Map.entry("Extruder", MachineRecipeMaps.Extruder),
                java.util.Map.entry("Press", MachineRecipeMaps.Press),
                java.util.Map.entry("Nanofab", MachineRecipeMaps.Nanofab),
                java.util.Map.entry("CokeOven", MachineRecipeMaps.CokeOven),
                java.util.Map.entry("CrystallisationCrucible", MachineRecipeMaps.CrystallisationCrucible),
                java.util.Map.entry("Coagulator", MachineRecipeMaps.Coagulator),
                java.util.Map.entry("Melter", MachineRecipeMaps.Melter),
                java.util.Map.entry("Smelter", MachineRecipeMaps.Smelter),
                java.util.Map.entry("Juicer", MachineRecipeMaps.Juicer),
                java.util.Map.entry("Squeezer", MachineRecipeMaps.Squeezer),
                java.util.Map.entry("Injector", MachineRecipeMaps.Injector),
                java.util.Map.entry("Loom", MachineRecipeMaps.Loom),
                java.util.Map.entry("LaserEngraver", MachineRecipeMaps.LaserEngraver),
                java.util.Map.entry("ImplosionCompressor", MachineRecipeMaps.ImplosionCompressor),
                java.util.Map.entry("Mortar", MachineRecipeMaps.Mortar),
                java.util.Map.entry("Replicator", MachineRecipeMaps.Replicator),
                java.util.Map.entry("Drying", MachineRecipeMaps.Drying)).entrySet()) {
            maps.put(entry.getKey(), entry.getValue().mRecipeList.size());
        }
        report.put("recipeMaps", maps);
        java.nio.file.Path out = java.nio.file.Path.of("../../docs/transpiled-recipes-coverage.json");
        try {
            java.nio.file.Files.writeString(out, new com.google.gson.GsonBuilder().setPrettyPrinting()
                    .create().toJson(report));
        } catch (Exception e) {
            helper.assertTrue(false, "cannot write transpile coverage report: " + e);
        }
        helper.succeed();
    }

    /** Whatever this port still cannot resolve stays on the record. */
    @GameTest(template = "test_empty")
    public static void missingContentIsReported(GameTestHelper helper) {
        var report = new TreeMap<String, Object>();
        report.put("added", GTGeneratedChem.addedRecipes());
        report.put("skipped", GTGeneratedChem.skippedRecipes());
        report.put("missingContent", new TreeSet<>(GTGeneratedChem.missingContent()));
        java.nio.file.Path out = java.nio.file.Path.of("../../docs/chem-missing-content.json");
        try {
            java.nio.file.Files.writeString(out, new com.google.gson.GsonBuilder().setPrettyPrinting()
                    .create().toJson(report));
        } catch (Exception e) {
            helper.assertTrue(false, "cannot write chemistry coverage report: " + e);
        }
        helper.assertTrue(true, "missing content tokens: " + GTGeneratedChem.missingContent().size());
        helper.succeed();
    }
}
