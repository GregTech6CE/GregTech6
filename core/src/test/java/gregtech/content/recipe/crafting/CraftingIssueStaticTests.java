package gregtech.content.recipe.crafting;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.recipe.ClayMoldCatalog;
import gregtech.content.recipe.OriginalFormConversions;
import gregtech.content.recipe.VanillaCraftingReplacements;
import gregtech.engine.recipe.data.JsonMaps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** Targeted static regressions for fixed crafting-table issues; see docs/development/testing.md. */
final class CraftingIssueStaticTests {
    private static final Path RECIPE_ROOT = recipeRoot();

    // Runs every targeted row and reports all mismatches in one pass.
    @Test
    void fixedIssueCasesMatchTheirStaticSources() throws IOException {
        var cases = readCases();
        assertFalse(cases.isEmpty(), "case table is empty");
        var checks = new ArrayList<Executable>();
        for (var testCase : cases) checks.add(() -> verify(testCase));
        assertAll("crafting issue static cases", checks);
    }

    // Dispatches a row to the static source it describes.
    private static void verify(Case testCase) throws IOException {
        switch (testCase.source()) {
            case "json" -> verifyJson(testCase);
            case "vanilla" -> verifyVanilla(testCase);
            case "form" -> verifyForm(testCase);
            case "mold" -> verifyMold(testCase);
            default -> throw new AssertionError(testCase.label("unknown source " + testCase.source()));
        }
    }

    // Compares one shipped recipe JSON with its table expectations.
    private static void verifyJson(Case testCase) throws IOException {
        var file = RECIPE_ROOT.resolve(testCase.target());
        assertTrue(Files.isRegularFile(file), testCase.label("missing " + file));
        Map<String, Object> json;
        try {
            json = JsonMaps.object(Files.readString(file, StandardCharsets.UTF_8));
        } catch (RuntimeException failure) {
            throw new AssertionError(testCase.label("cannot parse " + file), failure);
        }
        assertEquals(testCase.expected().get("type"), json.get("type"), testCase.label("type"));
        switch (testCase.kind()) {
            case "shaped" -> {
                assertExpectedKeys(testCase, "key:", "type", "pattern", "result", "count");
                assertEquals(testCase.expected().get("pattern"), pattern(testCase, json), testCase.label("pattern"));
                var keys = asMap(json.get("key"), testCase.label("key"));
                for (var entry : testCase.expected().entrySet()) {
                    if (!entry.getKey().startsWith("key:")) continue;
                    var symbol = entry.getKey().substring("key:".length());
                    assertEquals(entry.getValue(), ingredient(keys.get(symbol)), testCase.label("key " + symbol));
                }
                assertResult(testCase, json);
            }
            case "shapeless" -> {
                assertExpectedKeys(testCase, "ingredient:", "type", "result", "count");
                var ingredients = asList(json.get("ingredients"), testCase.label("ingredients"));
                for (var entry : testCase.expected().entrySet()) {
                    if (!entry.getKey().startsWith("ingredient:")) continue;
                    var index = Integer.parseInt(entry.getKey().substring("ingredient:".length()));
                    assertTrue(index < ingredients.size(), testCase.label("ingredient " + index));
                    assertEquals(
                            entry.getValue(),
                            ingredient(ingredients.get(index)),
                            testCase.label("ingredient " + index));
                }
                assertResult(testCase, json);
            }
            case "custom" -> {
                assertExpectedKeys(testCase, null, "type", "category");
                for (var entry : testCase.expected().entrySet()) {
                    if (entry.getKey().equals("type")) continue;
                    assertEquals(entry.getValue(), json.get(entry.getKey()), testCase.label(entry.getKey()));
                }
            }
            default -> throw new AssertionError(testCase.label("unknown json kind " + testCase.kind()));
        }
    }

    // Compares one generated vanilla replacement row with the table.
    private static void verifyVanilla(Case testCase) {
        assertEquals("row", testCase.kind(), testCase.label("kind"));
        assertExpectedKeys(testCase, "key:", "pattern", "result", "count");
        var row = VanillaCraftingReplacements.rows().stream()
                .filter(candidate -> candidate.id().equals(testCase.target()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(testCase.label("missing vanilla row")));
        assertEquals(
                testCase.expected().getOrDefault("pattern", ""),
                row.pattern() == null ? "" : row.pattern(),
                testCase.label("pattern"));
        assertEquals(testCase.expected().get("result"), row.result(), testCase.label("result"));
        assertEquals(
                Integer.parseInt(testCase.expected().getOrDefault("count", "1")), row.count(), testCase.label("count"));
        for (var entry : testCase.expected().entrySet()) {
            if (!entry.getKey().startsWith("key:")) continue;
            var symbol = entry.getKey().charAt("key:".length());
            assertEquals(entry.getValue(), row.keys().get(symbol), testCase.label("key " + symbol));
        }
    }

    // Compares one form-conversion catalog row with the table.
    private static void verifyForm(Case testCase) {
        assertEquals("conversion", testCase.kind(), testCase.label("kind"));
        assertExpectedKeys(testCase, null, "inputCount", "outputCount", "single");
        var parts = testCase.target().split("->");
        assertEquals(2, parts.length, testCase.label("form target"));
        var conversion = OriginalFormConversions.FIXED.stream()
                .filter(candidate ->
                        candidate.input().equals(parts[0]) && candidate.output().equals(parts[1]))
                .findFirst()
                .orElseThrow(() -> new AssertionError(testCase.label("missing conversion")));
        assertEquals(
                Integer.parseInt(testCase.expected().get("inputCount")),
                conversion.inputCount(),
                testCase.label("input count"));
        assertEquals(
                Integer.parseInt(testCase.expected().get("outputCount")),
                conversion.outputCount(),
                testCase.label("output count"));
        assertEquals(
                Boolean.parseBoolean(testCase.expected().get("single")), conversion.single(), testCase.label("single"));
    }

    // Compares one clay-mold form and its generated tag with the table.
    private static void verifyMold(Case testCase) {
        assertEquals("form", testCase.kind(), testCase.label("kind"));
        assertExpectedKeys(testCase, null, "prefix", "family", "tag");
        var prefix = testCase.expected().get("prefix");
        var family = testCase.expected().get("family");
        var form = ClayMoldCatalog.FORMS.stream()
                .filter(candidate ->
                        candidate.prefix().equals(prefix) && candidate.family().equals(family))
                .findFirst()
                .orElseThrow(() -> new AssertionError(testCase.label("missing mold form")));
        assertEquals(testCase.expected().get("tag"), form.tag(), testCase.label("mold tag"));
    }

    // Compares result item/count, accepting both item and id keys.
    private static void assertResult(Case testCase, Map<String, Object> json) {
        var result = asMap(json.get("result"), testCase.label("result"));
        var resultId = result.containsKey("item") ? result.get("item") : result.get("id");
        assertEquals(testCase.expected().get("result"), resultId, testCase.label("result"));
        var count = result.get("count") instanceof Number number ? number.intValue() : 1;
        assertEquals(Integer.parseInt(testCase.expected().getOrDefault("count", "1")), count, testCase.label("count"));
    }

    // Rejects unknown or missing expectation keys for this row kind.
    private static void assertExpectedKeys(Case testCase, String prefixed, String... required) {
        var requiredKeys = new HashSet<>(List.of(required));
        for (var key : testCase.expected().keySet()) {
            assertTrue(
                    requiredKeys.contains(key) || (prefixed != null && key.startsWith(prefixed)),
                    testCase.label("unknown expectation " + key));
        }
        for (var key : required) {
            assertTrue(testCase.expected().containsKey(key), testCase.label("missing expectation " + key));
        }
    }

    // Renders a shaped pattern as row1/row2/row3.
    private static String pattern(Case testCase, Map<String, Object> json) {
        var rows = new ArrayList<String>();
        for (var row : asList(json.get("pattern"), testCase.label("pattern"))) rows.add(String.valueOf(row));
        return String.join("/", rows);
    }

    // Renders item/tag/type ingredients as a compact string.
    private static String ingredient(Object node) {
        if (!(node instanceof Map<?, ?> map)) return String.valueOf(node);
        if (map.get("item") instanceof String item) return "item:" + item;
        if (map.get("tag") instanceof String tag) return "tag:" + tag;
        if (map.get("type") instanceof String type) return "type:" + type;
        return String.valueOf(map);
    }

    // Asserts an object value and returns it as a string-keyed map.
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value, String label) {
        assertNotNull(value, label);
        assertTrue(value instanceof Map<?, ?>, label + " is not an object");
        return (Map<String, Object>) value;
    }

    // Asserts an array value and returns it as a typed list.
    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object value, String label) {
        assertNotNull(value, label);
        assertTrue(value instanceof List<?>, label + " is not an array");
        return (List<Object>) value;
    }

    // Loads the tab-separated case table from test resources.
    private static List<Case> readCases() throws IOException {
        var stream = CraftingIssueStaticTests.class.getResourceAsStream("/gregtech/crafting-issue-static-cases.tsv");
        assertNotNull(stream, "case table resource");
        var cases = new ArrayList<Case>();
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                var columns = line.split("\t", -1);
                if (columns.length != 5) throw new IllegalStateException("bad case row: " + line);
                cases.add(new Case(columns[0], columns[1], columns[2], columns[3], expectations(columns[4])));
            }
        }
        return List.copyOf(cases);
    }

    // Parses semicolon-separated key=value expectations.
    private static Map<String, String> expectations(String text) {
        var values = new LinkedHashMap<String, String>();
        for (var token : text.split(";")) {
            var split = token.indexOf('=');
            if (split < 1) throw new IllegalStateException("bad expectation: " + token);
            values.put(token.substring(0, split), token.substring(split + 1));
        }
        return Map.copyOf(values);
    }

    // Resolves the recipe root from Gradle or the module directory.
    private static Path recipeRoot() {
        var configured = System.getProperty("gt6ce.recipeRoot");
        return Path.of(
                configured == null || configured.isBlank() ? "src/main/resources/data/gt6ce/recipes" : configured);
    }

    // One row: issue, source kind, target, and expected values.
    private record Case(String issue, String source, String kind, String target, Map<String, String> expected) {
        // Prefixes diagnostics with the issue and target row.
        private String label(String detail) {
            return "#" + issue + " " + target + " " + detail;
        }
    }
}
