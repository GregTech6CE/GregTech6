package gregtech.gametest;

import gregtech.content.block.energy.EnergyNodeBlock;
import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.GregTechTags;
import gregtech.content.item.ChemicalBatteryItem;
import gregtech.content.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Real server block-use dispatch; never substitutes a direct installBattery call. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class CrystalChargerInteractionTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void nativeCrystalInsertionUsesLuAndPreservesCharge(GameTestHelper h) {
        try (var fixture = new Fixture(h, 0)) {
            for (String path : new String[]{"crystal_charger_lv", "large_crystal_charger_lv"})
                for (boolean cyan : new boolean[]{false, true})
                    for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
                        var node = fixture.place(path);
                        var crystal = crystal(h, cyan);
                        var item = (ChemicalBatteryItem) crystal.getItem();
                        fixture.player.setGameMode(mode);
                        fixture.player.setItemInHand(InteractionHand.MAIN_HAND, crystal);
                        h.assertTrue(h.getLevel().hasChunkAt(fixture.pos) && !node.isRemoved()
                                && h.getLevel().getBlockEntity(fixture.pos) == node
                                && h.getLevel().mayInteract(fixture.player, fixture.pos)
                                && fixture.player.mayUseItemAt(fixture.pos, Direction.UP, crystal),
                                "loaded, live and permitted native block-use target");
                        var result = fixture.click(InteractionHand.MAIN_HAND);
                        h.assertTrue(result.consumesAction() && installed(node) == 1,
                                "native block use installs one LU crystal: " + path + "/" + cyan + "/" + mode);
                        var stored = node.batteryInventory().getStackInSlot(0);
                        h.assertTrue(stored.is(item) && stored.getCount() == 1 && item.stored(stored) == 1024,
                                "item identity and exact charge survive transfer into host");
                        h.assertTrue(fixture.player.getMainHandItem().getCount() == (mode == GameType.CREATIVE ? 1 : 0),
                                "survival consumes exactly one; creative retains held crystal");
                    }
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void nativeChargerInsertionKeepsEnergyAndInteractionGuards(GameTestHelper h) {
        try (var fixture = new Fixture(h, 16)) {
            var node = fixture.place("crystal_charger_lv");
            var battery = new ItemStack(BuiltInRegistries.ITEM.get(id("battery_lead_acid_lv")));
            h.assertTrue(!battery.isEmpty() && EnergyNodeBlockEntity.batteryCapacityOf(battery) > 0,
                    "real EU battery and legacy EU helper remain available");
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, battery);
            fixture.click(InteractionHand.MAIN_HAND);
            h.assertTrue(installed(node) == 0 && fixture.player.getMainHandItem().getCount() == 1,
                    "LU charger rejects EU battery without consumption");
            fixture.player.closeContainer();

            var lu = crystal(h, false);
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            fixture.player.setItemInHand(InteractionHand.OFF_HAND, lu);
            fixture.click(InteractionHand.OFF_HAND);
            h.assertTrue(installed(node) == 0 && fixture.player.getOffhandItem().getCount() == 1
                    && fixture.player.getMainHandItem().is(Items.STICK), "offhand does not double-insert");
            fixture.player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, lu);
            fixture.player.setGameMode(GameType.SPECTATOR);
            fixture.click(InteractionHand.MAIN_HAND);
            h.assertTrue(installed(node) == 0 && fixture.player.getMainHandItem().getCount() == 1,
                    "native spectator dispatch cannot insert or consume the crystal");
            fixture.player.closeContainer();
            fixture.player.setGameMode(GameType.SURVIVAL);

            node = fixture.place("battery_box_lv");
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, lu);
            fixture.click(InteractionHand.MAIN_HAND);
            h.assertTrue(installed(node) == 0 && fixture.player.getMainHandItem().getCount() == 1,
                    "ordinary EU box still rejects LU crystals");
            fixture.player.closeContainer();
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, battery);
            fixture.click(InteractionHand.MAIN_HAND);
            h.assertTrue(installed(node) == 1 && fixture.player.getMainHandItem().isEmpty()
                    && node.batteryInventory().getStackInSlot(0).is(BuiltInRegistries.ITEM.get(id("battery_lead_acid_lv"))),
                    "ordinary EU box retains real right-click insertion path");
        }
        h.succeed();
    }

    private static ItemStack crystal(GameTestHelper h, boolean cyan) {
        var material = GTMaterialRegistry.get(cyan ? "EnergiumCyan" : "EnergiumRed");
        var stack = GTItems.getStack(MaterialPrefix.gemFlawed, material).copyWithCount(1);
        h.assertTrue(stack.getItem() instanceof ChemicalBatteryItem, "actual canonical Energium crystal is registered");
        var item = (ChemicalBatteryItem) stack.getItem();
        h.assertTrue(BuiltInRegistries.ITEM.getKey(item).equals(id(MaterialPrefix.gemFlawed.getItemId(material)))
                        && item.isEnergyType(stack, GregTechTags.Energy.LU)
                        && !item.isEnergyType(stack, GregTechTags.Energy.EU)
                        && item.getEnergyCapacity(stack, GregTechTags.Energy.LU) == 32L * (cyan ? 800000 : 400000),
                "canonical LV identity and exact LU-only capacity");
        item.setCharge(stack, 1024);
        h.assertTrue(item.stored(stack) == 1024, "fixture starts with exact charged LU crystal");
        return stack;
    }

    private static int installed(EnergyNodeBlockEntity node) {
        int count = 0;
        for (int slot = 0; slot < node.batteryInventory().getSlots(); slot++)
            count += node.batteryInventory().getStackInSlot(slot).getCount();
        return count;
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("gt6ce", path); }

    private static final class Fixture implements AutoCloseable {
        private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        final GameTestHelper h;
        final BlockPos pos;
        final BlockState oldState;
        final CompoundTag oldData;
        final ServerPlayer player;

        Fixture(GameTestHelper h, int offset) {
            this.h = h;
            pos = new BlockPos(26_600_000 + offset, 220, 26_600_000);
            oldState = h.getLevel().getBlockState(pos);
            var old = h.getLevel().getBlockEntity(pos);
//? if >=1.21.1 {
            /*oldData = old == null ? null : old.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
            oldData = old == null ? null : old.saveWithoutMetadata();
//?}
            player = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[gt-lu-use]"));
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + 1.5);
        }

        EnergyNodeBlockEntity place(String path) {
            player.closeContainer();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            var block = BuiltInRegistries.BLOCK.get(id(path));
            h.assertTrue(block instanceof EnergyNodeBlock, "registered energy node: " + path);
            h.getLevel().removeBlockEntity(pos);
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(pos, block.defaultBlockState(), FLAGS);
            var entity = h.getLevel().getBlockEntity(pos);
            h.assertTrue(entity instanceof EnergyNodeBlockEntity node && node.isBatteryBox(), "real installed battery host");
            return (EnergyNodeBlockEntity) entity;
        }

        net.minecraft.world.InteractionResult click(InteractionHand hand) {
            return player.gameMode.useItemOn(player, h.getLevel(), player.getItemInHand(hand), hand,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        }

        @Override public void close() {
            player.closeContainer();
            player.getInventory().clearContent();
            h.getLevel().removeBlockEntity(pos);
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(pos, oldState, FLAGS);
            var restored = h.getLevel().getBlockEntity(pos);
            if (restored != null && oldData != null) {
//? if >=1.21.1 {
                /*restored.loadWithComponents(oldData, h.getLevel().registryAccess());
*///?} else {
                restored.load(oldData);
//?}
            }
        }
    }
}
