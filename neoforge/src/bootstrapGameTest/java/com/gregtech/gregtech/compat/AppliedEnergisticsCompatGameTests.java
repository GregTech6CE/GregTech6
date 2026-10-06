package com.gregtech.gregtech.compat;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.recipe.DyeProcessingRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
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
public final class AppliedEnergisticsCompatGameTests {
    private AppliedEnergisticsCompatGameTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 8000)
    public static void rowsFollowWhetherAppliedEnergisticsIsLoaded(GameTestHelper helper) {
        boolean loaded = ModList.get().isLoaded("ae2");
        helper.assertTrue(CompatRecipes.errors() == 0, "unresolved compat rows: " + CompatRecipes.problems());
        var manager = helper.getLevel().getServer().getRecipeManager();
        var replaced = ResourceLocation.parse("ae2:decorative/quartz_glass");
        var added = ResourceLocation.parse("gregtech:compat/ae2/quartz_glass");
        if (!loaded) {
            helper.assertTrue(manager.byKey(replaced).isEmpty(), "quartz glass recipe must stay absent without AE2");
            helper.assertTrue(manager.byKey(added).isEmpty(), "compat quartz glass must stay absent without AE2");
            helper.succeed();
            return;
        }
        var crystal = ae("ae2:certus_quartz_crystal");
        var press = ae("ae2:calculation_processor_press");
        var printed = MachineRecipeMaps.Press.findRecipe(List.of(press, crystal), List.of(), false,
                MachineRecipeMaps.Press.mInputItemsCount, MachineRecipeMaps.Press.mOutputItemsCount);
        helper.assertTrue(printed != null && printed.mOutputs[0].is(ae("ae2:printed_calculation_processor").getItem()),
                "calculation press prints a certus crystal");
        var anchor = MachineRecipeMaps.Cutter.findRecipe(List.of(GTItems.getStack(MaterialPrefix.ingot, Materials.Iron, 1)),
                List.of(DyeProcessingRecipes.fluid("Water", 40)), false,
                MachineRecipeMaps.Cutter.mInputItemsCount, MachineRecipeMaps.Cutter.mOutputItemsCount);
        helper.assertTrue(anchor != null && anchor.mOutputs[0].is(ae("ae2:cable_anchor").getItem()) && anchor.mOutputs[0].getCount() == 3,
                "an iron ingot saws into three cable anchors");
        var smashed = MachineRecipeMaps.Hammer.findRecipe(List.of(ae("ae2:quartz_block")), List.of(), false,
                MachineRecipeMaps.Hammer.mInputItemsCount, MachineRecipeMaps.Hammer.mOutputItemsCount);
        var gem = GTItems.getStack(MaterialPrefix.gem, Materials.CertusQuartz, 4);
        helper.assertTrue(smashed != null && smashed.mOutputs[0].is(gem.getItem()) && smashed.mOutputs[0].getCount() == 4,
                "a certus quartz block hammers into four gems");
        helper.assertTrue(manager.byKey(replaced).isEmpty(), "AE2 quartz glass recipe is disabled");
        helper.assertTrue(manager.byKey(added).isPresent(), "GT quartz glass recipe is present");
        var server = helper.getLevel().getServer();
        server.reloadResources(new ArrayList<>(server.getPackRepository().getSelectedIds())).join();
        helper.assertTrue(CompatRecipes.errors() == 0, "reload left unresolved rows: " + CompatRecipes.problems());
        helper.assertTrue(server.getRecipeManager().byKey(replaced).isEmpty(), "AE2 quartz glass recipe returned after reload");
        helper.assertTrue(server.getRecipeManager().byKey(added).isPresent(), "GT quartz glass recipe disappeared after reload");
        helper.succeed();
    }

    private static ItemStack ae(String id) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item == Items.AIR) throw new IllegalStateException("Missing " + id);
        return new ItemStack(item);
    }
}
