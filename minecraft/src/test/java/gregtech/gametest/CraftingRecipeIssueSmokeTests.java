package gregtech.gametest;

import gregtech.content.recipe.ToolShapedRecipe;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Data-driven smoke regressions for fixed crafting-table issues; see docs/development/testing.md. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class CraftingRecipeIssueSmokeTests {
    private static final Set<String> FIXED_ISSUES = Set.of(
            "2", "148", "151", "153", "155", "157", "158", "161", "171", "173", "176", "185", "209");

    // Checks the table covers every fixed issue and the GameTest runtime is wired.
    @GameTest(template = "test_empty", batch = "crafting_issue_smoke", timeoutTicks = 600)
    public static void tableCoversEveryFixedIssue(GameTestHelper helper) {
        var rows = loadRows();
        var issues = new HashSet<String>();
        for (var row : rows) {
            issues.add(row.issue());
            helper.assertFalse(row.issue().isEmpty(), "empty issue in " + row);
            helper.assertTrue(Set.of("R", "S", "T", "P").contains(row.level()), "bad level in " + row);
            helper.assertFalse(row.recipe().isEmpty(), "empty recipe in " + row);
        }
        helper.assertTrue(issues.equals(FIXED_ISSUES), "issue coverage " + issues);
        var id = ResourceLocation.tryParse("gt6ce:plate_graphene");
        helper.assertTrue(BuiltInRegistries.ITEM.getOptional(id).isPresent(), "gt6ce:plate_graphene is registered");
        helper.succeed();
    }

    // Runs every runtime row and reports all row failures together.
    @GameTest(template = "test_empty", batch = "crafting_issue_smoke", timeoutTicks = 600)
    public static void runtimeCasesMatchAndAssemble(GameTestHelper helper) {
        runLevel(helper, "R", CraftingRecipeIssueSmokeTests::checkRuntime);
        helper.succeed();
    }

    // Runs every structural row and reports all row failures together.
    @GameTest(template = "test_empty", batch = "crafting_issue_smoke", timeoutTicks = 600)
    public static void structuralCasesMatchShapeAndResult(GameTestHelper helper) {
        runLevel(helper, "S", CraftingRecipeIssueSmokeTests::checkStructural);
        helper.succeed();
    }

    // Runs tag and presence rows and reports all row failures together.
    @GameTest(template = "test_empty", batch = "crafting_issue_smoke", timeoutTicks = 600)
    public static void tagAndPresenceCases(GameTestHelper helper) {
        runLevel(helper, "T", CraftingRecipeIssueSmokeTests::checkTag);
        runLevel(helper, "P", CraftingRecipeIssueSmokeTests::checkPresence);
        helper.succeed();
    }

    // Runs all rows of one level, collecting every failure before aborting the test.
    private static void runLevel(GameTestHelper helper, String level, RowCheck check) {
        var rows = loadRows();
        var failures = new ArrayList<String>();
        var count = 0;
        for (var row : rows) {
            if (!row.level().equals(level)) continue;
            count++;
            try {
                check.run(helper, row);
            } catch (RuntimeException | AssertionError failure) {
                failures.add(label(row) + ": " + failure.getMessage());
            }
        }
        if (count == 0) helper.fail("no " + level + " cases");
        if (!failures.isEmpty()) helper.fail(String.join("\n", failures));
    }

    // Matches and assembles a runtime case from its setup description.
    private static void checkRuntime(GameTestHelper helper, Row row) {
        var found = resolve(helper, row);
        assertType(helper, row, found.recipe());
        if (row.setup().startsWith("reject@")) {
            var spec = row.setup().substring("reject@".length()).split("/");
            var slots = parseSlots(spec[0]);
            var dims = parseDims(spec[1]);
            var input = firstIngredient(found.recipe());
            var items = emptyGrid(dims[0], dims[1]);
            for (var slot : slots) items.set(slot, input.copy());
            helper.assertFalse(
                    matches(helper, found.recipe(), dims[0], dims[1], items),
                    label(row) + " should reject extra occupied slots");
            return;
        }
        int width;
        int height;
        List<ItemStack> items;
        if (row.setup().startsWith("one@")) {
            var spec = row.setup().substring("one@".length()).split("/");
            var slot = Integer.parseInt(spec[0]);
            var dims = parseDims(spec[1]);
            width = dims[0];
            height = dims[1];
            items = emptyGrid(width, height);
            items.set(slot, firstIngredient(found.recipe()));
        } else if (row.setup().equals("fill")) {
            var filled = fillFromRecipe(found.recipe());
            width = filled.width();
            height = filled.height();
            items = filled.items();
        } else {
            throw new AssertionError("unknown setup " + row.setup());
        }
        helper.assertTrue(matches(helper, found.recipe(), width, height, items), label(row) + " should match");
        var result = assemble(helper, found.recipe(), width, height, items);
        helper.assertFalse(result.isEmpty(), label(row) + " assembled an empty result");
        if (row.resultCount() > 0) {
            helper.assertTrue(
                    result.getCount() == row.resultCount(),
                    label(row) + " count " + result.getCount() + " != " + row.resultCount());
        }
        if (!row.resultItem().isEmpty()) {
            helper.assertTrue(result.getItem() == item(row.resultItem()), label(row) + " result " + result.getItem());
        }
    }

    // Checks a structural case's serializer, shape mask, and result.
    private static void checkStructural(GameTestHelper helper, Row row) {
        var found = resolve(helper, row);
        assertType(helper, row, found.recipe());
        if (!row.pattern().isEmpty()) {
            var mask = mask(found.recipe());
            helper.assertTrue(
                    mask.equals(row.pattern()), label(row) + " shape " + mask + " != " + row.pattern());
        }
        var result = resultItem(helper, found.recipe());
        helper.assertFalse(result.isEmpty(), label(row) + " has an empty result");
        if (!row.resultItem().isEmpty() && !row.resultItem().equals("*")) {
            helper.assertTrue(result.getItem() == item(row.resultItem()), label(row) + " result " + result.getItem());
        }
        if (row.resultCount() > 0) {
            helper.assertTrue(
                    result.getCount() == row.resultCount(),
                    label(row) + " count " + result.getCount() + " != " + row.resultCount());
        }
    }

    // Checks that a required item, or any gt6ce item, belongs to a tag.
    private static void checkTag(GameTestHelper helper, Row row) {
        var tagId = ResourceLocation.tryParse(row.recipe());
        helper.assertTrue(tagId != null, label(row) + " has a bad tag id");
        var tag = TagKey.create(Registries.ITEM, tagId);
        if (row.resultItem().equals("*")) {
            var any = BuiltInRegistries.ITEM.stream()
                    .anyMatch(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gt6ce")
                            && new ItemStack(item).is(tag));
            helper.assertTrue(any, label(row) + " has no gt6ce item in the tag");
            return;
        }
        var required = item(row.resultItem());
        helper.assertTrue(new ItemStack(required).is(tag), label(row) + " " + row.resultItem() + " is not in the tag");
    }

    // Checks a presence-only case and its serializer identity.
    private static void checkPresence(GameTestHelper helper, Row row) {
        var found = resolve(helper, row);
        assertType(helper, row, found.recipe());
    }

    // Resolves an exact recipe id or the first matching prefix row.
    private static Found resolve(GameTestHelper helper, Row row) {
        if (row.recipe().startsWith("prefix:")) {
            var prefix = row.recipe().substring("prefix:".length());
            return allRecipes(helper).stream()
                    .filter(found -> found.id().getNamespace().equals("gt6ce")
                            && found.id().getPath().startsWith(prefix))
                    .filter(found -> row.type().isEmpty() || serializerId(found.recipe()).equals(row.type()))
                    .filter(found -> row.pattern().isEmpty() || mask(found.recipe()).equals(row.pattern()))
                    .filter(found -> hasIngredient(found.recipe()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(label(row) + " no recipe for prefix " + prefix));
        }
        var id = ResourceLocation.tryParse(row.recipe());
        if (id == null) throw new AssertionError(label(row) + " has a bad recipe id");
        return new Found(id, byKey(helper, id).orElseThrow(() -> new AssertionError(label(row) + " is missing")));
    }

    // Asserts the registered serializer id when the row requires one.
    private static void assertType(GameTestHelper helper, Row row, Recipe<?> recipe) {
        if (row.type().isEmpty()) return;
        var actual = serializerId(recipe);
        helper.assertTrue(
                actual.equals(row.type()), label(row) + " type " + actual + " != " + row.type());
    }

    // Reads recipes with their ids on the active loader.
    private static List<Found> allRecipes(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
//? if >=1.21.1 {
        /*return manager.getRecipes().stream()
                .map(holder -> new Found(holder.id(), holder.value()))
                .toList();
*///?} else {
        return manager.getRecipes().stream()
                .map(recipe -> new Found(recipe.getId(), recipe))
                .toList();
//?}
    }

    // Looks up one recipe by id on the active loader.
    private static Optional<Recipe<?>> byKey(GameTestHelper helper, ResourceLocation id) {
        var manager = helper.getLevel().getRecipeManager();
//? if >=1.21.1 {
        /*return manager.byKey(id).map(holder -> (Recipe<?>) holder.value());
*///?} else {
        return manager.byKey(id).map(recipe -> (Recipe<?>) recipe);
//?}
    }

    // Returns the serializer key or an empty string when unregistered.
    private static String serializerId(Recipe<?> recipe) {
        var id = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
        return id == null ? "" : id.toString();
    }

    // Returns shaped width/height for vanilla and GT shaped recipes.
    private static int[] dimensions(Recipe<?> recipe) {
        if (recipe instanceof ToolShapedRecipe tool) return new int[] {tool.getWidth(), tool.getHeight()};
        if (recipe instanceof ShapedRecipe shaped) return new int[] {shaped.getWidth(), shaped.getHeight()};
        return null;
    }

    // Renders a shaped recipe as a #/_ occupancy mask.
    private static String mask(Recipe<?> recipe) {
        var dims = dimensions(recipe);
        if (dims == null) return "";
        var ingredients = recipe.getIngredients();
        var rows = new ArrayList<String>();
        for (int y = 0; y < dims[1]; y++) {
            var row = new StringBuilder();
            for (int x = 0; x < dims[0]; x++) {
                var index = y * dims[0] + x;
                var filled = index < ingredients.size() && ingredients.get(index).getItems().length > 0;
                row.append(filled ? '#' : '_');
            }
            rows.add(row.toString());
        }
        return String.join("/", rows);
    }

    // Returns a crafting result item on the active loader.
    private static ItemStack resultItem(GameTestHelper helper, Recipe<?> recipe) {
//? if >=1.21.1 {
        /*return ((CraftingRecipe) recipe).getResultItem(helper.getLevel().registryAccess());
*///?} else {
        return recipe.getResultItem(helper.getLevel().registryAccess());
//?}
    }

    // Builds a grid with one item per recipe ingredient.
    private static Filled fillFromRecipe(Recipe<?> recipe) {
        var dims = dimensions(recipe);
        var ingredients = recipe.getIngredients();
        var width = dims == null ? 3 : dims[0];
        var height = dims == null ? 3 : dims[1];
        var items = emptyGrid(width, height);
        for (int index = 0; index < ingredients.size() && index < items.size(); index++) {
            var choices = ingredients.get(index).getItems();
            if (choices.length > 0) items.set(index, choices[0].copyWithCount(1));
        }
        return new Filled(width, height, items);
    }

    // Returns the first bound item of a recipe or fails clearly.
    private static ItemStack firstIngredient(Recipe<?> recipe) {
        for (var ingredient : recipe.getIngredients()) {
            var choices = ingredient.getItems();
            if (choices.length > 0) return choices[0].copyWithCount(1);
        }
        throw new AssertionError("recipe has no bound ingredient");
    }

    // Returns whether at least one ingredient resolves to an item.
    private static boolean hasIngredient(Recipe<?> recipe) {
        for (var ingredient : recipe.getIngredients()) {
            if (ingredient.getItems().length > 0) return true;
        }
        return false;
    }

    // Matches a recipe against the active loader's crafting grid type.
    private static boolean matches(
            GameTestHelper helper, Recipe<?> recipe, int width, int height, List<ItemStack> items) {
//? if >=1.21.1 {
        /*var input = CraftingInput.ofPositioned(width, height, items).input();
        return ((CraftingRecipe) recipe).matches(input, helper.getLevel());
*///?} else {
        var container = container(width, height);
        for (int index = 0; index < items.size(); index++) container.setItem(index, items.get(index));
        return ((CraftingRecipe) recipe).matches(container, helper.getLevel());
//?}
    }

    // Assembles a recipe against the active loader's crafting grid type.
    private static ItemStack assemble(
            GameTestHelper helper, Recipe<?> recipe, int width, int height, List<ItemStack> items) {
//? if >=1.21.1 {
        /*var input = CraftingInput.ofPositioned(width, height, items).input();
        return ((CraftingRecipe) recipe).assemble(input, helper.getLevel().registryAccess());
*///?} else {
        var container = container(width, height);
        for (int index = 0; index < items.size(); index++) container.setItem(index, items.get(index));
        return ((CraftingRecipe) recipe).assemble(container, helper.getLevel().registryAccess());
//?}
    }

    // Creates a 1.20 crafting container without opening a real menu.
    //? if <1.21.1 {
    private static TransientCraftingContainer container(int width, int height) {
        var menu = new AbstractContainerMenu(MenuType.CRAFTING, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        return new TransientCraftingContainer(menu, width, height);
    }
    //?}

    // Creates an empty mutable grid of the requested size.
    private static List<ItemStack> emptyGrid(int width, int height) {
        var items = new ArrayList<ItemStack>(width * height);
        for (int index = 0; index < width * height; index++) items.add(ItemStack.EMPTY);
        return items;
    }

    // Parses a WxH grid descriptor.
    private static int[] parseDims(String text) {
        var parts = text.split("x");
        if (parts.length != 2) throw new AssertionError("bad grid size " + text);
        return new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    // Parses a comma-separated slot list.
    private static int[] parseSlots(String text) {
        var parts = text.split(",");
        var slots = new int[parts.length];
        for (int index = 0; index < parts.length; index++) slots[index] = Integer.parseInt(parts[index]);
        return slots;
    }

    // Resolves a required item id or fails clearly.
    private static Item item(String id) {
        var parsed = ResourceLocation.tryParse(id);
        if (parsed == null) throw new AssertionError("bad item id " + id);
        return BuiltInRegistries.ITEM
                .getOptional(parsed)
                .orElseThrow(() -> new AssertionError("missing item " + id));
    }

    // Loads the tab-separated case table from test resources.
    private static List<Row> loadRows() {
        var stream = CraftingRecipeIssueSmokeTests.class.getResourceAsStream("/gregtech/gametest/crafting-issue-cases.tsv");
        if (stream == null) throw new IllegalStateException("missing GameTest case table");
        var rows = new ArrayList<Row>();
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                var columns = line.split("\t", -1);
                if (columns.length != 9) throw new IllegalStateException("bad case row: " + line);
                rows.add(new Row(
                        columns[0],
                        columns[1],
                        columns[2],
                        value(columns[3]),
                        value(columns[4]),
                        value(columns[5]),
                        number(columns[6]),
                        value(columns[7]),
                        value(columns[8])));
            }
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read GameTest case table", failure);
        }
        return List.copyOf(rows);
    }

    // Treats a dash as an empty cell.
    private static String value(String text) {
        return text == null || text.equals("-") ? "" : text;
    }

    // Parses an optional count cell.
    private static int number(String text) {
        var value = value(text);
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }

    // Prefixes a failure message with the issue and recipe row.
    private static String label(Row row) {
        return "#" + row.issue() + " " + row.recipe();
    }

    // One case table row.
    private record Row(
            String issue,
            String recipe,
            String level,
            String type,
            String pattern,
            String resultItem,
            int resultCount,
            String setup,
            String notes) {}

    // One resolved recipe and its id.
    private record Found(ResourceLocation id, Recipe<?> recipe) {}

    // One filled crafting grid.
    private record Filled(int width, int height, List<ItemStack> items) {}

    // A level-specific row assertion.
    @FunctionalInterface
    private interface RowCheck {
        void run(GameTestHelper helper, Row row);
    }
}
