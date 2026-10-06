package com.gregtech.gregtech.compat;

import com.gregtech.gregtech.content.recipe.DyeProcessingRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Loaded only in development. Production jars do not contain this source set. */
@GameTestHolder("gregtech_compat")
@PrefixGameTestTemplate(false)
public final class ProjectRedCompatGameTests {
    private ProjectRedCompatGameTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 8000)
    public static void rowsFollowWhetherProjectRedIsLoaded(GameTestHelper helper) {
        boolean loaded = ModList.get().isLoaded("projectred_core");
        helper.assertTrue(CompatRecipes.errors() == 0, "unresolved compat rows: " + CompatRecipes.problems());
        var manager = helper.getLevel().getServer().getRecipeManager();
        var replaced = ResourceLocation.parse("projectred_core:red_iron_comp");
        var added = ResourceLocation.parse("gregtech:compat/projectred/red_iron_comp");
        var silicon = ResourceLocation.parse("gregtech:compat/projectred/silicon");
        if (!loaded) {
            helper.assertTrue(manager.byKey(replaced).isEmpty(), "red iron recipe must stay absent without Project Red");
            helper.assertTrue(manager.byKey(added).isEmpty(), "compat red iron recipe must stay absent without Project Red");
            helper.succeed();
            return;
        }
        var sawn = MachineRecipeMaps.Cutter.findRecipe(List.of(item("projectred_core:boule")),
                List.of(DyeProcessingRecipes.fluid("Water", 4000)), false,
                MachineRecipeMaps.Cutter.mInputItemsCount, MachineRecipeMaps.Cutter.mOutputItemsCount);
        helper.assertTrue(sawn != null && sawn.mOutputs[0].is(item("projectred_core:silicon").getItem())
                        && sawn.mOutputs[0].getCount() == 16,
                "a silicon boule saws into sixteen wafers");
        helper.assertTrue(manager.byKey(replaced).isEmpty(), "Project Red red iron recipe is disabled");
        helper.assertTrue(manager.byKey(added).isPresent(), "GT red iron recipe is present");
        helper.assertTrue(manager.byKey(silicon).isPresent(), "GT silicon wafer recipe is present");
        var server = helper.getLevel().getServer();
        server.reloadResources(new ArrayList<>(server.getPackRepository().getSelectedIds())).join();
        helper.assertTrue(CompatRecipes.errors() == 0, "reload left unresolved rows: " + CompatRecipes.problems());
        helper.assertTrue(server.getRecipeManager().byKey(replaced).isEmpty(), "Project Red red iron recipe returned after reload");
        helper.assertTrue(server.getRecipeManager().byKey(added).isPresent(), "GT red iron recipe disappeared after reload");
        helper.succeed();
    }

    private static ItemStack item(String id) {
        var value = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (value == Items.AIR) throw new IllegalStateException("Missing " + id);
        return new ItemStack(value);
    }
}
