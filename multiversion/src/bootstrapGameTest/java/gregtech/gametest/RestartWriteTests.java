package gregtech.gametest;

import gregtech.content.blockentity.tool.BumbliaryBlockEntity;
import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import gregtech.content.bumble.*;
import gregtech.content.nuclear.ReactorCoolants;
import gregtech.content.registry.GTBumbleBlocks;
import gregtech.content.registry.GTEnergyNodes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.UUID;
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

/** Phase 1 only: construct fixture, perform pure progress tick, then let normal server shutdown save. */
@GameTestHolder("gregtech_restart_write")
@PrefixGameTestTemplate(false)
public final class RestartWriteTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void writeDisposableFixture(GameTestHelper h) throws Exception {
        String enabled = System.getProperty("neoforge.enabledGameTestNamespaces",
                System.getProperty("forge.enabledGameTestNamespaces", ""));
        h.assertTrue("gregtech_restart_write".equals(enabled), "exact single phase namespace required");
        h.assertTrue(!RestartFixture.warm, "write phase must start cold");
        RestartFixture.warm = true;
        var world = RestartFixture.world(h);
        h.assertTrue(!Files.exists(world.resolve(RestartFixture.MARKER)), "phase 1 cannot overwrite a previous fixture");
        // A write attempt is one-shot even if setup or later assertions fail. Reader never consumes this as state.
        Files.writeString(world.resolve("gt6-restart-write-attempt.txt"),
                "writerPid=" + ProcessHandle.current().pid() + "\n", StandardOpenOption.CREATE_NEW);
        RestartFixture.nonTicking(h);
        prepareSealedCheckpoint(h);
        String nonce = UUID.randomUUID().toString();
        for (int i = 0; i < 4; i++) {
            var pos = RestartFixture.beePos(i);
            h.assertTrue(h.getLevel().isEmptyBlock(pos), "fresh empty apiary location required");
            var block = (i >= 2 ? GTBumbleBlocks.ADVANCED_BUMBLIARY : GTBumbleBlocks.BUMBLIARY).get();
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var bee = (BumbliaryBlockEntity) h.getLevel().getBlockEntity(pos);
            boolean queen = i % 2 == 0;
            bee.inventory().setStackInSlot(bee.layout().royal(), BumbleBeeType.stack(GTBumbleSpecies.byId(30),
                    BumbleBeeType.PRINCESS, RestartFixture.genes(nonce), 1));
            if (queen) bee.inventory().setStackInSlot(bee.layout().drone(), BumbleBeeType.stack(
                    GTBumbleSpecies.byId(30), BumbleBeeType.DRONE, RestartFixture.genes(nonce), 1));
            // Real native countdown and breeding; never inject BE NBT or private progress fields.
            for (int tick = 0; tick < (queen ? 1201 : 1101); tick++) bee.serverTick();
        }
        for (int i = 0; i < 2; i++) {
            var pos = RestartFixture.reactorPos(i);
            h.assertTrue(h.getLevel().isEmptyBlock(pos), "fresh empty reactor location required");
            var block = (i == 0 ? GTEnergyNodes.REACTOR_CORE_BLOCK : GTEnergyNodes.REACTOR_CORE_2X2).get();
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var reactor = (ReactorCoreBlockEntity) h.getLevel().getBlockEntity(pos);
            reactor.setStopped(true);
            h.assertTrue(reactor.output.fill(new FluidStack(ReactorCoolants.DISTILLED.outputFluid(), 64320),
                    FluidAction.EXECUTE) == 64320, "native expanded Steam fill");
        }
        RestartFixture.assertState(h, nonce);
        Properties marker = new Properties();
        marker.setProperty("world", world.toString());
        marker.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        marker.setProperty("nonce", nonce);
        marker.setProperty("contract", "gt6-bee-reactor-restart-v3-y304");
        RestartFixture.recordState(h, marker);
        try (var output = Files.newOutputStream(world.resolve(RestartFixture.MARKER), StandardOpenOption.CREATE_NEW)) {
            marker.store(output, "Phase 1 assertions passed; NOT proof that shutdown save completed");
        }
        com.mojang.logging.LogUtils.getLogger().info("GT6_RESTART_WRITE world={} pid={} nonce={}", world, ProcessHandle.current().pid(), nonce);
        h.succeed();
    }
    /** Phase1 ONLY, after explicit disposable-world and one-shot attempt guards. Never called by reader. */
    private static void prepareSealedCheckpoint(GameTestHelper h) {
        // Fixed 9 x 4 x 9 chamber (324 blocks), not a terrain-dependent air assumption.
        // Remote FULL loads do not add ticking tickets; assert every touched chunk, including boundary chunks.
        for (int x : new int[]{-1, 7}) for (int z : new int[]{-1, 7}) {
            var pos = RestartFixture.BASE.offset(x, 0, z);
            h.getLevel().getChunkAt(pos);
            h.assertTrue(!h.getLevel().getChunkSource().isPositionTicking(
                    new net.minecraft.world.level.ChunkPos(pos).toLong()), "setup chunk must not naturally tick");
        }
        // Preflight the ENTIRE fixed high-air footprint before changing anything.
        // High altitude is not a universal terrain guarantee: reject ANY natural block or BE, never clear one.
        for (int x = -1; x <= 7; x++) for (int y = -1; y <= 2; y++) for (int z = -1; z <= 7; z++) {
            var pos = RestartFixture.BASE.offset(x, y, z);
            h.assertTrue(h.getLevel().getBlockEntity(pos) == null, "checkpoint setup refuses existing block entity at " + pos);
            h.assertTrue(h.getLevel().isEmptyBlock(pos), "reserved high-air checkpoint refuses natural block at " + pos);
        }
        for (int i = 0; i < 6; i++) {
            var pos = i < 4 ? RestartFixture.beePos(i) : RestartFixture.reactorPos(i - 4);
            com.mojang.logging.LogUtils.getLogger().info("GT6_RESTART_SETUP original pos={} block={}",
                    pos, h.getLevel().getBlockState(pos));
        }
        // Install the shell first, then its air interior. Ordinary native block placement only; no drops/NBT.
        for (boolean shellPass : new boolean[]{true, false}) {
            for (int x = -1; x <= 7; x++) for (int y = -1; y <= 2; y++) for (int z = -1; z <= 7; z++) {
                boolean shell = x == -1 || x == 7 || y == -1 || y == 2 || z == -1 || z == 7;
                if (shell != shellPass) continue;
                var pos = RestartFixture.BASE.offset(x, y, z);
                var block = shell ? net.minecraft.world.level.block.Blocks.STONE : net.minecraft.world.level.block.Blocks.AIR;
                h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
                h.assertTrue(h.getLevel().getBlockState(pos).is(block), "native checkpoint placement at " + pos);
            }
        }
        RestartFixture.nonTicking(h);
    }

}
