package gregtech.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MultiblockCraftingRecipeTests {
    @GameTest(template = "test_empty", batch = "multiblock_crafting")
    public static void preservesPipeAndIronIngredients(GameTestHelper helper) throws IOException {
        var tower = recipe(helper, "distillation_tower_main");
        helper.assertTrue(tower.getAsJsonArray("pattern").equals(JsonParser.parseString("[\"PPP\",\"PMP\",\"PPP\"]")),
                "The distillation tower should retain its original pattern");
        assertIngredient(helper, tower, "P", "item", "gt6ce:pipe_nonuple_stainless_steel");
        assertIngredient(helper, tower, "M", "item", "gt6ce:distillation_tower_part");

        var oven = recipe(helper, "coke_oven_main");
        helper.assertTrue(oven.getAsJsonArray("pattern").equals(JsonParser.parseString("[\"hRT\",\"PMR\",\"dRT\"]")),
                "The coke oven should retain its original pattern");
        assertIngredient(helper, oven, "P", "tag", "gt6ce:technology/any_iron_plate");
        assertIngredient(helper, oven, "R", "tag", "gt6ce:technology/any_iron_rod");
        assertIngredient(helper, oven, "T", "tag", "gt6ce:technology/any_iron_screw");
        helper.succeed();
    }

    private static JsonObject recipe(GameTestHelper helper, String name) throws IOException {
//? if >=1.21.1 {
        /*String folder = "recipe";
*///?} else {
        String folder = "recipes";
//?}
        var id = ResourceLocation.fromNamespaceAndPath("gt6ce", folder + "/machines/multiblock/" + name + ".json");
        var resource = helper.getLevel().getServer().getResourceManager().getResource(id).orElseThrow();
        try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void assertIngredient(GameTestHelper helper, JsonObject recipe, String key, String kind, String id) {
        var ingredient = recipe.getAsJsonObject("key").getAsJsonObject(key);
        helper.assertTrue(ingredient.has(kind) && ingredient.get(kind).getAsString().equals(id),
                "Expected " + key + " to resolve to " + id + ", got " + ingredient);
    }
}
