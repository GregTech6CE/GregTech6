package gregtech.content.data;

import com.google.gson.Gson;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.platform.Mc;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.machine.BasicMachineRegistries;
*///?} else {
import gregtech.content.registry.GTBasicMachines;
//?}
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Crafting recipes for the single-block machines, generated from the original GT6
 * "Basic Machines" registrations (see {@link BasicMachineCraftingRecipes}).
 * <p>
 * The previous implementation invented three generic patterns based only on the energy
 * type, so every machine's crafting grid differed from GregTech 6. This contributor emits
 * the original pattern rows and key symbols for every tier, with ingredient translation
 * handled by {@link MachineRecipeIngredients}.
 * </p>
 */
public final class BasicMachineRecipePack {
    /** Machines whose crafting recipe comes from the multiblock table instead. */
    public static final Set<String> MULTIBLOCK_CONTROLLERS = Set.of(
            "fusionreactor", "cryodistillationtower", "distillationtower",
            "cokeoven", "implosioncompressor",
            "largecentrifuge", "largeelectrolyzer", "largecoagulator", "largeautoclave",
            "largebath", "largemixer", "largefermenter", "largeoven", "largesluice",
            "largecrusher", "largeshredder", "largesqueezer", "largemassfab");
    private static final Gson GSON = new Gson();

    private BasicMachineRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
//? if >=1.21.1 {
        /*for (var entry : BasicMachineRegistries.all()) {
            if (!entry.isBound()) continue;
*///?} else {
        for (var entry : GTBasicMachines.all()) {
            if (!entry.isPresent()) continue;
//?}
            BasicMachineBlock machine = entry.get();
            var spec = machine.basicSpec();
            if (MULTIBLOCK_CONTROLLERS.contains(spec.machineName())) continue;

            ResourceLocation outputId = Mc.itemId(machine.asItem());
            if (outputId == null) continue;

            BasicMachineCraftingRecipes.Entry table = BasicMachineCraftingRecipes.find(spec.machineName(), spec.tier());
            if (table == null) continue;

            Map<String, Object> recipe = buildRecipe(table, spec.material(), spec.tier(), outputId);
            if (recipe == null) continue;

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

    private static Map<String, Object> buildRecipe(BasicMachineCraftingRecipes.Entry table, GTMaterial material,
                                                   int tier, ResourceLocation outputId) {
        Map<Character, Object> ingredients = MachineRecipeIngredients.resolveAll(table.keys(), material, tier);
        Map<String, Object> key = new HashMap<>();
        for (var entry : ingredients.entrySet()) {
            key.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        if (key.isEmpty()) return null;

        List<String> pattern = new ArrayList<>(List.of(table.rows()));
        // GT6 omits trailing all-space rows from the shaped pattern.
        while (!pattern.isEmpty() && pattern.get(pattern.size() - 1).isBlank()) {
            pattern.remove(pattern.size() - 1);
        }
        if (pattern.isEmpty()) return null;

        Map<String, Object> recipe = new HashMap<>();
        recipe.put("type", "gt6ce:tool_shaped");
        recipe.put("pattern", pattern);
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
