package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.AdvancedCraftingTableBlockEntity;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.blockentity.inventory.DrawerQuadBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BehaviorMachineBlockEntity;
import com.gregtech.gregtech.client.gui.AdvancedCraftingMenu;
import com.gregtech.gregtech.client.gui.BehaviorMachineMenu;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Actual registered owners and native menu clicks; validity assertions are not packet/auto-close tests. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class MenuLifetimeCapacityTests {
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

    private static Tag saveItem(GameTestHelper h, ItemStack stack) {
//? if >=1.21.1 {
/*        return stack.save(h.getLevel().registryAccess());
*///?} else {
        return stack.save(new CompoundTag());
//?}
    }

    private static ItemStack loadItem(GameTestHelper h, CompoundTag tag) {
//? if >=1.21.1 {
/*        return ItemStack.parseOptional(h.getLevel().registryAccess(), tag);
*///?} else {
        return ItemStack.of(tag);
//?}
    }

    private static Block block(String namespace, String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    /** One unique high-Y cell per case; teardown restores native state/data without dropping test stock. */
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final BlockPos pos;
        final BlockState oldState;
        final CompoundTag oldData;
        final Player player;
        final List<AbstractContainerMenu> menus = new ArrayList<>();

        Fixture(GameTestHelper h, int index) {
            this.h = h;
            pos = new BlockPos(27_000_000 + index * 128, 220, 27_000_000);
            oldState = h.getLevel().getBlockState(pos);
            var old = h.getLevel().getBlockEntity(pos);
            oldData = old == null ? null : save(h, old);
            player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "MenuCapacity"));
            near();
        }

        void near() { player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5); }

        BlockEntity place(BlockState state) {
            h.getLevel().removeBlockEntity(pos);
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(pos, state, FLAGS);
            return h.getLevel().getBlockEntity(pos);
        }

        <T extends AbstractContainerMenu> T keep(T menu) {
            h.assertTrue(menu != null, "actual owner supplies its native menu");
            menus.add(menu);
            return menu;
        }

        @Override public void close() {
            for (var menu : menus) menu.setCarried(ItemStack.EMPTY);
            player.getInventory().clearContent();
            h.getLevel().removeBlockEntity(pos);
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(pos, oldState, FLAGS);
            var restored = h.getLevel().getBlockEntity(pos);
            if (restored != null && oldData != null) load(h, restored, oldData);
        }
    }

    private static void rejects(GameTestHelper h, AbstractContainerMenu menu, Player player, String reason) {
        h.assertTrue(!menu.stillValid(player), reason);
        if (menu instanceof BehaviorMachineMenu behavior) {
            boolean before = behavior.isOn(0);
            h.assertTrue(!behavior.clickMenuButton(player, 0) && behavior.isOn(0) == before,
                    "invalid behavior menu cannot toggle its retained owner: " + reason);
        }
    }

    private static void ownerLifecycle(GameTestHelper h, Fixture f, BlockEntity owner,
                                       AbstractContainerMenu menu, Function<BlockEntity, AbstractContainerMenu> provider) {
        h.assertTrue(menu.stillValid(f.player), "nearby live owner is valid");
        f.player.setPos(f.pos.getX() + 32.5, f.pos.getY() + .5, f.pos.getZ() + .5);
        rejects(h, menu, f.player, "distant player is invalid");
        f.near();
        h.assertTrue(menu.stillValid(f.player), "returning near the same owner is valid");

        var otherLevel = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(otherLevel != null && otherLevel != h.getLevel(), "native server exposes another dimension");
        var other = FakePlayerFactory.get(otherLevel, new GameProfile(UUID.randomUUID(), "OtherMenu"));
        other.setPos(f.player.getX(), f.player.getY(), f.player.getZ());
        rejects(h, menu, other, "same coordinates in a different world are invalid");

        owner.setRemoved();
        rejects(h, menu, f.player, "removed owner is invalid before replacement");
        owner.clearRemoved();
        h.assertTrue(menu.stillValid(f.player), "restoring the same live owner restores validity");
        BlockState state = owner.getBlockState();
        h.getLevel().setBlock(f.pos, Blocks.AIR.defaultBlockState(), FLAGS);
        rejects(h, menu, f.player, "native block removal invalidates the retained menu");
        h.getLevel().setBlock(f.pos, state, FLAGS);
        var replacement = h.getLevel().getBlockEntity(f.pos);
        h.assertTrue(replacement != null && replacement != owner, "same block creates a fresh native owner");
        var replacementMenu = f.keep(provider.apply(replacement));
        h.assertTrue(replacementMenu.stillValid(f.player), "replacement's own provider works");
        owner.clearRemoved(); // Isolate current-owner identity from the removed flag.
        try {
            boolean replacementToggle = replacementMenu instanceof BehaviorMachineMenu behavior && behavior.isOn(0);
            rejects(h, menu, f.player, "stale apparently-live owner cannot bind to same-block replacement");
            if (replacementMenu instanceof BehaviorMachineMenu behavior) {
                h.assertTrue(behavior.isOn(0) == replacementToggle, "stale button does not mutate replacement");
                h.assertTrue(behavior.clickMenuButton(f.player, 0) && behavior.isOn(0) != replacementToggle,
                        "replacement's live toggle still works");
            }
        } finally { owner.setRemoved(); }
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void behaviorMenuRequiresLiveOwnerAndNativeReach(GameTestHelper h) {
        try (var f = new Fixture(h, 0)) {
            var owner = f.place(block("addon_sample", "irrigator").defaultBlockState());
            h.assertTrue(owner instanceof BehaviorMachineBlockEntity, "shipped sample GUI has a registered behavior owner");
            var machine = (BehaviorMachineBlockEntity) owner;
            var menu = f.keep((BehaviorMachineMenu) machine.createMenu(1, f.player.getInventory(), f.player));
            boolean initial = menu.isOn(0);
            h.assertTrue(menu.clickMenuButton(f.player, 0) && menu.isOn(0) != initial, "live toggle changes exactly once");
            h.assertTrue(!menu.clickMenuButton(f.player, -1) && !menu.clickMenuButton(f.player, 99), "invalid toggle indexes are rejected");
            menu.setCarried(new ItemStack(Items.BONE_MEAL, 3));
            menu.clicked(0, 0, ClickType.PICKUP, f.player);
            h.assertTrue(menu.getCarried().isEmpty() && machine.inventory().getStackInSlot(0).getCount() == 3,
                    "live sample menu accepts its real bone-meal input");
            menu.clicked(0, 0, ClickType.PICKUP, f.player);
            h.assertTrue(menu.getCarried().getCount() == 3 && machine.inventory().getStackInSlot(0).isEmpty(),
                    "live sample extraction conserves its input");
            menu.setCarried(ItemStack.EMPTY);
//? if >=1.21.1 {
/*            var reach = f.player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
*///?} else {
            var reach = f.player.getAttribute(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get());
//?}
            h.assertTrue(reach != null, "native block-reach attribute exists");
            double oldReach = reach.getBaseValue();
            try {
                reach.setBaseValue(12);
                f.player.setPos(f.pos.getX() + 12.5, f.pos.getY() + .5, f.pos.getZ() + .5);
                h.assertTrue(menu.stillValid(f.player), "owner guard preserves the loader's extended block reach");
            } finally { reach.setBaseValue(oldReach); f.near(); }
            ownerLifecycle(h, f, machine, menu,
                    replacement -> ((BehaviorMachineBlockEntity) replacement).createMenu(2, f.player.getInventory(), f.player));
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void batteryMenusRequireLiveNearbyOwners(GameTestHelper h) {
        for (int index = 0; index < 2; index++) try (var f = new Fixture(h, 1 + index)) {
            var owner = f.place(block("gt6ce", index == 0 ? "battery_box_lv" : "energy_storage_lv").defaultBlockState());
            h.assertTrue(owner instanceof EnergyNodeBlockEntity, "registered battery owner exists");
            var node = (EnergyNodeBlockEntity) owner;
            h.assertTrue(node.isBatteryBox() && node.batteryInventory().getSlots() == (index == 0 ? 4 : 16),
                    "actual small and large battery providers have their native slot count");
            var menu = f.keep(node.batteryMenu().createMenu(1, f.player.getInventory(), f.player));
            var battery = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "battery_lead_acid_lv")));
            h.assertTrue(!battery.isEmpty() && EnergyNodeBlockEntity.batteryCapacityOf(battery) > 0, "real energy-item fixture exists");
            menu.setCarried(battery.copy());
            menu.clicked(0, 0, ClickType.PICKUP, f.player);
            h.assertTrue(menu.getCarried().isEmpty() && node.batteryInventory().getStackInSlot(0).getCount() == 1,
                    "live menu inserts one actual battery");
            menu.clicked(0, 0, ClickType.PICKUP, f.player);
            h.assertTrue(menu.getCarried().is(battery.getItem()) && menu.getCarried().getCount() == 1
                    && node.batteryInventory().getStackInSlot(0).isEmpty(), "live battery extraction conserves the cell");
            menu.setCarried(ItemStack.EMPTY);
            f.player.setPos(f.pos.getX() + 8.5, f.pos.getY() + .5, f.pos.getZ() + .5);
            h.assertTrue(menu.stillValid(f.player), "exact eight-block storage boundary remains valid");
            f.player.setPos(f.pos.getX() + 8.51, f.pos.getY() + .5, f.pos.getZ() + .5);
            rejects(h, menu, f.player, "beyond eight blocks is invalid");
            f.near();
            ownerLifecycle(h, f, node, menu,
                    replacement -> ((EnergyNodeBlockEntity) replacement).batteryMenu().createMenu(2, f.player.getInventory(), f.player));
        }
        h.succeed();
    }

    private static int drawerTotal(GameTestHelper h, DrawerQuadBlockEntity drawer) {
        var saved = save(h, drawer);
        int total = 0;
        for (var entry : saved.getCompound("Inventory").getList("Items", Tag.TAG_COMPOUND))
            total += loadItem(h, (CompoundTag) entry).getCount();
        for (var entry : saved.getList("LegacyOverflow", Tag.TAG_COMPOUND))
            total += loadItem(h, (CompoundTag) entry).getCount();
        return total;
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void drawerCapacityQueriesPreserveLegacyOverflow(GameTestHelper h) {
        try (var f = new Fixture(h, 3)) {
            var owner = f.place(block("gt6ce", "drawer_quad").defaultBlockState());
            h.assertTrue(owner instanceof DrawerQuadBlockEntity, "registered drawer creates its native inventory");
            var drawer = (DrawerQuadBlockEntity) owner;
            var legacy = new CompoundTag();
            var compartments = new ListTag();
            for (int page = 0; page < 4; page++) {
                var entry = new CompoundTag();
                entry.put("item", saveItem(h, new ItemStack(Items.STONE)));
                entry.putLong("count", 4096);
                compartments.add(entry);
            }
            legacy.put("gt.compartments", compartments);
            load(h, drawer, legacy);
            h.assertTrue(drawerTotal(h, drawer) == 16384 && drawer.items().getStackInSlot(143).getCount() == 64,
                    "supported legacy four-by4096 save fills144 normal slots and retains all overflow");
            for (int page = 0; page < 4; page++) {
                var pageMenu = f.keep(drawer.createMenu(page, page + 1, f.player.getInventory()));
                h.assertTrue(pageMenu.stillValid(f.player) && pageMenu.slotCount() == 36, "actual drawer page is usable");
                for (int slot : new int[]{0, 35}) h.assertTrue(pageMenu.getSlot(slot).getMaxStackSize(new ItemStack(Items.STONE)) == 64
                        && drawerTotal(h, drawer) == 16384, "capacity query does not consume legacy stock on any page");
            }
            var menu = f.keep(drawer.createMenu(0, 5, f.player.getInventory()));
            menu.setCarried(new ItemStack(Items.STONE));
            menu.clicked(0, 0, ClickType.PICKUP, f.player);
            h.assertTrue(menu.getCarried().getCount() == 1 && drawer.items().getStackInSlot(0).getCount() == 64
                    && drawerTotal(h, drawer) == 16384, "native full-slot PICKUP places nothing and destroys no overflow");
            menu.setCarried(ItemStack.EMPTY);
            menu.clicked(0, 0, ClickType.PICKUP, f.player);
            h.assertTrue(menu.getCarried().getCount() == 64 && drawer.items().getStackInSlot(0).getCount() == 64
                    && drawerTotal(h, drawer) == 16384 - 64, "real withdrawal still refills the slot and conserves stock");
            var saved = save(h, drawer);
            load(h, drawer, new CompoundTag());
            load(h, drawer, saved);
            h.assertTrue(drawerTotal(h, drawer) + menu.getCarried().getCount() == 16384,
                    "native serialization retains exactly the remaining migrated inventory");
            load(h, drawer, new CompoundTag());
            menu.setCarried(new ItemStack(Items.STONE, 2));
            menu.clicked(0, 0, ClickType.PICKUP, f.player);
            h.assertTrue(menu.getCarried().isEmpty() && drawer.items().getStackInSlot(0).getCount() == 2,
                    "ordinary empty drawer still accepts real items");
            menu.clicked(0, 0, ClickType.PICKUP, f.player);
            h.assertTrue(menu.getCarried().getCount() == 2 && drawer.items().getStackInSlot(0).isEmpty(),
                    "ordinary non-overflow withdrawal remains correct");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void advancedCraftingCapacityQueriesPreserveFlush(GameTestHelper h) {
        try (var f = new Fixture(h, 4)) {
            var owner = f.place(block("gt6ce", "advanced_crafting_table").defaultBlockState());
            h.assertTrue(owner instanceof AdvancedCraftingTableBlockEntity, "registered advanced table exists");
            var table = (AdvancedCraftingTableBlockEntity) owner;
            table.items().setStackInSlot(21, new ItemStack(Items.STONE, 64));
            table.flushGrid();
            var menu = f.keep(new AdvancedCraftingMenu(1, f.player.getInventory(), table));
            h.assertTrue(menu.stillValid(f.player) && (table.modes() & 16) != 0, "real table grid starts in flush mode");
            h.assertTrue(menu.getSlot(AdvancedCraftingMenu.GRID_START).getMaxStackSize(new ItemStack(Items.STONE)) == 64
                    && (table.modes() & 16) != 0, "capacity query cannot transiently empty the last grid cell");
            menu.setCarried(new ItemStack(Items.STONE));
            menu.clicked(AdvancedCraftingMenu.GRID_START, 0, ClickType.PICKUP, f.player);
            h.assertTrue(menu.getCarried().getCount() == 1 && table.items().getStackInSlot(21).getCount() == 64
                    && (table.modes() & 16) != 0, "native full-grid PICKUP preserves flush and all65 items");
            menu.setCarried(ItemStack.EMPTY);
            menu.clicked(AdvancedCraftingMenu.GRID_START, 0, ClickType.PICKUP, f.player);
            h.assertTrue(menu.getCarried().getCount() == 64 && table.items().getStackInSlot(21).isEmpty()
                    && (table.modes() & 16) == 0, "real final-grid extraction still clears flush");
            menu.setCarried(new ItemStack(Items.STONE, 2));
            menu.clicked(AdvancedCraftingMenu.GRID_START, 0, ClickType.PICKUP, f.player);
            h.assertTrue(menu.getCarried().isEmpty() && table.items().getStackInSlot(21).getCount() == 2,
                    "normal grid insertion remains functional");
            var clientCopy = f.keep(new AdvancedCraftingMenu(2, f.player.getInventory()));
            h.assertTrue(clientCopy.getSlot(0).getMaxStackSize(new ItemStack(Items.STONE, 64)) == 1,
                    "client constructor still honors the blueprint slot's specialized limit");
        }
        h.succeed();
    }
}
