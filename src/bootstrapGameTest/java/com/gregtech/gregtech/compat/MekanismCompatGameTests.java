package com.gregtech.gregtech.compat;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;

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
        var salt = new ResourceLocation("mekanism", "storage_blocks/salt");
        var rod = new ResourceLocation("mekanism", "hdpe_rod");
        if (!loaded) {
            helper.assertTrue(manager.byKey(salt).isEmpty(), "salt block recipe must stay absent without Mekanism");
            helper.assertTrue(manager.byKey(rod).isEmpty(), "HDPE rod recipe must stay absent without Mekanism");
            helper.succeed();
            return;
        }
        var saltItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("mekanism", "salt"));
        helper.assertTrue(saltItem != null && saltItem != Items.AIR, "mekanism:salt is registered");
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
