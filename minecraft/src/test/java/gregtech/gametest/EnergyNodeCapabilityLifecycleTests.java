package gregtech.gametest;

import gregtech.content.block.energy.EnergyNodeBlock;
import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.ChemicalBatterySpec;
import gregtech.content.registry.GTChemicalBatteries;
import gregtech.content.registry.GTFluids;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class EnergyNodeCapabilityLifecycleTests {
    private EnergyNodeCapabilityLifecycleTests() {}

    private static EnergyNodeBlock block(String id) {
        return (EnergyNodeBlock) BuiltInRegistries.BLOCK.get(Mc.gregtechId(id));
    }

    private static EnergyNodeBlockEntity place(GameTestHelper helper, String id) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var state = block(id).defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH);
        helper.getLevel().setBlockAndUpdate(pos, state);
        return (EnergyNodeBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    @Nullable
    private static IEnergyStorage rfPort(EnergyNodeBlockEntity node, @Nullable Direction side) {
//? if >=1.21.1 {
        /*return node.energyCapability(side);
*///?} else {
        return node.getCapability(ForgeCapabilities.ENERGY, side).orElse(null);
//?}
    }

    @GameTest(template = "test_empty")
    public static void removedBatteryBoxRejectsRetainedPorts(GameTestHelper helper) {
        var node = place(helper, "battery_box_lv");
        var level = helper.getLevel();
        var unsided = NativeItemInventories.at(level, node.getBlockPos(), null);
        var side = NativeItemInventories.at(level, node.getBlockPos(), Direction.SOUTH);
        var slots = node.batteryInventory();
        var cell = new ItemStack(GTChemicalBatteries.item(ChemicalBatterySpec.Chemistry.LEAD_ACID, 1));
        helper.assertTrue(unsided != null && side != null && side.insertItem(0, cell, true).isEmpty(),
                "the live battery box accepts the cell");
        level.setBlockAndUpdate(node.getBlockPos(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(!side.isItemValid(0, cell) && !unsided.isItemValid(0, cell)
                        && side.insertItem(0, cell, false).getCount() == 1
                        && unsided.insertItem(1, cell, false).getCount() == 1,
                "removed inventory ports return the offered battery");
        slots.setStackInSlot(0, cell);
        helper.assertTrue(slots.getStackInSlot(0).isEmpty() && slots.extractItem(0, 1, false).isEmpty(),
                "the old menu inventory cannot regain items");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fluxMotorPortUsesLiveControlsAndRemoval(GameTestHelper helper) {
        var node = place(helper, "flux_motor_lv");
        var side = rfPort(node, Direction.SOUTH);
        var unsided = rfPort(node, null);
        helper.assertTrue(rfPort(node, Direction.NORTH) == null && side != null && unsided != null,
                "the front is reserved for motor output");
        node.machineControl(null).setEnabled(false);
        helper.assertTrue(!side.canReceive() && side.receiveEnergy(8, false) == 0 && !unsided.canReceive(),
                "the stopped motor rejects RF");
        node.machineControl(null).setEnabled(true);
        helper.assertTrue(side.receiveEnergy(8, false) == 8 && node.stored() == 8, "the motor receives RF again");
        helper.getLevel().setBlockAndUpdate(node.getBlockPos(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(!unsided.canReceive() && unsided.receiveEnergy(8, true) == 0 && side.receiveEnergy(8, false) == 0
                        && unsided.getEnergyStored() == 0 && side.getMaxEnergyStored() == 0 && node.stored() == 8,
                "removed RF ports reject transfers and hide the old buffer");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void bufferedRfPortsFollowFacingAndSpec(GameTestHelper helper) {
        var state = block("flux_motor_lv").defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH);
        var node = new EnergyNodeBlockEntity(BlockPos.ZERO, state);
        var spec = node.spec();
        node.setSpec(EnergyNodeSpec.builder("test_rf_buffer", spec.material()).kind(EnergyNodeSpec.Kind.STORAGE)
                .texture(spec.textureFolder()).input(GregTechTags.Energy.RF, 8).output(spec.outType(), 4)
                .capacity(64).name("RF buffer").build());
        var south = rfPort(node, Direction.SOUTH);
        var unsided = rfPort(node, null);
        helper.assertTrue(south != null && unsided != null && south.receiveEnergy(12, false) == 12,
                "the generic RF port fills the buffer directly");
        node.setBlockState(state.setValue(DirectionalBlock.FACING, Direction.SOUTH));
        helper.assertTrue(rfPort(node, Direction.SOUTH) == null && !south.canReceive() && south.receiveEnergy(8, false) == 0
                        && south.getMaxEnergyStored() == 0 && unsided.canReceive() && unsided.getEnergyStored() == 12,
                "only the retained view of the new front loses access");
        node.setSpec(block("rotation_transformer_wood").spec());
        helper.assertTrue(rfPort(node, null) == null && !unsided.canReceive() && unsided.receiveEnergy(8, false) == 0
                        && unsided.getEnergyStored() == 0 && unsided.getMaxEnergyStored() == 0,
                "the old unsided RF port cannot access a non-RF device");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void turbineInletReflectsMissingTankAndRemoval(GameTestHelper helper) {
        var node = place(helper, "steam_turbine_bronze");
        var level = helper.getLevel();
//? if >=1.21.1 {
        /*var inlet = level.getCapability(Capabilities.FluidHandler.BLOCK, node.getBlockPos(), Direction.SOUTH);
*///?} else {
        var inlet = node.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.SOUTH).orElse(null);
//?}
        var steam = new FluidStack(GTFluids.still("Steam").get(), 9);
        helper.assertTrue(inlet != null && inlet.fill(steam, IFluidHandler.FluidAction.EXECUTE) == 9,
                "steam enters the rear inlet");
        NbtCompat.loadBlockEntity(node, new CompoundTag(), level.registryAccess());
        helper.assertTrue(inlet.getFluidInTank(0).isEmpty() && node.turbineSteamAmount() == 0
                        && inlet.getTankCapacity(0) == node.spec().inputRate() * 4,
                "a snapshot without a tank clears the retained inlet and preserves its rated capacity");
        level.setBlockAndUpdate(node.getBlockPos(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(inlet.getTanks() == 0 && inlet.getTankCapacity(0) == 0 && !inlet.isFluidValid(0, steam)
                        && inlet.fill(steam, IFluidHandler.FluidAction.EXECUTE) == 0,
                "a removed turbine exposes no tank through the old inlet");
        helper.succeed();
    }
}
