package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.recipe.GTFlintAndTinderRecipe;
import com.gregtech.gregtech.recipe.GTToolAssemblyRecipe;
import com.gregtech.gregtech.recipe.GTToolCraftingRecipe;
import com.gregtech.gregtech.recipe.GTToolHeadRecipe;
import com.gregtech.gregtech.recipe.GTToolRecipes;
import com.gregtech.gregtech.recipe.ToolAssemblyCatalog;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Guards the manual tool recipes.
 *
 * <p>Regression these tests exist for: the port used to describe <em>every</em> tool with one
 * shapeless, ingredient-less data recipe, so a wrench or a knife could be assembled in any
 * arrangement and none of the tools showed up in the crafting-table recipe list. GT6 makes the
 * one-piece tools and the tool heads <em>shaped</em> ({@code Loader_Tools:255-380}) and only the head
 * + handle tools shapeless ({@code AdvancedCraftingTool extends ShapelessOreRecipe}); these tests pin
 * that split, the patterns, and that the catalog's input sets really craft.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class ToolAssemblyTests {

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void everyToolHasGt6sRecipeFamily(GameTestHelper helper) {
        List<ToolAssemblyCatalog.ToolAssemblyInfo> catalog = ToolAssemblyCatalog.build();
        int headTools = 0;
        int shapedTools = 0;
        List<String> withoutRecipe = new ArrayList<>();
        for (GTToolType type : GTToolType.values()) {
            boolean shaped = !GTToolRecipes.shaped(type).isEmpty();
            boolean assembly = GTToolRecipes.isHeadAssembly(type) || type == GTToolType.MAGNIFYING_GLASS;
            if (!shaped && !assembly) withoutRecipe.add(type.id());
        }
        for (ToolAssemblyCatalog.ToolAssemblyInfo info : catalog) {
            if (info.headAssembly()) headTools++;
            else shapedTools++;
            if (info.output().isEmpty()) {
                helper.fail("catalog entry has no output: " + info.type().id());
                return;
            }
        }
        var json = new TreeMap<String, Object>();
        json.put("tools", catalog.size());
        json.put("headAndHandle", headTools);
        json.put("shapedOrForms", shapedTools);
        json.put("toolTypes", GTToolType.values().length);
        json.put("shapedPatterns", GTToolRecipes.allShaped().values().stream().mapToInt(List::size).sum());
        json.put("headPatterns", GTToolRecipes.allHeads().values().stream().mapToInt(List::size).sum());
        json.put("toolsWithoutManualRecipe", withoutRecipe);
        // The full pattern table, so tools/check_tool_recipes.py can compare it with GT6's rows.
        var shapedDump = new TreeMap<String, Object>();
        GTToolRecipes.allShaped().forEach((tool, patterns) -> shapedDump.put(tool.id(), dump(patterns)));
        var headDump = new TreeMap<String, Object>();
        GTToolRecipes.allHeads().forEach((tool, patterns) -> headDump.put(tool.id(), dump(patterns)));
        json.put("shaped", shapedDump);
        json.put("heads", headDump);
        var assemblies = new java.util.TreeSet<String>();
        for (GTToolType type : GTToolType.values()) {
            if (GTToolRecipes.isHeadAssembly(type) || type == GTToolType.MAGNIFYING_GLASS) {
                assemblies.add(type.id());
            }
        }
        json.put("assemblies", assemblies);
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/tool-assembly-coverage.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            helper.fail("cannot write docs/tool-assembly-coverage.json: " + e);
            return;
        }
        helper.assertTrue(withoutRecipe.isEmpty(),
                "every port tool has a GT6 manual recipe family, missing: " + withoutRecipe);
        helper.assertTrue(headTools >= 15, "head + handle tools: " + headTools);
        helper.assertTrue(shapedTools >= 10, "tools with a shaped GT6 row: " + shapedTools);
        helper.succeed();
    }

    /** GT6's shaped rows are shaped recipes in the crafting category, so the recipe list shows them. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void shapedToolRowsAreCraftingRecipes(GameTestHelper helper) {
        int checked = 0;
        for (var entry : GTToolRecipes.allShaped().entrySet()) {
            GTToolType type = entry.getKey();
            for (int i = 0; i < entry.getValue().size(); i++) {
                var pattern = entry.getValue().get(i);
                var recipe = type == GTToolType.FLINT_AND_TINDER
                        ? new GTFlintAndTinderRecipe(id("tools/" + type.id()))
                        : new GTToolCraftingRecipe(id("tools/" + type.id() + (i == 0 ? "" : "_" + i)), type, i);
                helper.assertTrue(recipe instanceof ShapedRecipe,
                        type.id() + " is a shaped recipe");
                helper.assertTrue(recipe.getType() == RecipeType.CRAFTING,
                        type.id() + " is in the crafting recipe list");
                helper.assertTrue(recipe.getWidth() == pattern.width() && recipe.getHeight() == pattern.height(),
                        type.id() + " keeps GT6's pattern size");
                boolean anyIngredient = false;
                for (var ingredient : recipe.getIngredients()) anyIngredient |= !ingredient.isEmpty();
                helper.assertTrue(anyIngredient, type.id() + " has ingredients to display");
                helper.assertTrue(!recipe.getResultItem(helper.getLevel().registryAccess()).isEmpty(),
                        type.id() + " has a display result");
                checked++;
            }
        }
        helper.assertTrue(checked >= 20, "shaped tool rows checked: " + checked);
        // The head recipes are shaped too, and yield the head of the matched material.
        int heads = 0;
        for (var entry : GTToolRecipes.allHeads().entrySet()) {
            for (int i = 0; i < entry.getValue().size(); i++) {
                GTToolHeadRecipe recipe = new GTToolHeadRecipe(
                        id("tool_heads/" + entry.getKey().id() + (i == 0 ? "" : "_gem")), entry.getKey(), i);
                helper.assertTrue(recipe instanceof ShapedRecipe && recipe.getType() == RecipeType.CRAFTING,
                        entry.getKey().id() + " head row is a shaped crafting recipe");
                heads++;
            }
        }
        helper.assertTrue(heads >= 18, "tool head rows checked: " + heads);
        helper.succeed();
    }

    /** Every catalog entry must be accepted by the recipe instance that implements it. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void catalogEntriesAreCraftable(GameTestHelper helper) {
        int checked = 0;
        for (ToolAssemblyCatalog.ToolAssemblyInfo info : ToolAssemblyCatalog.build()) {
            // First item of every ingredient: a material that can make this tool with all its forms.
            List<ItemStack> stacks = new ArrayList<>();
            for (List<ItemStack> forms : info.inputs()) {
                if (forms.isEmpty()) {
                    helper.fail("empty ingredient list for " + info.type().id());
                    return;
                }
                stacks.add(forms.get(0).copy());
            }
            TransientCraftingContainer container = info.headAssembly()
                    ? container(stacks)
                    : patternContainer(info, stacks);
            var recipe = info.headAssembly()
                    ? new GTToolAssemblyRecipe(id("tools/" + info.type().id()), info.type())
                    : info.type() == GTToolType.FLINT_AND_TINDER
                    ? new GTFlintAndTinderRecipe(id("tools/" + info.type().id()))
                    : new GTToolCraftingRecipe(id("tools/" + info.type().id()), info.type(), 0);
            if (!recipe.matches(container, helper.getLevel())) {
                helper.fail("the catalog's input set is accepted by the recipe: " + info.type().id()
                        + " pattern=" + java.util.Arrays.toString(
                                info.pattern() == null ? new String[0] : info.pattern().rows())
                        + " grid=" + describe(container));
                return;
            }
            ItemStack result = recipe.assemble(container, helper.getLevel().registryAccess());
            helper.assertTrue(!result.isEmpty() && result.getItem() == info.output().getItem(),
                    "assembling the catalog row yields the tool: " + info.type().id());
            checked++;
        }
        helper.assertTrue(checked >= 30, "catalog rows verified against the recipe: " + checked);
        helper.succeed();
    }

    /** The head + handle rows stay shapeless, exactly like GT6's {@code AdvancedCraftingTool}. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void headAndHandleRowsStayShapeless(GameTestHelper helper) {
        GTToolAssemblyRecipe pickaxe = new GTToolAssemblyRecipe(id("tools/pickaxe"), GTToolType.PICKAXE);
        helper.assertTrue(pickaxe instanceof ShapelessRecipe, "pickaxe is a shapeless recipe");
        helper.assertTrue(pickaxe.getType() == RecipeType.CRAFTING, "pickaxe is in the crafting list");
        helper.assertTrue(pickaxe.getIngredients().size() == 2, "pickaxe displays head + handle");
        // and the head order does not matter, which is what shapeless means
        List<ItemStack> head = ToolAssemblyCatalog.stacksOf(GTToolType.PICKAXE.headPrefix(), GTToolType.PICKAXE);
        List<ItemStack> sticks = ToolAssemblyCatalog.sticks();
        helper.assertTrue(!head.isEmpty() && !sticks.isEmpty(), "pickaxe head and handle exist");
        helper.assertTrue(pickaxe.matches(container(List.of(head.get(0), sticks.get(0))), helper.getLevel()),
                "head then handle matches");
        ItemStack tool = pickaxe.assemble(container(List.of(sticks.get(0), head.get(0))), helper.getLevel().registryAccess());
        helper.assertTrue(!tool.isEmpty(), "handle then head matches too");
        // three items do not
        helper.assertTrue(!pickaxe.matches(container(List.of(head.get(0), sticks.get(0), sticks.get(0))),
                helper.getLevel()), "three items do not match");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("gregtech", path);
    }

    /** Grid contents as item ids, for failure messages. */
    private static String describe(TransientCraftingContainer grid) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack stack = grid.getItem(i);
            if (i > 0) out.append(", ");
            out.append(stack.isEmpty() ? "-" : net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getKey(stack.getItem()).getPath());
        }
        return out.append(']').toString();
    }

    /** A pattern table as JSON: rows, form letters, tool letters and the flags. */
    private static List<Object> dump(List<GTToolRecipes.Pattern> patterns) {
        List<Object> out = new ArrayList<>();
        for (GTToolRecipes.Pattern pattern : patterns) {
            var entry = new TreeMap<String, Object>();
            entry.put("rows", List.of(pattern.rows()));
            var forms = new TreeMap<String, String>();
            pattern.forms().forEach((letter, prefix) -> forms.put(String.valueOf(letter), prefix.getName()));
            entry.put("forms", forms);
            var tools = new TreeMap<String, String>();
            pattern.tools().forEach((letter, tool) -> tools.put(String.valueOf(letter), tool.id()));
            entry.put("tools", tools);
            entry.put("mirror", pattern.mirror());
            entry.put("normalHandle", pattern.normalHandle());
            var gate = new TreeMap<String, Object>();
            gate.put("toolMaterial", pattern.gate().toolMaterial());
            gate.put("onlyMaterial", pattern.gate().onlyMaterial());
            gate.put("onlyStone", pattern.gate().onlyStone());
            var items = new TreeMap<String, String>();
            pattern.gate().items().forEach((letter, item) ->
                    items.put(String.valueOf(letter),
                            net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item).toString()));
            gate.put("items", items);
            entry.put("gate", gate);
            out.add(entry);
        }
        return out;
    }

    /** GT6's hand-written early tools: flint, bone, obsidian, petrified wood and stone. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void earlyToolsComeFromRocksAndFlint(GameTestHelper helper) {
        ItemStack stick = GTItems.getStack(MaterialPrefix.stick, GTToolHelper.firstHandleMaterial(), 1);
        helper.assertTrue(!stick.isEmpty(), "a handle stick exists");
        // A rock that carries GT6's STONE property (ANY.Stone, Loader_Tools:272).
        GTMaterial granite = null;
        for (StoneType type : StoneType.values()) {
            if (type.material().has(com.gregtech.gregtech.api.material.MaterialProperty.STONE)) {
                granite = type.material();
                break;
            }
        }
        helper.assertTrue(granite != null, "a stone material with the STONE property exists");
        ItemStack rock = GTItems.getStack(MaterialPrefix.rockGt, granite, 1);
        ItemStack obsidian = GTItems.getStack(MaterialPrefix.rockGt, GTMaterialRegistry.get("Obsidian"), 1);
        helper.assertTrue(!rock.isEmpty() && !obsidian.isEmpty(), "rock items exist");

        // Pickaxe: "XXX"/" H " from any stone, mirroring off, material = the rock.
        int stonePick = indexOf(GTToolType.PICKAXE, "stone");
        helper.assertTrue(stonePick >= 0, "a stone pickaxe row exists");
        Recipe pickaxe = craftRow(helper, GTToolType.PICKAXE, stonePick, new ItemStack[]{
                rock, rock, rock, ItemStack.EMPTY, stick, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY});
        helper.assertTrue(pickaxe != null, "three rocks and a stick craft a stone pickaxe");
        if (pickaxe != null) {
            ItemStack result = result(helper, pickaxe, new ItemStack[]{
                    rock, rock, rock, ItemStack.EMPTY, stick, ItemStack.EMPTY,
                    ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY});
            helper.assertTrue(!result.isEmpty(), "the stone pickaxe assembles");
            helper.assertTrue(result.getItem() instanceof com.gregtech.gregtech.item.GTToolItem,
                    "the result is a GT tool");
            if (result.getItem() instanceof com.gregtech.gregtech.item.GTToolItem) {
                helper.assertTrue(GTToolHelper.getHead(result) == granite,
                        "the pickaxe is made of the rock that was used");
            }
        }

        // Flint knife: "HX" with a piece of flint; the tool's material is Flint.
        ItemStack flint = new ItemStack(net.minecraft.world.item.Items.FLINT);
        ItemStack knifeGrid[] = {stick, flint, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        int flintKnife = indexOf(GTToolType.KNIFE, "flint");
        helper.assertTrue(flintKnife >= 0, "a flint knife row exists");
        Recipe knife = craftRow(helper, GTToolType.KNIFE, flintKnife, knifeGrid);
        helper.assertTrue(knife != null, "a stick plus flint crafts a flint knife");
        if (knife != null) {
            helper.assertTrue(GTToolHelper.getHead(result(helper, knife, knifeGrid)) == GTMaterialRegistry.get("Flint"),
                    "the flint knife is made of flint");
        }

        // Obsidian rows only accept obsidian, the stone rows any rock. Axe pattern "XX"/"XH":
        // rocks at (0,0), (1,0), (0,1) and the handle at (1,1).
        ItemStack[] obsidianAxe = {obsidian, obsidian, ItemStack.EMPTY, obsidian, stick, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        int obsidianRow = indexOf(GTToolType.AXE, "obsidian");
        helper.assertTrue(obsidianRow >= 0, "an obsidian axe row exists");
        helper.assertTrue(craftRow(helper, GTToolType.AXE, obsidianRow, obsidianAxe) != null,
                "obsidian plus a stick crafts an obsidian axe");
        ItemStack[] graniteAxe = {rock, rock, ItemStack.EMPTY, rock, stick, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        helper.assertTrue(!row(helper, GTToolType.AXE, obsidianRow)
                        .matches(container(java.util.Arrays.asList(graniteAxe)), helper.getLevel()),
                "the obsidian row refuses granite");
        int stoneRow = indexOf(GTToolType.AXE, "stone");
        helper.assertTrue(stoneRow >= 0, "a stone axe row exists");
        helper.assertTrue(row(helper, GTToolType.AXE, stoneRow)
                        .matches(container(java.util.Arrays.asList(graniteAxe)), helper.getLevel()),
                "the stone row accepts granite");
        helper.succeed();
    }

    /** Index of a tool's early row: {@code stone}, {@code obsidian}, {@code flint} or {@code bone}. */
    private static int indexOf(GTToolType type, String family) {
        var patterns = GTToolRecipes.shaped(type);
        for (int i = 0; i < patterns.size(); i++) {
            var gate = patterns.get(i).gate();
            boolean match = switch (family) {
                case "stone" -> gate.onlyStone();
                case "flint" -> "Flint".equals(gate.toolMaterial());
                case "bone" -> "Bone".equals(gate.toolMaterial());
                default -> family.equalsIgnoreCase(gate.onlyMaterial());
            };
            if (match) return i;
        }
        return -1;
    }

    private static com.gregtech.gregtech.recipe.GTToolCraftingRecipe row(GameTestHelper helper, GTToolType type, int index) {
        return new com.gregtech.gregtech.recipe.GTToolCraftingRecipe(id("tools/" + type.id() + "_test"), type, index);
    }

    /** The recipe of one GT6 row, or null when the grid does not match it. */
    private static Recipe craftRow(GameTestHelper helper, GTToolType type, int index, ItemStack[] grid) {
        var recipe = row(helper, type, index);
        return recipe.matches(container(java.util.Arrays.asList(grid)), helper.getLevel()) ? recipe : null;
    }

    private static ItemStack result(GameTestHelper helper, Recipe recipe, ItemStack[] grid) {
        return ((net.minecraft.world.item.crafting.CraftingRecipe) recipe)
                .assemble(container(java.util.Arrays.asList(grid)), helper.getLevel().registryAccess());
    }

    /** A 3x3 crafting container holding the given stacks in reading order, spaces included. */
    private static TransientCraftingContainer container(List<ItemStack> stacks) {
        var menu = new AbstractContainerMenu(null, 0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) {
                return ItemStack.EMPTY;
            }

            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 3, 3);
        int slot = 0;
        for (ItemStack stack : stacks) {
            if (slot >= 9) break;
            grid.setItem(slot++, stack);
        }
        return grid;
    }

    /** Places the catalog's per-cell options into the grid at their pattern positions. */
    private static TransientCraftingContainer patternContainer(ToolAssemblyCatalog.ToolAssemblyInfo info,
                                                               List<ItemStack> stacks) {
        var pattern = info.pattern();
        TransientCraftingContainer grid = container(List.of());
        if (pattern == null) return grid;
        int index = 0;
        for (int y = 0; y < pattern.height(); y++) {
            for (int x = 0; x < pattern.width(); x++) {
                if (pattern.at(x, y) == ' ') continue;
                grid.setItem(x + y * 3, stacks.get(index++));
            }
        }
        return grid;
    }
}
