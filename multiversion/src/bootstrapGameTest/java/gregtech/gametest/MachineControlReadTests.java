package gregtech.gametest;

import gregtech.api.machine.MachineControl;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.cover.MachineCoverSpec;
import gregtech.content.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Read-only control surface and the external switch input on a built-in machine. */
@GameTestHolder("gregtech_machine_control")
@PrefixGameTestTemplate(false)
public final class MachineControlReadTests {
    private MachineControlReadTests() {}

    /**
     * A plain single-block processing machine: it must consume energy, and it must not be a
     * controller or engine subclass that overrides the cover gate.
     */
    private static BasicMachineBlockEntity machine(GameTestHelper helper, BlockPos pos) {
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof BasicMachineBlock basic) || basic.basicSpec().energyIn() <= 0) continue;
            helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var entity = helper.getLevel().getBlockEntity(pos);
            if (entity != null && entity.getClass() == BasicMachineBlockEntity.class) {
                return (BasicMachineBlockEntity) entity;
            }
            helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        throw new IllegalStateException("no plain built-in processing machine");
    }

    private static void tick(BasicMachineBlockEntity machine) {
        BasicMachineBlockEntity.serverTick(machine.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void readsEnergyCapacityAndProgressWhileWritesStayClosed(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var machine = machine(helper, pos);
        var control = MachineControl.find(machine, null);
        helper.assertTrue(control != null && control.available(), "built-in machine exposes controls");
        helper.assertTrue(control.energy() == 0, "idle machine reports no buffered energy");
        helper.assertTrue(control.energyCapacity() > 0, "capacity comes from the machine spec");
        helper.assertTrue(control.progress() == 0 && control.progressMax() == 0, "no recipe means no progress");
        helper.assertTrue(control.progressPerTick() == 0, "idle machine adds no progress");
        helper.assertTrue(control.enabled() && !control.running() && !control.active(), "initial switch state");

        helper.assertTrue(!control.supportsWrite(), "built-in writes stay closed");
        helper.assertTrue(!control.setEnergy(100) && control.energy() == 0, "energy write is rejected");
        helper.assertTrue(!control.setProgress(100) && control.progress() == 0, "progress write is rejected");

        var spec = ((BasicMachineBlock) machine.getBlockState().getBlock()).basicSpec();
        long packet = Math.max(1, spec.energyInMax());
        machine.doEnergyInjection(spec.energyTag(), null, packet, packet, true);
        helper.assertTrue(control.energy() > 0, "stored energy becomes readable");
        helper.assertTrue(control.energy() <= control.energyCapacity(), "reported energy stays within capacity");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void externalSignalDrivesTheSwitchFaceAndPersists(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 4, 3));
        var machine = machine(helper, pos);
        var control = MachineControl.find(machine, null);
        helper.assertTrue(machine.attachCover(Direction.NORTH,
                new ItemStack(GTTechnological.get(MachineCoverSpec.REDSTONE.id))), "redstone switch attaches");

        tick(machine);
        helper.assertTrue(!control.enabled(), "unpowered switch keeps the machine stopped");
        helper.assertTrue(control.externalSignal(Direction.NORTH) == 0, "no external signal yet");

        helper.assertTrue(control.setExternalSignal(Direction.NORTH, 15), "external signal accepted");
        tick(machine);
        helper.assertTrue(control.enabled(), "external signal opens the switch face");
        helper.assertTrue(control.externalSignal(Direction.NORTH) == 15, "external signal reads back");
        helper.assertTrue(!control.setExternalSignal(Direction.NORTH, 16), "out-of-range signal is rejected");

        //? if >=1.21.1 {
        /*var saved = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
        control.setExternalSignal(Direction.NORTH, 0);
        machine.loadWithComponents(saved, helper.getLevel().registryAccess());
        *///?} else {
        var saved = machine.saveWithoutMetadata();
        control.setExternalSignal(Direction.NORTH, 0);
        machine.load(saved);
        //?}
        helper.assertTrue(control.externalSignal(Direction.NORTH) == 15, "external signal survives save/load");
        tick(machine);
        helper.assertTrue(control.enabled(), "reloaded signal still opens the switch");

        control.setExternalSignal(Direction.NORTH, 0);
        tick(machine);
        helper.assertTrue(!control.enabled(), "clearing the signal closes the switch again");
        helper.succeed();
    }
}
