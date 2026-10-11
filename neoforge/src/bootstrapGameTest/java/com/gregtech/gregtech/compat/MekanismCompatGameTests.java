package com.gregtech.gregtech.compat;

import java.util.ArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Loaded only in development. Production jars do not contain this source set. */
@GameTestHolder("gregtech_compat")
@PrefixGameTestTemplate(false)
public final class MekanismCompatGameTests {
    private MekanismCompatGameTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 8000)
    public static void saltBlockRecipeFollowsWhetherMekanismIsLoaded(GameTestHelper helper) {
        boolean loaded = ModList.get().isLoaded("mekanism");
        helper.assertTrue(CompatRecipes.errors() == 0, "unresolved compat rows: " + CompatRecipes.problems());
        var manager = helper.getLevel().getServer().getRecipeManager();
        var salt = ResourceLocation.parse("mekanism:storage_blocks/salt");
        var rod = ResourceLocation.parse("mekanism:hdpe_rod");
        if (!loaded) {
            helper.assertTrue(manager.byKey(salt).isEmpty(), "salt block recipe must stay absent without Mekanism");
            helper.assertTrue(manager.byKey(rod).isEmpty(), "HDPE rod recipe must stay absent without Mekanism");
            helper.succeed();
            return;
        }
        var saltItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:salt"));
        helper.assertTrue(saltItem != Items.AIR, "mekanism:salt is registered");
        helper.assertTrue(manager.byKey(rod).isPresent(), "Mekanism HDPE rod recipe should load");
        helper.assertTrue(manager.byKey(salt).isEmpty(), "Mekanism 2x2 salt block recipe is disabled");
        var server = helper.getLevel().getServer();
        server.reloadResources(new ArrayList<>(server.getPackRepository().getSelectedIds())).join();
        helper.assertTrue(CompatRecipes.errors() == 0, "reload left unresolved rows: " + CompatRecipes.problems());
        helper.assertTrue(server.getRecipeManager().byKey(salt).isEmpty(), "salt block recipe returned after reload");
        helper.assertTrue(server.getRecipeManager().byKey(rod).isPresent(), "HDPE rod recipe disappeared after reload");
        helper.succeed();
    }
}
