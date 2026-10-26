package gregtech.gametest;

import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import gregtech.content.nuclear.ReactorCoolants;
import gregtech.content.registry.GTEnergyNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
//?}

/** Actual coolant capabilities and the BE packet handler used by the client renderer. */
@GameTestHolder("gt6ce_nuclear_fixes")
@PrefixGameTestTemplate(false)
public final class ReactorCoolantSyncTests {
    private static ReactorCoreBlockEntity core(GameTestHelper h, boolean four) {
        var block = four ? GTEnergyNodes.REACTOR_CORE_2X2.get() : GTEnergyNodes.REACTOR_CORE_BLOCK.get();
        var pos = h.absolutePos(new BlockPos(four ? 3 : 1, 2, 1));
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        return (ReactorCoreBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static ReactorCoreBlockEntity receive(GameTestHelper h, ReactorCoreBlockEntity server) {
        var client = (ReactorCoreBlockEntity) server.getType().create(server.getBlockPos(), server.getBlockState());
        h.assertTrue(client != null && client.getLevel() == null, "independent receiver has no server level");
        var packet = server.getUpdatePacket();
        h.assertTrue(packet != null && packet.getTag() != null, "actual native BE update packet exists");
//? if >=1.21.1 {
        /*client.onDataPacket(null, packet, h.getLevel().registryAccess());
*///?} else {
        client.onDataPacket(null, packet);
//?}
        return client;
    }

    @GameTest(template="test_empty")
    public static void everyCoolantSurvivesPacketAndHandRecovery(GameTestHelper h) {
        for (boolean four : new boolean[]{false, true}) {
            var server = core(h, four);
            for (var coolant : ReactorCoolants.values()) {
                var offered = new FluidStack(coolant.inputFluid(), 1234);
                h.assertTrue(server.fluids.fill(offered, FluidAction.SIMULATE) == 1234 && server.input.isEmpty(),
                        "simulation accepts coolant without rendering phantom content: " + coolant);
                h.assertTrue(receive(h, server).input.isEmpty(), "empty input remains empty in packet");
                h.assertTrue(server.fluids.fill(offered, FluidAction.EXECUTE) == 1234,
                        "actual core capability accepts coolant: " + coolant);
                var client = receive(h, server);
                h.assertTrue(client.input.getAmount() == 1234 && client.coolant() == coolant,
                        "packet preserves actual rendered cold coolant on either core size: " + coolant);
                h.assertTrue(server.handFluids.drain(1234, FluidAction.SIMULATE).getAmount() == 1234
                                && server.input.getAmount() == 1234,
                        "simulated hand recovery does not clear coolant");
                h.assertTrue(server.handFluids.drain(1234, FluidAction.EXECUTE).getAmount() == 1234,
                        "hand recovery empties cold input");
                var empty = receive(h, server);
                h.assertTrue(empty.input.isEmpty() && empty.coolant() == null,
                        "drained cold input clears renderer state on packet reception");
            }
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void coolingExhaustionAndOutputOnlyPacketClearColdSurface(GameTestHelper h) {
        for (boolean four : new boolean[]{false, true}) {
            var server = core(h, four);
            var coolant = ReactorCoolants.INDUSTRIAL;
            server.fluids.fill(new FluidStack(coolant.inputFluid(), 1), FluidAction.EXECUTE);
            var client = receive(h, server);
            h.assertTrue(client.input.getAmount() == 1, "even one litre renders source fixed coolant volume");
            server.storedHeat = coolant.heat;
            h.assertTrue(server.coolHeat() && server.input.isEmpty() && !server.output.isEmpty(),
                    "actual cooling consumes final cold litre while retaining hot output");
            var packet = server.getUpdatePacket();
//? if >=1.21.1 {
            /*client.onDataPacket(null, packet, h.getLevel().registryAccess());
*///?} else {
            client.onDataPacket(null, packet);
//?}
            h.assertTrue(client.input.isEmpty() && client.output.getAmount() == 1,
                    "existing client clears cold surface; hot output is never substituted as coolant");
        }
        h.succeed();
    }
}
