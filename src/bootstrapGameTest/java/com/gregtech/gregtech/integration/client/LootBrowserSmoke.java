package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.content.loot.LootViewerData;
import com.gregtech.gregtech.emi.LootEmiRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import java.nio.file.Files;
import java.util.UUID;

/** Two actual recipe-screen captures inside the already-required isolated world launch. */
final class LootBrowserSmoke {
    private static final boolean EMI = net.minecraftforge.fml.ModList.get().isLoaded("emi");
    private static int stage, frames;
    private static JsonObject receipt;
    private static volatile Throwable failure;
    static boolean start(JsonObject result) {
        if (failure != null) throw new IllegalStateException("Loot viewer screenshot failed", failure);
        if (stage == (EMI ? 6 : 3)) return true;
        if (stage > 0) return false;
        if (!EMI && !LootJeiSmokePlugin.ready()) return false;
        if (EMI) {
            var manager = dev.emi.emi.api.EmiApi.getRecipeManager();
            var recipes = manager.getRecipes().stream().filter(LootEmiRecipe.class::isInstance).map(LootEmiRecipe.class::cast).toList();
            long loot = recipes.stream().filter(r -> r.getCategory().getId().getPath().equals("loot_tables")).count();
            long mobs = recipes.stream().filter(r -> r.getCategory().getId().getPath().equals("mob_drops")).count();
            if (loot != LootViewerData.lootTables().size() || mobs != LootViewerData.mobDrops().size())
                throw new IllegalStateException("Actual EMI omitted GT loot rows: " + loot + "/" + mobs);
            var row = recipes.stream().filter(r -> r.row().output().getItem().toString().endsWith("cow_hoof")).findFirst().orElseThrow();
            if (!manager.getRecipesByOutput(row.getOutputs().get(0)).contains(row) || row.supportsRecipeTree() || !row.hideCraftable())
                throw new IllegalStateException("Actual EMI loot lookup/craftability is wrong");

            var structures = manager.getRecipes().stream().filter(com.gregtech.gregtech.emi.StructureEmiRecipe.class::isInstance).toList();
            if (structures.size() != com.gregtech.gregtech.jei.MultiblockInfoData.recipes().size())
                throw new IllegalStateException("EMI omitted runtime structures");
            var geology = manager.getRecipes().stream().filter(com.gregtech.gregtech.emi.WorldgenEmiRecipe.class::isInstance).toList();
            var counts = java.util.Map.of("ore_veins", com.gregtech.gregtech.jei.WorldgenInfoData.buildVeins().size(),
                "small_ores_info", com.gregtech.gregtech.jei.WorldgenInfoData.buildSmallOres().size(),
                "stone_layers_info", com.gregtech.gregtech.jei.WorldgenInfoData.buildLayers().size(),
                "bedrock_ores", com.gregtech.gregtech.jei.WorldgenInfoData.buildBedrockOres().size());
            for (var entry : counts.entrySet()) {
                long actual = geology.stream().filter(r -> r.getCategory().getId().getPath().equals(entry.getKey())).count();
                if (actual != entry.getValue()) throw new IllegalStateException("EMI omitted geology " + entry.getKey() + ": " + actual);
                result.addProperty("emi_" + entry.getKey(), actual);
            }
            result.addProperty("emiStructureRows", structures.size());
        } else LootJeiSmokePlugin.check();
        receipt = result;
        receipt.addProperty("lootViewer", EMI ? "emi" : "jei");
        receipt.addProperty("lootViewerRows", LootViewerData.lootTables().size());
        receipt.addProperty("mobViewerRows", LootViewerData.mobDrops().size());
        receipt.addProperty("lootViewerTables", com.gregtech.gregtech.content.loot.GTLootTables.tableNames().size());
        stage = 1; frames = 0; show(1); return false;
    }
    private static void show(int page) {
        if (!EMI) { LootJeiSmokePlugin.show(page == 1); return; }
        var recipes = dev.emi.emi.api.EmiApi.getRecipeManager().getRecipes();
        dev.emi.emi.api.recipe.EmiRecipe recipe;
        if (page <= 2) recipe = recipes.stream().filter(LootEmiRecipe.class::isInstance)
                .map(LootEmiRecipe.class::cast).filter(r -> page == 1 ? r.row().output().getItem().toString().endsWith("cow_hoof") : r.row().id().contains("table/gt/misc/"))
                .findFirst().orElseThrow();
        else recipe = recipes.stream().filter(r -> r.getCategory().getId().getNamespace().equals("gregtech")
                && r.getCategory().getId().getPath().equals(page == 3 ? "multiblock_assembly" : page == 4 ? "ore_veins" : "stone_layers_info"))
                .findFirst().orElseThrow();
        dev.emi.emi.api.EmiApi.displayRecipe(recipe);
    }
    static void frame(net.minecraft.client.gui.screens.Screen screen) {
        if (stage < 1 || stage > (EMI ? 5 : 2) || frames < 0 || failure != null) return;
        if (!screen.getClass().getName().contains(EMI ? "RecipeScreen" : "RecipesGui")) return;
        if (++frames < 12) return;
        int captured = stage; frames = -1;
        var minecraft = Minecraft.getInstance();
        String kind = switch (captured) { case 1 -> "mobs"; case 2 -> "tables"; case 3 -> "structure"; case 4 -> "veins"; default -> "layers"; };
        String file = "loot-" + (EMI ? "emi" : "jei") + "-" + kind + "-" + UUID.randomUUID() + ".png";
        Screenshot.grab(minecraft.gameDirectory, file, minecraft.getMainRenderTarget(), ignored -> {
            try {
                var path = minecraft.gameDirectory.toPath().resolve("screenshots").resolve(file).toAbsolutePath();
                if (!Files.isRegularFile(path) || Files.size(path) == 0) throw new IllegalStateException("Actual loot UI screenshot missing");
                minecraft.execute(() -> {
                    receipt.addProperty(switch (captured) { case 1 -> "mobViewerScreenshot"; case 2 -> "lootViewerScreenshot";
                        case 3 -> "emiStructureScreenshot"; case 4 -> "emiVeinsScreenshot"; default -> "emiLayersScreenshot"; }, path.toString());
                    if (captured < (EMI ? 5 : 2)) { stage = captured + 1; frames = 0; show(stage); }
                    else { stage = EMI ? 6 : 3; minecraft.setScreen(null); }
                });
            } catch (Throwable error) { failure = error; minecraft.execute(() -> minecraft.setScreen(null)); }
        });
    }
}
