package gregtech.gametest;

import gregtech.content.block.energy.EnergyNodeBlock;
import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.content.data.GregTechTags;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.energy.ChemicalBatterySpec;
import gregtech.content.material.Materials;
import gregtech.content.registry.GTChemicalBatteries;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.inventory.NativeItemInventories;
import gregtech.engine.stack.NbtCompat;
import gregtech.platform.Mc;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.phys.AABB;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
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
public final class EnergyDeviceBehaviorTests {
    private EnergyDeviceBehaviorTests() {}

    private static EnergyNodeBlock energyBlock(String id) {
        return (EnergyNodeBlock) BuiltInRegistries.BLOCK.get(Mc.gregtechId(id));
    }

    private static EnergyNodeBlockEntity placeNode(GameTestHelper helper, String id) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var state = energyBlock(id).defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH);
        helper.getLevel().setBlockAndUpdate(pos, state);
        return (EnergyNodeBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    @Nullable
    private static IFluidHandler fluidPort(EnergyNodeBlockEntity node, @Nullable Direction side) {
//? if >=1.21.1 {
        /*return node.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, node.getBlockPos(), side);
*///?} else {
        return node.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
//?}
    }

    @GameTest(template = "test_empty")
    public static void rebindingBatteryBoxReleasesItsInventory(GameTestHelper helper) {
        var node = placeNode(helper, "battery_box_lv");
        var pos = node.getBlockPos();
        var level = helper.getLevel();
        var unsided = NativeItemInventories.at(level, pos, null);
        var rear = NativeItemInventories.at(level, pos, Direction.SOUTH);
        helper.assertTrue(node.batteryEnergy() != null && unsided != null && rear != null,
                "the box exposes its energy state and both inventory ports");
        var cell = new ItemStack(GTChemicalBatteries.item(ChemicalBatterySpec.Chemistry.LEAD_ACID, 1));
        helper.assertTrue(unsided.insertItem(0, cell, false).isEmpty(), "the inventory accepts a battery");
        node.setSpec(energyBlock("electric_motor_lv").spec());
        try {
            helper.assertTrue(node.batteryEnergy() == null && NativeItemInventories.at(level, pos, null) == null
                            && NativeItemInventories.at(level, pos, Direction.SOUTH) == null,
                    "the rebound motor no longer exposes the battery box");
            helper.assertTrue(rear.extractItem(0, 1, false).isEmpty()
                            && unsided.insertItem(1, cell, false).getCount() == 1,
                    "retained ports cannot extract or insert into the retired inventory");
            helper.assertTrue(countDrops(node, cell) == 1, "rebinding releases the installed battery");
        } finally {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void rebindingSteamTurbineRetiresItsInlets(GameTestHelper helper) {
        var node = placeNode(helper, "steam_turbine_bronze");
        var rear = fluidPort(node, Direction.SOUTH);
        var unsided = fluidPort(node, null);
        var legacyRotor = GTItems.getStack(MaterialPrefix.rotor, Materials.StainlessSteel, 1);
        var tag = new CompoundTag();
        var lookup = helper.getLevel().registryAccess();
        tag.put("gt.rotor", NbtCompat.saveItem(legacyRotor, lookup));
        NbtCompat.loadBlockEntity(node, tag, lookup);
        helper.assertTrue(node.hasRotor(), "the turbine restores its old rotor slot");
        var steam = new FluidStack(GTFluids.still("Steam").get(), 1);
        helper.assertTrue(rear != null && rear.fill(steam, IFluidHandler.FluidAction.SIMULATE) == 1
                        && unsided != null && unsided.fill(steam, IFluidHandler.FluidAction.SIMULATE) == 1,
                "both turbine inlets accept steam before rebinding");
        node.setSpec(energyBlock("electric_motor_lv").spec());
        try {
            helper.assertTrue(fluidPort(node, Direction.SOUTH) == null && fluidPort(node, null) == null,
                    "the rebound motor exposes no fluid ports");
            helper.assertTrue(rear.fill(steam, IFluidHandler.FluidAction.EXECUTE) == 0
                            && unsided.fill(steam, IFluidHandler.FluidAction.EXECUTE) == 0,
                    "retained inlets cannot fill the retired turbine");
            helper.assertTrue(countDrops(node, legacyRotor) == 1, "rebinding releases the old rotor");
        } finally {
            node.getLevel().setBlockAndUpdate(node.getBlockPos(), Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void removingBatteryBoxDropsItsInstalledCellOnce(GameTestHelper helper) {
        var node = placeNode(helper, "battery_box_lv");
        var battery = GTChemicalBatteries.item(ChemicalBatterySpec.Chemistry.LEAD_ACID, 1);
        helper.assertTrue(node.installBattery(new ItemStack(battery)), "the box accepts the cell");
        var slots = node.batteryInventory();
        var level = helper.getLevel();
        var pos = node.getBlockPos();
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(slots.getStackInSlot(0).isEmpty(), "removing the box clears its occupied slot");
        node.dropContents();
        helper.assertTrue(countDrops(node, new ItemStack(battery)) == 1,
                "block removal and repeated cleanup drop exactly one cell");
        helper.succeed();
    }

    private static int countDrops(EnergyNodeBlockEntity node, ItemStack expected) {
        return node.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(node.getBlockPos())).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(expected.getItem()))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    @GameTest(template = "test_empty")
    public static void deviceQueriesFollowTheBoundSpec(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var node = new EnergyNodeBlockEntity(pos, Blocks.STONE.defaultBlockState());
        helper.assertTrue(!node.isTransformer() && !node.isOriginalMotor() && !node.isTurbine(),
                "queries are safe before binding a spec");

        var transformer = (EnergyNodeBlock) BuiltInRegistries.BLOCK.get(Mc.gregtechId("rotation_transformer_wood"));
        node.setSpec(transformer.spec());
        helper.assertTrue(transformer.isTransformer() && node.isRotationTransformer() && node.isInvertible(),
                "block and entity recognize the same rotation transformer");

        var motor = (EnergyNodeBlock) BuiltInRegistries.BLOCK.get(Mc.gregtechId("electric_motor_lv"));
        node.setSpec(motor.spec());
        helper.assertTrue(motor.isOriginalMotor() && node.isOriginalMotor() && !node.isTransformer(),
                "rebinding uses the current spec");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void rotationTransformerWaitsForWorldBeforeEmitting(GameTestHelper helper) {
        var block = BuiltInRegistries.BLOCK.get(Mc.gregtechId("rotation_transformer_wood"));
        var state = block.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH);
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var node = new EnergyNodeBlockEntity(pos, state);
        var ru = GregTechTags.Energy.RU;
        long size = node.spec().inputRate();
        helper.assertTrue(node.doEnergyInjection(ru, Direction.NORTH, -size, 1, true) == 1,
                "rotation transformer accepts the signed input packet");
        EnergyNodeBlockEntity.serverTick(helper.getLevel(), pos, state, node);
        helper.assertTrue(node.stored() == size, "a detached transformer must keep its input");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void customCoolerUsesDeclaredBehavior(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = BuiltInRegistries.BLOCK.get(Mc.gregtechId("electric_cooler_lv"));
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var state = block.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH);
        level.setBlockAndUpdate(pos, state);
        var node = (EnergyNodeBlockEntity) level.getBlockEntity(pos);
        var oldControl = node.machineControl(null);
        var eu = GregTechTags.Energy.EU;
        var cold = GregTechTags.Energy.CU;
        var heat = GregTechTags.Energy.HU;
        var spec = EnergyNodeSpec.builder("test_conditioner", node.spec().material())
                .deviceType(EnergyNodeSpec.DeviceType.COOLER)
                .texture(node.spec().textureFolder())
                .input(eu, 32)
                .output(cold, 8)
                .capacity(64)
                .name("Test Conditioner")
                .build();
        node.setSpec(spec);

        helper.assertTrue(node.isEnergyEmittingTo(cold, Direction.NORTH, false)
                        && node.isEnergyEmittingTo(heat, Direction.SOUTH, false)
                        && !node.isEnergyEmittingTo(heat, Direction.NORTH, false),
                "the cooler has distinct front cold and rear heat outputs");
        oldControl.setMode(4);
        helper.assertTrue(node.machineControl(null).mode() == 4, "the retained control updates the new converter");
        helper.assertTrue(node.doEnergyInjection(eu, Direction.SOUTH, 32, 1, true) == 0
                        && node.doEnergyInjection(eu, Direction.EAST, 32, 1, false) == 1 && node.stored() == 0,
                "only lateral inputs accept simulated packets");
        helper.assertTrue(node.doEnergyInjection(eu, Direction.EAST, 32, 1, true) == 1 && node.stored() == 32,
                "an actual packet fills the converter buffer");
        EnergyNodeBlockEntity.serverTick(level, pos, state, node);
        helper.assertTrue(oldControl.running() && node.stored() == 0,
                "the converter consumes its input even without an output receiver");
        helper.succeed();
    }
}
