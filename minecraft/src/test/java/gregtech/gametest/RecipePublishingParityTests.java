package gregtech.gametest;

import com.google.gson.JsonParser;
import gregtech.compat.common.RecipeMachines;
import gregtech.content.blockentity.tool.ProcessingToolBlockEntity;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.recipe.ToolShapedRecipe;
import gregtech.content.recipe.ToolShapelessRecipe;
import gregtech.content.registry.GTFluids;
import gregtech.engine.recipe.AutocraftableCraftingRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Verifies published recipe tables and captures canonical RecipeManager snapshots on request. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class RecipePublishingParityTests {
    private static final String BATCH = "recipe_publishing_parity";
    private static final Path CAPTURE_FLAG = Path.of("capture-recipes.flag");
    private static final Path SNAPSHOT_OUT = Path.of("build", "recipe-snapshot.tsv");

    private RecipePublishingParityTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void openVesselsShowOnlyTheirProcessingCategory(GameTestHelper helper) {
        var expected = Map.of(
                "bathing_pot", MachineRecipeMaps.Bath,
                "bathing_pot_table", MachineRecipeMaps.Bath,
                "bathing_pot_wood", MachineRecipeMaps.Bath,
                "bathing_pot_table_wood", MachineRecipeMaps.Bath,
                "mixing_bowl", MachineRecipeMaps.Mixer,
                "mixing_bowl_table", MachineRecipeMaps.Mixer,
                "juicer", MachineRecipeMaps.Juicer);
        var stations = RecipeMachines.collect();
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        expected.forEach((id, map) -> {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            helper.assertTrue(level.getBlockEntity(pos) instanceof ProcessingToolBlockEntity vessel && vessel.recipes() == map,
                    id + " should process " + map.internalName);
            var categories = stations.entrySet().stream()
                    .filter(entry -> entry.getValue().stream().anyMatch(stack -> stack.is(block.asItem())))
                    .map(entry -> entry.getKey().internalName)
                    .toList();
            helper.assertTrue(categories.equals(List.of(map.internalName)),
                    id + " should only appear in " + map.internalName + ", got " + categories);
        });
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void captureOrVerifyRecipeTable(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        var manager = level.getRecipeManager();
        var access = level.registryAccess();

        if (Files.exists(CAPTURE_FLAG)) {
            var rows = snapshot(server, manager, access);
            try {
                Path parent = SNAPSHOT_OUT.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.writeString(SNAPSHOT_OUT, String.join("\n", rows) + "\n", StandardCharsets.UTF_8);
            } catch (IOException error) {
                helper.fail("failed writing recipe snapshot: " + error.getMessage());
                return;
            }
        }

        // Core sanity checks across RecipeManager and ResourceManager.
        helper.assertTrue(!manager.getRecipes().isEmpty(), "RecipeManager must not be empty");
        assertPrefixCount(helper, manager, "wire_working/", 0);
        assertPrefixCount(helper, manager, "polymer_forming/", 0);
        assertPrefixCount(helper, manager, "books/", 290);
        assertPrefixCount(helper, manager, "extruder_shapes/", 78);
        assertPrefixCount(helper, manager, "decorative_panels/", 75);
        assertPrefixCount(helper, manager, "bookshelves/", 59);
        assertPrefixCount(helper, manager, "scaffolds/", 44);
        assertPrefixCount(helper, manager, "chemical_batteries/", 25);
        assertPrefixCount(helper, manager, "engines/", 46);
        assertPrefixCount(helper, manager, "energy_nodes/", 28);
        assertPrefixCount(helper, manager, "long_distance/", 27);
        assertPrefixCount(helper, manager, "components/", 84);
        assertPrefixCount(helper, manager, "smeltery_survival/", 131);
        // 422 generated rules minus 40 empty-form imprints skipped by OvenRecipeBridge plus 31 runtime blast_* rows.
        assertPrefixCount(helper, manager, "clay_molds/", 413);
        helper.assertTrue(
                manager.byKey(ResourceLocation.fromNamespaceAndPath("gt6ce", "clay_molds/form_clay_mold")).isPresent(),
                "missing gt6ce:clay_molds/form_clay_mold");
        helper.assertTrue(
                manager.byKey(ResourceLocation.fromNamespaceAndPath("gt6ce", "smeltery_survival/ceramic_crucible_firing"))
                        .isPresent(),
                "missing gt6ce:smeltery_survival/ceramic_crucible_firing");
        assertWireTagsAndNoBarriers(helper, manager, "copper");
        assertWireTagsAndNoBarriers(helper, manager, "efrine");
        assertFoodMoldAndSlicerUses(helper);
        helper.succeed();
    }

    private static void assertWireTagsAndNoBarriers(
            GameTestHelper helper, RecipeManager manager, String material) {
        var wire = new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "wire_01_" + material)));
        helper.assertTrue(!wire.isEmpty(), "missing gt6ce:wire_01_" + material);
        helper.assertTrue(
                wire.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "wires/" + material))),
                "wire_01_" + material + " must carry forge:wires/" + material);
//? if >=1.21.1 {
        /*helper.assertTrue(
                wire.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "wires/" + material))),
                "wire_01_" + material + " must carry c:wires/" + material);
*///?}
        var id = ResourceLocation.fromNamespaceAndPath("gt6ce", "wire_sizes/" + material + "_1_to_2");
        var entry = manager.byKey(id);
        helper.assertTrue(entry.isPresent(), "missing " + id);
//? if >=1.21.1 {
        /*var ingredients = entry.get().value().getIngredients();
*///?} else {
        var ingredients = entry.get().getIngredients();
//?}
        for (var ingredient : ingredients) {
            for (var stack : ingredient.getItems()) {
//? if >=1.21.1 {
                /*helper.assertTrue(
                        !stack.is(Items.BARRIER),
                        id + " must not contain barrier empty-tag placeholder, found " + stack);
*///?} else {
                if (stack.is(Items.BARRIER)) {
                    helper.assertTrue(
                            !stack.getHoverName().getString().contains("forge:wires/" + material),
                            id + " must not contain empty forge:wires/" + material + " barrier, found " + stack);
                }
//?}
            }
        }
    }

    private static void assertFoodMoldAndSlicerUses(GameTestHelper helper) {
        var dough = new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "dough")), 3);
        var baguetteMold = new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "foodmold_shape_baguette")));
        var pressRow = MachineRecipeMaps.Press.findRecipe(List.of(dough, baguetteMold), List.of(), true, 2, 1);
        helper.assertTrue(
                pressRow != null
                        && BuiltInRegistries.ITEM
                                .getKey(pressRow.itemOutputs[0].getItem())
                                .equals(ResourceLocation.fromNamespaceAndPath("gt6ce", "dough_baguette"))
                        && pressRow.isCatalystInput(1),
                "Press must form dough_baguette while keeping foodmold_shape_baguette");

        var gridBlade = new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "slicer_shape_grid")));
        var slicePotato = MachineRecipeMaps.Slicer.findRecipe(
                List.of(new ItemStack(Items.POTATO), gridBlade), List.of(), true, 2, 1);
        helper.assertTrue(
                slicePotato != null
                        && BuiltInRegistries.ITEM
                                .getKey(slicePotato.itemOutputs[0].getItem())
                                .equals(ResourceLocation.fromNamespaceAndPath("gt6ce", "potato_strips"))
                        && slicePotato.isCatalystInput(1),
                "Slicer must slice potato into potato_strips while keeping slicer_shape_grid");

        var oil = GTFluids.stack("Oil_Frying", 2);
        var fryRow = MachineRecipeMaps.Bath.findRecipe(
                List.of(new ItemStack(BuiltInRegistries.ITEM.get(
                        ResourceLocation.fromNamespaceAndPath("gt6ce", "potato_strips")))),
                List.of(oil),
                true,
                1,
                1);
        helper.assertTrue(
                fryRow != null
                        && BuiltInRegistries.ITEM
                                .getKey(fryRow.itemOutputs[0].getItem())
                                .equals(ResourceLocation.fromNamespaceAndPath("gt6ce", "fries")),
                "Bath must fry potato_strips into fries");
    }

    private static void assertPrefixCount(
            GameTestHelper helper, RecipeManager manager, String pathPrefix, int expectedCount) {
        long actual = 0;
//? if >=1.21.1 {
        /*for (var holder : manager.getRecipes()) {
            var id = holder.id();
            if (id.getNamespace().equals("gt6ce") && id.getPath().startsWith(pathPrefix)) actual++;
        }
*///?} else {
        for (var recipe : manager.getRecipes()) {
            var id = recipe.getId();
            if (id.getNamespace().equals("gt6ce") && id.getPath().startsWith(pathPrefix)) actual++;
        }
//?}
        helper.assertTrue(
                actual == expectedCount,
                "expected " + expectedCount + " recipes under gt6ce:" + pathPrefix + ", got " + actual);
    }

    private static List<String> snapshot(MinecraftServer server, RecipeManager manager, RegistryAccess access) {
        record Entry(ResourceLocation id, Recipe<?> recipe) {}
        List<Entry> entries = new ArrayList<>();
//? if >=1.21.1 {
        /*for (var holder : manager.getRecipes()) entries.add(new Entry(holder.id(), holder.value()));
*///?} else {
        for (var recipe : manager.getRecipes()) entries.add(new Entry(recipe.getId(), recipe));
//?}
        entries.sort(Comparator.comparing(e -> e.id().toString()));

        List<String> lines = new ArrayList<>(entries.size() + 1);
        lines.add("id\tserializer\ttype\tgroup\tcategory\tdims\tform\tautocraftable\tresult\tingredients");
        for (var entry : entries) {
            lines.add(formatRow(server, access, entry.id(), entry.recipe()));
        }
        return lines;
    }

    private static String formatRow(
            MinecraftServer server, RegistryAccess access, ResourceLocation id, Recipe<?> recipe) {
        String serializer = String.valueOf(BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()));
        String type = String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()));
        String group = sanitize(recipe.getGroup());
        String category = recipe instanceof CraftingRecipe cr ? cr.category().getSerializedName() : "";

        String dims = "";
        if (recipe instanceof ToolShapedRecipe ts) {
            dims = ts.getWidth() + "x" + ts.getHeight() + "|m=" + ts.allowMirror() + "|c=" + sanitize(ts.constructionColor());
        } else if (recipe instanceof ShapedRecipe s) {
            dims = s.getWidth() + "x" + s.getHeight();
        }

        String form = "";
        if (recipe instanceof ToolShapelessRecipe tls) {
            form = tls.formSelector().variants() + "/" + tls.formSelector().offset() + "/" + tls.formPlacement();
        }

        boolean autoMem = !(recipe instanceof AutocraftableCraftingRecipe gt) || gt.isAutocraftableByGT();
        String autoJson = readJsonAutocraftable(server, id);
        String autocraftable = autoMem + "|json=" + autoJson;

        ItemStack result = recipe.getResultItem(access);
        String resultStr = formatStack(result);

        String ingredients = recipe.getIngredients().stream()
                .map(RecipePublishingParityTests::formatIngredient)
                .collect(Collectors.joining(";"));

        return String.join(
                "\t",
                id.toString(),
                serializer,
                type,
                group,
                category,
                dims,
                form,
                autocraftable,
                resultStr,
                ingredients);
    }

    private static String formatStack(ItemStack stack) {
        if (stack.isEmpty()) return "EMPTY";
        String id = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        String extra = "";
//? if >=1.21.1 {
        /*var components = stack.getComponentsPatch();
        if (!components.isEmpty()) extra = "|" + sanitize(components.toString());
*///?} else {
        var tag = stack.getTag();
        if (tag != null && !tag.isEmpty()) extra = "|" + sanitize(tag.toString());
//?}
        return id + "*" + stack.getCount() + extra;
    }

    private static String formatIngredient(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) return "[]";
        TreeSet<String> choices = new TreeSet<>();
        for (ItemStack item : items) {
            choices.add(formatStack(item));
        }
        return String.join(",", choices);
    }

    private static String readJsonAutocraftable(MinecraftServer server, ResourceLocation id) {
        if (server == null) return "no_server";
//? if >=1.21.1 {
        /*var location = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "recipe/" + id.getPath() + ".json");
*///?} else {
        var location = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "recipes/" + id.getPath() + ".json");
//?}
        var resource = server.getResourceManager().getResource(location);
        if (resource.isEmpty()) return "missing";
        try (var reader = resource.get().openAsReader()) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            return json.has("gregtech_autocraftable")
                    ? String.valueOf(json.get("gregtech_autocraftable").getAsBoolean())
                    : "default";
        } catch (IOException | RuntimeException error) {
            return "err";
        }
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
