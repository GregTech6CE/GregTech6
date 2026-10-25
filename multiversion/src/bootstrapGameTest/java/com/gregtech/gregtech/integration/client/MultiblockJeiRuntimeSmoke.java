package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.jei.MultiblockInfoCategory;
import com.gregtech.gregtech.jei.MultiblockInfoData;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Issue #118: ask the live JEI index, then render controller usages and a shared part lookup. */
final class MultiblockJeiRuntimeSmoke {
    private static int stage, frames;
    private static JsonObject receipt;
    private static Throwable failure;
    private static List<MultiblockInfoData.Info> recipes;
    private static final List<ItemStack> samples = new ArrayList<>();
    private static final JsonArray captures = new JsonArray();

    private MultiblockJeiRuntimeSmoke() {}

    static boolean frame(Minecraft minecraft, JsonObject result) {
        if (failure != null) throw new IllegalStateException("Multiblock JEI screenshot failed", failure);
        if (stage == 4) return true;
        if (stage > 0 || RecipePresentationJeiSmoke.runtime == null) return false;
        var runtime = RecipePresentationJeiSmoke.runtime;
        var manager = runtime.getRecipeManager();
        recipes = manager.createRecipeLookup(MultiblockInfoCategory.TYPE).get().toList();
        var declared = MultiblockInfoData.recipes();
        require(recipes.size() > 1 && recipes.size() == declared.size(), "complete assembly catalog");
        require(controllers(recipes).equals(controllers(declared)), "assembly controller catalog");

        var uses = new LinkedHashMap<Item, Set<Item>>();
        for (var info : declared) {
            for (var part : MultiblockInfoData.parts(info))
                uses.computeIfAbsent(part.getItem(), ignored -> new LinkedHashSet<>()).add(info.controller().getItem());
        }
        int checks = 0;
        for (var info : declared) {
            var controller = info.controller();
            var expected = Set.of(controller.getItem());
            check(controller, List.of(RecipeIngredientRole.OUTPUT), expected, "controller recipes");
            check(controller, List.of(RecipeIngredientRole.CATALYST), expected, "controller assembly catalyst");
            check(controller, List.of(RecipeIngredientRole.INPUT), uses.getOrDefault(controller.getItem(), Set.of()), "controller ingredient uses");
            var expectedUses = new LinkedHashSet<>(uses.getOrDefault(controller.getItem(), Set.of()));
            expectedUses.add(controller.getItem());
            check(controller, List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.CATALYST), expectedUses, "controller U lookup");
            checks += 4;
        }
        for (var entry : uses.entrySet()) {
            check(new ItemStack(entry.getKey()), List.of(RecipeIngredientRole.INPUT), entry.getValue(), "structural part uses");
            checks++;
        }
        var unrelated = new ItemStack(net.minecraft.world.item.Items.DIAMOND);
        if (!uses.containsKey(unrelated.getItem()) && !controllers(declared).contains(unrelated.getItem())) {
            check(unrelated, List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.CATALYST), Set.of(), "unrelated item uses");
            checks++;
        }
        result.addProperty("multiblockJeiRuntimeOnly", true);
        result.addProperty("multiblockJeiAssemblyRows", recipes.size());
        result.addProperty("multiblockJeiControllersChecked", declared.size());
        result.addProperty("multiblockJeiPartsChecked", uses.size());
        result.addProperty("multiblockJeiFocusChecks", checks);
        receipt = result;

        // Keep both focused controller examples to one assembly page, as reported in #118.
        for (var info : recipes) {
            if (uses.containsKey(info.controller().getItem())) continue;
            samples.add(info.controller());
            if (samples.size() == 2) break;
        }
        require(samples.size() == 2, "two independent controller focus samples");
        var shared = uses.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1 && entry.getValue().size() < recipes.size())
                .findFirst().orElseThrow(() -> new IllegalStateException("No shared structural part sample"));
        samples.add(new ItemStack(shared.getKey()));
        stage = 1;
        show();
        return false;
    }

    private static List<IFocus<?>> focuses(ItemStack stack, List<RecipeIngredientRole> roles) {
        var factory = RecipePresentationJeiSmoke.runtime.getJeiHelpers().getFocusFactory();
        var result = new ArrayList<IFocus<?>>();
        for (var role : roles) result.add(factory.createFocus(role, VanillaTypes.ITEM_STACK, stack));
        return result;
    }

    private static List<MultiblockInfoData.Info> lookup(ItemStack stack, List<RecipeIngredientRole> roles) {
        return RecipePresentationJeiSmoke.runtime.getRecipeManager().createRecipeLookup(MultiblockInfoCategory.TYPE)
                .limitFocus(focuses(stack, roles)).get().toList();
    }

    private static Set<Item> controllers(List<MultiblockInfoData.Info> rows) {
        var result = new LinkedHashSet<Item>();
        for (var row : rows) result.add(row.controller().getItem());
        return result;
    }

    private static void check(ItemStack stack, List<RecipeIngredientRole> roles, Set<Item> expected, String label) {
        var actual = lookup(stack, roles);
        require(actual.size() == expected.size() && controllers(actual).equals(expected),
                label + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()) + ": " + actual.size() + "/" + expected.size());
    }

    private static void show() {
        var stack = samples.get(stage - 1);
        var roles = stage < 3 ? List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.CATALYST)
                : List.of(RecipeIngredientRole.INPUT);
        var matches = lookup(stack, roles);
        require(stage == 3 ? matches.size() > 1 : matches.size() == 1, "focused sample recipe count");
        var runtime = RecipePresentationJeiSmoke.runtime;
        runtime.getRecipesGui().showRecipes(runtime.getRecipeManager().getRecipeCategory(MultiblockInfoCategory.TYPE), matches, focuses(stack, roles));
        frames = 0;
        var capture = new JsonObject();
        capture.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        capture.addProperty("rows", matches.size());
        capture.addProperty("lookup", stage < 3 ? "INPUT+CATALYST" : "INPUT");
        captures.add(capture);
    }

    static void screen(Minecraft minecraft, net.minecraft.client.gui.screens.Screen screen) {
        if (stage < 1 || stage > 3 || frames < 0 || failure != null || !screen.getClass().getName().contains("RecipesGui")) return;
        if (++frames < 30) return;
        int captured = stage;
        frames = -1;
        String file = "issue118-jei-" + stage + "-" + UUID.randomUUID() + ".png";
        Screenshot.grab(minecraft.gameDirectory, file, minecraft.getMainRenderTarget(), ignored -> {
            try {
                var path = minecraft.gameDirectory.toPath().resolve("screenshots").resolve(file).toAbsolutePath();
                require(Files.isRegularFile(path) && Files.size(path) > 0, "focused JEI screenshot");
                captures.get(captured - 1).getAsJsonObject().addProperty("screenshot", path.toString());
                minecraft.execute(() -> {
                    stage++;
                    if (stage < 4) show();
                    else {
                        receipt.add("multiblockJeiFocusedSamples", captures);
                        minecraft.setScreen(null);
                    }
                });
            } catch (Throwable problem) {
                failure = problem;
                minecraft.execute(() -> minecraft.setScreen(null));
            }
        });
    }

    private static void require(boolean value, String detail) {
        if (!value) throw new IllegalStateException("Multiblock JEI: " + detail);
    }
}
