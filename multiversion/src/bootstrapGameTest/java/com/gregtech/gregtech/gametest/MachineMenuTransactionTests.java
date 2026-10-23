package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BehaviorMachineBlockEntity;
import com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity;
import com.gregtech.gregtech.client.gui.BasicMachineContainerMenu;
import com.gregtech.gregtech.content.bumble.BumbleBeeType;
import com.gregtech.gregtech.content.bumble.GTBumbleProducts;
import com.gregtech.gregtech.content.bumble.GTBumbleSpecies;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Real provider/click contracts: dirty notifications and slot admission are separate assertions. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class MachineMenuTransactionTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

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

    /** Test-owned high-Y chunks; no ticks/yields occur inside an immediate dirty measurement. */
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final BlockPos pos;
        final LevelChunk chunk;
        final boolean oldUnsaved;
        final BlockState oldState;
        final CompoundTag oldData;
        final Player player;
        BlockEntity owner;
        AbstractContainerMenu menu;
        int playerStart;

        Fixture(GameTestHelper h, int index) {
            this.h = h;
            pos = new BlockPos(25_500_008 + index * 128, 220, 27_000_008);
            chunk = h.getLevel().getChunkAt(pos);
            oldUnsaved = chunk.isUnsaved();
            oldState = h.getLevel().getBlockState(pos);
            var old = h.getLevel().getBlockEntity(pos);
            oldData = old == null ? null : save(h, old);
            player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "MachineMenu"));
            player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        }

        void open(String namespace, String path) {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
            h.assertTrue(block != Blocks.AIR, "registered machine exists: " + namespace + ":" + path);
            h.getLevel().removeBlockEntity(pos);
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(pos, block.defaultBlockState(), FLAGS);
            owner = h.getLevel().getBlockEntity(pos);
            h.assertTrue(owner instanceof MenuProvider, "actual registered owner supplies menu");
            menu = ((MenuProvider) owner).createMenu(1, player.getInventory(), player);
            h.assertTrue(menu != null && menu.stillValid(player), "actual provider accepts nearby survival player");
            h.assertTrue(!player.isCreative(), "fixture does not bypass slot rules using creative mode");
            playerStart = menu.slots.size() - 36;
            h.assertTrue(playerStart > 0 && menu.getSlot(playerStart).container == player.getInventory(),
                    "first player menu slot is backed by the real inventory");
            tick(); // Stabilize idle/environment bookkeeping before any measured mutation.
        }

        void tick() {
            if (owner instanceof BasicMachineBlockEntity basic)
                BasicMachineBlockEntity.serverTick(h.getLevel(), pos, owner.getBlockState(), basic);
            else if (owner instanceof BehaviorMachineBlockEntity behavior)
                BehaviorMachineBlockEntity.serverTick(h.getLevel(), pos, owner.getBlockState(), behavior);
            else if (owner instanceof BumbliaryBlockEntity bumbliary) bumbliary.serverTick();
            else throw new IllegalStateException("Not a native machine fixture");
        }

        void reset() {
            menu.setCarried(ItemStack.EMPTY);
            player.getInventory().clearContent();
            // Explicit transfer-boundary fixture seeding through normal native slot setters.
            for (int i = 0; i < playerStart; i++) menu.getSlot(i).set(ItemStack.EMPTY);
        }

        void cleanBaseline() {
            h.assertTrue(menu.stillValid(player) && menu.getCarried().isEmpty(), "live menu and empty cursor before click");
            chunk.setUnsaved(false);
            h.assertTrue(!chunk.isUnsaved(), "chunk is clean after all fixture setup");
        }

        void click(int slot) { menu.clicked(slot, 0, ClickType.QUICK_MOVE, player); }

        @Override public void close() {
            if (menu != null) { menu.setCarried(ItemStack.EMPTY); menu.removed(player); }
            player.getInventory().clearContent();
            h.getLevel().removeBlockEntity(pos); // Do not drop disposable test stock.
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
            for (int i = 0; i < f.playerStart; i++) machine.add(f.menu.getSlot(i).getItem().copy());
            for (int i = 0; i < f.player.getInventory().getContainerSize(); i++) player.add(f.player.getInventory().getItem(i).copy());
            return new Snapshot(machine, player);
        }

        void assertMatches(Fixture f, String stage) { assertExcept(f, stage, -1, -1); }

        void assertExcept(Fixture f, String stage, int machineExcept, int playerExcept) {
            for (int i = 0; i < machine.size(); i++) if (i != machineExcept)
                f.h.assertTrue(ItemStack.matches(machine.get(i), f.menu.getSlot(i).getItem()), stage + ": exact machine slot " + i);
            for (int i = 0; i < player.size(); i++) if (i != playerExcept)
                f.h.assertTrue(ItemStack.matches(player.get(i), f.player.getInventory().getItem(i)), stage + ": exact player slot " + i);
            f.h.assertTrue(f.menu.getCarried().isEmpty(), stage + ": empty cursor");
        }
    }

    private static void fillPlayer(Fixture f) {
        for (int i = 0; i < 36; i++) f.player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
    }

    private static ItemStack stackable(GameTestHelper h, ItemStack stack) {
        h.assertTrue(!stack.isEmpty() && stack.getMaxStackSize() == 64, "registered real item supports64-stack fixture");
        return stack.copyWithCount(1);
    }

    private static ItemStack drone(GameTestHelper h) {
        return stackable(h, BumbleBeeType.stack(GTBumbleSpecies.byId(30), BumbleBeeType.DRONE, null, 1));
    }

    private static ItemStack comb(GameTestHelper h) { return stackable(h, GTBumbleProducts.stack(30, 1)); }

    /** Full-set positive and blocked-transfer negative run before any expected failing assertion. */
    private static void withdrawalControls(Fixture f, int source, ItemStack item) {
        f.reset();
        f.menu.getSlot(source).set(item.copyWithCount(64));
        f.h.assertTrue(f.menu.getSlot(source).mayPickup(f.player), "survival may take the selected real source");
        var full = Snapshot.of(f);
        full.machine().set(source, ItemStack.EMPTY);
        full.player().set(8, item.copyWithCount(64));
        f.cleanBaseline();
        f.click(source);
        full.assertMatches(f, "full withdrawal control");
        f.h.assertTrue(f.chunk.isUnsaved(), "full native setter notifies owner");

        f.reset();
        f.menu.getSlot(source).set(item.copyWithCount(64));
        fillPlayer(f);
        var blocked = Snapshot.of(f);
        f.cleanBaseline();
        f.click(source);
        blocked.assertMatches(f, "blocked withdrawal control");
        f.h.assertTrue(!f.chunk.isUnsaved(), "blocked native transfer leaves chunk clean");
    }

    private static boolean partialWithdrawal(Fixture f, int source, ItemStack item) {
        f.reset();
        f.menu.getSlot(source).set(item.copyWithCount(64));
        fillPlayer(f);
        f.player.getInventory().setItem(8, item.copyWithCount(63));
        var expected = Snapshot.of(f);
        expected.machine().set(source, item.copyWithCount(63));
        expected.player().set(8, item.copyWithCount(64));
        f.cleanBaseline();
        f.click(source);
        expected.assertMatches(f, "partial withdrawal conserves127 matching items and all unrelated stacks");
        return f.chunk.isUnsaved();
    }

    private static boolean permittedMerge(Fixture f, int target, ItemStack item) {
        f.reset();
        f.menu.getSlot(target).set(item.copyWithCount(63));
        f.player.getInventory().setItem(9, item.copyWithCount(1));
        f.h.assertTrue(f.menu.getSlot(target).mayPlace(item), "real slot permits the intended input merge");
        var expected = Snapshot.of(f);
        expected.machine().set(target, item.copyWithCount(64));
        expected.player().set(9, ItemStack.EMPTY);
        f.cleanBaseline();
        f.click(f.playerStart);
        expected.assertMatches(f, "permitted existing-input merge conserves64 matching items");
        return f.chunk.isUnsaved();
    }

    private static boolean idleObservation(Fixture f) {
        var expected = Snapshot.of(f);
        f.tick();
        expected.assertMatches(f, "one idle native tick preserves transferred inventory");
        return f.chunk.isUnsaved();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void basicInputAndOutputQuickMovesNotifyOwner(GameTestHelper h) {
        try (var f = new Fixture(h, 0)) {
            f.open("gt6ce", "lathe_steel");
            var machine = (BasicMachineBlockEntity) f.owner;
            h.assertTrue(machine.recipeMap() == MachineRecipeMaps.Lathe && machine.recipeMap().mInputItemsCount == 1,
                    "actual core lathe has the expected input/output layout");
            var item = stackable(h, new ItemStack(Items.STONE));
            int output = machine.recipeMap().mInputItemsCount;
            withdrawalControls(f, output, item);
            boolean input = partialWithdrawal(f, 0, item);
            boolean outputDirty = partialWithdrawal(f, output, item);
            boolean afterIdle = idleObservation(f);
            boolean merge = permittedMerge(f, 0, item);
            h.assertTrue(input && outputDirty && afterIdle && merge, "basic notifications: inputPartial=" + input
                    + ", outputPartial=" + outputDirty + ", afterIdle=" + afterIdle + ", inputMerge=" + merge);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void autocrafterProgramQuickMovesNotifyOwner(GameTestHelper h) {
        try (var f = new Fixture(h, 1)) {
            f.open("gt6ce", "autocrafter_galvanized_steel");
            var menu = (BasicMachineContainerMenu) f.menu;
            h.assertTrue(menu.recipeMap() == MachineRecipeMaps.Autocrafter && menu.programSlot() >= 0,
                    "actual core autocrafter exposes its separate program slot");
            var item = stackable(h, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "blueprint"))));
            withdrawalControls(f, menu.programSlot(), item);
            boolean partial = partialWithdrawal(f, menu.programSlot(), item);
            boolean merge = permittedMerge(f, menu.programSlot(), item);
            h.assertTrue(partial && merge, "program notifications: partial=" + partial + ", existingMerge=" + merge);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void behaviorQuickMovesNotifyActualOwners(GameTestHelper h) {
        boolean partial, afterIdle, merge;
        try (var f = new Fixture(h, 2)) {
            f.open("addon_sample", "sample_miner");
            var item = stackable(h, new ItemStack(Items.STONE));
            h.assertTrue(f.playerStart == 1 && !f.menu.getSlot(0).mayPlace(item), "shipped miner has one real output");
            withdrawalControls(f, 0, item);
            partial = partialWithdrawal(f, 0, item);
            afterIdle = idleObservation(f); // Fresh miner has no energy/progress/source; its idle branch does not dirty.
        }
        try (var f = new Fixture(h, 3)) {
            f.open("addon_sample", "irrigator");
            var item = stackable(h, new ItemStack(Items.BONE_MEAL));
            withdrawalControls(f, 0, item);
            merge = permittedMerge(f, 0, item);
            // Do not tick here: irrigator state.setInt dirties on every tick and would mask this boundary.
        }
        h.assertTrue(partial && afterIdle && merge, "behavior notifications: minerPartial=" + partial
                + ", minerAfterIdle=" + afterIdle + ", irrigatorImmediateMerge=" + merge);
        h.succeed();
    }

    private static void bumbliaryNotifications(GameTestHelper h, boolean advanced) {
        try (var f = new Fixture(h, advanced ? 5 : 4)) {
            f.open("gt6ce", advanced ? "advanced_bumbliary" : "bumbliary");
            var machine = (BumbliaryBlockEntity) f.owner;
            h.assertTrue(machine.layout().advanced() == advanced, "actual apiary layout matches registered variant");
            int output = machine.layout().combs()[0];
            withdrawalControls(f, output, comb(h));
            boolean partial = partialWithdrawal(f, output, comb(h));
            boolean afterIdle = idleObservation(f); // Queenless default state; no lifespan/pairing mutation masks it.
            boolean merge = permittedMerge(f, machine.layout().drone(), drone(h));
            h.assertTrue(partial && afterIdle && merge, (advanced ? "advanced" : "normal")
                    + " apiary notifications: combPartial=" + partial + ", afterIdle=" + afterIdle + ", droneMerge=" + merge);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void bumbliaryQuickMovesNotifyOwner(GameTestHelper h) { bumbliaryNotifications(h, false); }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void advancedBumbliaryQuickMovesNotifyOwner(GameTestHelper h) { bumbliaryNotifications(h, true); }

    private static void emptyOutputRejects(Fixture f, int target, ItemStack item) {
        f.reset();
        f.player.getInventory().setItem(9, item.copyWithCount(1));
        f.h.assertTrue(!f.menu.getSlot(target).mayPlace(item), "declared output rejects player placement");
        var expected = Snapshot.of(f);
        f.cleanBaseline();
        f.click(f.playerStart);
        expected.assertMatches(f, "empty output rejects shift-insertion control");
        f.h.assertTrue(!f.chunk.isUnsaved(), "rejected empty-output insertion is a real no-op");
    }

    private record Admission(boolean rejected, int outputCount, int playerCount) {}

    private static Admission existingOutputAdmission(Fixture f, int target, ItemStack item) {
        f.reset();
        f.menu.getSlot(target).set(item.copyWithCount(63));
        f.player.getInventory().setItem(9, item.copyWithCount(1));
        f.h.assertTrue(!f.menu.getSlot(target).mayPlace(item), "existing output still forbids placement");
        var before = Snapshot.of(f);
        f.cleanBaseline();
        f.click(f.playerStart);
        before.assertExcept(f, "output admission changes no unrelated slot or cursor", target, 9);
        var output = f.menu.getSlot(target).getItem();
        var player = f.player.getInventory().getItem(9);
        int outputCount = output.getCount(), playerCount = player.getCount();
        f.h.assertTrue((outputCount == 63 || outputCount == 64) && outputCount + playerCount == 64
                && ItemStack.matches(output, item.copyWithCount(outputCount))
                && ItemStack.matches(player, playerCount == 0 ? ItemStack.EMPTY : item.copyWithCount(playerCount)),
                "output-admission observation conserves exactly64 identical items; not a duplication claim");
        return new Admission(outputCount == 63 && playerCount == 1, outputCount, playerCount);
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void behaviorQuickMoveRejectsExistingOutputMerge(GameTestHelper h) {
        try (var f = new Fixture(h, 6)) {
            f.open("addon_sample", "irrigator");
            permittedMerge(f, 0, stackable(h, new ItemStack(Items.BONE_MEAL))); // Positive admission, independent of dirty result.
        }
        Admission result;
        try (var f = new Fixture(h, 7)) {
            f.open("addon_sample", "sample_miner");
            var item = stackable(h, new ItemStack(Items.STONE));
            withdrawalControls(f, 0, item);
            emptyOutputRejects(f, 0, item);
            result = existingOutputAdmission(f, 0, item);
        }
        h.assertTrue(result.rejected(), "behavior existing-output merge must be rejected: " + result);
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void bumbliaryQuickMovesRejectExistingOutputMerges(GameTestHelper h) {
        var results = new ArrayList<Admission>();
        for (boolean advanced : new boolean[]{false, true}) try (var f = new Fixture(h, advanced ? 9 : 8)) {
            f.open("gt6ce", advanced ? "advanced_bumbliary" : "bumbliary");
            var machine = (BumbliaryBlockEntity) f.owner;
            int output = machine.layout().combs()[0];
            withdrawalControls(f, output, comb(h));
            permittedMerge(f, machine.layout().drone(), drone(h));
            emptyOutputRejects(f, output, comb(h));
            results.add(existingOutputAdmission(f, output, comb(h)));
        }
        h.assertTrue(results.stream().allMatch(Admission::rejected), "normal/advanced apiary existing-output merges must be rejected: " + results);
        h.succeed();
    }
}
