package com.gregtech.gregtech.compat;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
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
public final class ImmersiveEngineeringCompatGameTests {
    private ImmersiveEngineeringCompatGameTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 8000)
    public static void rowsFollowWhetherImmersiveEngineeringIsLoaded(GameTestHelper helper) {
        boolean loaded = ModList.get().isLoaded("immersiveengineering");
        helper.assertTrue(CompatRecipes.errors() == 0, "unresolved compat rows: " + CompatRecipes.problems());
        var manager = helper.getLevel().getServer().getRecipeManager();
        var conversion = new ResourceLocation("gregtech", "compat/immersiveengineering/gt_to_horizontal");
        if (!loaded) {
            helper.assertTrue(CompatRecipes.added() == 0, "machine rows must stay absent without IE");
            helper.assertTrue(manager.byKey(conversion).isEmpty(), "crafting rows must stay absent without IE");
            helper.succeed();
            return;
        }
        helper.assertTrue(CompatRecipes.added() > 0, "IE machine rows were not registered");
        var coke = MachineRecipeMaps.Compressor.findRecipe(List.of(GTItems.getStack(MaterialPrefix.plateGem, Materials.CoalCoke, 8)),
                List.of(), false, MachineRecipeMaps.Compressor.mInputItemsCount, MachineRecipeMaps.Compressor.mOutputItemsCount);
        helper.assertTrue(coke != null && coke.mOutputs[0].is(ie("immersiveengineering:coke").getItem()) && coke.mOutputs[0].getCount() == 1,
                "eight coke plates compress to an IE coke block");
        var dust = MachineRecipeMaps.Shredder.findRecipe(List.of(ie("immersiveengineering:coke")), List.of(), false,
                MachineRecipeMaps.Shredder.mInputItemsCount, MachineRecipeMaps.Shredder.mOutputItemsCount);
        helper.assertTrue(dust != null && dust.mOutputs[0].is(ie("immersiveengineering:dust_coke").getItem()),
                "IE coke block shreds to IE coke dust");
        var saw = MachineRecipeMaps.Cutter.findRecipe(List.of(ie("immersiveengineering:wooden_barrel")),
                List.of(DyeProcessingRecipes.fluid("Water", 400)), false,
                MachineRecipeMaps.Cutter.mInputItemsCount, MachineRecipeMaps.Cutter.mOutputItemsCount);
        var sawdust = GTItems.getStack(MaterialPrefix.dustSmall, WoodMaterials.Wood, 2);
        helper.assertTrue(saw != null && saw.mOutputs[0].is(Items.OAK_PLANKS) && saw.mOutputs[0].getCount() == 6
                        && saw.mOutputs[1].is(sawdust.getItem()) && saw.mOutputs[1].getCount() == 2,
                "wooden barrel saws into six oak planks and two small wood dust");
        var fabric = MachineRecipeMaps.Loom.findRecipe(List.of(ie("immersiveengineering:hemp_fiber", 8), ie("immersiveengineering:stick_treated")),
                List.of(), false, MachineRecipeMaps.Loom.mInputItemsCount, MachineRecipeMaps.Loom.mOutputItemsCount);
        helper.assertTrue(fabric != null && fabric.mOutputs[0].is(ie("immersiveengineering:hemp_fabric").getItem()),
                "hemp fiber and a treated stick weave IE fabric");
        var treated = GTItems.getStack(MaterialPrefix.plate, WoodMaterials.WoodTreated, 1);
        var generified = MachineRecipeMaps.Generifier.findRecipe(List.of(treated), List.of(), false,
                MachineRecipeMaps.Generifier.mInputItemsCount, MachineRecipeMaps.Generifier.mOutputItemsCount);
        helper.assertTrue(generified != null && generified.mOutputs[0].is(ie("immersiveengineering:treated_wood_horizontal").getItem()),
                "GT treated planks generify to IE horizontal treated wood");
        var bath = MachineRecipeMaps.Bath.findRecipe(List.of(new ItemStack(Items.OAK_STAIRS)),
                List.of(DyeProcessingRecipes.fluid("Oil_Creosote", 75)), false,
                MachineRecipeMaps.Bath.mInputItemsCount, MachineRecipeMaps.Bath.mOutputItemsCount);
        helper.assertTrue(bath != null && bath.mOutputs[0].is(ie("immersiveengineering:stairs_treated_wood_horizontal").getItem()),
                "creosote bath turns oak stairs into IE treated stairs");
        helper.assertTrue(manager.byKey(new ResourceLocation("immersiveengineering", "crafting/hammer")).isPresent(),
                "IE hammer recipe remains");
        helper.assertTrue(manager.byKey(new ResourceLocation("immersiveengineering", "crafting/plate_iron_hammering")).isEmpty(),
                "IE hammered iron plate recipe is disabled");
        helper.assertTrue(manager.byKey(conversion).isPresent(), "GT treated planks convert to IE horizontal wood");
        int rows = CompatRecipes.added();
        var server = helper.getLevel().getServer();
        server.reloadResources(new ArrayList<>(server.getPackRepository().getSelectedIds())).join();
        helper.assertTrue(CompatRecipes.added() == rows, "reload duplicated or dropped compat rows: " + CompatRecipes.added() + " vs " + rows);
        helper.assertTrue(CompatRecipes.errors() == 0, "reload left unresolved rows: " + CompatRecipes.problems());
        helper.assertTrue(server.getRecipeManager().byKey(new ResourceLocation("immersiveengineering", "crafting/plate_iron_hammering")).isEmpty(),
                "hammered plate recipe returned after reload");
        helper.succeed();
    }

    private static ItemStack ie(String id) { return ie(id, 1); }

    private static ItemStack ie(String id, int count) {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null || item == Items.AIR) throw new IllegalStateException("Missing " + id);
        return new ItemStack(item, count);
    }
}
