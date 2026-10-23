package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.recipe.PartRecipeLoader;
import com.gregtech.gregtech.content.recipe.PulverizationRules;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.util.OM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
//? if >=1.21.1 {
/*@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_issue_audit")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
@net.minecraftforge.gametest.GameTestHolder("gregtech_issue_audit")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
public final class RecipeRemainderTests {
    private record Row(RecipeMap map, MaterialPrefix in, MaterialPrefix out, int count) {}
    private static ItemStack stack(MaterialPrefix prefix, GTMaterial material, int count) {
        return MaterialStackItemHelper.mat(prefix, material, count);
    }
    private static boolean same(ItemStack a, ItemStack b) {
//? if >=1.21.1 {
        /*return ItemStack.isSameItemSameComponents(a, b);
*///?} else {
        return ItemStack.isSameItemSameTags(a, b);
//?}
    }
    private static Object invoke(BasicMachineBlockEntity machine, String name) throws Exception {
        var method = BasicMachineBlockEntity.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(machine);
    }

    @GameTest(template="test_empty", timeoutTicks=100)
    public static void registered_machining_remainders_and_blocking(GameTestHelper h) throws Exception {
        // Check the final bootstrapped recipe table, not a newly constructed replacement recipe.
        var rows = java.util.List.of(
                new Row(MachineRecipeMaps.Lathe, MaterialPrefix.bolt, MaterialPrefix.screw, 1),
                new Row(MachineRecipeMaps.Lathe, MaterialPrefix.nugget, MaterialPrefix.round, 1),
                new Row(MachineRecipeMaps.Lathe, MaterialPrefix.plateGem, MaterialPrefix.lens, 1),
                new Row(MachineRecipeMaps.Lathe, MaterialPrefix.lens, MaterialPrefix.ring, 1),
                new Row(MachineRecipeMaps.Lathe, MaterialPrefix.bouleGt, MaterialPrefix.stickLong, 3),
                new Row(MachineRecipeMaps.Lathe, MaterialPrefix.gemChipped, MaterialPrefix.bolt, 1),
                new Row(MachineRecipeMaps.Lathe, MaterialPrefix.gemFlawed, MaterialPrefix.bolt, 3),
                new Row(MachineRecipeMaps.Lathe, MaterialPrefix.billet, MaterialPrefix.stick, 1),
                new Row(MachineRecipeMaps.Lathe, MaterialPrefix.ingot, MaterialPrefix.stick, 1),
                new Row(MachineRecipeMaps.Lathe, MaterialPrefix.gem, MaterialPrefix.stick, 1),
                new Row(MachineRecipeMaps.Cutter, MaterialPrefix.plate, MaterialPrefix.plateTiny, 8),
                new Row(MachineRecipeMaps.Cutter, MaterialPrefix.plateGem, MaterialPrefix.plateGemTiny, 8),
                new Row(MachineRecipeMaps.Cutter, MaterialPrefix.gemChipped, MaterialPrefix.plateGemTiny, 2),
                new Row(MachineRecipeMaps.Cutter, MaterialPrefix.gemFlawed, MaterialPrefix.plateGemTiny, 4),
                new Row(MachineRecipeMaps.Cutter, MaterialPrefix.stickLong, MaterialPrefix.stick, 2));
        var indexed = new java.util.IdentityHashMap<RecipeMap, java.util.Map<Item, java.util.List<Recipe>>>();
        for (var map : java.util.List.of(MachineRecipeMaps.Lathe, MachineRecipeMaps.Cutter)) {
            var byItem = new java.util.HashMap<Item, java.util.List<Recipe>>();
            for (var recipe : map.mRecipeList) if (!recipe.mFakeRecipe && recipe.mEnabled && recipe.mInputs.length == 1)
                byItem.computeIfAbsent(recipe.mInputs[0].getItem(), unused -> new java.util.ArrayList<>()).add(recipe);
            indexed.put(map, byItem);
        }
        int checked = 0, changedTarget = 0;
        for (var row : rows) {
            int rowChecked = 0;
            for (var material : GTMaterialRegistry.allMaterials()) {
                if (!material.isValid() || material.resolve() != material || material.has(MaterialProperty.ANTIMATTER)) continue;
                var input = stack(row.in(), material, 1);
                var output = stack(row.out(), material, row.count());
                if (input.isEmpty() || output.isEmpty()) continue;
                for (var recipe : indexed.get(row.map()).getOrDefault(input.getItem(), java.util.List.of())) {
                    if (recipe.mFakeRecipe || !recipe.mEnabled || recipe.mInputs.length != 1
                            || !same(recipe.mInputs[0], input) || recipe.mInputs[0].getCount() != 1
                            || recipe.mOutputs.length == 0 || !same(recipe.mOutputs[0], output)
                            || recipe.mOutputs[0].getCount() != row.count()) continue;
                    long units = row.in().getMaterialWeight() - row.out().getMaterialWeight() * row.count();
                    // Independent source-unit anchors: these must not drift with prefix metadata.
                    if (row.in() == MaterialPrefix.plateGem && row.out() == MaterialPrefix.lens)
                        h.assertTrue(units == 162_162_000L, "source plateGem-to-lens remainder is exactly U/4");
                    if (row.in() == MaterialPrefix.lens && row.out() == MaterialPrefix.ring)
                        h.assertTrue(units == 324_324_000L, "source lens-to-ring remainder is exactly U/2");
                    if (row.in() == MaterialPrefix.plate && row.out() == MaterialPrefix.plateTiny)
                        h.assertTrue(units == 72_072_000L, "source plate-to-eight-tiny remainder is exactly U/9");
                    var expected = OM.dust(material.getTargetPulverMaterial(),
                            PulverizationRules.amount(units, material.getTargetPulverAmount()));
                    h.assertTrue(recipe.mOutputs.length == (expected.isEmpty() ? 1 : 2),
                            "final remainder output count " + row.in() + " " + material.getName());
                    if (!expected.isEmpty()) {
                        h.assertTrue(same(recipe.mOutputs[1], expected)
                                && recipe.mOutputs[1].getCount() == expected.getCount(),
                                "final exact pulver target quantity " + material.getName() + " " + row.in());
                        if (material.getTargetPulverMaterial() != material) changedTarget++;
                    }
                    checked++; rowChecked++;
                }
            }
            h.assertTrue(rowChecked > 0, "registered source row exercised " + row.in() + " to " + row.out());
        }
        h.assertTrue(checked > rows.size() && changedTarget > 0, "real catalog and different pulver identities exercised");

        var iron = GTMaterialRegistry.get("Fe");
        var plate = stack(MaterialPrefix.plate, iron, 1);
        var map = MachineRecipeMaps.Cutter;
        var recipe = map.mRecipeList.stream().filter(r -> !r.mFakeRecipe && r.mEnabled && r.mInputs.length == 1
                && same(r.mInputs[0], plate) && r.mOutputs.length == 2
                && same(r.mOutputs[0], stack(MaterialPrefix.plateTiny, iron, 8))).findFirst().orElseThrow();
        var block = java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(), false)
                .filter(b -> b instanceof BasicMachineBlock).map(b -> (BasicMachineBlock)b)
                .filter(b -> b.basicSpec().recipeMap() == map && b.basicSpec().tier() == 1
                        && b.basicSpec().energyInMax() >= recipe.mEUt).findFirst().orElseThrow();
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        try {
            var machine = (BasicMachineBlockEntity)h.getLevel().getBlockEntity(pos);
            int firstOutput = machine.spec().recipeMap().mInputItemsCount;
            h.assertTrue(firstOutput == map.mInputItemsCount && map.mOutputItemsCount >= 2,
                    "registered cutter exposes separate main and remainder output cells");
            machine.inventory().setStackInSlot(0, plate.copy());
            machine.getTanksInput()[0].setFluid(recipe.mFluidInputs[0].copy());
            for (int i = firstOutput + 1; i < machine.inventory().getSlots(); i++)
                machine.inventory().setStackInSlot(i, new ItemStack(Items.BARRIER, 64));
            invoke(machine, "checkRecipe");
            h.assertTrue(machine.inventory().getStackInSlot(0).getCount() == 1
                    && machine.getTanksInput()[0].getAmount() == recipe.mFluidInputs[0].getAmount(),
                    "main output alone fits but blocked remainder prevents consuming item or coolant");
            machine.inventory().setStackInSlot(firstOutput + 1, ItemStack.EMPTY);
            invoke(machine, "checkRecipe");
            h.assertTrue(machine.inventory().getStackInSlot(0).isEmpty(), "free remainder slot admits actual recipe");
            h.assertTrue(Boolean.TRUE.equals(invoke(machine, "produceOutputs")), "reserved outputs fully deliver");
            h.assertTrue(same(machine.inventory().getStackInSlot(firstOutput), recipe.mOutputs[0])
                    && machine.inventory().getStackInSlot(firstOutput).getCount() == 8
                    && same(machine.inventory().getStackInSlot(firstOutput + 1), recipe.mOutputs[1])
                    && machine.inventory().getStackInSlot(firstOutput + 1).getCount() == recipe.mOutputs[1].getCount(),
                    "main and remainder delivered once");
            h.assertTrue(same(machine.inventory().getStackInSlot(firstOutput + 1), stack(MaterialPrefix.dustTiny, iron, 1))
                    && machine.inventory().getStackInSlot(firstOutput + 1).getCount() == 1,
                    "independent source anchor: one iron tiny dust, exactly 72072000 material units, not recomputed by helper");
            invoke(machine, "produceOutputs");
            h.assertTrue(machine.inventory().getStackInSlot(firstOutput).getCount() == 8
                    && machine.inventory().getStackInSlot(firstOutput + 1).getCount() == recipe.mOutputs[1].getCount(), "retry does not duplicate");
        } finally { h.getLevel().removeBlock(pos, false); }
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=40)
    public static void pulverized_remainder_ratios_and_floor(GameTestHelper h) throws Exception {
        // Isolated unregistered source material: never mutate the shared catalog or recipe tables.
        var constructor = GTMaterial.class.getDeclaredConstructor(int.class, String.class, String.class, int.class);
        constructor.setAccessible(true);
        var source = constructor.newInstance(-2, "RemainderFixture", "Remainder fixture", 0);
        var iron = GTMaterialRegistry.get("Fe");
        var method = PartRecipeLoader.class.getDeclaredMethod("pulverizedRemains", GTMaterial.class, long.class);
        method.setAccessible(true);
        source.setPulver(iron, GTValues.U / 2);
        var result = (ItemStack)method.invoke(null, source, GTValues.U / 4);
        var expected = stack(MaterialPrefix.dustDiv72, iron, 9);
        h.assertTrue(!expected.isEmpty() && same(result, expected) && result.getCount() == expected.getCount(),
                "different target at half ratio returns exact nine 1/72 dust");
        source.setPulver(iron, 0);
        h.assertTrue(((ItemStack)method.invoke(null, source, GTValues.U)).isEmpty(), "disabled target yields no dust");
        source.setPulver(iron, GTValues.U);
        h.assertTrue(((ItemStack)method.invoke(null, source, GTValues.U / 72 - 1)).isEmpty(),
                "source dust quantization does not round sub-1/72 remains up");
        h.assertTrue(((ItemStack)method.invoke(null, source, 0L)).isEmpty(), "lossless row gets no byproduct");
        h.succeed();
    }
}
