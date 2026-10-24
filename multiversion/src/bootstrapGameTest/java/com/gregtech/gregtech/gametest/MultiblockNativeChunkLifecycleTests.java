package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.LargeRecipeMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Real engine unload and staged FULL reload, not a simulated partial-unload/ticking test. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class MultiblockNativeChunkLifecycleTests {
    // Fixture construction/cleanup must not load exterior chunks through shape updates.
    private static final int FIXTURE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    @GameTest(template="test_empty", timeoutTicks=600)
    public static void registeredMultiblockSurvivesRealUnloadAndStagedReload(GameTestHelper h) {
        var fixture = new NativeChunkLifecycleFixture(h);
        // Install cleanup before registering the bus listener or touching remote chunks.
        attachCleanup(h, fixture);
        try {
            fixture.start();
            h.onEachTick(() -> {
                if (fixture.closed) return;
                try { fixture.poll(); }
                catch (RuntimeException | Error failure) { fixture.close(); throw failure; }
            });
        } catch (RuntimeException | Error failure) {
            fixture.close();
            throw failure;
        }
    }

    private static void attachCleanup(GameTestHelper h, NativeChunkLifecycleFixture fixture) {
        // Neither pinned helper exposes its testInfo. Read the actual harness by type;
        // no field name/remapping dependency and no replacement of engine state.
        try {
            GameTestInfo info = null;
            for (var field : GameTestHelper.class.getDeclaredFields()) {
                if (field.getType() != GameTestInfo.class) continue;
                field.setAccessible(true);
                info = (GameTestInfo) field.get(h);
                break;
            }
            if (info == null) throw new IllegalStateException("GameTestInfo cleanup hook unavailable");
            info.addListener(new GameTestListener() {
                @Override public void testStructureLoaded(GameTestInfo test) {}
                //? if >=1.21.1 {
                /*@Override public void testPassed(GameTestInfo test, net.minecraft.gametest.framework.GameTestRunner runner) { fixture.close(); }
                @Override public void testFailed(GameTestInfo test, net.minecraft.gametest.framework.GameTestRunner runner) { fixture.close(); }
                @Override public void testAddedForRerun(GameTestInfo oldTest, GameTestInfo newTest, net.minecraft.gametest.framework.GameTestRunner runner) { fixture.close(); }
                *///?} else {
                @Override public void testPassed(GameTestInfo test) { fixture.close(); }
                @Override public void testFailed(GameTestInfo test) { fixture.close(); }
                //?}
            });
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot install native GameTest completion cleanup", failure);
        }
    }

    /** Unique simple name also avoids Forge's generated subscriber-name collisions. */
    public static final class NativeChunkLifecycleFixture {
        private final GameTestHelper h;
        private final ServerLevel level;
        private final BlockPos origin = new BlockPos(18_000_015, 80, 18_000_008);
        private final ChunkPos controllerChunk = new ChunkPos(origin);
        private final ChunkPos shellChunk = new ChunkPos(origin.east());
        private final Set<ChunkPos> unloaded = new HashSet<>();
        private final java.util.List<BlockPos> cells = new java.util.ArrayList<>();
        private LargeRecipeMachineBlockEntity oldController;
        private MultiblockPortBlockEntity oldLocalPort;
        private MultiblockPortBlockEntity oldRemotePort;
        private long seededEnergy;
        private boolean registered;
        private boolean closed;
        private int phase;

        private NativeChunkLifecycleFixture(GameTestHelper h) { this.h = h; level = h.getLevel(); }

        @SubscribeEvent
        public void nativeMultiblockChunkUnloaded(ChunkEvent.Unload event) {
            if (event.getLevel() == level && (event.getChunk().getPos().equals(controllerChunk)
                    || event.getChunk().getPos().equals(shellChunk))) {
                unloaded.add(event.getChunk().getPos());
                System.out.println("[multiblock-native-lifecycle] phase=" + phase + " unload=" + event.getChunk().getPos());
            }
        }

        private void start() {
            h.assertTrue(!level.hasChunkAt(origin) && !level.hasChunkAt(origin.east()), "remote fixture chunks initially unavailable");
            // FULL/UNKNOWN33, never FORCED31: forced controller tickets retain adjacent
            // chunks at FULL and would invalidate the staged availability premise.
            //? if >=1.21.1 {
            /*NeoForge.EVENT_BUS.register(this);
            *///?} else {
            MinecraftForge.EVENT_BUS.register(this);
            //?}
            registered = true;
            level.getChunk(controllerChunk.x, controllerChunk.z);
            level.getChunk(shellChunk.x, shellChunk.z);
            var block = (BasicMachineBlock) BuiltInRegistries.BLOCK.stream()
                    .filter(b -> b instanceof BasicMachineBlock m && m.basicSpec().machineName().equals("largecentrifuge"))
                    .findFirst().orElseThrow();
            level.setBlock(origin, block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), FIXTURE_FLAGS);
            oldController = (LargeRecipeMachineBlockEntity) level.getBlockEntity(origin);
            Set<ChunkPos> actualChunks = new HashSet<>();
            actualChunks.add(controllerChunk);
            for (var cell : LargeMachineLayouts.cells("largecentrifuge")) {
                var pos = cell.at(origin, Direction.NORTH);
                cells.add(pos);
                actualChunks.add(new ChunkPos(pos));
                level.setBlock(pos, cell.block().defaultBlockState(), FIXTURE_FLAGS);
            }
            h.assertTrue(actualChunks.equals(Set.of(controllerChunk, shellChunk)), "registered native layout spans exactly two chunks");
            h.assertTrue(StructureTestSupport.ok(oldController), "registered centrifuge forms before unload");
            oldLocalPort = (MultiblockPortBlockEntity) level.getBlockEntity(origin.south());
            oldRemotePort = (MultiblockPortBlockEntity) level.getBlockEntity(origin.east());
            h.assertTrue(oldLocalPort.isBoundTo(origin) && oldRemotePort.isBoundTo(origin), "both native ports initially bound");
            oldController.inventory().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
            long size = oldController.getEnergySizeInputRecommended(oldController.spec().energyTag(), null);
            h.assertTrue(oldController.doInject(oldController.spec().energyTag(), null, size, 1, true) == 1,
                    "native energy injection seeds one legal packet");
            seededEnergy = oldController.getEnergyTick();
            h.assertTrue(seededEnergy > 0, "energy seed is nonzero");
            oldController.setChanged();
            phase = 1;
            System.out.println("[multiblock-native-lifecycle] phase=1 formed chunks=2 inventory=7 energy=" + seededEnergy);
            // No read of either remote chunk while waiting: UNKNOWN tickets expire naturally.
        }

        private void poll() {
            if (h.getTick() >= 550) h.fail("native unload deadline: phase=" + phase + " events=" + unloaded
                    + " removed=" + oldController.isRemoved() + " full=" + level.hasChunkAt(origin) + "/" + level.hasChunkAt(origin.east()));
            if (unloaded.size() != 2 || !oldController.isRemoved() || !oldLocalPort.isRemoved()
                    || !oldRemotePort.isRemoved() || level.hasChunkAt(origin) || level.hasChunkAt(origin.east())) return;
            phase = 2;
            System.out.println("[multiblock-native-lifecycle] phase=2 actual-unloads=2 old-removed=3 full=0 tick=" + h.getTick());
            // All reload assertions run in one callback, before UNKNOWN can expire.
            level.getChunk(controllerChunk.x, controllerChunk.z);
            h.assertTrue(!level.hasChunkAt(origin.east()), "controller-only FULL load leaves neighboring shell unavailable");
            var controller = (LargeRecipeMachineBlockEntity) level.getBlockEntity(origin);
            h.assertTrue(controller != null && controller != oldController && !controller.isRemoved(), "native reload creates fresh controller identity");
            assertSavedState(controller);
            var localPort = (MultiblockPortBlockEntity) level.getBlockEntity(origin.south());
            h.assertTrue(localPort != null && localPort != oldLocalPort, "native reload creates fresh local port");
            h.assertTrue(!StructureTestSupport.ok(controller), "partial native reload safely refuses incomplete structure");
            h.assertTrue(!localPort.isBoundTo(origin) && localPort.getEnergyTypes(Direction.DOWN).isEmpty(), "loaded port remains inactive without shell");
            h.assertTrue(!level.hasChunkAt(origin.east()), "validation and port query never force-load missing shell");
            phase = 3;
            System.out.println("[multiblock-native-lifecycle] phase=3 controller-reloaded shell-full=false saved-state=true");
            level.getChunk(shellChunk.x, shellChunk.z);
            var remotePort = (MultiblockPortBlockEntity) level.getBlockEntity(origin.east());
            h.assertTrue(remotePort != null && remotePort != oldRemotePort && !remotePort.isRemoved(), "shell reload creates fresh native port");
            h.assertTrue(StructureTestSupport.ok(controller), "complete native reload reforms registered structure");
            h.assertTrue(localPort.isBoundTo(origin) && remotePort.isBoundTo(origin), "fresh ports rebind without manual reset");
            h.assertTrue(localPort.getEnergyTypes(Direction.DOWN).contains(controller.spec().energyTag()), "energy port permissions recover");
            assertSavedState(controller);
            phase = 4;
            System.out.println("[multiblock-native-lifecycle] phase=4 reformed ports-rebound=2 inventory=7 energy=" + seededEnergy);
            close();
            h.succeed();
        }

        private void assertSavedState(LargeRecipeMachineBlockEntity controller) {
            var stack = controller.inventory().getStackInSlot(0);
            h.assertTrue(stack.is(Items.DIAMOND) && stack.getCount() == 7, "native chunk persistence retains exact inventory without reseeding");
            h.assertTrue(controller.getEnergyTick() == seededEnergy, "native chunk persistence retains exact energy without reseeding");
        }

        private void close() {
            if (closed) return;
            closed = true;
            if (registered) {
                //? if >=1.21.1 {
                /*NeoForge.EVENT_BUS.unregister(this);
                *///?} else {
                MinecraftForge.EVENT_BUS.unregister(this);
                //?}
            }
            // Never acquire cleanup tickets or load unavailable chunks on failure.
            // No forced tickets were acquired; any remaining UNKNOWN ticket expires.
            if (level.hasChunkAt(origin)) level.setBlock(origin, Blocks.AIR.defaultBlockState(), FIXTURE_FLAGS);
            for (var pos : cells) if (level.hasChunkAt(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), FIXTURE_FLAGS);
            System.out.println("[multiblock-native-lifecycle] cleanup phase=" + phase + " listener-removed=" + registered + " forced-tickets=0");
        }
    }
}
