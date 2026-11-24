package gregtech.gametest;

import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.tool.ProcessingToolBlockEntity;
import gregtech.content.recipe.RecipeMapSpec;
import gregtech.engine.machine.BasicMachineSpec;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MachineRecipeExecutionTests {
    @GameTest(template = "test_empty")
    public static void mixingBowlUsesHighestExecutablePriority(GameTestHelper helper) {
        var bowl = (ProcessingToolBlockEntity) place(helper, "mixing_bowl");
        var map = bowl.recipes();
        var gold = mixerRow(Items.GOLD_INGOT, 1, 10);
        var diamond = mixerRow(Items.DIAMOND, 1, 20);
        var tooPowerful = mixerRow(Items.EMERALD, 64, 30);
        var missingInput = mixerRow(Items.IRON_INGOT, 1, 40);
        var resolved = mixerRow(Items.IRON_INGOT, 1, 40);
        resolved.itemInputs[0] = new ItemStack(Items.NETHER_STAR);
        missingInput.withRuntimeResolver(inputs -> resolved);
        var rows = List.of(gold, diamond, tooPowerful, missingInput);
        try {
            for (var row : rows) helper.assertTrue(map.addRecipe(row) == row, "Mixer fixture row rejected");
            var slots = bowl.inventory();
            slots.setStackInSlot(0, new ItemStack(Items.HEART_OF_THE_SEA, 3));
            slots.setStackInSlot(1, new ItemStack(Items.POPPED_CHORUS_FRUIT, 3));
            int output = map.itemInputSlots;
            helper.assertTrue(bowl.process(null) && slots.getStackInSlot(output).is(Items.DIAMOND),
                    "Bowl must choose the highest priority row it can power and consume");
            slots.setStackInSlot(output, new ItemStack(Items.GOLD_INGOT));
            helper.assertTrue(bowl.process(null) && slots.getStackInSlot(output).getCount() == 2
                    && slots.getStackInSlot(output).is(Items.GOLD_INGOT),
                    "A blocked higher priority output should allow the compatible row");
            slots.setStackInSlot(output, new ItemStack(Items.COBBLESTONE, 64));
            helper.assertTrue(!bowl.process(null) && slots.getStackInSlot(0).getCount() == 1
                    && slots.getStackInSlot(1).getCount() == 1, "Rejected recipes must keep their inputs");
        } finally {
            map.removeAll(rows);
        }
        helper.succeed();
    }

    private static Recipe mixerRow(Item output, int power, int priority) {
        return new Recipe(new ItemStack[]{new ItemStack(Items.HEART_OF_THE_SEA), new ItemStack(Items.POPPED_CHORUS_FRUIT)},
                new ItemStack[]{new ItemStack(output)}, null, null, null, 20, power, 0).withPriority(priority);
    }

    @GameTest(template = "test_empty")
    public static void mixingBowlAgreesWithNativeDyeRecipes(GameTestHelper helper) {
        var bowl = (ProcessingToolBlockEntity) place(helper, "mixing_bowl");
        var map = bowl.recipes();
//? if >=1.21.1 {
        /*var fluids = bowl.fluidHandler();
*///?} else {
        var fluids = bowl.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(AssertionError::new);
//?}
        var colors = DyeColor.values();
        var water = new FluidStack(Fluids.WATER, 144);
        for (int first = 0; first < colors.length; first++) {
            for (int second = first + 1; second < colors.length; second++) {
                var left = dye(colors[first]);
                var right = dye(colors[second]);
                var expected = map.findRecipe(List.of(left, right), List.of(water), false, map.itemInputSlots, map.itemOutputSlots);
                helper.assertTrue(expected != null && expected.fluidOutputs.length == 1, "Missing dye row for " + left + " and " + right);
                bowl.inventory().setStackInSlot(0, left);
                bowl.inventory().setStackInSlot(1, right);
                helper.assertTrue(fluids.fill(water.copy(), IFluidHandler.FluidAction.EXECUTE) == 144 && bowl.process(null),
                        "Bowl failed to process " + left + " and " + right);
                var actual = fluids.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                helper.assertTrue(actual.getFluid() == expected.fluidOutputs[0].getFluid() && actual.getAmount() == 216,
                        "Bowl and mixer disagreed for " + colors[first] + " and " + colors[second]);
            }
        }
        helper.succeed();
    }

    private static ItemStack dye(DyeColor color) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("minecraft", color.getName() + "_dye")));
    }

    @GameTest(template = "test_empty")
    public static void machineKeepsInputsWithoutOutputPorts(GameTestHelper helper) {
        var machine = (BasicMachineBlockEntity) place(helper, "compressor_bronze");
        var spec = machine.spec();
        helper.assertTrue(machine.getTanksOutput().length == 0, "Expected a compressor without an output tank");
        var row = new Recipe(new ItemStack[]{new ItemStack(Items.ECHO_SHARD)}, new ItemStack[]{new ItemStack(Items.DIAMOND)},
                null, null, new FluidStack[]{new FluidStack(Fluids.WATER, 1)}, 1, 1, 0).withPriority(20);
        assertRefusedWithoutConsumption(helper, machine, row);

        var noItemSlots = new RecipeMap(RecipeMapSpec.builder("gt.test.recipe.no_item_output", "Test", "textures/gui/test")
                .items(1, 0, 1).fluids(0, 1, 0).build());
        try {
            machine.setSpec(new BasicMachineSpec(spec.id(), spec.namespace(), spec.machineName(), spec.casing(), noItemSlots,
                    spec.energy(), spec.processing(), spec.layout(), spec.auxiliaryEnergy()));
            assertRefusedWithoutConsumption(helper, machine, row);
        } finally {
            machine.setSpec(spec);
            RecipeMap.unregister(noItemSlots);
        }
        helper.succeed();
    }

    private static void assertRefusedWithoutConsumption(GameTestHelper helper, BasicMachineBlockEntity machine, Recipe row) {
        var map = machine.recipeMap();
        try {
            helper.assertTrue(map.addRecipe(row) == row, "Fixture row was rejected");
            var items = machine.autoIOInventory().items();
            items.setStackInSlot(0, new ItemStack(Items.ECHO_SHARD));
            helper.assertTrue(machine.doInject(machine.spec().energyTag(), null, machine.spec().energyInMax(), 1, true) == 1,
                    "Compressor did not accept its rated energy packet");
            BasicMachineBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
            helper.assertTrue(items.getStackInSlot(0).is(Items.ECHO_SHARD) && machine.recipeDetails() == null
                    && machine.inspectionPendingFluids().isEmpty() && machine.inspectionPendingItemCount() == 0,
                    "Machine started a batch that cannot deliver all its products");
        } finally {
            map.removeAll(List.of(row));
        }
    }

    private static BlockEntity place(GameTestHelper helper, String id) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        return helper.getLevel().getBlockEntity(pos);
    }
}
