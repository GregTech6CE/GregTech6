package gregtech.compat;

import gregtech.content.material.Materials;
import gregtech.content.recipe.DyeProcessingRecipes;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
//? if >=1.21.1 {
/*import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
//?}
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
//?}

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
//? if >=1.21.1 {
        /*var jerky = ResourceLocation.parse("pamhc2foodcore:beefjerkyitem");
        var seeds = ResourceLocation.parse("pamhc2foodcore:sunflowerseedsitem");
        var dough = ResourceLocation.parse("pamhc2foodcore:doughitem_x2");
*///?} else {
        var jerky = new ResourceLocation("pamhc2foodcore", "beefjerkyitem");
        var seeds = new ResourceLocation("pamhc2foodcore", "sunflowerseedsitem");
        var dough = new ResourceLocation("pamhc2foodcore", "doughitem_x2");
//?}
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
        helper.assertTrue(doughRow != null && doughRow.mOutputs[0].is(item("gt6ce:dough").getItem()),
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
//? if >=1.21.1 {
        /*var value = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (value == Items.AIR) throw new IllegalStateException("Missing " + id);
*///?} else {
        var value = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (value == null || value == Items.AIR) throw new IllegalStateException("Missing " + id);
//?}
        return new ItemStack(value);
    }
}
