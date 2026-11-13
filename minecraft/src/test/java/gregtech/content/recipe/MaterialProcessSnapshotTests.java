package gregtech.content.recipe;

import gregtech.content.material.Materials;
import gregtech.content.worldgen.MaterialWorldgenFacts;
import gregtech.content.worldgen.MaterialWorldgenFacts.Fact;
import gregtech.content.worldgen.MaterialWorldgenFacts.Kind;
import gregtech.content.worldgen.MaterialWorldgenFacts.Ore;
import gregtech.engine.material.MaterialProcessGraph.*;
import gregtech.engine.material.MaterialPublication;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MaterialProcessSnapshotTests {
    private MaterialProcessSnapshotTests() {}

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void nativeProjectionKeepsOutputsAndCrossMaterialRoutes(GameTestHelper helper) {
        var first = row(new ItemStack[] {new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.PAPER),
                new ItemStack(Items.STICK)}, Items.COPPER_INGOT).withCatalystInputs(2);
        var second = row(new ItemStack[] {new ItemStack(Items.COPPER_INGOT)}, Items.GOLD_INGOT);
        var third = row(new ItemStack[] {new ItemStack(Items.GOLD_INGOT)}, Items.DIAMOND);
        var tagged = new Recipe(new ItemStack[] {new ItemStack(Items.GOLD_INGOT)},
                new ItemStack[] {new ItemStack(Items.IRON_INGOT)}, null, null,
                new FluidStack[] {new FluidStack(Fluids.WATER, 1000)},
                new FluidStack[] {new FluidStack(Fluids.LAVA, 1000)}, 20, 16, 0)
                .withItemTagInput(0, ResourceLocation.fromNamespaceAndPath("c", "ingots/gold"))
                .withFluidTagInput(0, ResourceLocation.fromNamespaceAndPath("c", "water"));
        var map = new RecipeMap(new ArrayList<>(List.of(first, second, third, tagged)),
                "gt.test.process.snapshot", "Test", "textures/gui/test", 9, 9, 0, 0, 0, 0, 0,
                false, false, false, false);
        RecipeMap.unregister(map);
        var ores = List.of(new Ore(Materials.Iron, "item:minecraft:iron_nugget", 0, 20, 1, 0, List.of()));
        var facts = new MaterialWorldgenFacts.Snapshot(List.of(
                new Fact("test", "test", Kind.LARGE_VEIN, "minecraft:overworld", 0, 20, 1, 1, ores)));
        var snapshot = MaterialProcessSnapshot.build(MaterialPublication.current(), 1, facts, List.of(map));
        var root = snapshot.roots().stream().filter(value -> value.material().equals("Copper")).findFirst().orElseThrow();
        var query = snapshot.query(root.resource(), Direction.BOTH, Budgets.DEFAULT);
        helper.assertTrue(query.steps().size() == 4, "route traverses other materials and the native worldgen form");
        var firstStep = query.steps().stream().filter(step -> snapshot.recipe(step.reference())
                .map(nativeRow -> nativeRow.recipe() == first).orElse(false)).findFirst().orElseThrow();
        helper.assertTrue(firstStep.inputs().size() == 2, "ordinary co-input survives; auxiliary tool stays in details");
        helper.assertTrue(firstStep.inputs().stream().anyMatch(port -> port.resource().state().equals("item:minecraft:paper")),
                "unclassified ordinary input keeps its native identity");
        helper.assertTrue(firstStep.details().stream().anyMatch(detail -> detail.contains("minecraft:stick")),
                "auxiliary catalyst is available in recipe details");
        var taggedStep = snapshot.roots().stream()
                .flatMap(value -> snapshot.query(value.resource(), Direction.BOTH, Budgets.DEFAULT).steps().stream())
                .filter(step -> snapshot.recipe(step.reference())
                .map(nativeRow -> nativeRow.recipe() == tagged).orElse(false)).findFirst().orElseThrow();
        helper.assertTrue(taggedStep.outputs().get(0).resource().state().equals("item:minecraft:iron_ingot"),
                "input tag never changes the output identity");
        helper.assertTrue(taggedStep.outputs().stream().anyMatch(port -> port.resource().state().equals("fluid:minecraft:lava")),
                "fluid input tag never changes the fluid output identity");
        var replacement = MaterialProcessSnapshot.build(MaterialPublication.current(), 1, facts, List.of(map));
        helper.assertTrue(replacement.query(root.resource(), Direction.BOTH, Budgets.DEFAULT).steps().isEmpty(),
                "resource references cannot cross snapshots even with the same stamp");
        helper.assertTrue(snapshot.query(root.resource(), Direction.BOTH, Budgets.DEFAULT).equals(query),
                "old query remains self-consistent");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void worldgenRevisionIncludesSelectionAndKind(GameTestHelper helper) {
        var ores = List.of(new Ore(Materials.Iron, "item:minecraft:iron_nugget", 0, 20, 1, 0, List.of()));
        var baseline = new Fact("test", "test", Kind.LARGE_VEIN, "minecraft:overworld", 0, 20, 1, 10, ores);
        long revision = new MaterialWorldgenFacts.Snapshot(List.of(baseline)).revision();
        for (Fact changed : List.of(
                new Fact("test", "test", Kind.LARGE_VEIN, "minecraft:overworld", 0, 20, 2, 10, ores),
                new Fact("test", "test", Kind.LARGE_VEIN, "minecraft:overworld", 0, 20, 1, 20, ores),
                new Fact("test", "test", Kind.BEDROCK, "minecraft:overworld", 0, 20, 1, 10, ores),
                new Fact("test", "renamed", Kind.LARGE_VEIN, "minecraft:overworld", 0, 20, 1, 10, ores))) {
            helper.assertTrue(new MaterialWorldgenFacts.Snapshot(List.of(changed)).revision() != revision,
                    "each changed worldgen field changes the revision");
        }
        var activeFacts = MaterialWorldgenFacts.capture();
        helper.assertFalse(activeFacts.facts().isEmpty(), "active worldgen facts are projected");
        helper.assertFalse(activeFacts.facts().stream().flatMap(fact -> fact.ores().stream())
                .anyMatch(ore -> ore.itemState().equals("item:minecraft:stone")
                        || ore.itemState().equals("item:minecraft:air")),
                "missing ore registrations never turn into stone or air sources");
        var current = MaterialProcessSnapshot.Store.publish(MachineRecipeOverlay.lastPublication(),
                MachineRecipeOverlay.generation());
        helper.assertTrue(current.samePublication(MachineRecipeOverlay.lastPublication()),
                "snapshot follows the last successful publication");
        helper.assertTrue(MaterialProcessSnapshot.Store.publish(MachineRecipeOverlay.lastPublication(),
                MachineRecipeOverlay.generation()) == current, "unchanged publication reuses its index");
        helper.succeed();
    }

    private static Recipe row(ItemStack[] inputs, net.minecraft.world.item.Item output) {
        return new Recipe(inputs, new ItemStack[] {new ItemStack(output)}, null, null, null, null, 20, 16, 0);
    }
}
