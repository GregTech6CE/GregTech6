package gregtech.gametest;

import gregtech.content.blockentity.machine.QueueHopperBlockEntity;
import gregtech.client.gui.HopperContainerMenu;
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
//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
//?}

/** Actual manual-menu permissions are distinct from the queue's automation entry/exit policy. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class QueueMenuPermissionTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static CompoundTag save(GameTestHelper h, BlockEntity owner) {
//? if >=1.21.1 {
/*        return owner.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return owner.saveWithoutMetadata();
//?}
    }

    private static void load(GameTestHelper h, BlockEntity owner, CompoundTag tag) {
//? if >=1.21.1 {
/*        owner.loadWithComponents(tag, h.getLevel().registryAccess());
*///?} else {
        owner.load(tag);
//?}
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final BlockPos pos;
        final LevelChunk chunk;
        final boolean oldUnsaved;
        final BlockState oldState;
        final CompoundTag oldData;
        final Player player;
        QueueHopperBlockEntity queue;
        HopperContainerMenu menu;

        Fixture(GameTestHelper h, int index) {
            this.h = h;
            pos = new BlockPos(24_500_008 + index * 128, 220, 26_500_008);
            chunk = h.getLevel().getChunkAt(pos);
            oldUnsaved = chunk.isUnsaved();
            oldState = h.getLevel().getBlockState(pos);
            var old = h.getLevel().getBlockEntity(pos);
            oldData = old == null ? null : save(h, old);
            player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "QueueGui"));
            player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        }

        void open() {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "queue_hopper_steel"));
            h.assertTrue(block != Blocks.AIR, "actual steel queue hopper is registered");
            h.getLevel().removeBlockEntity(pos);
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(pos, block.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP), FLAGS);
            h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof QueueHopperBlockEntity, "actual queue owner");
            queue = (QueueHopperBlockEntity) h.getLevel().getBlockEntity(pos);
            menu = (HopperContainerMenu) queue.createMenu(1, player.getInventory(), player);
            h.assertTrue(menu != null && menu.slotCount() == 5 && menu.stillValid(player), "actual live five-slot menu");
            h.assertTrue(queue.getMode() == 64, "ordinary default capacity, not a private-field fixture");
        }

        void reset() {
            menu.setCarried(ItemStack.EMPTY);
            player.getInventory().clearContent();
            for (int slot = 0; slot < menu.slotCount(); slot++) queue.setStackInSlot(slot, ItemStack.EMPTY);
        }

        void clean() {
            h.assertTrue(menu.stillValid(player), "menu retains its live nearby owner");
            chunk.setUnsaved(false); // Only after all normal fixture seeding.
            h.assertTrue(!chunk.isUnsaved(), "native chunk starts clean");
        }

        IItemHandler sided(Direction side) {
//? if >=1.21.1 {
/*            var handler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, side);
*///?} else {
            var handler = queue.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
//?}
            h.assertTrue(handler != null, "real sided item capability exists: " + side);
            return handler;
        }

        @Override public void close() {
            if (menu != null) {
                menu.setCarried(ItemStack.EMPTY);
                menu.removed(player);
            }
            player.getInventory().clearContent();
            h.getLevel().removeBlockEntity(pos); // Do not drop disposable test stock.
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(pos, oldState, FLAGS);
            var restored = h.getLevel().getBlockEntity(pos);
            if (restored != null && oldData != null) load(h, restored, oldData);
            chunk.setUnsaved(oldUnsaved);
        }
    }

    private record Snapshot(List<ItemStack> machine, List<ItemStack> player, ItemStack cursor) {
        static Snapshot of(Fixture f) {
            var machine = new ArrayList<ItemStack>();
            var player = new ArrayList<ItemStack>();
            for (int slot = 0; slot < f.menu.slotCount(); slot++) machine.add(f.queue.getStackInSlot(slot).copy());
            for (int slot = 0; slot < f.player.getInventory().getContainerSize(); slot++)
                player.add(f.player.getInventory().getItem(slot).copy());
            return new Snapshot(machine, player, f.menu.getCarried().copy());
        }
        Snapshot cursor(ItemStack stack) { return new Snapshot(machine, player, stack); }
        void check(Fixture f, String stage) {
            for (int slot = 0; slot < machine.size(); slot++)
                f.h.assertTrue(ItemStack.matches(machine.get(slot), f.queue.getStackInSlot(slot)), stage + ": machine " + slot);
            for (int slot = 0; slot < player.size(); slot++)
                f.h.assertTrue(ItemStack.matches(player.get(slot), f.player.getInventory().getItem(slot)), stage + ": player " + slot);
            f.h.assertTrue(ItemStack.matches(cursor, f.menu.getCarried()), stage + ": exact cursor");
        }
    }

    private static ItemStack stone(int count) { return count == 0 ? ItemStack.EMPTY : new ItemStack(Items.STONE, count); }

    /** Test automation independently, before any expected-to-fail manual-permission assertion. */
    private static void automationControls(Fixture f) {
        int last = f.menu.slotCount() - 1;
        for (int slot = 0; slot <= last; slot++) {
            f.reset();
            var empty = Snapshot.of(f);
            ItemStack rest = f.queue.insertItem(slot, stone(8), true);
            f.h.assertTrue(rest.getCount() == (slot == 0 ? 0 : 8), "raw simulated insertion is entry-only " + slot);
            empty.check(f, "raw insertion simulation does not mutate");
            rest = f.queue.insertItem(slot, stone(8), false);
            f.h.assertTrue(rest.getCount() == (slot == 0 ? 0 : 8), "raw real insertion is entry-only " + slot);
            if (slot == 0) empty.machine().set(0, stone(8));
            empty.check(f, "raw insertion exact source acceptance");

            f.reset();
            f.queue.setStackInSlot(slot, stone(8));
            var before = Snapshot.of(f);
            ItemStack taken = f.queue.extractItem(slot, 1, true);
            f.h.assertTrue(ItemStack.matches(taken, stone(slot == last ? 1 : 0)), "raw simulated extraction is exit-only " + slot);
            before.check(f, "raw extraction simulation does not mutate");
            taken = f.queue.extractItem(slot, 1, false);
            f.h.assertTrue(ItemStack.matches(taken, stone(slot == last ? 1 : 0)), "raw real extraction is exit-only " + slot);
            if (slot == last) before.machine().set(last, stone(7));
            before.check(f, "raw extraction exact debit");
        }

        f.reset();
        var side = f.sided(Direction.NORTH); // Not the UP-facing output face.
        var outputFace = f.sided(Direction.UP);
        f.h.assertTrue(side.getSlots() == 2 && outputFace.getSlots() == 2, "sided automation exposes only entry and exit");
        var empty = Snapshot.of(f);
        f.h.assertTrue(side.insertItem(0, stone(8), true).isEmpty(), "side simulation accepts entry");
        f.h.assertTrue(ItemStack.matches(side.insertItem(1, stone(8), true), stone(8)), "side simulation rejects exit insertion");
        f.h.assertTrue(ItemStack.matches(outputFace.insertItem(0, stone(8), true), stone(8)), "output face rejects insertion");
        empty.check(f, "all sided insertion simulations are pure");
        f.h.assertTrue(side.insertItem(0, stone(8), false).isEmpty(), "non-output face really inserts at entry");
        empty.machine().set(0, stone(8));
        empty.check(f, "sided entry insertion exact debit");
        f.queue.setStackInSlot(last, stone(8));
        var seeded = Snapshot.of(f);
        f.h.assertTrue(ItemStack.matches(side.getStackInSlot(0), stone(8)) && ItemStack.matches(side.getStackInSlot(1), stone(8)),
                "sided addresses map to entry and exit");
        f.h.assertTrue(side.extractItem(0, 1, true).isEmpty() && side.extractItem(0, 1, false).isEmpty(), "side cannot extract entry");
        f.h.assertTrue(outputFace.extractItem(1, 1, true).isEmpty() && outputFace.extractItem(1, 1, false).isEmpty(),
                "output-face lock remains effective outside queue-owned transfer");
        f.h.assertTrue(ItemStack.matches(side.extractItem(1, 1, true), stone(1)), "non-output face simulates exit extraction");
        seeded.check(f, "sided extraction simulations and rejected real operations are pure");
        f.h.assertTrue(ItemStack.matches(side.extractItem(1, 1, false), stone(1)), "non-output face really extracts exit");
        seeded.machine().set(last, stone(7));
        seeded.check(f, "sided exit extraction exact debit");
    }

    private record Transfer(int slot, int targetBefore, int targetAfter, int otherBefore, int otherAfter, int intendedAdded) {
        boolean accepted() { return targetAfter == targetBefore + intendedAdded && otherAfter == otherBefore - intendedAdded; }
    }

    private record Capacity(int slot, int existing, int expected, int observed, boolean clean) {
        boolean accepted() { return observed == expected && clean; }
    }

    private static Transfer pickup(Fixture f, int slot, boolean half) {
        f.reset();
        f.queue.setStackInSlot(slot, stone(8));
        var before = Snapshot.of(f);
        f.clean();
        f.menu.clicked(slot, half ? 1 : 0, ClickType.PICKUP, f.player);
        int taken = f.menu.getCarried().getCount();
        int expected = half ? 4 : 8;
        f.h.assertTrue(taken == 0 || taken == expected, "PICKUP is either rejected or transfers its exact normal amount");
        before.machine().set(slot, stone(8 - taken));
        before.cursor(stone(taken)).check(f, "PICKUP conserves source/cursor and all unrelated inventory");
        if (taken > 0) f.h.assertTrue(f.chunk.isUnsaved(), "real PICKUP still notifies queue owner");
        return new Transfer(slot, 8, 8 - taken, 0, taken, -expected);
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void queueGuiPickupCanExtractEverySlot(GameTestHelper h) {
        try (var f = new Fixture(h, 0)) {
            f.open();
            automationControls(f);
            h.assertTrue(pickup(f, 4, false).accepted() && pickup(f, 4, true).accepted(), "LAST full/half PICKUP positive controls");
            var results = new ArrayList<Transfer>();
            for (int slot = 0; slot < 4; slot++) {
                results.add(pickup(f, slot, false));
                results.add(pickup(f, slot, true));
            }
            h.assertTrue(results.stream().allMatch(Transfer::accepted), "manual full/half PICKUP permits slots0..3: " + results);
        }
        h.succeed();
    }

    private static Transfer quickMove(Fixture f, int slot, boolean partial) {
        f.reset();
        f.queue.setStackInSlot(slot, stone(partial ? 64 : 8));
        if (partial) {
            for (int i = 0; i < 36; i++) f.player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
            f.player.getInventory().setItem(8, stone(63));
        }
        var before = Snapshot.of(f);
        f.clean();
        f.menu.clicked(slot, 0, ClickType.QUICK_MOVE, f.player);
        int moved = (partial ? 64 : 8) - f.queue.getStackInSlot(slot).getCount();
        f.h.assertTrue(moved == 0 || moved == (partial ? 1 : 8), "QUICK_MOVE has exact normal transfer amount or is rejected");
        before.machine().set(slot, stone((partial ? 64 : 8) - moved));
        before.player().set(8, stone((partial ? 63 : 0) + moved));
        before.check(f, "QUICK_MOVE conserves machine/player/cursor and every unrelated item");
        if (moved > 0) f.h.assertTrue(f.chunk.isUnsaved(), "real full/partial QUICK_MOVE still notifies queue owner");
        return new Transfer(slot, partial ? 64 : 8, (partial ? 64 : 8) - moved, partial ? 63 : 0, (partial ? 63 : 0) + moved, -(partial ? 1 : 8));
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void queueGuiQuickMoveCanExtractEverySlot(GameTestHelper h) {
        try (var f = new Fixture(h, 1)) {
            f.open();
            automationControls(f);
            h.assertTrue(quickMove(f, 4, false).accepted() && quickMove(f, 4, true).accepted(), "LAST full/partial QUICK_MOVE positive controls");
            f.reset();
            f.queue.setStackInSlot(4, stone(8));
            for (int i = 0; i < 36; i++) f.player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
            var blocked = Snapshot.of(f);
            f.clean();
            f.menu.clicked(4, 0, ClickType.QUICK_MOVE, f.player);
            blocked.check(f, "full-player negative control");
            h.assertTrue(!f.chunk.isUnsaved(), "blocked QUICK_MOVE leaves chunk clean");
            var results = new ArrayList<Transfer>();
            for (int slot = 0; slot < 4; slot++) {
                results.add(quickMove(f, slot, false));
                results.add(quickMove(f, slot, true));
            }
            h.assertTrue(results.stream().allMatch(Transfer::accepted), "manual full/partial QUICK_MOVE permits slots0..3: " + results);
        }
        h.succeed();
    }

    private static Transfer insert(Fixture f, int slot, int existing, int carried, int accepted) {
        f.reset();
        f.queue.setStackInSlot(slot, stone(existing));
        f.menu.setCarried(stone(carried));
        var before = Snapshot.of(f);
        f.clean();
        f.h.assertTrue(f.menu.getSlot(slot).mayPlace(stone(1)), "manual placement policy permits this ordinary item");
        f.menu.clicked(slot, 0, ClickType.PICKUP, f.player);
        int added = f.queue.getStackInSlot(slot).getCount() - existing;
        // Forge's automation-backed LAST capacity0 can reverse an existing merge.
        // Record that known candidate outcome without hiding loss or unrelated mutation;
        // the final assertion below still requires the intended forward insertion.
        f.h.assertTrue(added == 0 || added == accepted || existing > 0 && added == -existing,
                "PICKUP has a known bounded candidate outcome before exact conservation");
        before.machine().set(slot, stone(existing + added));
        before.cursor(stone(carried - added)).check(f, "PICKUP insertion conserves cursor/target and all unrelated items");
        if (added > 0) f.h.assertTrue(f.chunk.isUnsaved(), "real PICKUP insertion notifies queue owner");
        return new Transfer(slot, existing, existing + added, carried, f.menu.getCarried().getCount(), accepted);
    }

    private static Capacity capacity(Fixture f, int slot, int existing, int expected) {
        f.reset();
        f.queue.setStackInSlot(slot, stone(existing));
        var before = Snapshot.of(f);
        f.clean();
        int limit = f.menu.getSlot(slot).getMaxStackSize(stone(1)); // This query IS the measured candidate, not setup.
        before.check(f, "capacity query leaves every item and cursor unchanged");
        return new Capacity(slot, existing, expected, limit, !f.chunk.isUnsaved());
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void queueGuiPickupInsertionAndCapacityUseManualSlots(GameTestHelper h) {
        try (var f = new Fixture(h, 2)) {
            f.open();
            automationControls(f);
            h.assertTrue(insert(f, 0, 0, 1, 1).accepted(), "entry empty PICKUP insertion positive control");
            h.assertTrue(insert(f, 0, 64, 1, 0).accepted(), "full entry retains carried item");
            var inserted = new ArrayList<Transfer>();
            var capacities = new ArrayList<Capacity>();
            for (int slot = 0; slot < 5; slot++) {
                inserted.add(insert(f, slot, 0, 1, 1));
                inserted.add(insert(f, slot, 63, 1, 1));
                capacities.add(capacity(f, slot, 0, 64));
                capacities.add(capacity(f, slot, 64, 64));
            }
            f.queue.cycleMode(false); // Real public screwdriver action: 64 wraps to1.
            h.assertTrue(f.queue.getMode() == 1, "real mode cycle sets one-item capacity");
            for (int slot = 0; slot < 5; slot++) {
                inserted.add(insert(f, slot, 0, 2, 1));
                capacities.add(capacity(f, slot, 0, 1));
            }
            h.assertTrue(inserted.stream().allMatch(Transfer::accepted) && capacities.stream().allMatch(Capacity::accepted),
                    "manual all-slot PICKUP insertion=" + inserted + ", pure default/reduced capacity=" + capacities);
        }
        h.succeed();
    }
}
