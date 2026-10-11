package com.gregtech.gregtech.compat;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.recipe.DyeProcessingRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Loaded only in development. Production jars do not contain this source set. */
@GameTestHolder("gregtech_compat")
@PrefixGameTestTemplate(false)
public final class HarvestCraftCompatGameTests {
    private HarvestCraftCompatGameTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 8000)
    public static void rowsFollowWhetherHarvestCraftIsLoaded(GameTestHelper helper) {
        boolean loaded = ModList.get().isLoaded("pamhc2foodcore");
        helper.assertTrue(CompatRecipes.errors() == 0, "unresolved compat rows: " + CompatRecipes.problems());
        var manager = helper.getLevel().getServer().getRecipeManager();
        var jerky = new ResourceLocation("pamhc2foodcore", "beefjerkyitem");
        var seeds = new ResourceLocation("pamhc2foodcore", "sunflowerseedsitem");
        var dough = new ResourceLocation("pamhc2foodcore", "doughitem_x2");
        if (!loaded) {
            helper.assertTrue(manager.byKey(jerky).isEmpty(), "beef jerky recipe must stay absent without HarvestCraft");
            helper.assertTrue(manager.byKey(seeds).isEmpty(), "sunflower seed recipe must stay absent without HarvestCraft");
            helper.succeed();
            return;
        }
        helper.assertTrue(CompatRecipes.added() == 12, "food core machine rows " + CompatRecipes.added());
        var shredded = MachineRecipeMaps.Shredder.findRecipe(List.of(item("minecraft:sunflower")), List.of(),
                false, MachineRecipeMaps.Shredder.mInputItemsCount, MachineRecipeMaps.Shredder.mOutputItemsCount);
        helper.assertTrue(shredded != null && shredded.mOutputs[0].is(item("pamhc2foodcore:sunflowerseedsitem").getItem())
                        && shredded.mOutputs[0].getCount() == 1,
                "a sunflower shreds into one seed");
        var dried = MachineRecipeMaps.Mixer.findRecipe(
                List.of(item("minecraft:beef"), GTItems.getStack(MaterialPrefix.dustSmall, Materials.Salt, 1)),
                List.of(), false, MachineRecipeMaps.Mixer.mInputItemsCount, MachineRecipeMaps.Mixer.mOutputItemsCount);
        helper.assertTrue(dried != null && dried.mOutputs[0].is(item("pamhc2foodcore:beefjerkyitem").getItem()),
                "beef and a small salt dust dry into jerky");
        var doughRow = MachineRecipeMaps.Mixer.findRecipe(List.of(item("pamhc2foodcore:flouritem")),
                List.of(DyeProcessingRecipes.fluid("Water", 1000)), false,
                MachineRecipeMaps.Mixer.mInputItemsCount, MachineRecipeMaps.Mixer.mOutputItemsCount);
        helper.assertTrue(doughRow != null && doughRow.mOutputs[0].is(item("gregtech:dough").getItem()),
                "food-core flour and water mix into GT dough");
        helper.assertTrue(manager.byKey(jerky).isEmpty(), "HarvestCraft beef jerky crafting is disabled");
        helper.assertTrue(manager.byKey(dough).isEmpty(), "HarvestCraft dough crafting is disabled");
        helper.assertTrue(manager.byKey(seeds).isPresent(), "sunflower seed crafting stays");
        var server = helper.getLevel().getServer();
        server.reloadResources(new ArrayList<>(server.getPackRepository().getSelectedIds())).join();
        helper.assertTrue(CompatRecipes.errors() == 0, "reload left unresolved rows: " + CompatRecipes.problems());
        helper.assertTrue(CompatRecipes.added() == 12, "reload changed food core rows " + CompatRecipes.added());
        helper.assertTrue(server.getRecipeManager().byKey(jerky).isEmpty(), "beef jerky crafting returned after reload");
        helper.assertTrue(server.getRecipeManager().byKey(seeds).isPresent(), "sunflower seed crafting disappeared after reload");
        helper.succeed();
    }

    private static ItemStack item(String id) {
        var value = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (value == null || value == Items.AIR) throw new IllegalStateException("Missing " + id);
        return new ItemStack(value);
    }
}
