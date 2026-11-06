package gregtech.gametest;

import gregtech.api.multiblock.*;
import gregtech.content.blockentity.machine.*;
import gregtech.content.data.GregTechTags;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.content.multiblock.LargeMachineParts;
import gregtech.content.registry.MultiblockRegistration;
import gregtech.engine.multiblock.MultiblockLayout.Role;
import gregtech.engine.multiblock.StructurePortHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MultiblockPortApiTests {
    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fluidBackpressureChecksEveryMatchingOutputTank(GameTestHelper helper) {
        var map = new gregtech.engine.recipe.RecipeMap(null, "gt6ce_test_backpressure", "Test", "textures/gui/test",
                1, 0, 1, 0, 2, 0, 0, false, false, false, false);
        try {
            var recipe = new gregtech.engine.recipe.Recipe(new ItemStack[] {new ItemStack(Items.IRON_INGOT)},
                    new ItemStack[0], null, null, new FluidStack[0], new FluidStack[] {new FluidStack(Fluids.WATER, 100)}, 20, 16, 0);
            helper.assertTrue(map.addRecipe(recipe) != null, "Backpressure fixture recipe was rejected");
            var inventory = new gregtech.engine.machine.MachineInventory(map, 1, () -> {});
            inventory.items().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 2));
            inventory.tanksOutput()[0].setFluid(new FluidStack(Fluids.WATER, 100));
            inventory.tanksOutput()[1].setFluid(new FluidStack(Fluids.WATER, 16000));
            var owner = new net.minecraft.world.level.block.entity.FurnaceBlockEntity(helper.absolutePos(BlockPos.ZERO), Blocks.FURNACE.defaultBlockState());
            owner.setLevel(helper.getLevel());
            var config = new gregtech.engine.machine.RecipeProcessor.Config() {
                public gregtech.engine.recipe.RecipeMap recipeMap() { return map; }
                public boolean searchOutputTanks() { return false; }
                public boolean exclusiveOutputs() { return false; }
                public int parallelLimit() { return 1; }
                public boolean limitFluidOutputAccumulation() { return true; }
                public boolean parallelScalesDuration() { return false; }
                public boolean cheapOverclocking() { return false; }
                public boolean usesTimeEnergy() { return false; }
                public int efficiency() { return 10000; }
                public long inputMinimum() { return 1; }
                public long inputMaximum() { return 32; }
                public long nominalEnergyInput() { return 16; }
                public long startupEnergyFor(gregtech.engine.recipe.Recipe row) { return 0; }
            };
            var io = new gregtech.engine.machine.RecipeProcessor.IO() {
                public gregtech.engine.machine.MachineInventory inventory() { return inventory; }
                public net.minecraft.world.level.Level level() { return helper.getLevel(); }
                public net.minecraft.world.level.block.entity.BlockEntity owner() { return owner; }
                public ItemStack autocraftingProgram() { return ItemStack.EMPTY; }
                public void setStartupEnergy(long value) {}
                public void clearStartupEnergy() {}
                public void markJobStarted() {}
            };
            var gate = new gregtech.engine.machine.RecipeProcessor.Gate() {
                public boolean structureComplete() { return true; }
                public boolean switchesAllowRunning() { return true; }
                public boolean recipeStartAllowed() { return true; }
                public boolean lightAllowsStart() { return true; }
            };
            var processor = new gregtech.engine.machine.RecipeProcessor(config, io, gate);
            helper.assertTrue(processor.startJob() != gregtech.engine.machine.RecipeProcessor.StartResult.STARTED
                    && inventory.items().getStackInSlot(0).getCount() == 2,
                    "A full later tank was ignored or blocked work consumed inputs");
            inventory.tanksOutput()[1].drain(1, IFluidHandler.FluidAction.EXECUTE);
            processor.invalidate();
            helper.assertTrue(processor.startJob() == gregtech.engine.machine.RecipeProcessor.StartResult.STARTED
                    && inventory.items().getStackInSlot(0).getCount() == 1,
                    "Draining below the backpressure limit did not resume processing");
            helper.succeed();
        } finally {
            gregtech.engine.recipe.RecipeMap.unregister(map);
        }
    }

    @GameTest(template = "test_empty", timeoutTicks = 160)
    public static void activeItemAndFluidOutputsUseTheSamePortPolicies(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var controller = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce_api_test", "ports_api_test"));
        var receiver = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce_api_test", "ports_api_receiver"));
        var machine = new BehaviorMachineBlockEntity[1];
        var sequence = helper.startSequence();
        for (var front : Direction.Plane.HORIZONTAL) {
            var part = origin.relative(front.getClockWise());
            var target = part.relative(front.getOpposite());
            var blocked = part.relative(front);
            var shell = java.util.List.of(part, part.above(), origin.relative(front.getClockWise(), 2), origin.relative(front.getOpposite()));
            sequence.thenExecute(() -> {
                level.setBlockAndUpdate(origin, controller.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, front));
                machine[0] = (BehaviorMachineBlockEntity) level.getBlockEntity(origin);
                for (var pos : shell) level.setBlockAndUpdate(pos, LargeMachineParts.block(18009).defaultBlockState());
                level.setBlockAndUpdate(target, receiver.defaultBlockState());
                level.setBlockAndUpdate(blocked, receiver.defaultBlockState());
                ((StructureApiTestAddon.PortsBehavior) machine[0].behavior()).seed = true;
            }).thenWaitUntil(() -> helper.assertTrue(machine[0].isStructureOk()
                    && ((StructureApiTestAddon.PortsBehavior) machine[0].behavior()).seeded, "Active output fixture not formed"))
                    .thenExecute(() -> {
                        var itemOut = gregtech.engine.multiblock.StructureOutputs.at(machine[0], PortChannel.ITEM_OUTPUT, target, front, false);
                        var wrong = gregtech.engine.multiblock.StructureOutputs.at(machine[0], PortChannel.ITEM_OUTPUT, blocked, front.getOpposite(), false);
                        var source = machine[0].portItems(Role.ITEM_IO);
                        var coverHost = (gregtech.content.cover.PanelCoverHost) level.getBlockEntity(part);
                        var shutter = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "shutter_cover")));
                        gregtech.content.cover.CoverStackData.putBoolean(shutter, "gt.cover.inverted", true);
                        helper.assertTrue(coverHost.attachCover(front.getOpposite(), shutter)
                                && coverHost.panels().shuttered(front.getOpposite()), "Could not close the output face");
                        var fluidOut = gregtech.engine.multiblock.StructureOutputs.at(machine[0], PortChannel.FLUID_OUTPUT, target, front, false);
                        helper.assertTrue(gregtech.engine.multiblock.StructureOutputs.items(machine[0], itemOut, source, 0) == 0,
                                "Active items bypassed the source shutter");
                        helper.assertTrue(gregtech.engine.multiblock.StructureOutputs.fluid(machine[0], fluidOut, 3,
                                machine[0].portFluidTank(Role.FLUID_OUTPUT, 3), 250) == 0, "Active fluid bypassed the source shutter");
                        coverHost.removeCover(front.getOpposite());
                        helper.assertTrue(gregtech.engine.multiblock.StructureOutputs.items(machine[0], wrong, source, 0) == 0, "Active items ignored face rule");
                        helper.assertTrue(gregtech.engine.multiblock.StructureOutputs.items(machine[0], itemOut.restrict(PortRule.ALL.filter("minecraft:iron_ingot")), source, 0) == 0,
                                "Route and cell filters were combined as OR");
                        helper.assertTrue(gregtech.engine.multiblock.StructureOutputs.items(machine[0], itemOut, source, 0) == 4
                                && source.getStackInSlot(2).getCount() == 2, "Active item slot/filter selection failed");
                        helper.assertTrue(items(level, target, front).getStackInSlot(0).getCount() == 4, "Active items were lost");
                        helper.assertTrue(gregtech.engine.multiblock.StructureOutputs.fluid(machine[0], fluidOut, 2,
                                machine[0].portFluidTank(Role.FLUID_OUTPUT, 2), 250) == 0, "Active fluid ignored tank selection");
                        helper.assertTrue(gregtech.engine.multiblock.StructureOutputs.fluid(machine[0], fluidOut, 3,
                                machine[0].portFluidTank(Role.FLUID_OUTPUT, 3), 250) == 250, "Active fluid transfer failed");
                        helper.assertTrue(fluids(level, target, front).getFluidInTank(0).getAmount() == 250
                                && machine[0].portFluids(Role.FLUID_OUTPUT).getFluidInTank(3).getAmount() == 150,
                                "Active fluid transfer duplicated or lost contents");
                        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
//? if >=1.21.1 {
                        /*var eject = new net.neoforged.neoforge.items.ItemStackHandler(4);
*///?} else {
                        var eject = new net.minecraftforge.items.ItemStackHandler(4);
//?}
                        eject.setStackInSlot(3, new ItemStack(Items.COAL, 4));
                        helper.assertTrue(gregtech.engine.multiblock.StructureOutputs.items(machine[0], itemOut, eject, 0) == 0,
                                "A remote port unexpectedly dropped items into air");
                        var bounds = new net.minecraft.world.phys.AABB(target).inflate(0.1);
                        int before = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream()
                                .mapToInt(entity -> entity.getItem().getCount()).sum();
                        helper.assertTrue(gregtech.engine.multiblock.StructureOutputs.items(machine[0], itemOut.withAirDrop(), eject, 0) == 4
                                && eject.getStackInSlot(3).isEmpty(), "Face ejection failed or duplicated inventory");
                        int after = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream()
                                .mapToInt(entity -> entity.getItem().getCount()).sum();
                        helper.assertTrue(after - before == 4, "Face ejection lost items");
                        level.setBlockAndUpdate(part, Blocks.AIR.defaultBlockState());
                        helper.assertTrue(gregtech.engine.multiblock.StructureOutputs.fluid(machine[0], fluidOut, 3,
                                machine[0].portFluidTank(Role.FLUID_OUTPUT, 3), 250) == 0, "Broken port still actively emitted fluid");
                        for (var pos : shell) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
                        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                        level.setBlockAndUpdate(blocked, Blocks.AIR.defaultBlockState());
                    });
        }
        sequence.thenSucceed();
    }

    // Reloads also invalidate tagged structures, so run after the callback-count fixtures.
    @GameTest(template = "test_empty", batch = "structure_port_reload", timeoutTicks = 300)
    public static void cellPoliciesGuardActualCapabilitiesAndCachedViews(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce_api_test", "ports_api_test"));
        var machine = new BehaviorMachineBlockEntity[1];
        var sequence = helper.startSequence();
        for (var front : Direction.Plane.HORIZONTAL) {
            var portPos = origin.relative(front.getClockWise());
            var energyPos = origin.relative(front.getClockWise(), 2);
            sequence.thenExecute(() -> {
                level.setBlockAndUpdate(origin, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, front));
                machine[0] = (BehaviorMachineBlockEntity) level.getBlockEntity(origin);
                for (var pos : java.util.List.of(portPos, portPos.above(), energyPos, origin.relative(front.getOpposite())))
                    level.setBlockAndUpdate(pos, LargeMachineParts.block(18009).defaultBlockState());
                ((StructureApiTestAddon.PortsBehavior) machine[0].behavior()).seed = true;
            }).thenWaitUntil(() -> helper.assertTrue(machine[0].isStructureOk()
                    && ((StructureApiTestAddon.PortsBehavior) machine[0].behavior()).seeded, "Ports structure did not form: " + front))
                    .thenExecute(() -> {
                var input = items(level, portPos, front);
                var output = items(level, portPos, front.getOpposite());
                var fluidsIn = fluids(level, portPos, front);
                var fluidsOut = fluids(level, portPos, front.getOpposite());
                var iron = new ItemStack(Items.IRON_INGOT, 3);
                var water = new FluidStack(Fluids.WATER, 200);
                var lava = new FluidStack(Fluids.LAVA, 200);
                helper.assertTrue(input.insertItem(1, iron, true).isEmpty() && input.getStackInSlot(1).isEmpty(), "Item simulation mutated storage");
                helper.assertTrue(input.insertItem(0, iron, false).getCount() == 3, "Wrong slot accepted items");
                helper.assertTrue(input.insertItem(1, new ItemStack(Items.COAL), false).getCount() == 1, "Wrong item passed filter");
                helper.assertTrue(output.insertItem(1, iron, false).getCount() == 3, "Wrong face accepted items");
                helper.assertTrue(items(level, portPos, null).insertItem(1, iron, false).getCount() == 3, "Unsided item bypass");
                helper.assertTrue(input.insertItem(1, iron, false).isEmpty() && input.getStackInSlot(1).getCount() == 3, "Selected input slot failed");
                helper.assertTrue(input.extractItem(1, 3, false).isEmpty() && input.extractItem(3, 1, false).isEmpty(), "Input face extracted contents");
                helper.assertTrue(output.extractItem(2, 1, false).isEmpty(), "Non-selected output slot exposed");
                helper.assertTrue(output.extractItem(3, 1, false).is(Items.COAL), "Selected output slot failed");
                helper.assertTrue(fluidsIn.fill(water, IFluidHandler.FluidAction.SIMULATE) == 200
                        && fluidsIn.getFluidInTank(1).isEmpty(), "Fluid simulation mutated storage");
                helper.assertTrue(fluidsIn.fill(lava, IFluidHandler.FluidAction.EXECUTE) == 0, "Wrong fluid passed tag filter");
                helper.assertTrue(fluidsOut.fill(water, IFluidHandler.FluidAction.EXECUTE) == 0, "Wrong face filled tanks");
                helper.assertTrue(fluids(level, portPos, null).fill(water, IFluidHandler.FluidAction.EXECUTE) == 0, "Unsided fluid bypass");
                helper.assertTrue(fluidsIn.fill(water, IFluidHandler.FluidAction.EXECUTE) == 200, "Selected tank did not fill");
                helper.assertTrue(machine[0].portFluids(Role.FLUID_IO).getFluidInTank(0).isEmpty(), "Fluid went to first rather than selected tank");
                helper.assertTrue(fluidsIn.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Input face drained fluid");
                helper.assertTrue(fluidsOut.drain(water, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Output filter ignored requested fluid");
                var preview = fluidsOut.drain(1000, IFluidHandler.FluidAction.SIMULATE);
                helper.assertTrue(preview.getFluid() == Fluids.LAVA && preview.getAmount() == 400, "Output mixed fluids or used wrong tank");
                helper.assertTrue(fluidsOut.drain(200, IFluidHandler.FluidAction.EXECUTE).getFluid() == Fluids.LAVA, "Selected tank did not drain");
                helper.assertTrue(fluidsOut.getFluidInTank(3).getAmount() == 200, "Drain simulation consumed fluid");
                helper.assertTrue(fluids(level, portPos.above(), front).fill(water, IFluidHandler.FluidAction.EXECUTE) == 200, "Repeat cell lost its policy");
                helper.assertTrue(fluids(level, portPos.above(), front.getOpposite()).fill(water, IFluidHandler.FluidAction.EXECUTE) == 0, "Repeat cell used unrestricted policy");
                var behavior = (StructureApiTestAddon.PortsBehavior) machine[0].behavior();
                helper.assertTrue(behavior.itemFace == gregtech.api.machine.MachineFace.FRONT
                        && behavior.fluidFace == gregtech.api.machine.MachineFace.FRONT, "Behavior did not receive the actual part side");
                helper.assertTrue(machine[0].portItems(Role.ITEM_IO).insertItem(0, new ItemStack(Items.COAL), false).getCount() == 1,
                        "Sided override bypassed the machine-wide item policy");
                helper.assertTrue(machine[0].portFluids(Role.FLUID_INPUT).fill(lava, IFluidHandler.FluidAction.EXECUTE) == 0, "Behavior canFill bypassed");
                var combined = machine[0].portFluids(Role.FLUID_OUTPUT).drain(1000, IFluidHandler.FluidAction.SIMULATE);
                helper.assertTrue(combined.getFluid() == Fluids.WATER && combined.getAmount() == 300, "Unrestricted handler merged water and lava");
                var energy = (MultiblockPortBlockEntity) level.getBlockEntity(energyPos);
                var eu = GregTechTags.Energy.EU;
                helper.assertTrue(energy.isEnergyAcceptingFrom(eu, front.getClockWise(), false)
                        && !energy.isEnergyEmittingTo(eu, front.getClockWise(), false), "Mixed role confused energy input and output");
                helper.assertTrue(energy.doInject(eu, front, 32, 1, true) == 0
                        && energy.doInject(eu, null, 32, 1, true) == 0, "Energy side bypass");
                helper.assertTrue(energy.doInject(eu, front.getClockWise(), 32, 1, false) == 1
                        && energy.doInject(eu, front.getClockWise(), 32, 1, true) == 1, "Relative energy face did not rotate");
                helper.assertTrue(behavior.energyFace == gregtech.api.machine.MachineFace.LEFT, "Energy callback lost the actual part side");
                var selected = StructurePorts.builder().fluidInput(PortRule.ALL.slots(0, 1).filter("minecraft:water"))
                        .fluidOutput(PortRule.ALL.slots(2, 3).filter("minecraft:water", "minecraft:lava")).build();
                var multiple = StructurePortHandlers.fluids(() -> new StructurePortHandlers.View(machine[0], Role.FLUID_IO, selected, null));
                var larger = new FluidStack(Fluids.WATER, 2000);
                helper.assertTrue(multiple.fill(larger, IFluidHandler.FluidAction.SIMULATE) == 1600
                        && multiple.fill(larger, IFluidHandler.FluidAction.EXECUTE) == 1600, "Selected tanks were not filled together");
                var oneFluid = multiple.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                helper.assertTrue(oneFluid.getFluid() == Fluids.WATER && oneFluid.getAmount() == 300
                        && multiple.getFluidInTank(3).getAmount() == 200, "Filtered drain mixed different output fluids");
                level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
                helper.assertTrue(input.getSlots() == 0 && input.insertItem(1, new ItemStack(Items.IRON_INGOT, 3), false).getCount() == 3
                        && output.extractItem(3, 1, false).isEmpty(), "Cached item view survived removal");
                helper.assertTrue(fluidsIn.fill(water, IFluidHandler.FluidAction.EXECUTE) == 0
                        && fluidsOut.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Cached fluid view survived removal");
                for (var pos : java.util.List.of(portPos, portPos.above(), energyPos, origin.relative(front.getOpposite())))
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            });
        }
        var cached = new IFluidHandler[1];
        var name = gregtech.engine.addon.AddonMachines.structureName("gt6ce_api_test", "ports_api_test");
        var source = StructureApiTestAddon.portsLayout();
        var closed = new StructureLayout(name, source.cells().stream().map(cell -> {
            if (!cell.ports().rules().containsKey(PortChannel.FLUID_INPUT)) return cell;
            var rules = new java.util.EnumMap<PortChannel, PortRule>(PortChannel.class);
            rules.putAll(cell.ports().rules());
            rules.put(PortChannel.FLUID_INPUT, PortRule.NONE);
            return cell.withPorts(new StructurePorts(rules));
        }).toList(), source.repeats(), source.constraints());
        var portPos = origin.east();
        sequence.thenExecute(() -> {
            level.setBlockAndUpdate(origin, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            machine[0] = (BehaviorMachineBlockEntity) level.getBlockEntity(origin);
            for (var pos : java.util.List.of(portPos, portPos.above(), origin.east(2), origin.south()))
                level.setBlockAndUpdate(pos, LargeMachineParts.block(18009).defaultBlockState());
        }).thenWaitUntil(() -> helper.assertTrue(machine[0].isStructureOk(), "Reload fixture not formed"))
                .thenExecute(() -> {
                    cached[0] = fluids(level, portPos, Direction.NORTH);
                    helper.assertTrue(cached[0].fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE) == 1, "Original rule rejected fill");
                    gregtech.content.multiblock.StructureLayoutOverlayRuntime.applySynced(java.util.Map.of(name,
                            gregtech.engine.kubejs.StructureLayoutDocuments.canonical(closed)));
                    gregtech.engine.multiblock.tracking.StructureEpoch.bump(java.util.Set.of(name));
                }).thenWaitUntil(() -> helper.assertTrue(machine[0].isStructureOk(), "Reloaded structure not formed"))
                .thenExecute(() -> {
                    try {
                        helper.assertTrue(cached[0].fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE) == 0,
                                "Cached capability ignored reloaded policy");
                    } finally {
                        gregtech.content.multiblock.StructureLayoutOverlayRuntime.applySynced(java.util.Map.of());
                        gregtech.engine.multiblock.tracking.StructureEpoch.bump(java.util.Set.of(name));
                    }
                }).thenWaitUntil(() -> helper.assertTrue(machine[0].isStructureOk(), "Baseline structure not restored"))
                .thenExecute(() -> helper.assertTrue(cached[0].fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE) == 1,
                        "Cached capability did not recover with baseline policy"))
                .thenSucceed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 160)
    public static void outputPartsRespectSidesAndEnergyAccounting(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 1, 1));
        var controller = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce_api_test", "ports_api_test"));
        var receiver = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce_api_test", "ports_api_receiver"));
        level.setBlockAndUpdate(origin, controller.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        var machine = (BehaviorMachineBlockEntity) level.getBlockEntity(origin);
        var behavior = (StructureApiTestAddon.PortsBehavior) machine.behavior();
        for (var pos : java.util.List.of(origin.east(), origin.east().above(), origin.east(2), origin.south()))
            level.setBlockAndUpdate(pos, LargeMachineParts.block(18009).defaultBlockState());
        level.setBlockAndUpdate(origin.south(2), receiver.defaultBlockState());
        level.setBlockAndUpdate(origin.south().above(), receiver.defaultBlockState());
        var allowed = (BehaviorMachineBlockEntity) level.getBlockEntity(origin.south(2));
        var blocked = (BehaviorMachineBlockEntity) level.getBlockEntity(origin.south().above());
        var eu = gregtech.api.energy.EnergyType.EU;
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk(), "Output fixture not formed"))
                .thenExecute(() -> behavior.emit = true)
                .thenWaitUntil(() -> helper.assertTrue(behavior.emitted >= 0, "Output callback did not run"))
                .thenExecute(() -> {
                    helper.assertTrue(behavior.emitted == 32 && machine.getEnergyStored(eu, null) == 0, "Source energy accounting differs from accepted output");
                    helper.assertTrue(allowed.getEnergyStored(eu, null) == 32 && blocked.getEnergyStored(eu, null) == 0, "Energy left through the wrong part face");
                    level.setBlockAndUpdate(origin.south(2), Blocks.AIR.defaultBlockState());
                    behavior.directTarget = origin.south().above();
                    behavior.emitted = -1;
                    behavior.emit = true;
                }).thenWaitUntil(() -> helper.assertTrue(behavior.emitted >= 0, "Blocked output callback did not run"))
                .thenExecute(() -> {
                    helper.assertTrue(behavior.emitted == 0 && machine.getEnergyStored(eu, null) == 32, "Rejected energy was consumed");
                    helper.assertTrue(blocked.getEnergyStored(eu, null) == 0, "Blocked face accepted energy");
                    level.setBlockAndUpdate(origin.east(2), Blocks.AIR.defaultBlockState());
                    behavior.directTarget = null;
                    behavior.emitted = -1;
                    behavior.emit = true;
                }).thenWaitUntil(() -> helper.assertTrue(behavior.emitted >= 0, "Unformed output callback did not run"))
                .thenExecute(() -> helper.assertTrue(behavior.emitted == 0 && machine.getEnergyStored(eu, null) == 64,
                        "Unformed output consumed energy"))
                .thenSucceed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void recipeRolesAndInvalidPortDeclarationsAreChecked(GameTestHelper helper) {
        int checked = 0;
        for (var tracked : MultiblockRegistration.controllers().values()) {
            if (!(tracked instanceof LargeRecipeMachineBlockEntity machine)) continue;
            helper.assertTrue(machine.portFluids(Role.FLUID_INPUT) != null && machine.portFluids(Role.FLUID_IO) != null, "Recipe fluid roles not implemented");
            helper.assertTrue(machine.portItems(Role.ITEM_IO) != null && machine.portFluids(Role.ITEM_IO) == null, "Item-only role differs from API");
            var input = machine.portFluids(Role.FLUID_INPUT);
            var output = machine.portFluids(Role.FLUID_OUTPUT);
            helper.assertTrue(output.fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE) == 0
                    && input.drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Unidirectional role allowed reverse flow");
            for (int i = 0; i < machine.getTanks(); i++)
                helper.assertTrue(machine.portFluidTank(Role.ITEM_FLUID_IO, i) != null, "Recipe tank cannot be selected");
            checked++;
        }
        helper.assertTrue(checked > 0, "No recipe controllers checked");
        var machine = MultiblockRegistration.controllers().entrySet().stream()
                .filter(entry -> BuiltInRegistries.BLOCK.getKey(entry.getKey()).getPath().equals("ports_api_test"))
                .findFirst().orElseThrow().getValue();
        var invalid = new LargeMachineLayouts.Cell(1, 0, 0, 18009, Role.FLUID_INPUT, null, -1, java.util.List.of(),
                StructurePorts.builder().fluidInput(PortRule.ALL.slots(4)).build());
        expectInvalid(helper, () -> StructurePortHandlers.validate(machine, invalid), "outside handler size");
        var unknown = new LargeMachineLayouts.Cell(1, 0, 0, 18009, Role.ITEM_IO, null, -1, java.util.List.of(),
                StructurePorts.builder().itemInput(PortRule.ALL.filter("minecraft:unknown_port_filter")).build());
        expectInvalid(helper, () -> StructurePortHandlers.validate(machine, unknown), "Unknown ITEM_INPUT filter");
        var plain = new LargeMachineLayouts.Cell(1, 0, 0, 0, Role.ITEM_IO, "minecraft:stone", -1, java.util.List.of(),
                StructurePorts.builder().itemInput(PortRule.ALL).build());
        expectInvalid(helper, () -> StructurePortHandlers.validate(machine, plain), "controller-proxy part");
        helper.succeed();
    }

    private static void expectInvalid(GameTestHelper helper, Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError("Invalid port declaration was accepted");
        } catch (IllegalArgumentException expected) {
            helper.assertTrue(expected.getMessage().contains(message), "Wrong declaration error: " + expected.getMessage());
        }
    }
    private static IItemHandler items(ServerLevel level, BlockPos pos, Direction side) {
//? if >=1.21.1 {
        /*return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, side);
*///?} else {
        return level.getBlockEntity(pos).getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElseThrow(AssertionError::new);
//?}
    }
    private static IFluidHandler fluids(ServerLevel level, BlockPos pos, Direction side) {
//? if >=1.21.1 {
        /*return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side);
*///?} else {
        return level.getBlockEntity(pos).getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElseThrow(AssertionError::new);
//?}
    }
}
