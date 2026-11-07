package gregtech.content.data;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Full original shaped/head/assembly recipes over shared tool patterns; owned by {@link RecipePublishing}. */
public final class ManualToolRecipePack {
    private static final Gson GSON = new Gson();

    private ManualToolRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        int shaped = 0, heads = 0, assemblies = 0;
        List<String> without = new ArrayList<>();
        for (var type : gregtech.engine.tool.GTToolType.values()) {
            var patterns = gregtech.content.recipe.GTToolRecipes.shaped(type);
            for (int i = 0; i < patterns.size(); i++) {
                if (!gregtech.content.recipe.GTToolPatternRecipe.hasMaterials(type, patterns.get(i))) continue;
                add(generated, "tools/" + type.id() + (i == 0 ? "" : "_" + i), "tool_crafting", type.id(), i);
                shaped++;
            }
            boolean assembly = gregtech.content.tool.ManualToolRecipeCatalog.isHeadAssembly(type.definition());
            if (assembly) {
                add(generated, "tools/" + type.id() + "_assembly", "tool_assembly", type.id(), 0);
                assemblies++;
            }
            if (patterns.isEmpty() && !assembly) without.add(type.id());
            var headPatterns = gregtech.content.recipe.GTToolRecipes.heads(type);
            for (int i = 0; i < headPatterns.size(); i++) {
                if (!gregtech.content.recipe.GTToolPatternRecipe.hasMaterials(type, headPatterns.get(i))) continue;
                add(generated, "tool_heads/" + type.id() + (i == 0 ? "" : "_" + i), "tool_head", type.id(), i);
                heads++;
            }
        }
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "[gregtech] Original manual tool datapack: {} shaped tools, {} head+handle assemblies, {} heads; original table without manual row: {}",
                        shaped,
                        assemblies,
                        heads,
                        without);
        int powered = 0;
        for (var material : gregtech.engine.material.GTMaterialRegistry.allMaterials())
            for (var spec : gregtech.content.tool.ElectricToolAssembly.values()) {
                java.util.List<net.minecraft.world.item.ItemStack> batteries = new ArrayList<>();
                for (var entry : gregtech.content.registry.GTChemicalBatteries.all())
                    if (entry.get().spec().tier() == spec.definition().tier())
                        batteries.add(new net.minecraft.world.item.ItemStack(
                                entry.get().asItem()));
                for (var battery : batteries) {
                    if (spec.recipe(material, battery, "") == null) continue;
                    var batteryId = BuiltInRegistries.ITEM.getKey(battery.getItem());
                    String suffix = "/" + batteryId.getPath();
                    var id = ResourceLocation.fromNamespaceAndPath(
                            "gt6ce",
                            "recipe/electric_tools/" + spec.id + "/"
                                    + material.getName().toLowerCase(java.util.Locale.ROOT) + suffix + ".json");
                    var bytes = GSON.toJson(Map.of(
                                    "type",
                                    "gt6ce:electric_tool_assembly",
                                    "tool",
                                    spec.id,
                                    "material",
                                    material.getName(),
                                    "battery",
                                    batteryId.toString()))
                            .getBytes(StandardCharsets.UTF_8);
                    if (generated.putIfAbsent(id, bytes) != null)
                        throw new IllegalStateException("Duplicate powered assembly " + id);
                    powered++;
                }
            }
        com.mojang.logging.LogUtils.getLogger()
                .info("[gregtech] Original powered tool datapack: {} concrete material/battery assemblies", powered);
        OriginalHandCraftingRows.add(generated);
        gregtech.content.recipe.ConstructionCraftingRecipeInjector.add(generated);
        gregtech.content.recipe.FormConversionCraftingRecipeInjector.add(generated);
        gregtech.content.recipe.StoneCraftingRecipeInjector.add(generated);
        gregtech.content.recipe.BedrockFlowerCraftingRecipeInjector.add(generated);
    }

    private static void add(
            Map<ResourceLocation, byte[]> data, String id, String serializer, String tool, int pattern) {
        var key = ResourceLocation.fromNamespaceAndPath("gt6ce", "recipe/" + id + ".json");
        byte[] value = GSON.toJson(Map.of("type", "gt6ce:" + serializer, "tool", tool, "pattern", pattern))
                .getBytes(StandardCharsets.UTF_8);
        if (data.putIfAbsent(key, value) != null) throw new IllegalStateException("Duplicate manual tool recipe " + id);
    }
}
