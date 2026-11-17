package gregtech.content.recipe;

import gregtech.api.addon.material.FormKey;
import gregtech.api.addon.material.MaterialFormBinding;
import gregtech.api.addon.material.MaterialFormKind;
import gregtech.api.addon.material.MaterialFormsView;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.SpecialProcessValue;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.material.SpecialProcessMethods;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialViews;
import gregtech.engine.recipe.Recipe;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class SpecialProcessRecipeTests {
    private SpecialProcessRecipeTests() {}

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void addonOutputAliasesResolvePublishedBindings(GameTestHelper helper) {
        var material = new MaterialKey("Gold");
        var input = new FormKey("crushedPurified");
        var plan = (SpecialProcessValue.Target) MaterialViews.catalog().processes()
                .find(material, RecoveryRecipePublisherTests.ExistingFormAddon.PROCESS).get(0).value();
        var output = plan.itemOutputs().get(0);
        helper.assertTrue(output.material().equals(new MaterialKey("Iron")), "Fe is published as Iron");
        helper.assertTrue(output.forms().equals(List.of(
                new SpecialProcessValue.FormCount(new FormKey("itemCasing"), 1),
                new SpecialProcessValue.FormCount(new FormKey("dust"), 2))), "output alternatives retain order");
        var result = SharedSpecialProcessRows.build(material, input, 1, plan,
                new SharedSpecialProcessRows.ReagentSelector(new MaterialKey("Lava"), Set.of(new FormKey("liquid"))),
                new SharedSpecialProcessRows.Resolver());
        var casing = GTItems.getStack(MaterialPrefix.itemCasing, GTMaterialRegistry.get("Iron"), 1);
        helper.assertTrue(!casing.isEmpty() && result.recipe() != null, "aliased output resolves to a recipe: " + result.reason());
        helper.assertTrue(result.recipe().mOutputs[0].getItem() == casing.getItem(), "first available canonical output wins");
        helper.assertTrue(SharedSpecialProcessRows.owned().stream().anyMatch(row -> sameInputs(row, result.recipe())
                        && row.mOutputs.length == 1 && row.mOutputs[0].getItem() == casing.getItem()),
                "the alias-based special process row is published");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void publishedFluidsAndAnyFormReagentsNeedNoUnitFacts(GameTestHelper helper) {
        var custom = new MaterialKey("AddonProcessFluid");
        var liquid = new FormKey("liquid");
        var delegate = MaterialViews.catalog().formBindings();
        // A platform-published addon binding is sufficient; there is no internal unit-table entry.
        MaterialFormsView forms = material -> material.equals(custom)
                ? Set.of(new MaterialFormBinding(custom, liquid, MaterialFormKind.FLUID, "minecraft:lava", 0))
                : delegate.find(material);
        var target = new MaterialKey("Gold");
        var input = new FormKey("crushedPurified");
        var itemOutput = new SpecialProcessValue.ItemOutput(target,
                List.of(new SpecialProcessValue.FormCount(new FormKey("crushedCentrifuged"), 1)), 5000);
        var fluidOutput = new SpecialProcessValue.FluidOutput(custom,
                List.of(new SpecialProcessValue.FormCount(liquid, 1)), 123);
        var plan = new SpecialProcessValue.Target(Map.of(input, 1), List.of(itemOutput), List.of(fluidOutput), 37, 256, 0);
        var result = SharedSpecialProcessRows.build(target, input, 1, plan,
                new SharedSpecialProcessRows.ReagentSelector(custom, Set.of()), new SharedSpecialProcessRows.Resolver(forms));
        helper.assertTrue(result.recipe() != null, "anyForm fluid reagent resolves: " + result.reason());
        var recipe = result.recipe();
        helper.assertTrue(recipe.mFluidInputs[0].getFluid() == Fluids.LAVA && recipe.mFluidInputs[0].getAmount() == 37,
                "reagent uses the published identity and exact mB");
        helper.assertTrue(recipe.mFluidOutputs[0].getFluid() == Fluids.LAVA && recipe.mFluidOutputs[0].getAmount() == 123,
                "output uses the published identity and exact mB");
        helper.assertTrue(recipe.mChances[0] == 5000, "item chance is preserved");

        var missingOutput = new SpecialProcessValue.FluidOutput(custom,
                List.of(new SpecialProcessValue.FormCount(new FormKey("gas"), 1)), 123);
        var invalid = new SpecialProcessValue.Target(Map.of(input, 1), List.of(itemOutput), List.of(missingOutput), 37, 256, 0);
        result = SharedSpecialProcessRows.build(target, input, 1, invalid,
                new SharedSpecialProcessRows.ReagentSelector(custom, Set.of(liquid)), new SharedSpecialProcessRows.Resolver(forms));
        helper.assertTrue(result.recipe() == null && "fluid-output".equals(result.reason()),
                "missing output rejects the whole row without substituting its phase");

        var itemPlan = new SpecialProcessValue.Target(Map.of(input, 1), List.of(itemOutput), List.of(), 2, 256, 0);
        result = SharedSpecialProcessRows.build(target, input, 1, itemPlan,
                new SharedSpecialProcessRows.ReagentSelector(new MaterialKey("SodiumPersulfate"), Set.of()),
                new SharedSpecialProcessRows.Resolver());
        helper.assertTrue(result.recipe() != null && result.recipe().mInputs[1].getCount() == 2,
                "anyForm also resolves a registered item reagent");
        var itemForm = new FormKey("dust");
        MaterialFormsView mixed = material -> material.equals(custom)
                ? Set.of(new MaterialFormBinding(custom, liquid, MaterialFormKind.FLUID, "minecraft:lava", 0),
                        new MaterialFormBinding(custom, itemForm, MaterialFormKind.ITEM, "minecraft:diamond", 0))
                : delegate.find(material);
        result = SharedSpecialProcessRows.build(target, input, 1, itemPlan,
                new SharedSpecialProcessRows.ReagentSelector(custom, Set.of()), new SharedSpecialProcessRows.Resolver(mixed));
        helper.assertTrue(result.recipe() == null && "reagent".equals(result.reason()),
                "mixed item/fluid anyForm rejects ambiguous units");
        result = SharedSpecialProcessRows.build(target, input, 1, itemPlan,
                new SharedSpecialProcessRows.ReagentSelector(custom, Set.of(itemForm)), new SharedSpecialProcessRows.Resolver(mixed));
        helper.assertTrue(result.recipe() != null && result.recipe().mInputs[1].is(Items.DIAMOND),
                "a declared item binding resolves without an internal material or prefix entry");
        var oversized = new SpecialProcessValue.Target(Map.of(input, 1), List.of(itemOutput), List.of(), 65, 256, 0);
        result = SharedSpecialProcessRows.build(target, input, 1, oversized,
                new SharedSpecialProcessRows.ReagentSelector(custom, Set.of(itemForm)), new SharedSpecialProcessRows.Resolver(mixed));
        helper.assertTrue(result.recipe() == null && "reagent".equals(result.reason()),
                "a reagent cannot exceed the item's maximum stack size");
        for (String name : List.of("Water", "Lava", "Mercury")) {
            result = SharedSpecialProcessRows.build(target, input, 1, itemPlan,
                    new SharedSpecialProcessRows.ReagentSelector(new MaterialKey(name), Set.of(liquid)),
                    new SharedSpecialProcessRows.Resolver());
            helper.assertTrue(result.recipe() != null && result.recipe().mFluidInputs.length == 1
                            && result.recipe().mFluidInputs[0].getAmount() == 2,
                    name + " resolves exact mB through its published native form");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void aquaRegiaRowsHaveOneOwnerAndAllOutputs(GameTestHelper helper) {
        var catalog = MaterialViews.catalog();
        for (String name : List.of("Gold", "Copper", "Tin", "Cassiterite", "Platinum", "Nickel")) {
            var material = new MaterialKey(name);
            var plan = (SpecialProcessValue.Target) catalog.processes().find(material, SpecialProcessMethods.AQUA_REGIA_LEACHING)
                    .stream().map(rule -> rule.value()).filter(SpecialProcessValue.Target.class::isInstance).findFirst().orElseThrow();
            boolean platinum = name.equals("Platinum") || name.equals("Nickel");
            boolean tin = name.equals("Tin") || name.equals("Cassiterite");
            long[] sourceFluidAmounts = platinum ? new long[] {4500, 1500, 4125}
                    : tin ? new long[] {2500, 4000, 3000} : new long[] {3000, 1000, 3000};
            helper.assertTrue(plan.reagentAmount() == (platinum ? 9750 : tin ? 13000 : 6500), "source acid amount");
            helper.assertTrue(plan.inputCounts().equals(Map.of(new FormKey("crushedPurified"), 1,
                    new FormKey("crushedPurifiedTiny"), 9)), "source purified input ratios");
            for (var input : plan.inputCounts().entrySet()) {
                var result = SharedSpecialProcessRows.build(material, input.getKey(), input.getValue(), plan,
                        new SharedSpecialProcessRows.ReagentSelector(new MaterialKey("AquaRegia"), Set.of(new FormKey("liquid"))),
                        new SharedSpecialProcessRows.Resolver());
                helper.assertTrue(result.recipe() != null, name + "/" + input.getKey() + ": " + result.reason());
                var built = result.recipe();
                helper.assertTrue(built.mOutputs.length == 6 && built.mFluidOutputs.length == 3, "all aqua-regia outputs resolve");
                for (int i = 0; i < 6; i++) helper.assertTrue(built.mChances[i] == (i == 0 ? 10000 : 5000), "source output chances");
                for (int i = 0; i < 3; i++) helper.assertTrue(built.mFluidOutputs[i].getAmount() == sourceFluidAmounts[i], "source fluid amounts");
                var published = MachineRecipeMaps.Bath.mRecipeList.stream().filter(row -> sameInputs(row, built)).toList();
                helper.assertTrue(published.size() == 1, name + "/" + input.getKey() + " publishes exactly once, got " + published.size());
                helper.assertTrue(SharedSpecialProcessRows.owned().contains(published.get(0)), "declarative producer owns the published row");
                helper.assertTrue(published.get(0).mFluidOutputs.length == 3, "published row retains every fluid output");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void persulfateBathKeepsSourceRatiosAndByproducts(GameTestHelper helper) {
        var copper = GTMaterialRegistry.get("Copper");
        var rows = SharedOreWashingRows.definitions(List.of(copper), new SharedOreWashingRows.Snapshot());
        helper.assertTrue(SourceOreWashingRules.bathShapes().stream().map(shape -> shape.input().getName()
                + "/" + shape.inputCount() + "/" + shape.reagent().getName() + "/" + shape.reagentCount()).toList()
                .equals(List.of("crushed/1/dust/1", "crushedTiny/9/dust/1", "crushed/1/dustTiny/9", "crushedTiny/1/dustTiny/1")),
                "all four source input/reagent ratios");
        for (String reagent : List.of("SodiumPersulfate", "PotassiumPersulfate")) {
            for (var shape : SourceOreWashingRules.bathShapes()) {
                var input = GTItems.getStack(shape.input(), copper, shape.inputCount());
                var dust = GTItems.getStack(shape.reagent(), GTMaterialRegistry.get(reagent), shape.reagentCount());
                var matches = rows.stream().filter(row -> row.map() == MachineRecipeMaps.Bath && row.inputs().length == 2
                        && ItemStack.matches(row.inputs()[0], input) && ItemStack.matches(row.inputs()[1], dust)).toList();
                helper.assertTrue(matches.size() == 3, "each persulfate shape has three supported waters");
                for (var row : matches) {
                    boolean whole = shape.outputSize() == SourceOreWashingRules.Size.WHOLE;
                    helper.assertTrue(row.fluidInputs()[0].getAmount() == (whole ? 900 : 100), "source water amount");
                    helper.assertTrue(row.duration() == (whole ? 144 : 16) && row.power() == 0, "source time and power");
                    helper.assertTrue(row.fluidOutputs().length == 0, "persulfate produces no steam");
                    helper.assertTrue(row.outputs().length == (whole ? 6 : 2), "source byproduct slot count");
                    for (int i = 1; i < row.outputs().length; i++) helper.assertTrue(row.outputs()[i].getCount() == (whole && i < 5 ? 2 : 1), "source byproduct count");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void woodDustsStillMakePaper(GameTestHelper helper) {
        for (String name : List.of("Bamboo", "WoodRubber", "WoodTreated")) {
            for (var prefix : List.of(MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny)) {
                var stack = GTItems.getStack(prefix, GTMaterialRegistry.get(name), 1);
                if (stack.isEmpty()) continue;
                helper.assertTrue(MachineRecipeMaps.Bath.mRecipeList.stream().anyMatch(row -> row.mInputs.length == 1
                        && row.itemTag(0) != null && stack.is(row.itemTag(0))
                        && Arrays.stream(row.mOutputs).anyMatch(output -> output.is(Items.PAPER))),
                        name + "/" + prefix.getName() + " remains in the paper input tag");
            }
        }
        for (var prefix : List.of(MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny)) {
            var petrified = GTItems.getStack(prefix, GTMaterialRegistry.get("PetrifiedWood"), 1);
            if (petrified.isEmpty()) continue;
            helper.assertFalse(MachineRecipeMaps.Bath.mRecipeList.stream().anyMatch(row -> row.mInputs.length == 1
                    && row.itemTag(0) != null && petrified.is(row.itemTag(0))
                    && Arrays.stream(row.mOutputs).anyMatch(output -> output.is(Items.PAPER))),
                    "petrified wood is excluded from paper pulp");
        }
        helper.succeed();
    }

    private static boolean sameInputs(Recipe row, Recipe expected) {
        return row.mInputs.length == 1 && row.mFluidInputs.length == 1
                && ItemStack.matches(row.mInputs[0], expected.mInputs[0])
                && row.mFluidInputs[0].getFluid() == expected.mFluidInputs[0].getFluid()
                && row.mFluidInputs[0].getAmount() == expected.mFluidInputs[0].getAmount();
    }
}
