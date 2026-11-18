package gregtech.content.data;

import com.google.gson.Gson;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.machine.BasicMachineCatalog;
import gregtech.content.registry.GTBasicMachines;
import gregtech.platform.Mc;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Publishes the catalog crafting recipes for registered single-block machines. */
public final class BasicMachineRecipePack {
    private static final Gson GSON = new Gson();

    private BasicMachineRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        for (var entry : GTBasicMachines.all()) {
            if (!entry.isBound()) continue;
            BasicMachineBlock machine = entry.get();
            var spec = machine.basicSpec();

            ResourceLocation outputId = Mc.itemId(machine.asItem());
            if (outputId == null) continue;

            BasicMachineCraftingRecipes.Recipe crafting = BasicMachineCatalog.crafting(spec.machineName(), spec.tier());
            if (crafting == null) continue;

            GTMaterial casing = BasicMachineCatalog.casing(spec.machineName(), spec.tier());
            Map<String, Object> recipe = buildRecipe(crafting, casing, outputId);

            generated.put(ResourceLocation.fromNamespaceAndPath("gt6ce",
//? if >=1.21.1 {
                    /*"recipe/machines/basic/" + spec.id() + ".json"),
*///?} else {
                    "recipes/machines/basic/" + spec.id() + ".json"),
//?}
                    GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8));
        }
        MachineRecipeIngredients.logSubstitutions();
    }

    private static Map<String, Object> buildRecipe(BasicMachineCraftingRecipes.Recipe crafting, GTMaterial casing,
                                                   ResourceLocation outputId) {
        Map<String, Object> key = new HashMap<>();
        crafting.keys().forEach((symbol, ingredient) ->
                key.put(String.valueOf(symbol), MachineRecipeIngredients.resolve(ingredient, casing)));

        Map<String, Object> recipe = new HashMap<>();
        recipe.put("type", "gt6ce:tool_shaped");
        recipe.put("pattern", crafting.rows());
        recipe.put("key", key);
        // GT6's machine patterns are aRegistry.add(...) recipes, i.e. CR.DEF_REV without the MIR
        // flag (CR); 1.20.1 mirrors shaped recipes by default.
        recipe.put("allow_mirror", false);
//? if >=1.21.1 {
        /*recipe.put("result", Map.of("id", outputId.toString()));
*///?} else {
        recipe.put("result", Map.of("item", outputId.toString()));
//?}
        recipe.put("group", "gregtech_machines");
        return recipe;
    }
}
