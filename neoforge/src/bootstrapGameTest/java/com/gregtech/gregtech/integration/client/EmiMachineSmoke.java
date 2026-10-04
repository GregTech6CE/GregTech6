package com.gregtech.gregtech.integration.client;

import com.gregtech.gregtech.emi.GregTechEMIPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import java.util.ArrayList;

/** Calls the actual optional plugin against a recording native EMI registry. */
final class EmiMachineSmoke {
    /** Validate EMI's real loaded registry and output lookup, without JEI installed. */
    static EmiRecipe installedRecipe() throws ReflectiveOperationException {
        if (net.neoforged.fml.ModList.get().isLoaded("jei"))
            throw new IllegalStateException("EMI-only probe must run without JEI");
        // RecipeManager is published before search baking finishes. Wait for the whole reload.
        if (!(boolean) Class.forName("dev.emi.emi.runtime.EmiReloadManager")
                .getMethod("isLoaded").invoke(null)) return null;
        // EMI performs a second, sorted bake in a separate worker after publishing that manager.
        if (Class.forName("dev.emi.emi.registry.EmiRecipes")
                .getField("activeWorker").get(null) != null) return null;
        var manager = dev.emi.emi.api.EmiApi.getRecipeManager();
        var nativeRecipes = manager.getRecipes().stream()
                .filter(r -> r instanceof com.gregtech.gregtech.emi.MachineEmiRecipe).toList();
        long expected = com.gregtech.gregtech.api.recipe.RecipeMap.RECIPE_MAP_LIST.stream()
                .flatMap(m -> m.mRecipeList.stream()).filter(r -> r.mEnabled && !r.mHidden).count();
        if (nativeRecipes.size() != expected) return null; // EMI reload runs on its own thread.
        for (String machine : new String[]{"rollingmill", "extruder"}) {
            var recipe = nativeRecipes.stream()
                    .filter(r -> r.getCategory().getId().getPath().contains(machine))
                    .filter(r -> r.getOutputs().stream().anyMatch(s -> s.getId().getPath().startsWith("plate_")))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Installed EMI missing " + machine));
            var plate = recipe.getOutputs().stream().filter(s -> s.getId().getPath().startsWith("plate_")).findFirst().orElseThrow();
            if (!manager.getRecipesByOutput(plate).contains(recipe))
                throw new IllegalStateException("EMI plate output search omits " + machine);
        }
        for (String machine : new String[]{"crusher", "bath", "centrifuge", "shredder", "sluice", "magneticseparator", "sifter"}) {
            var ore = nativeRecipes.stream().filter(r -> r.getCategory().getId().getPath().contains(machine))
                .filter(r -> r.getInputs().stream().anyMatch(s -> s.getEmiStacks().stream().anyMatch(t ->
                    t.getId().getPath().startsWith("ore_") || t.getId().getPath().startsWith("crushed_") || t.getId().getPath().startsWith("crushed_purified_"))))
                .findFirst().orElseThrow(() -> new IllegalStateException("Installed EMI missing ore rows: " + machine));
            var input = ore.getInputs().stream().filter(s -> !s.getEmiStacks().isEmpty()).findFirst().orElseThrow().getEmiStacks().get(0);
            if (!manager.getRecipesByInput(input).contains(ore)) throw new IllegalStateException("EMI ore input search omits " + machine);
            var output = ore.getOutputs().stream().filter(s -> !s.isEmpty()).findFirst().orElseThrow();
            if (!manager.getRecipesByOutput(output).contains(ore)) throw new IllegalStateException("EMI ore output search omits " + machine);
            com.mojang.logging.LogUtils.getLogger().info("EMI_ORE_LOOKUP_SUCCESS {} {}", machine, ore.getId());
        }
        com.mojang.logging.LogUtils.getLogger().info("EMI_INSTALLED_SMOKE_SUCCESS {} native recipes, plate output lookups work, JEI absent", nativeRecipes.size());
        return nativeRecipes.stream().filter(r -> r.getCategory().getId().getPath().contains("extruder"))
                .filter(r -> r.getOutputs().stream().anyMatch(s -> s.getId().getPath().startsWith("plate_")))
                .findFirst().orElseThrow();
    }

    static void check() {
        var recipes = new ArrayList<EmiRecipe>();
        EmiRegistry recording = (EmiRegistry) java.lang.reflect.Proxy.newProxyInstance(
                EmiRegistry.class.getClassLoader(), new Class<?>[]{EmiRegistry.class}, (proxy, method, args) -> {
                    if (method.getName().equals("addRecipe")) recipes.add((EmiRecipe)args[0]);
                    return null;
                });
        new GregTechEMIPlugin().register(recording);
        for (String machine : new String[]{"rollingmill", "extruder"}) {
            long plates = recipes.stream().filter(r -> r.getCategory().getId().getPath().toLowerCase().contains(machine))
                    .filter(r -> r.getOutputs().stream().anyMatch(s -> s.getId().getPath().startsWith("plate_"))).count();
            if (plates == 0) throw new IllegalStateException("EMI missing plate recipes in " + machine);
        }
        long expected = com.gregtech.gregtech.api.recipe.RecipeMap.RECIPE_MAP_LIST.stream()
                .flatMap(m -> m.mRecipeList.stream()).filter(r -> r.mEnabled && !r.mHidden).count();
        if (recipes.stream().filter(r -> r instanceof com.gregtech.gregtech.emi.MachineEmiRecipe).count() != expected) throw new IllegalStateException("EMI omitted enabled machine recipes");
        com.mojang.logging.LogUtils.getLogger().info("EMI_MACHINE_SMOKE_SUCCESS {} registered recipes; rolling mill and extruder plate outputs present", recipes.size());
    }
}
