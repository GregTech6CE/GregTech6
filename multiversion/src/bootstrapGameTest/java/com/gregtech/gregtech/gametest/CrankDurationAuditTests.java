package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.tool.CrankBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayerFactory;
//?}

/** Uses only existing public API: real server interaction plus scheduled world tick progression. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class CrankDurationAuditTests {
    private static CrankBlock crankBlock() {
        return (CrankBlock) BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof CrankBlock).findFirst().orElseThrow();
    }

    /** Readiness may retry; fixture mutation and gameplay assertions execute exactly once. */
    private static void whenReady(GameTestHelper h, Runnable start) {
        h.startSequence().thenWaitUntil(() -> {
            var center = h.absolutePos(new BlockPos(4, 4, 4));
            for (var at : BlockPos.betweenClosed(center.offset(-3, -3, -3), center.offset(3, 3, 3))) {
                h.assertTrue(h.getLevel().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(at))
                        && h.getLevel().isPositionEntityTicking(at), "waiting for loaded/ticking fixture chunk");
            }
        }).thenExecute(start);
        // Completion belongs to the final delayed assertion, never to this readiness sequence.
    }

    private static final class Fixture {
        final GameTestHelper h;
        final BlockPos crank;
        final BlockPos other;
        Fixture(GameTestHelper h) {
            this.h = h;
            crank = h.absolutePos(new BlockPos(4, 4, 4));
            other = crank.offset(2, 0, 0);
            var owned = new java.util.HashSet<BlockPos>();
            h.forEveryBlockInStructure(at -> owned.add(h.absolutePos(at)));
            for (var at : new BlockPos[]{crank, crank.south(), other}) {
                h.assertTrue(owned.contains(at), "fixture mutation must stay inside its own 9-cube template");
                h.assertTrue(h.getLevel().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(at))
                        && h.getLevel().isPositionEntityTicking(at), "fixture chunk is loaded and ticking");
            }
            // GameTestServer can use noise terrain: clearing a 9-cube does not remove aquifers
            // above it. Seal a wholly template-owned chamber before clearing its interior;
            // otherwise natural falling water can wash the crank away during the 500 ticks.
            var low = h.absolutePos(new BlockPos(1, 1, 1));
            var high = h.absolutePos(new BlockPos(7, 7, 7));
            for (var at : BlockPos.betweenClosed(low, high)) {
                h.assertTrue(owned.contains(at), "sealed chamber must remain inside owned template");
            }
            int quiet = net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                    | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
            for (var at : BlockPos.betweenClosed(low, high)) {
                if (at.getX() == low.getX() || at.getX() == high.getX()
                        || at.getY() == low.getY() || at.getY() == high.getY()
                        || at.getZ() == low.getZ() || at.getZ() == high.getZ()) {
                    h.getLevel().setBlock(at, Blocks.STONE.defaultBlockState(), quiet);
                }
            }
            for (var at : BlockPos.betweenClosed(low.offset(1, 1, 1), high.offset(-1, -1, -1))) {
                h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), quiet);
            }
            h.getLevel().setBlock(crank.south(), Blocks.STONE.defaultBlockState(), 3);
            h.getLevel().setBlock(other, Blocks.STONE.defaultBlockState(), 3);
            h.getLevel().setBlock(crank, crankBlock().defaultBlockState()
                    .setValue(CrankBlock.FACING, Direction.NORTH), 3);
        }
        ServerPlayer player(boolean creative) {
            var p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(
                    java.util.UUID.randomUUID(), "[gt-crank-time]"));
            p.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            p.setPos(crank.getX() + .5, crank.getY(), crank.getZ() - 1);
            return p;
        }
        net.minecraft.world.InteractionResult click(ServerPlayer p, BlockPos pos) {
            return p.gameMode.useItemOn(p, h.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
        }
        String diagnostic(ServerPlayer p) {
            Object tracked;
            try {
                var field = Class.forName("com.gregtech.gregtech.block.tool.CrankInteractionEvents")
                        .getDeclaredField("LAST_TARGET");
                field.setAccessible(true);
                tracked = ((java.util.Map<?, ?>) field.get(null)).get(p);
            } catch (ReflectiveOperationException exception) { tracked = exception.toString(); }
            return "gameTick=" + h.getLevel().getGameTime() + ", crank=" + crank
                    + ", state=" + h.getLevel().getBlockState(crank)
                    + ", mount=" + h.getLevel().getBlockState(crank.south())
                    + ", lastTarget=" + tracked + ", player=" + p.getUUID() + ", alive=" + p.isAlive()
                    + ", removed=" + p.isRemoved() + ", health=" + p.getHealth() + ", position=" + p.position()
                    + ", distanceSquared=" + p.distanceToSqr(Vec3.atCenterOf(crank))
                    + ", creative=" + p.isCreative() + ", spectator=" + p.isSpectator()
                    + ", mayBuild=" + p.mayBuild() + ", sameLevel=" + (p.level() == h.getLevel())
                    + ", loaded=" + h.getLevel().hasChunkAt(crank)
                    + ", entitiesLoaded=" + h.getLevel().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(crank))
                    + ", entityTicking=" + h.getLevel().isPositionEntityTicking(crank)
                    + ", scheduled=" + h.getLevel().getBlockTicks().hasScheduledTick(crank, crankBlock());
        }

        void active(boolean expected, String why) {
            var state = h.getLevel().getBlockState(crank);
            h.assertTrue(state.is(crankBlock()) && state.getValue(CrankBlock.ACTIVE) == expected, why);
            h.assertTrue(h.getLevel().getBestNeighborSignal(crank.south()) == (expected ? 15 : 0),
                    why + ": actual mount redstone");
        }
        void finish(ServerPlayer... players) {
            for (var player : players) { player.discard(); }
            h.getLevel().setBlock(crank, Blocks.AIR.defaultBlockState(), 3);
            h.succeed();
        }
    }

    @GameTest(template = "crank_duration_empty", timeoutTicks = 650)
    public static void singleSurvivalClickRunsOverTwentySecondsUntilAnotherInteraction(GameTestHelper h) {
        whenReady(h, () -> {
        var f = new Fixture(h);
        var p = f.player(false);
        var result = f.click(p, f.crank);
        var observations = new java.util.ArrayList<String>();
        observations.add("activationResult=" + result + "; " + f.diagnostic(p));
        h.assertTrue(result.consumesAction(), "initial native crank interaction must be accepted: " + observations);
        f.active(true, "initial native crank activation must be active: " + observations);
        for (int checkpoint : new int[]{1, 10, 100, 250, 499}) {
            h.runAfterDelay(checkpoint, () -> observations.add(f.diagnostic(p)));
        }
        // Turning the head is not a new interaction and must not release the crank.
        p.setYRot(180);
        p.setXRot(-70);
        h.runAfterDelay(500, () -> {
            f.active(true, "one click still turns after 25 seconds, no machine required; observations=" + observations
                    + "; final=" + f.diagnostic(p));
            f.click(p, f.other);
        });
        h.runAfterDelay(503, () -> {
            f.active(false, "another block interaction releases the last target");
            f.finish(p);
        });
            });
    }

    @GameTest(template = "crank_duration_empty", timeoutTicks = 200)
    public static void repeatedClicksDoNotCreateAStopDeadlineAndLeavingRangeStops(GameTestHelper h) {
        whenReady(h, () -> {
        var f = new Fixture(h);
        var p = f.player(false);
        f.click(p, f.crank);
        h.runAfterDelay(8, () -> f.click(p, f.crank));
        h.runAfterDelay(30, () -> {
            f.active(true, "reclick does not preserve the old ten-tick pulse stop");
            p.setPos(f.crank.getX() + 7, f.crank.getY(), f.crank.getZ());
        });
        h.runAfterDelay(33, () -> {
            f.active(false, "survival operator beyond six blocks no longer drives crank");
            p.setPos(f.crank.getX() + .5, f.crank.getY(), f.crank.getZ() - 1);
        });
        h.runAfterDelay(40, () -> {
            f.active(false, "returning in range alone does not reactivate a stopped crank");
            f.click(p, f.crank);
        });
        h.runAfterDelay(55, () -> {
            f.active(true, "a new click reactivates the stopped crank");
            f.finish(p);
        });
            });
    }

    @GameTest(template = "crank_duration_empty", timeoutTicks = 650)
    public static void creativeDistanceBypassAndRemovedOperatorStops(GameTestHelper h) {
        whenReady(h, () -> {
        var f = new Fixture(h);
        var p = f.player(true);
        f.click(p, f.crank);
        p.setPos(f.crank.getX() + 20, f.crank.getY(), f.crank.getZ());
        h.runAfterDelay(500, () -> {
            f.active(true, "creative range bypass remains active after 25 seconds");
            p.discard();
        });
        h.runAfterDelay(503, () -> {
            f.active(false, "removed operator no longer turns crank");
            f.finish(p);
        });
            });
    }

    @GameTest(template = "crank_duration_empty", timeoutTicks = 200)
    public static void multiplayerLastRemainingOperatorKeepsCrankActive(GameTestHelper h) {
        whenReady(h, () -> {
        var f = new Fixture(h);
        var a = f.player(false);
        var b = f.player(false);
        f.click(a, f.crank);
        f.click(b, f.crank);
        h.runAfterDelay(20, () -> f.click(a, f.other));
        h.runAfterDelay(25, () -> {
            f.active(true, "second player continues after first targets elsewhere");
            f.click(b, f.other);
        });
        h.runAfterDelay(28, () -> {
            f.active(false, "crank stops only after final operator changes target");
            f.finish(a, b);
        });
            });
    }
    private static final class CountingReceiver extends com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity {
        long offers, accepted, lastTick = Long.MIN_VALUE;
        boolean packetValid = true, duplicateTick;
        CountingReceiver(net.minecraft.world.level.block.entity.BlockEntity original) {
            super(original.getType(), original.getBlockPos(), original.getBlockState());
        }
        @Override public long doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Tag type,
                Direction side, long size, long amount, boolean inject) {
            if (inject) {
                offers++;
                packetValid &= type == com.gregtech.gregtech.data.GregTechTags.Energy.RU
                        && side == Direction.NORTH && size == -16 && amount == 1;
                duplicateTick |= lastTick == getLevel().getGameTime();
                lastTick = getLevel().getGameTime();
            }
            long used = super.doEnergyInjection(type, side, size, amount, inject);
            if (inject) accepted += used;
            return used;
        }
    }

    @GameTest(template = "crank_duration_empty", timeoutTicks = 650)
    public static void energyStartsNextTickAndContinuesEveryTickPastTwentySeconds(GameTestHelper h) {
        whenReady(h, () -> {
        var f = new Fixture(h);
        var mount = f.crank.south();
        var block = (com.gregtech.gregtech.block.machine.BasicMachineBlock) BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock).findFirst().orElseThrow();
        h.getLevel().setBlock(mount, block.defaultBlockState(), 3);
        var receiver = new CountingReceiver(h.getLevel().getBlockEntity(mount));
        receiver.setSpec(com.gregtech.gregtech.api.machine.BasicMachineSpec.builder("crank_duration_receiver", block.basicSpec().material())
                .machineType("test").energy(com.gregtech.gregtech.data.GregTechTags.Energy.RU, 16)
                .recipes(block.basicSpec().recipeMap()).faces(com.gregtech.gregtech.api.energy.FaceConfig.ALL_SIDES).build());
        h.getLevel().setBlockEntity(receiver);
        var p = f.player(false);
        f.click(p, f.crank);
        h.assertTrue(receiver.offers == 0, "GT6 activation latches only: no immediate click packet");
        long[] snapshot = new long[2];
        h.runAfterDelay(20, () -> {
            snapshot[0] = receiver.offers;
            snapshot[1] = h.getLevel().getGameTime();
            // Repeated click must not inject another packet during this same tick.
            f.click(p, f.crank);
            h.assertTrue(receiver.offers == snapshot[0], "repeat click adds no out-of-tick RU packet");
        });
        h.runAfterDelay(500, () -> {
            f.active(true, "receiver load does not limit activation duration");
            h.assertTrue(receiver.offers - snapshot[0] == h.getLevel().getGameTime() - snapshot[1],
                    "exactly one real energy-boundary offer per elapsed world tick");
            h.assertTrue(receiver.offers > 400 && receiver.accepted > 400,
                    "loaded real machine receives ongoing output, not a cosmetic active flag");
            h.assertTrue(receiver.packetValid && !receiver.duplicateTick,
                    "RU sign, side, packet size and count match original");
            h.getLevel().setBlock(mount, Blocks.STONE.defaultBlockState(), 3);
            f.finish(p);
        });
            });
    }

}
