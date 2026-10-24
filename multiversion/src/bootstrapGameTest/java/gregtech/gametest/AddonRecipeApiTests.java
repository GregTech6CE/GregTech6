package gregtech.gametest;

import gregtech.api.addon.platform.AddonRecipes;
import gregtech.api.recipe.RecipeMap;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Native API boundaries: mutable Minecraft stacks must not leak into stored recipe definitions. */
@GameTestHolder("gt6ce_api_contracts")
@PrefixGameTestTemplate(false)
public final class AddonRecipeApiTests {
    private AddonRecipeApiTests() {}

    @GameTest(template = "test_empty")
    public static void recipeRowsOwnTheirArraysAndStacks(GameTestHelper helper) {
        ItemStack[] inputs = {new ItemStack(Items.STICK, 4), new ItemStack(Items.FLINT)};
        ItemStack[] outputs = {new ItemStack(Items.PAPER, 2)};
        FluidStack[] fluidsIn = {new FluidStack(Fluids.WATER, 250), new FluidStack(Fluids.LAVA, 50)};
        FluidStack[] fluidsOut = {new FluidStack(Fluids.WATER, 125)};
        int[] chances = {2500};
        int[] kept = {1};
        int[] keptFluids = {0};
        int[] presence = {1};
        var row = new AddonRecipes.Line(inputs, outputs, fluidsIn, fluidsOut, 20, 8, 300,
                chances, kept, keptFluids, presence);

        helper.assertTrue(fluidsIn[1].getAmount() == 50, "presence normalization must not alter caller fluid");
        inputs[0].setCount(1);
        inputs[1] = ItemStack.EMPTY;
        outputs[0].setCount(1);
        fluidsIn[0].setAmount(1);
        fluidsIn[1] = FluidStack.EMPTY;
        fluidsOut[0].setAmount(1);
        chances[0] = 1;
        kept[0] = 0;
        keptFluids[0] = 1;
        presence[0] = 0;
        assertOriginalRow(helper, row, "constructor copies");

        row.itemInputs()[0].setCount(1);
        row.itemInputs()[1] = ItemStack.EMPTY;
        row.itemOutputs()[0].setCount(1);
        row.fluidInputs()[0].setAmount(1);
        row.fluidInputs()[1] = FluidStack.EMPTY;
        row.fluidOutputs()[0].setAmount(1);
        row.outputChances()[0] = 1;
        row.keptInputs()[0] = 0;
        row.keptFluidInputs()[0] = 1;
        row.presenceFluidInputs()[0] = 0;
        assertOriginalRow(helper, row, "accessor copies");

        var stored = AddonRecipes.Line.read(new ItemStack[]{ItemStack.EMPTY}, null,
                new FluidStack[]{FluidStack.EMPTY}, null, 0, 0, 0, null, null);
        helper.assertTrue(stored.itemInputs().length == 1 && stored.itemInputs()[0].isEmpty()
                        && stored.fluidInputs().length == 1 && stored.fluidInputs()[0].isEmpty()
                        && stored.duration() == 1,
                "reading preexisting empty slots retains the existing read contract");
        boolean strict = false;
        try {
            new AddonRecipes.Line(new ItemStack[]{ItemStack.EMPTY}, null, null, null, 1, 0, 0);
        } catch (IllegalArgumentException expected) {
            strict = true;
        }
        helper.assertTrue(strict, "reading a row must not disable validation for subsequent constructors");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void builderKeepsConstructorSemanticsAndOwnsItsArguments(GameTestHelper helper) {
        ItemStack[] inputs = {new ItemStack(Items.STICK, 4), new ItemStack(Items.FLINT)};
        ItemStack[] outputs = {new ItemStack(Items.PAPER, 2)};
        FluidStack[] fluidsIn = {new FluidStack(Fluids.WATER, 250), new FluidStack(Fluids.LAVA, 50)};
        FluidStack[] fluidsOut = {new FluidStack(Fluids.WATER, 125)};
        int[] chances = {2500};
        int[] kept = {1};
        int[] keptFluids = {0};
        int[] presence = {1};
        var positional = new AddonRecipes.Line(inputs, outputs, fluidsIn, fluidsOut, 20, 8, 300,
                chances, kept, keptFluids, presence);
        var builder = AddonRecipes.Line.builder()
                .itemInputs(inputs).itemOutputs(outputs).fluidInputs(fluidsIn).fluidOutputs(fluidsOut)
                .duration(20).euPerTick(8).special(300).outputChances(chances)
                .keptInputs(kept).keptFluidInputs(keptFluids).presenceFluidInputs(presence);

        inputs[0].setCount(1);
        outputs[0].setCount(1);
        fluidsIn[0].setAmount(1);
        fluidsOut[0].setAmount(1);
        chances[0] = 1;
        kept[0] = 0;
        keptFluids[0] = 1;
        presence[0] = 0;
        var named = builder.build();
        assertOriginalRow(helper, named, "builder setter copies");
        helper.assertTrue(named.duration() == positional.duration() && named.euPerTick() == positional.euPerTick()
                        && named.special() == positional.special(), "named scalar values equal constructor values");
        builder.itemInputs(new ItemStack(Items.DIRT), new ItemStack(Items.FLINT)).duration(40);
        assertOriginalRow(helper, named, "later builder edits leave existing rows unchanged");
        helper.assertTrue(builder.build().itemInputs()[0].is(Items.DIRT) && named.duration() == 20,
                "a reusable builder creates independent definitions");

        var defaults = AddonRecipes.Line.builder().itemOutputs(new ItemStack(Items.PAPER)).duration(0).build();
        helper.assertTrue(defaults.itemInputs().length == 0 && defaults.fluidInputs().length == 0
                        && defaults.fluidOutputs().length == 0 && defaults.duration() == 1
                        && defaults.euPerTick() == 0 && defaults.special() == 0
                        && defaults.outputChances()[0] == 10000 && defaults.keptInputs().length == 0
                        && defaults.keptFluidInputs().length == 0 && defaults.presenceFluidInputs().length == 0,
                "builder defaults retain constructor defaults and duration normalization");
        boolean rejected = false;
        try {
            AddonRecipes.Line.builder().fluidInputs(new FluidStack(Fluids.WATER, 100))
                    .keptFluidInputs(0).presenceFluidInputs(0).build();
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "builder preserves constructor validation of conflicting fluid roles");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void recipeRowsCompareByValue(GameTestHelper helper) {
        var first = AddonRecipes.Line.builder()
                .itemInputs(new ItemStack(Items.STICK, 4)).itemOutputs(new ItemStack(Items.PAPER, 2))
                .fluidInputs(new FluidStack(Fluids.WATER, 250)).fluidOutputs(new FluidStack(Fluids.LAVA, 50))
                .duration(20).euPerTick(8).special(300).build();
        var same = AddonRecipes.Line.builder()
                .itemInputs(new ItemStack(Items.STICK, 4)).itemOutputs(new ItemStack(Items.PAPER, 2))
                .fluidInputs(new FluidStack(Fluids.WATER, 250)).fluidOutputs(new FluidStack(Fluids.LAVA, 50))
                .duration(20).euPerTick(8).special(300).build();
        helper.assertTrue(first.equals(same) && first.hashCode() == same.hashCode(),
                "separately built rows with the same content are equal");

        var fewerItems = AddonRecipes.Line.builder()
                .itemInputs(new ItemStack(Items.STICK, 3)).itemOutputs(new ItemStack(Items.PAPER, 2))
                .fluidInputs(new FluidStack(Fluids.WATER, 250)).fluidOutputs(new FluidStack(Fluids.LAVA, 50))
                .duration(20).euPerTick(8).special(300).build();
        helper.assertTrue(!first.equals(fewerItems), "item count takes part in row equality");

        var lessWater = AddonRecipes.Line.builder()
                .itemInputs(new ItemStack(Items.STICK, 4)).itemOutputs(new ItemStack(Items.PAPER, 2))
                .fluidInputs(new FluidStack(Fluids.WATER, 249)).fluidOutputs(new FluidStack(Fluids.LAVA, 50))
                .duration(20).euPerTick(8).special(300).build();
        helper.assertTrue(!first.equals(lessWater), "fluid amount takes part in row equality");

//? if >=1.21.1 {
        /*var tagged = new ItemStack(Items.PAPER, 2);
        tagged.set(DataComponents.CUSTOM_NAME, Component.literal("tagged"));
        var taggedCopy = new ItemStack(Items.PAPER, 2);
        taggedCopy.set(DataComponents.CUSTOM_NAME, Component.literal("tagged"));
*///?} else {
        var tagged = new ItemStack(Items.PAPER, 2);
        tagged.getOrCreateTag().putString("tag", "value");
        var taggedCopy = new ItemStack(Items.PAPER, 2);
        taggedCopy.getOrCreateTag().putString("tag", "value");
//?}
        var taggedRow = AddonRecipes.Line.builder().itemOutputs(tagged).build();
        var taggedCopyRow = AddonRecipes.Line.builder().itemOutputs(taggedCopy).build();
        helper.assertTrue(taggedRow.equals(taggedCopyRow) && taggedRow.hashCode() == taggedCopyRow.hashCode(),
                "item data takes part by value");
        helper.assertTrue(!first.equals(AddonRecipes.Line.builder()
                        .itemInputs(new ItemStack(Items.STICK, 4)).itemOutputs(tagged)
                        .fluidInputs(new FluidStack(Fluids.WATER, 250)).fluidOutputs(new FluidStack(Fluids.LAVA, 50))
                        .duration(20).euPerTick(8).special(300).build()),
                "different item data alone is not equal when counts and other fields match");

        var storedA = AddonRecipes.Line.read(new ItemStack[]{ItemStack.EMPTY}, null, null, null, 1, 0, 0, null, null);
        var storedB = AddonRecipes.Line.read(new ItemStack[]{ItemStack.EMPTY}, null, null, null, 1, 0, 0, null, null);
        helper.assertTrue(storedA.equals(storedB) && storedA.hashCode() == storedB.hashCode(),
                "stored empty slots compare equal");

        var presenceA = new AddonRecipes.Line(new ItemStack[0], new ItemStack[0],
                new FluidStack[]{new FluidStack(Fluids.WATER, 100)}, new FluidStack[0],
                1, 0, 0, null, null, null, new int[]{0});
        var presenceB = new AddonRecipes.Line(new ItemStack[0], new ItemStack[0],
                new FluidStack[]{new FluidStack(Fluids.WATER, 300)}, new FluidStack[0],
                1, 0, 0, null, null, null, new int[]{0});
        helper.assertTrue(presenceA.equals(presenceB) && presenceA.hashCode() == presenceB.hashCode(),
                "presence fluids normalize their amount before comparison");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void dynamicProvidersCannotMutateInventoryOrLaterQueries(GameTestHelper helper) {
        var nativeMap = new RecipeMap(new ArrayList<>(), "gt.recipe.test.api_snapshot", "API snapshot", "test",
                1, 1, 1, 1, 0, 1, 2, false, false, false, false);
        try {
            var api = AddonRecipes.map(nativeMap.mNameInternal);
            var retainedItems = new AtomicReference<List<ItemStack>>();
            var retainedFluids = new AtomicReference<List<FluidStack>>();
            var laterCalls = new AtomicInteger();
            api.addDynamic((items, fluids) -> {
                helper.assertTrue(items.get(0).getCount() == 8 && fluids.get(0).getAmount() == 250,
                        "every query begins with original amounts");
                assertUnmodifiable(helper, items, fluids);
                retainedItems.set(items);
                retainedFluids.set(fluids);
                items.get(0).setCount(1);
                fluids.get(0).setAmount(1);
                return null;
            });
            api.addDynamic((items, fluids) -> {
                laterCalls.incrementAndGet();
                helper.assertTrue(items.get(0).getCount() == 8 && fluids.get(0).getAmount() == 250,
                        "a later provider sees inventory, not earlier provider mutations");
                return AddonRecipes.Line.builder().itemInputs(new ItemStack(Items.STICK, 4))
                        .fluidInputs(new FluidStack(Fluids.WATER, 100))
                        .itemOutputs(new ItemStack(Items.PAPER)).duration(20).euPerTick(8).build();
            });
            var item = new ItemStack(Items.STICK, 8);
            var fluid = new FluidStack(Fluids.WATER, 250);
            var items = List.of(item);
            var fluids = List.of(fluid);
            helper.assertTrue(nativeMap.findRecipe(items, fluids, false, 1, 1) != null,
                    "native machine lookup remains matchable after a provider modifies its snapshots");
            helper.assertTrue(item.getCount() == 8 && fluid.getAmount() == 250,
                    "native machine inventory is untouched");
            retainedItems.get().get(0).setCount(64);
            retainedFluids.get().get(0).setAmount(1000);
            helper.assertTrue(api.find(items, fluids, false, 1, 1) != null && laterCalls.get() == 2,
                    "retained snapshots do not change a subsequent public lookup");
            helper.assertTrue(item.getCount() == 8 && fluid.getAmount() == 250,
                    "public query inputs remain untouched");
        } finally {
            RecipeMap.unregister(nativeMap);
        }
        helper.succeed();
    }

    private static void assertOriginalRow(GameTestHelper helper, AddonRecipes.Line row, String stage) {
        helper.assertTrue(row.itemInputs()[0].is(Items.STICK) && row.itemInputs()[0].getCount() == 4
                        && row.itemInputs()[1].is(Items.FLINT)
                        && row.itemOutputs()[0].is(Items.PAPER) && row.itemOutputs()[0].getCount() == 2
                        && row.fluidInputs()[0].getFluid() == Fluids.WATER && row.fluidInputs()[0].getAmount() == 250
                        && row.fluidInputs()[1].getFluid() == Fluids.LAVA && row.fluidInputs()[1].getAmount() == 1
                        && row.fluidOutputs()[0].getAmount() == 125
                        && Arrays.equals(row.outputChances(), new int[]{2500})
                        && Arrays.equals(row.keptInputs(), new int[]{1})
                        && Arrays.equals(row.keptFluidInputs(), new int[]{0})
                        && Arrays.equals(row.presenceFluidInputs(), new int[]{1}), stage);
    }

    private static void assertUnmodifiable(GameTestHelper helper, List<ItemStack> items, List<FluidStack> fluids) {
        boolean itemsProtected = false;
        boolean fluidsProtected = false;
        try { items.clear(); } catch (UnsupportedOperationException expected) { itemsProtected = true; }
        try { fluids.clear(); } catch (UnsupportedOperationException expected) { fluidsProtected = true; }
        helper.assertTrue(itemsProtected && fluidsProtected, "provider list structure is read-only");
    }
}
