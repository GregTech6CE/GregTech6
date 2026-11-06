package gregtech.buildtools;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class RecipeResourceConverterTest {
    @TempDir
    Path temp;

    @Test
    void itemAndTagIngredientsKeepTheNativeObjectForm() {
        var converted = RecipeResourceConverter.convertRecipe(
                "test",
                JsonParser.parseString("""
                        {"type":"minecraft:crafting_shapeless",
                         "ingredients":[{"item":"gt6ce:a"},{"tag":"forge:b"},[{"item":"gt6ce:c"}]],
                         "result":{"item":"gt6ce:d","count":4}}
                        """)
                        .getAsJsonObject());
        assertEquals("gt6ce:d", converted.getAsJsonObject("result").get("id").getAsString());
        assertEquals(4, converted.getAsJsonObject("result").get("count").getAsInt());
        // 1.21.1 rejects a bare string ingredient: the value object is the native form.
        assertEquals(
                "gt6ce:a",
                converted.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString());
        assertEquals(
                "forge:b",
                converted.getAsJsonArray("ingredients").get(1).getAsJsonObject().get("tag").getAsString());
        assertEquals(
                "gt6ce:c",
                converted.getAsJsonArray("ingredients")
                        .get(2)
                        .getAsJsonArray()
                        .get(0)
                        .getAsJsonObject()
                        .get("item")
                        .getAsString());
    }

    @Test
    void shapedKeysAreKeptAsValueObjects() {
        var converted = RecipeResourceConverter.convertRecipe(
                "test",
                JsonParser.parseString("""
                        {"type":"gt6ce:tool_shaped","pattern":["AB"],
                         "key":{"A":{"item":"gt6ce:a"},"B":{"tag":"gt6ce:b"}},
                         "result":{"item":"gt6ce:c"}}
                        """)
                        .getAsJsonObject());
        assertEquals("gt6ce:a", converted.getAsJsonObject("key").getAsJsonObject("A").get("item").getAsString());
        assertEquals("gt6ce:b", converted.getAsJsonObject("key").getAsJsonObject("B").get("tag").getAsString());
        assertEquals("gt6ce:c", converted.getAsJsonObject("result").get("id").getAsString());
    }

    @Test
    void smeltingResultKeepsASingleItem() {
        var converted = RecipeResourceConverter.convertRecipe(
                "test",
                JsonParser.parseString("""
                        {"type":"minecraft:smelting","ingredient":{"item":"minecraft:clay"},
                         "result":"minecraft:brick","experience":0.3,"cookingtime":200}
                        """)
                        .getAsJsonObject());
        assertEquals("minecraft:brick", converted.getAsJsonObject("result").get("id").getAsString());
        assertEquals(
                "minecraft:clay", converted.getAsJsonObject("ingredient").get("item").getAsString());
    }

    @Test
    void customIngredientTypesStayObjects() {
        var converted = RecipeResourceConverter.convertRecipe(
                "test",
                JsonParser.parseString("""
                        {"type":"minecraft:crafting_shapeless",
                         "ingredients":[{"type":"gt6ce:block_tag","tag":"gt6ce:stone"}],
                         "result":{"id":"gt6ce:a"}}
                        """)
                        .getAsJsonObject());
        assertTrue(converted.getAsJsonArray("ingredients").get(0).isJsonObject());
    }

    @Test
    void toolRepairNeedsNoResult() {
        var converted = RecipeResourceConverter.convertRecipe(
                "test", JsonParser.parseString("{\"type\":\"gt6ce:tool_repair\"}").getAsJsonObject());
        assertFalse(converted.has("result"));
    }

    @Test
    void unsupportedDocumentsFailWithTheirPath() {
        var failure = assertThrows(
                RecipeResourceConverter.ConversionException.class,
                () -> RecipeResourceConverter.convertRecipe(
                        "gt6ce:recipes/x.json",
                        JsonParser.parseString(
                                        "{\"type\":\"minecraft:campfire_cooking\",\"result\":{\"item\":\"a\"}}")
                                .getAsJsonObject()));
        assertTrue(failure.getMessage().contains("gt6ce:recipes/x.json"));
    }

    @Test
    void resultNbtIsRejectedRatherThanDropped() {
        assertThrows(
                RecipeResourceConverter.ConversionException.class,
                () -> RecipeResourceConverter.convertRecipe(
                        "x",
                        JsonParser.parseString("""
                                {"type":"minecraft:crafting_shapeless","ingredients":[{"item":"a"}],
                                 "result":{"item":"b","nbt":"{}"}}
                                """)
                                .getAsJsonObject()));
    }

    @Test
    void resultsOutsideTheVanillaStackRangeAreRejected() {
        assertThrows(
                RecipeResourceConverter.ConversionException.class,
                () -> RecipeResourceConverter.convertRecipe(
                        "x",
                        JsonParser.parseString("""
                                {"type":"minecraft:crafting_shapeless","ingredients":[{"item":"a"}],
                                 "result":{"item":"b","count":100}}
                                """)
                                .getAsJsonObject()));
    }

    @Test
    void writerProducesOneNativeTreeAndRejectsCollisions() throws IOException {
        var source = temp.resolve("source");
        Files.createDirectories(source.resolve("gt6ce/recipes/sub"));
        Files.createDirectories(source.resolve("gt6ce/tags/items/group"));
        Files.writeString(
                source.resolve("gt6ce/recipes/sub/a.json"),
                "{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:a\"},\"result\":\"minecraft:b\"}",
                StandardCharsets.UTF_8);
        Files.writeString(
                source.resolve("gt6ce/recipes/replaced.json"),
                "{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:a\"},\"result\":\"minecraft:b\"}",
                StandardCharsets.UTF_8);
        Files.writeString(
                source.resolve("gt6ce/tags/items/group/t.json"),
                "{\"replace\":false,\"values\":[\"gt6ce:a\"]}",
                StandardCharsets.UTF_8);

        Path output = temp.resolve("output");
        var summary = RecipeResourceWriter.write(
                source, output, Set.of("gt6ce/recipes/replaced.json"), null, null);
        assertEquals(1, summary.recipes());
        assertEquals(1, summary.tags());
        assertEquals(1, summary.excluded());
        assertTrue(Files.exists(output.resolve("gt6ce/recipe/sub/a.json")));
        assertTrue(Files.exists(output.resolve("gt6ce/tags/item/group/t.json")));
        assertFalse(Files.exists(output.resolve("gt6ce/recipe/replaced.json")));

        // A second identical run must not leave stale or duplicated files.
        RecipeResourceWriter.write(source, output, Set.of("gt6ce/recipes/replaced.json"), null, null);
        try (var walk = Files.walk(output)) {
            assertEquals(2, walk.filter(Files::isRegularFile).count());
        }

        var existing = temp.resolve("existing");
        Files.createDirectories(existing.resolve("gt6ce/recipe/sub"));
        Files.writeString(existing.resolve("gt6ce/recipe/sub/a.json"), "{}");
        assertThrows(
                IOException.class,
                () -> RecipeResourceWriter.write(
                        source, output, Set.of("gt6ce/recipes/replaced.json"), existing, null));
    }

    @Test
    void staleExclusionsFailTheBuild() throws IOException {
        var source = temp.resolve("source");
        Files.createDirectories(source.resolve("gt6ce/recipes"));
        Files.writeString(
                source.resolve("gt6ce/recipes/a.json"),
                "{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:a\"},\"result\":\"minecraft:b\"}");
        assertThrows(
                IOException.class,
                () -> RecipeResourceWriter.write(
                        source, temp.resolve("out"), Set.of("gt6ce/recipes/gone.json"), null, null));
    }

    @Test
    void outputIsStableAcrossRuns() throws IOException {
        var source = temp.resolve("source");
        Files.createDirectories(source.resolve("gt6ce/recipes"));
        Files.writeString(
                source.resolve("gt6ce/recipes/b.json"),
                "{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"a\"}],\"result\":{\"item\":\"b\"}}");
        Files.writeString(
                source.resolve("gt6ce/recipes/a.json"),
                "{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"b\"}],\"result\":{\"item\":\"a\"}}");
        var first = temp.resolve("first");
        var second = temp.resolve("second");
        RecipeResourceWriter.write(source, first, Set.of(), null, null);
        RecipeResourceWriter.write(source, second, Set.of(), null, null);
        for (String rel : List.of("gt6ce/recipe/a.json", "gt6ce/recipe/b.json"))
            assertEquals(
                    Files.readString(first.resolve(rel)), Files.readString(second.resolve(rel)));
    }
}
