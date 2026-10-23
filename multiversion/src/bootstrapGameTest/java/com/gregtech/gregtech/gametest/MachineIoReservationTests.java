package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.cover.CoverStackData;
import com.gregtech.gregtech.content.cover.CoverUtilityBehaviors;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
//?}

/** Actual registered machine methods with isolated recipe maps; no mutation of survival recipes. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class MachineIoReservationTests {
    private static BasicMachineBlockEntity machine(GameTestHelper h, String suffix) {
        var block = (BasicMachineBlock) BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof BasicMachineBlock basic && basic.basicSpec().machineName().equals("crusher"))
                .findFirst().orElseThrow();
        var pos = h.absolutePos(new BlockPos(2, 3, 2));
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var machine = (BasicMachineBlockEntity) h.getLevel().getBlockEntity(pos);
        var map = new RecipeMap(null, "gt.recipe.test.machine_io_" + suffix + "_" + java.util.UUID.randomUUID(),
                "Machine IO fixture", "test", 1, 1, 1, 0, 2, 0, 1, false, false, false, false);
        machine.setSpec(BasicMachineSpec.builder("machine_io_fixture", block.basicSpec().material())
                .machineType("crusher").energy(GregTechTags.Energy.TU, 1).energyRange(1, 1)
                .processingEfficiency(10000).parallel(1).recipes(map).faces(FaceConfig.ALL_SIDES).build());
        return machine;
    }

    private static Recipe recipe(FluidStack... outputs) {
        return new Recipe(new ItemStack[]{new ItemStack(Items.STICK)}, new ItemStack[0],
                null, null, null, outputs, 4, 1, 0);
    }

    private static Object invoke(BasicMachineBlockEntity machine, String name, Class<?>[] types, Object... args) {
        try {
            var method = BasicMachineBlockEntity.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(machine, args);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Actual machine method failed: " + name, failure);
        }
    }

    private static boolean fits(BasicMachineBlockEntity machine, Recipe recipe, int parallel) {
        return (boolean) invoke(machine, "fluidsFit", new Class<?>[]{Recipe.class, int.class}, recipe, parallel);
    }

    private static void auto(BasicMachineBlockEntity machine, String method, int side) {
        invoke(machine, method, new Class<?>[]{Level.class, BlockPos.class, int.class},
                machine.getLevel(), machine.getBlockPos(), side);
    }

    @GameTest(template="test_empty")
    public static void distinctFluidReservationsBlockStartWithoutConsumingInputs(GameTestHelper h) {
        var machine = machine(h, "distinct");
        var outputs = machine.getTanksOutput();
        var row = recipe(new FluidStack(Fluids.WATER, 2000), new FluidStack(Fluids.LAVA, 1000));
        machine.recipeMap().mRecipeList.add(row);
        outputs[1].setFluid(new FluidStack(Fluids.WATER, 8000));
        machine.inventory().setStackInSlot(0, new ItemStack(Items.STICK));
        invoke(machine, "checkRecipe", new Class<?>[0]);
        h.assertTrue(machine.inventory().getStackInSlot(0).getCount() == 1,
                "distinct outputs cannot reserve the sole empty tank: input remains untouched");
        h.assertTrue(machine.machineControl(null).progressMax() == 0 && outputs[0].isEmpty()
                && outputs[1].getAmount() == 8000, "blocked start does not create job or mutate tanks");
        outputs[1].setEmpty();
        invoke(machine, "checkRecipe", new Class<?>[0]);
        h.assertTrue(machine.inventory().getStackInSlot(0).isEmpty(), "two empty tanks admit the job");
        h.assertTrue(machine.spec().energyTag() == GregTechTags.Energy.TU
                && machine.spec().energyInMin() == 1 && machine.spec().energyInMax() == 1
                && machine.machineControl(null).progressMax() == 4,
                "fixture is a four-TU job; tickServer supplies one TU before the power gate");
        for (int tick = 0; tick < 5; tick++)
            BasicMachineBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
        h.assertTrue(outputs[0].getAmount() == 2000 && outputs[1].getAmount() == 1000,
                "actual processing flushes both reserved fluid identities");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void fluidReservationsMergeSplitAndRespectFullIdentity(GameTestHelper h) {
        var machine = machine(h, "controls");
        var tanks = machine.getTanksOutput();
        tanks[1].setFluid(new FluidStack(Fluids.LAVA, 8000));
        h.assertTrue(fits(machine, recipe(new FluidStack(Fluids.WATER, 2000), new FluidStack(Fluids.WATER, 1000)), 1),
                "same fluid outputs share one empty tank");
        h.assertTrue(!fits(machine, recipe(new FluidStack(Fluids.WATER, 5000)), 2), "parallel quantities respect capacity");
        tanks[0].setFluid(new FluidStack(Fluids.WATER, 7900));
        h.assertTrue(!fits(machine, recipe(new FluidStack(Fluids.WATER, 200)), 1), "partial matching tank cannot overfill");
        tanks[1].setEmpty();
        h.assertTrue(fits(machine, recipe(new FluidStack(Fluids.WATER, 200)), 1), "one output splits across matching and empty tanks");
        tanks[0].setEmpty();
        tanks[1].setFluid(new FluidStack(Fluids.LAVA, 8000));
        var first = new FluidStack(Fluids.WATER, 1000);
        var second = new FluidStack(Fluids.WATER, 1000);
//? if >=1.21.1 {
/*        first.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("first"));
        second.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("second"));
*///?} else {
        var firstTag = new CompoundTag(); firstTag.putString("fixture", "first"); first.setTag(firstTag);
        var secondTag = new CompoundTag(); secondTag.putString("fixture", "second"); second.setTag(secondTag);
//?}
        h.assertTrue(!fits(machine, recipe(first, second), 1), "different tags/components cannot reserve the same tank");
        h.assertTrue(tanks[0].isEmpty() && tanks[1].getAmount() == 8000, "all planning remains non-mutating");
        h.succeed();
    }

    private static void configure(GameTestHelper h, ItemStack filter, boolean inverted) {
        var tag = CoverStackData.readOrEmpty(filter);
//? if >=1.21.1 {
/*        tag.put("gt.filter.item", new ItemStack(Items.STICK).save(h.getLevel().registryAccess()));
*///?} else {
        tag.put("gt.filter.item", new ItemStack(Items.STICK).save(new CompoundTag()));
//?}
        tag.putBoolean("gt.cover.inverted", inverted);
        CoverStackData.write(filter, tag);
    }

    @GameTest(template="test_empty")
    public static void activeItemInputRespectsOwnWhitelistAndBlacklist(GameTestHelper h) {
        var machine = machine(h, "input");
        var sourcePos = machine.getBlockPos().above();
        h.getLevel().setBlockAndUpdate(sourcePos, Blocks.CHEST.defaultBlockState());
        var source = (ChestBlockEntity) h.getLevel().getBlockEntity(sourcePos);
        source.setItem(0, new ItemStack(Items.STICK, 4));
        h.assertTrue(machine.attachCover(Direction.UP, new ItemStack(GTTechnological.get(CoverUtilityBehaviors.FILTER_ITEM))), "attach real top item filter");
        auto(machine, "autoInputItems", FaceConfig.TOP);
        h.assertTrue(source.getItem(0).getCount() == 4 && machine.inventory().getStackInSlot(0).isEmpty(), "unconfigured whitelist blocks active input");
        configure(h, machine.getCover(Direction.UP), true);
        auto(machine, "autoInputItems", FaceConfig.TOP);
        h.assertTrue(source.getItem(0).getCount() == 4 && machine.inventory().getStackInSlot(0).isEmpty(), "matching blacklist blocks active input");
        configure(h, machine.getCover(Direction.UP), false);
        auto(machine, "autoInputItems", FaceConfig.TOP);
        h.assertTrue(source.getItem(0).isEmpty() && machine.inventory().getStackInSlot(0).getCount() == 4, "matching whitelist transfers exactly four");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void activeItemOutputAndAirEjectionRespectOwnFilter(GameTestHelper h) {
        var machine = machine(h, "output");
        var targetPos = machine.getBlockPos().below();
        h.getLevel().setBlockAndUpdate(targetPos, Blocks.CHEST.defaultBlockState());
        var target = (ChestBlockEntity) h.getLevel().getBlockEntity(targetPos);
        machine.inventory().setStackInSlot(1, new ItemStack(Items.STICK, 4));
        h.assertTrue(machine.attachCover(Direction.DOWN, new ItemStack(GTTechnological.get(CoverUtilityBehaviors.FILTER_ITEM))), "attach real bottom item filter");
        auto(machine, "autoOutputItems", FaceConfig.BOTTOM);
        h.assertTrue(target.isEmpty() && machine.inventory().getStackInSlot(1).getCount() == 4, "own filter blocks active output to chest");
        configure(h, machine.getCover(Direction.DOWN), false);
        auto(machine, "autoOutputItems", FaceConfig.BOTTOM);
        h.assertTrue(target.getItem(0).getCount() == 4 && machine.inventory().getStackInSlot(1).isEmpty(), "allowed output conserves four items");
        target.clearContent();
        h.getLevel().setBlockAndUpdate(targetPos, Blocks.AIR.defaultBlockState());
        var area = new AABB(targetPos).inflate(1);
        for (var item : h.getLevel().getEntitiesOfClass(ItemEntity.class, area)) item.discard();
        machine.inventory().setStackInSlot(1, new ItemStack(Items.STICK, 4));
        configure(h, machine.getCover(Direction.DOWN), true);
        auto(machine, "autoOutputItems", FaceConfig.BOTTOM);
        h.assertTrue(machine.inventory().getStackInSlot(1).getCount() == 4
                && h.getLevel().getEntitiesOfClass(ItemEntity.class, area).isEmpty(), "blacklist blocks air ejection without removing source");
        configure(h, machine.getCover(Direction.DOWN), false);
        auto(machine, "autoOutputItems", FaceConfig.BOTTOM);
        int emitted = h.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream()
                .filter(item -> item.getItem().is(Items.STICK)).mapToInt(item -> item.getItem().getCount()).sum();
        h.assertTrue(machine.inventory().getStackInSlot(1).isEmpty() && emitted == 4, "allowed air ejection emits exactly four");
        h.succeed();
    }
}
