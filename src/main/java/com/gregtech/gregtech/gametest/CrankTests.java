package com.gregtech.gregtech.gametest;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.block.tool.CrankBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTToolBlocks;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Guards the Hand Crank ({@code GTToolBlocks.CRANK} / {@link CrankBlock}) against GT6's
 * {@code MultiTileEntityCrank} (multi-tile {@code 32111}, {@code Loader_MultiTileEntities:2105}).
 *
 * <p>The bug report this test answers: right-clicking a free standing crank used to answer
 * {@code message.gregtech.crank.no_target} ("mount the crank on an RU machine") and did nothing else,
 * although GT6's crank is a redstone lever that turns anywhere. GT6's own rules, all asserted here:</p>
 * <ul>
 *   <li>every right click turns the crank, with or without a machine
 *       ({@code MultiTileEntityCrank:103-112}),</li>
 *   <li>a turned crank powers its mount with weak and strong signal 15; the original
 *       OPOS[mFacing] is a query side (TileEntityBase06Covers:405), not an emission direction,</li>
 *   <li>a crank on an RU machine hands the machine GT6's {@code -divup(8L * pot2Strength, pot1Weakness)}
 *       packet ({@code :78}); a bare player has strength {@code 2} and weakness {@code 1}, i.e. -16 RU,</li>
 *   <li>GT6 shows no chat message for any of this, so neither does the port.</li>
 * </ul>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class CrankTests {

    /** A far away, flat corner of the test world; every test uses its own slot. */
    private static final int BASE_X = 39200;
    private static final int BASE_Y = 210;
    private static final int BASE_Z = 39200;

    /** The handle direction every test cranks: the mount (wall or machine) sits south of the crank. */
    private static final Direction HANDLE = Direction.NORTH;

    /** GT6's packet for a player without potion effects: -divup(8 * 2, 1) = -16 RU. */
    private static final long CRANK_RU = 16;

    private static BlockPos base(int index) {
        return new BlockPos(BASE_X + index * 8, BASE_Y, BASE_Z);
    }

    private static BlockHitResult hit(BlockPos pos, Direction side) {
        return new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), side, pos, false);
    }

    /** Clears the little stage around the mount, so repeated runs cannot see leftovers. */
    private static void clear(ServerLevel level, BlockPos mount) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -3; dz <= 1; dz++) {
                    level.setBlock(mount.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(mount.below(), Blocks.STONE.defaultBlockState(), 3);
    }

    /** GT6's crank: mounted on {@code mount}, handle pointing north. Returns the crank's position. */
    private static BlockPos placeCrank(ServerLevel level, BlockPos mount) {
        BlockPos crank = mount.relative(HANDLE);
        level.setBlock(crank, GTToolBlocks.CRANK.get().defaultBlockState()
                .setValue(CrankBlock.FACING, HANDLE), 3);
        return crank;
    }

    private static InteractionResult crank(ServerLevel level, BlockPos crank, Player player) {
        BlockState state = level.getBlockState(crank);
        return state.getBlock().use(state, level, crank, player, InteractionHand.MAIN_HAND, hit(crank, HANDLE));
    }

    /**
     * A crank on a plain stone wall turns and powers its mounting block: GT6's
     * {@code onBlockActivated3} + {@code isProvidingWeakPower2} on a crank that is on no machine at
     * all, which is the case the bug report was about.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void freeStandingCrankTurnsAndPowersRedstone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos mount = base(0);
        clear(level, mount);
        level.setBlock(mount, Blocks.STONE.defaultBlockState(), 3);
        BlockPos crank = placeCrank(level, mount);
        BlockPos front = crank.relative(HANDLE);

        BlockState state = level.getBlockState(crank);
        helper.assertTrue(state.getBlock() == GTToolBlocks.CRANK.get(), "the crank is the port's crank block");
        helper.assertTrue(state.getValue(CrankBlock.FACING) == HANDLE,
                "the port's FACING is the handle side - GT6's OPOS[mFacing] - so the mount is opposite it");
        helper.assertTrue(!level.getBlockState(crank.relative(HANDLE.getOpposite())).isAir(),
                "the crank's mount (GT6's mFacing side) is the solid block behind it");
        helper.assertTrue(GTToolBlocks.CRANK.get().canSurvive(state, level, crank),
                "a crank on a plain wall survives");
        helper.assertTrue(level.getBlockEntity(crank.relative(HANDLE.getOpposite())) == null,
                "the crank is deliberately on no machine here");
        helper.assertTrue(!state.getValue(CrankBlock.ACTIVE), "a fresh crank is not turning");

        RecordingPlayer player = new RecordingPlayer(level);
        InteractionResult result = crank(level, crank, player);
        helper.assertTrue(result.consumesAction(), "right-clicking the crank always works");
        helper.assertTrue(player.messages.isEmpty(),
                "no message at all - GT6 has no 'mount it on a machine' answer: " + player.messages);

        helper.assertTrue(level.getBlockState(crank).getValue(CrankBlock.ACTIVE),
                "the crank is turning (GT6's mActive)");
        helper.assertTrue(level.getSignal(crank, HANDLE) == CrankBlock.ACTIVE_SIGNAL,
                "the original opposite-side query powers the mounting block");
        helper.assertTrue(level.getSignal(crank, HANDLE.getOpposite()) == 0,
                "the handle neighbour receives no signal");
        helper.assertTrue(level.getBestNeighborSignal(mount) == CrankBlock.ACTIVE_SIGNAL,
                "the real mounting neighbour receives 15");
        helper.assertTrue(level.getBestNeighborSignal(front) == 0,
                "the real handle neighbour stays unpowered");

        // GT6 clears mActive once the crank is not turned any more (:74-96); the port's pulse does the
        // same on its scheduled tick.
        BlockState active = level.getBlockState(crank);
        ((CrankBlock) active.getBlock()).tick(active, level, crank, level.getRandom());
        helper.assertTrue(!level.getBlockState(crank).getValue(CrankBlock.ACTIVE), "the pulse ends");
        helper.assertTrue(level.getSignal(crank, HANDLE.getOpposite()) == 0, "an idle crank powers nothing");
        helper.assertTrue(level.getBestNeighborSignal(front) == 0, "and the block in front goes dark again");
        helper.succeed();
    }

    /**
     * A crank mounted on an RU machine still drives it: GT6's {@code emitEnergyToSide(TD.Energy.RU,
     * mFacing, -divup(8 * strength, weakness), haste)} ({@code MultiTileEntityCrank:78}), here as the
     * port's {@code EnergyTransfer} injection into the receiving block entity.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void crankOnARuMachineStillDrivesIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos mount = base(1);
        clear(level, mount);
        BasicMachineBlockEntity machine = placeReceiver(level, mount);
        BlockPos crank = placeCrank(level, mount);

        helper.assertTrue(machine.getEnergyTick() == 0, "the machine starts empty");
        RecordingPlayer player = new RecordingPlayer(level);
        InteractionResult result = crank(level, crank, player);
        helper.assertTrue(result.consumesAction(), "right-clicking a mounted crank works");
        helper.assertTrue(player.messages.isEmpty(), "the machine takes the packet, so nothing complains");
        helper.assertTrue(machine.getEnergyTick() == CRANK_RU,
                "GT6's -divup(8 * 2, 1) = 16 RU packet arrives, got " + machine.getEnergyTick());
        helper.assertTrue(level.getBestNeighborSignal(mount) == CrankBlock.ACTIVE_SIGNAL,
                "a mounted crank powers its mount as well");
        helper.succeed();
    }

    /**
     * A machine that cannot take the packet changes nothing about the crank: GT6 still turns (it does
     * not even look at the answer of {@code emitEnergyToSide}), and GT6 has no "the machine is full"
     * message - the port used to answer {@code message.gregtech.crank.full} here.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void aFullMachineDoesNotStopTheCrank(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos mount = base(2);
        clear(level, mount);
        BasicMachineBlockEntity machine = placeReceiver(level, mount);
        BlockPos crank = placeCrank(level, mount);

        // Fill the receiver to its real input maximum, so the crank's packet is refused. One packet is
        // not enough: BasicMachineSpec.defaultEnergyInMax is energyIn * 2 (BasicMachineSpec:62), so a
        // machine rated at 16 RU/t takes two 16 RU packets. The simulation run (doInject = false) tells
        // when there is no room left without touching mEnergy.
        int guard = 0;
        while (guard++ < 16 && machine.doEnergyInjection(GregTechTags.Energy.RU, HANDLE.getOpposite(),
                -CRANK_RU, 1, false) > 0) {
            machine.doEnergyInjection(GregTechTags.Energy.RU, HANDLE.getOpposite(), -CRANK_RU, 1, true);
        }
        long filled = machine.getEnergyTick();
        helper.assertTrue(filled > 0, "the receiver holds energy");
        helper.assertTrue(machine.doEnergyInjection(GregTechTags.Energy.RU, HANDLE.getOpposite(),
                        -CRANK_RU, 1, false) == 0,
                "the receiver is full: " + filled + " of its input maximum");

        RecordingPlayer player = new RecordingPlayer(level);
        crank(level, crank, player);
        helper.assertTrue(machine.getEnergyTick() == filled,
                "a full machine swallows nothing more, got " + machine.getEnergyTick() + " of " + filled);
        helper.assertTrue(player.messages.isEmpty(),
                "GT6 does not tell the player that the machine is full: " + player.messages);
        helper.assertTrue(level.getBlockState(crank).getValue(CrankBlock.ACTIVE), "the crank turns anyway");
        helper.assertTrue(level.getBestNeighborSignal(mount) == CrankBlock.ACTIVE_SIGNAL,
                "the crank is still a redstone source");
        helper.succeed();
    }

    /** The crank's name and tooltips exist in both languages, and the removed messages are gone. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void crankTranslationKeysStayComplete(GameTestHelper helper) throws Exception {
        for (String language : new String[]{"en_us", "zh_cn"}) {
            JsonObject lang = resourceJson("assets/gregtech/lang/" + language + ".json");
            for (String key : new String[]{"block.gregtech.crank", "gt.tooltip.crank.1", "gt.tooltip.crank.2"}) {
                helper.assertTrue(lang.has(key), language + " has no key " + key);
            }
            helper.assertTrue(!lang.has("message.gregtech.crank.no_target"),
                    language + " still carries the removed 'mount it on a machine' message");
            helper.assertTrue(!lang.has("message.gregtech.crank.full"),
                    language + " still carries the removed 'machine is full' message");
        }
        helper.succeed();
    }

    /** A language file from the mod jar, like {@code DungeonTests} reads its resources. */
    private static JsonObject resourceJson(String path) throws Exception {
        try (InputStream stream = CrankTests.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("missing resource " + path);
            return GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    /** A basic machine set up as an RU receiver on every face, GT6's {@code ANY_SIDE} equivalent. */
    private static BasicMachineBlockEntity placeReceiver(ServerLevel level, BlockPos pos) {
        var block = MachineRegistry.basicMachines().iterator().next().get();
        level.setBlock(pos, block.defaultBlockState(), 3);
        BasicMachineBlockEntity receiver = (BasicMachineBlockEntity) level.getBlockEntity(pos);
        receiver.setSpec(BasicMachineSpec.builder("crank_test_receiver", block.basicSpec().material())
                .machineType("test")
                .energy(GregTechTags.Energy.RU, (int) CRANK_RU)
                .recipes(block.basicSpec().recipeMap())
                .faces(FaceConfig.ALL_SIDES)
                .build());
        return receiver;
    }

    /**
     * A player that records every chat message it is sent, so the tests can assert that the crank
     * answers nothing ({@code Player.displayClientMessage} does nothing on a mock player, so the
     * assertion needs this subclass).
     */
    private static final class RecordingPlayer extends Player {
        private final List<Component> messages = new ArrayList<>();

        private RecordingPlayer(Level level) {
            super(level, BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "gt-crank-test"));
        }

        @Override
        public void displayClientMessage(Component message, boolean actionBar) {
            messages.add(message);
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return true;
        }
    }
}
