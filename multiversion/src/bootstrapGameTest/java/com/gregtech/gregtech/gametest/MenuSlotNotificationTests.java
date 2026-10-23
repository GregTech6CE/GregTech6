package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.blockentity.inventory.DrawerQuadBlockEntity;
import com.gregtech.gregtech.blockentity.inventory.EnderGarbageBlockEntity;
import com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity;
import com.gregtech.gregtech.blockentity.inventory.SafeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.HopperBlockEntity;
import com.gregtech.gregtech.blockentity.machine.QueueHopperBlockEntity;
import com.gregtech.gregtech.client.gui.HopperContainerMenu;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Native menu mutation/dirty boundaries, not disk-restart or packet delivery tests. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class MenuSlotNotificationTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private enum Kind {
        DRAWER("drawer_quad", 36), CHEST("chest_steel", 54), SAFE("safe", 15),
        BIN("ender_garbage_bin", 9), HOPPER("hopper_steel", 5), QUEUE("queue_hopper_steel", 5);
        final String path;
        final int slots;
        Kind(String path, int slots) { this.path = path; this.slots = slots; }
    }

    private static CompoundTag save(GameTestHelper h, BlockEntity entity) {
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }

    private static void load(GameTestHelper h, BlockEntity entity, CompoundTag tag) {
//? if >=1.21.1 {
/*        entity.loadWithComponents(tag, h.getLevel().registryAccess());
*///?} else {
        entity.load(tag);
//?}
    }

    /** Unique chunks, one synchronous method each; preserve pre-test state and dirty flag. */
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final BlockPos pos;
        final LevelChunk chunk;
        final boolean oldUnsaved;
        final BlockState oldState;
        final CompoundTag oldData;
        final Player player;
        BlockEntity owner;
        HopperContainerMenu menu;

        Fixture(GameTestHelper h, int index) {
            this.h = h;
            pos = new BlockPos(26_500_000 + index * 128, 220, 27_500_000);
            chunk = h.getLevel().getChunkAt(pos);
            oldUnsaved = chunk.isUnsaved();
            oldState = h.getLevel().getBlockState(pos);
            var old = h.getLevel().getBlockEntity(pos);
            oldData = old == null ? null : save(h, old);
            // A unique, test-owned fake player, never an existing player's inventory.
            player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "MenuNotify"));
            player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        }

        void open(Kind kind) {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", kind.path));
            h.assertTrue(block != Blocks.AIR, "registered provider exists: " + kind.path);
            var state = block.defaultBlockState();
            if (kind == Kind.HOPPER || kind == Kind.QUEUE) state = state.setValue(DirectionalBlock.FACING, Direction.UP);
            h.getLevel().removeBlockEntity(pos);
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(pos, state, FLAGS);
            owner = h.getLevel().getBlockEntity(pos);
            menu = switch (kind) {
                case DRAWER -> ((DrawerQuadBlockEntity) owner).createMenu(0, 1, player.getInventory());
                case CHEST -> (HopperContainerMenu) ((MetalChestBlockEntity) owner).createMenu(1, player.getInventory(), player);
                case SAFE -> {
                    var safe = (SafeBlockEntity) owner;
                    h.assertTrue(safe.claimAndOpen(player), "actual mechanical safe claims this player");
                    yield safe.createMenu(1, player.getInventory());
                }
                case BIN -> (HopperContainerMenu) ((EnderGarbageBlockEntity) owner).createMenu(1, player.getInventory(), player);
                case HOPPER -> (HopperContainerMenu) ((HopperBlockEntity) owner).createMenu(1, player.getInventory(), player);
                case QUEUE -> (HopperContainerMenu) ((QueueHopperBlockEntity) owner).createMenu(1, player.getInventory(), player);
            };
            h.assertTrue(menu != null && menu.slotCount() == kind.slots && menu.stillValid(player),
                    "actual live provider has its registered inventory size: " + kind.path);
        }

        void reset() {
            menu.setCarried(ItemStack.EMPTY);
            player.getInventory().clearContent();
            // Explicit fixture seeding through native slot setters, before the measured action.
            for (int i = 0; i < menu.slotCount(); i++) menu.getSlot(i).set(ItemStack.EMPTY);
        }

        void cleanBaseline() {
            h.assertTrue(menu.stillValid(player) && menu.getCarried().isEmpty(), "live menu, empty cursor before click");
            chunk.setUnsaved(false);
            h.assertTrue(!chunk.isUnsaved(), "native chunk starts clean after all setup");
        }

        void click(int index) { menu.clicked(index, 0, ClickType.QUICK_MOVE, player); }

        @Override public void close() {
            if (menu != null) {
                menu.setCarried(ItemStack.EMPTY);
                menu.removed(player); // Balance actual chest opener bookkeeping.
            }
            player.getInventory().clearContent();
            // Remove the disposable owner before changing blocks, so no test stock drops.
            h.getLevel().removeBlockEntity(pos);
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(pos, oldState, FLAGS);
            var restored = h.getLevel().getBlockEntity(pos);
            if (restored != null && oldData != null) load(h, restored, oldData);
            chunk.setUnsaved(oldUnsaved);
        }
    }

    private record Snapshot(List<ItemStack> machine, List<ItemStack> player) {
        static Snapshot of(Fixture f) {
            var machine = new ArrayList<ItemStack>();
            var player = new ArrayList<ItemStack>();
            for (int i = 0; i < f.menu.slotCount(); i++) machine.add(f.menu.getSlot(i).getItem().copy());
            for (int i = 0; i < f.player.getInventory().getContainerSize(); i++) player.add(f.player.getInventory().getItem(i).copy());
            return new Snapshot(machine, player);
        }

        void assertMatches(Fixture f, String stage) {
            for (int i = 0; i < machine.size(); i++) f.h.assertTrue(ItemStack.matches(machine.get(i), f.menu.getSlot(i).getItem()),
                    stage + ": exact machine slot " + i);
            for (int i = 0; i < player.size(); i++) f.h.assertTrue(ItemStack.matches(player.get(i), f.player.getInventory().getItem(i)),
                    stage + ": exact player slot " + i);
            f.h.assertTrue(f.menu.getCarried().isEmpty(), stage + ": cursor remains empty");
        }
    }

    private static ItemStack stone(int count) { return new ItemStack(Items.STONE, count); }

    private static void fillPlayer(Fixture f) {
        for (int i = 0; i < 36; i++) f.player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
    }

    private static void dirtyTransferCases(GameTestHelper h, Kind kind) {
        try (var f = new Fixture(h, kind.ordinal())) {
            f.open(kind); // Complete safe claims/chest loot and opener setup before any dirty reset.
            int output = kind == Kind.QUEUE ? f.menu.slotCount() - 1 : 0;

            // Positive: full withdrawal uses the real handler's set(EMPTY) notification.
            f.reset();
            f.menu.getSlot(output).set(stone(64));
            var full = Snapshot.of(f);
            full.machine().set(output, ItemStack.EMPTY);
            full.player().set(8, stone(64)); // Reverse player traversal ends at hotbar 8 first.
            f.cleanBaseline();
            f.click(output);
            full.assertMatches(f, "full withdrawal control");
            h.assertTrue(f.chunk.isUnsaved(), "full withdrawal control dirties native chunk");

            // Negative: every destination is full with a different item, so no mutation occurs.
            f.reset();
            f.menu.getSlot(output).set(stone(64));
            fillPlayer(f);
            var noOp = Snapshot.of(f);
            f.cleanBaseline();
            f.click(output);
            noOp.assertMatches(f, "blocked withdrawal control");
            h.assertTrue(!f.chunk.isUnsaved(), "blocked withdrawal leaves native chunk clean");

            // Partial: move exactly one item; only Slot.setChanged sees the remaining live stack.
            f.reset();
            f.menu.getSlot(output).set(stone(64));
            fillPlayer(f);
            f.player.getInventory().setItem(8, stone(63));
            var partial = Snapshot.of(f);
            partial.machine().set(output, stone(63));
            partial.player().set(8, stone(64));
            f.cleanBaseline();
            f.click(output);
            partial.assertMatches(f, "partial withdrawal conserves 127 stone and all dirt");
            boolean partialDirty = f.chunk.isUnsaved();

            // Merge: main inventory slot 9 maps to the first menu slot after machine slots.
            f.reset();
            for (int i = 0; i < f.menu.slotCount(); i++) f.menu.getSlot(i).set(new ItemStack(Items.DIRT, 64));
            f.menu.getSlot(0).set(stone(63));
            f.player.getInventory().setItem(9, stone(1));
            var merge = Snapshot.of(f);
            merge.machine().set(0, stone(64));
            merge.player().set(9, ItemStack.EMPTY);
            f.cleanBaseline();
            f.click(f.menu.slotCount());
            merge.assertMatches(f, "existing-stack merge conserves 64 stone and all dirt");
            boolean mergeDirty = f.chunk.isUnsaved();

            // Delay this assertion so both boundaries and both controls run in the baseline.
            h.assertTrue(partialDirty && mergeDirty, kind.path + " native dirty notifications: partial="
                    + partialDirty + ", existingMerge=" + mergeDirty + " (both must be true)");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void drawerQuickMovesDirtyNativeChunk(GameTestHelper h) { dirtyTransferCases(h, Kind.DRAWER); }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void metalChestQuickMovesDirtyNativeChunk(GameTestHelper h) { dirtyTransferCases(h, Kind.CHEST); }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void safeQuickMovesDirtyNativeChunk(GameTestHelper h) { dirtyTransferCases(h, Kind.SAFE); }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void garbageBinQuickMovesDirtyNativeChunk(GameTestHelper h) { dirtyTransferCases(h, Kind.BIN); }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void hopperQuickMovesDirtyNativeChunk(GameTestHelper h) { dirtyTransferCases(h, Kind.HOPPER); }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void queueHopperQuickMovesDirtyNativeChunk(GameTestHelper h) { dirtyTransferCases(h, Kind.QUEUE); }

    private static void tick(Fixture f) {
        if (f.owner instanceof HopperBlockEntity hopper)
            HopperBlockEntity.serverTick(f.h.getLevel(), f.pos, hopper.getBlockState(), hopper);
        else if (f.owner instanceof QueueHopperBlockEntity queue)
            QueueHopperBlockEntity.serverTick(f.h.getLevel(), f.pos, queue.getBlockState(), queue);
        else throw new IllegalStateException("Not a native hopper fixture");
    }

    private static void compactionAfterMenuChange(GameTestHelper h, boolean queue) {
        try (var f = new Fixture(h, queue ? 7 : 6)) {
            f.open(queue ? Kind.QUEUE : Kind.HOPPER);
            h.assertTrue(h.getLevel().isEmptyBlock(f.pos.above()) && !h.getLevel().hasNeighborSignal(f.pos)
                    && h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(f.pos).inflate(2)).isEmpty(),
                    "isolated UP-facing fixture has no source, redstone, or loose items");
            f.reset();
            for (int i = 0; i < f.menu.slotCount(); i++)
                f.menu.getSlot(i).set(queue || i < 2 ? stone(64) : new ItemStack(Items.DIRT, 64));
            fillPlayer(f);
            f.player.getInventory().setItem(8, stone(63));
            var initial = Snapshot.of(f);
            for (int i = 0; i < 4; i++) tick(f); // Consume mCheck=3 and the normal setter's work flag.
            initial.assertMatches(f, "native setup ticks do not move any item");

            int output = queue ? f.menu.slotCount() - 1 : 0;
            var afterClick = Snapshot.of(f);
            afterClick.machine().set(output, stone(63));
            afterClick.player().set(8, stone(64));
            f.cleanBaseline();
            f.click(output);
            afterClick.assertMatches(f, "native menu withdraws exactly one before compaction");

            var compacted = Snapshot.of(f);
            compacted.machine().set(output, stone(64));
            compacted.machine().set(queue ? 0 : 1, stone(63));
            tick(f); // No reflection or private flag seeding: exercise the real next native tick.
            compacted.assertMatches(f, queue ? "menu notification wakes FIFO cascade" : "menu notification wakes hopper defragmentation");
            h.assertTrue(f.chunk.isUnsaved(), "real compaction dirties native chunk");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void hopperMenuPartialWithdrawalWakesCompaction(GameTestHelper h) { compactionAfterMenuChange(h, false); }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void queueMenuPartialWithdrawalWakesCascade(GameTestHelper h) { compactionAfterMenuChange(h, true); }
}
