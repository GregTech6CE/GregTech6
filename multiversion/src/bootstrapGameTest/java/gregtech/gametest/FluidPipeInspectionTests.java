package gregtech.gametest;

import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.tool.GTToolType;
import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import gregtech.content.item.GTToolItem;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class FluidPipeInspectionTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static FluidPipeBlockEntity place(GameTestHelper h, BlockPos pos, BlockState state, int amount) {
        h.assertTrue(h.getLevel().setBlock(pos, state, FLAGS), "registered pipe fixture placed");
        var pipe = (FluidPipeBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(pipe != null && pipe.fill(new FluidStack(Fluids.WATER, amount), FluidAction.EXECUTE) == amount,
                "native pipe accepts exact fixture fluid");
        return pipe;
    }

    private static void inspect(GameTestHelper h, BlockPos pos, Player player) {
        var state = h.getLevel().getBlockState(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(Direction.UP, .5), Direction.UP, pos, false);
//? if >=1.21.1 {
        /*h.assertTrue(state.useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "native magnifying-glass block interaction consumed");
*///?} else {
        h.assertTrue(state.use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "native magnifying-glass block interaction consumed");
//?}
    }

    private static void assertReport(GameTestHelper h, List<String> messages, int total) {
        h.assertTrue(messages.size() == 3 && messages.get(0).startsWith("100 L of ")
                && messages.get(1).equals("=== This Fluid Pipe Network contains: ===")
                && messages.get(2).startsWith(total + " L of "), "local content then exact loaded-network sum: " + messages);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void fluidPipeMagnifierSkipsUnloadedChunksAndReportsLoadedNetwork(GameTestHelper h) {
        var fixture = new PipeInspectionChunkFixture(h);
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

    private static void attachCleanup(GameTestHelper h, PipeInspectionChunkFixture fixture) {
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

    /** Unique subscriber name avoids native generated subscriber-name collisions. */
    public static final class PipeInspectionChunkFixture {
        private final GameTestHelper h;
        private final BlockPos origin = new BlockPos(21_600_014, 180, 21_600_008);
        private final BlockPos edge = origin.east();
        private final BlockPos outside = edge.east();
        private final ChunkPos ownerChunk = new ChunkPos(origin);
        private final ChunkPos outsideChunk = new ChunkPos(outside);
        private final List<BlockPos> touched = new ArrayList<>();
        private final List<String> messages = new ArrayList<>();
        private FakePlayer player;
        private FluidPipeBlock block;
        private FluidPipeBlockEntity first, second;
        private boolean registered, ownerUnloaded, outsideUnloaded, closed;

        private PipeInspectionChunkFixture(GameTestHelper h) { this.h = h; }

        @SubscribeEvent
        public void pipeInspectionExteriorChunkUnloaded(ChunkEvent.Unload event) {
            if (event.getLevel() != h.getLevel()) return;
            if (event.getChunk().getPos().equals(ownerChunk)) ownerUnloaded = true;
            if (event.getChunk().getPos().equals(outsideChunk)) outsideUnloaded = true;
        }

        private void start() {
            h.assertTrue(!h.getLevel().hasChunkAt(origin) && !h.getLevel().hasChunkAt(outside), "isolated remote fixture begins unloaded");
//? if >=1.21.1 {
            /*NeoForge.EVENT_BUS.register(this);
*///?} else {
            MinecraftForge.EVENT_BUS.register(this);
//?}
            registered = true;
            // Only transient native UNKNOWN tickets from placement. Let ALL
            // fixture chunks unload: a retained FULL owner also retains nearby
            // generation holders and would prevent actual exterior unload events.
            block = (FluidPipeBlock) BuiltInRegistries.BLOCK.stream()
                    .filter(b -> b instanceof FluidPipeBlock pipe && pipe.spec().capacity() >= 300).findFirst().orElseThrow();
            touched.add(origin);
            first = place(h, origin, block.defaultBlockState().setValue(FluidPipeBlock.EAST, true), 100);
            touched.add(edge);
            second = place(h, edge, block.defaultBlockState().setValue(FluidPipeBlock.WEST, true).setValue(FluidPipeBlock.EAST, true), 200);
            // Establish a real exterior lifecycle independently of placement's
            // auto-connect policy (which now correctly skips absent neighbors).
            // This FULL/UNKNOWN load adds no persistent or forced ticket.
            h.getLevel().getChunk(outsideChunk.x, outsideChunk.z);
            h.assertTrue(h.getLevel().hasChunkAt(outside), "explicit native exterior load precedes lifecycle wait");
            player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "PipeInspector")) {
                @Override public void sendSystemMessage(Component message) { messages.add(message.getString()); }
            };
            var glass = GTToolItem.create(GTToolType.MAGNIFYING_GLASS, GTMaterialRegistry.get("Glass"), GTMaterialRegistry.get("Wood"));
            h.assertTrue(!glass.isEmpty(), "registered magnifying glass exists");
            player.setItemInHand(InteractionHand.MAIN_HAND, glass);
            player.setPos(Vec3.atCenterOf(origin));
            player.setShiftKeyDown(false);
        }

        private void poll() {
            if (h.getTick() >= 550) h.fail("native unload deadline: owner=" + ownerUnloaded + " exterior=" + outsideUnloaded);
            // No remote state/BE reads while UNKNOWN tickets expire.
            if (!ownerUnloaded || !outsideUnloaded || !first.isRemoved() || !second.isRemoved()
                    || h.getLevel().hasChunkAt(origin) || h.getLevel().hasChunkAt(outside)) return;
            var oldFirst = first;
            var oldSecond = second;
            // Native saved-state hydration, not placement: only the owner returns
            // to FULL. This is the same staged reload boundary as the multiblock test.
            h.getLevel().getChunk(ownerChunk.x, ownerChunk.z);
            h.assertTrue(!h.getLevel().hasChunkAt(outside), "owner-only FULL reload leaves exterior absent");
            first = (FluidPipeBlockEntity) h.getLevel().getBlockEntity(origin);
            second = (FluidPipeBlockEntity) h.getLevel().getBlockEntity(edge);
            h.assertTrue(first != null && second != null && first != oldFirst && second != oldSecond
                    && !first.isRemoved() && !second.isRemoved(), "native reload creates fresh saved pipe entities");
            h.assertTrue(first.getBlockState().getValue(FluidPipeBlock.EAST)
                    && second.getBlockState().getValue(FluidPipeBlock.WEST)
                    && second.getBlockState().getValue(FluidPipeBlock.EAST), "saved connected path still reaches the absent exterior");
            inspect(h, origin, player);
            h.assertTrue(!h.getLevel().hasChunkAt(outside), "inspection does not load the unavailable connected chunk");
            assertReport(h, messages, 300);
            h.assertTrue(first.getFluidInTank(0).getAmount() == 100 && second.getFluidInTank(0).getAmount() == 200,
                    "inspection leaves both loaded tanks unchanged");
            touched.add(outside);
            var third = place(h, outside, block.defaultBlockState().setValue(FluidPipeBlock.WEST, true), 300);
            messages.clear();
            inspect(h, origin, player);
            assertReport(h, messages, 600);
            h.assertTrue(first.getFluidInTank(0).getAmount() == 100 && second.getFluidInTank(0).getAmount() == 200
                    && third.getFluidInTank(0).getAmount() == 300, "loaded positive control counts each pipe once without moving fluid");
            close();
            h.succeed();
        }

        private void close() {
            if (closed) return;
            closed = true;
            try {
                if (player != null) player.getInventory().clearContent();
                // No custom/forced tickets or world player were registered.
                // Native transient UNKNOWN tickets expire; never load for cleanup.
                for (var pos : touched) if (h.getLevel().hasChunkAt(pos)) {
                    if (h.getLevel().getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe)
                        for (int tank = 0; tank < pipe.getTanks(); tank++) pipe.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
                }
                for (var pos : touched) if (h.getLevel().hasChunkAt(pos))
                    h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            } finally {
                if (registered) {
//? if >=1.21.1 {
                    /*NeoForge.EVENT_BUS.unregister(this);
*///?} else {
                    MinecraftForge.EVENT_BUS.unregister(this);
//?}
                }
            }
        }
    }
}
