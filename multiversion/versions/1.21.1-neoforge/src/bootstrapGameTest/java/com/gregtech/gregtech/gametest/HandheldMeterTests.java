package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.AxleBlock;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.misc.ChargingCraftingTableBlockEntity;
import com.gregtech.gregtech.blockentity.energy.AxleBlockEntity;
import com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.blockentity.energy.GearboxBlockEntity;
import com.gregtech.gregtech.content.energy.ChemicalBatterySpec;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTChemicalBatteries;
import com.gregtech.gregtech.registry.GTWires;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Registered items, actual block entities and transfer methods; only chat delivery is recorded. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class HandheldMeterTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void electrometerReadsAcceptedWirePowerThroughRegisteredItem(GameTestHelper helper) {
        var pos = new BlockPos(1, 2, 1);
        var block = GTWires.allCables().stream().map(entry -> entry.get())
                .filter(entry -> entry.spec().material() == Materials.Copper && entry.spec().size() == 1)
                .findFirst().orElseThrow();
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, block.defaultBlockState().setValue(ElectricWireBlock.WEST, true).setValue(ElectricWireBlock.EAST, true));
        var wire = (ElectricWireBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.setBlock(pos.east(), BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "charging_crafting_table")));
        var receiver = (ChargingCraftingTableBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos.east()));
        receiver.items().setStackInSlot(16, new ItemStack(GTChemicalBatteries.item(ChemicalBatterySpec.Chemistry.NICKEL_CADMIUM, 1)));
        wire.serverTick();
        helper.assertTrue(wire.doEnergyInjection(GregTechTags.Energy.EU, Direction.WEST, 32, 1, true) == 1, "real receiver accepts wire packet");
        wire.serverTick();
        long expected = 32 - wire.lossPerBlock();
        helper.assertTrue(expected > 0 && wire.lastWattage() == expected, "actual transferred EU after cable loss");
        var player = new RecordingPlayer(helper.getLevel());
        var meter = item("electrometer");
        for (var face : Direction.values()) {
            helper.assertTrue(click(helper, player, meter, pos, face) == InteractionResult.SUCCESS, "meter handles every clicked face");
            reading(helper, player.messages.get(player.messages.size() - 1), "message.gt6ce.meter.electric", expected);
        }
        helper.assertTrue(player.messages.size() == 6 && meter.getCount() == 1, "one server message per click, no item consumption");
        helper.assertTrue(wire.lastWattage() == expected, "measurement leaves wire counter unchanged");
        wire.serverTick();
        click(helper, player, meter, pos, Direction.UP);
        reading(helper, player.messages.get(6), "message.gt6ce.meter.electric", 0);
        helper.assertTrue(click(helper, player, item("tachometer"), pos, Direction.UP) == InteractionResult.PASS
                && player.messages.size() == 7, "rotary meter cannot invent wire measurement");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void tachometerReadsRealAxleStartupTransfer(GameTestHelper helper) {
        var pos = new BlockPos(1, 2, 1);
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "axle_steel_1"));
        helper.assertTrue(block instanceof AxleBlock, "registered steel axle exists");
        helper.setBlock(pos, block.defaultBlockState().setValue(AxleBlock.WEST, true).setValue(AxleBlock.EAST, true));
        var axle = (AxleBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        axle.serverTick();
        helper.assertTrue(axle.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, 32, 1, true) == 1, "actual startup interval accepts RU");
        axle.serverTick();
        helper.assertTrue(axle.transferredLast() == 32, "native axle publishes previous transferred RU");
        var player = new RecordingPlayer(helper.getLevel());
        var meter = item("tachometer");
        helper.assertTrue(click(helper, player, meter, pos, Direction.NORTH) == InteractionResult.SUCCESS, "meter reads even off-axis clicked face");
        reading(helper, player.messages.get(0), "message.gt6ce.meter.rotary", 32);
        helper.assertTrue(meter.getCount() == 1 && axle.transferredLast() == 32, "reading consumes neither item nor energy");
        helper.assertTrue(click(helper, player, item("electrometer"), pos, Direction.UP) == InteractionResult.PASS
                && player.messages.size() == 1, "electric meter cannot invent axle measurement");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void tachometerReportsGearboxSidesAndTransferredPower(GameTestHelper helper) {
        var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "gearbox_bronze")));
        var box = (GearboxBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        box.mountGear(Direction.NORTH);
        box.mountGear(Direction.EAST);
        var sink = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "rotation_transformer_bronze"));
        helper.setBlock(pos.east(), sink.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.WEST));
        var receiver = (EnergyNodeBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos.east()));
        helper.assertTrue(box.doEnergyInjection(GregTechTags.Energy.RU, Direction.NORTH, 32, 2, true) == 2, "real gearbox accepts packets");
        var player = new RecordingPlayer(helper.getLevel());
        var meter = item("tachometer");
        helper.assertTrue(click(helper, player, meter, pos, Direction.UP) == InteractionResult.SUCCESS, "registered tachometer handles gearbox");
        helper.assertTrue(player.messages.size() == 3, "only two connected faces plus rotary reading");
        side(helper, player.messages.get(0), "message.gt6ce.meter.input_side", "north");
        side(helper, player.messages.get(1), "message.gt6ce.meter.output_side", "east");
        reading(helper, player.messages.get(2), "message.gt6ce.meter.rotary", 0);
        GearboxBlockEntity.serverTick(helper.getLevel(), box.getBlockPos(), box.getBlockState(), box);
        helper.assertTrue(receiver.stored() == 64 && box.transferredLast() == 64, "production gearbox emits conserved64RU to real transformer");
        player.messages.clear();
        click(helper, player, meter, pos, Direction.WEST);
        helper.assertTrue(player.messages.size() == 3, "same connected faces after transfer");
        reading(helper, player.messages.get(2), "message.gt6ce.meter.rotary", 64);
        helper.assertTrue(meter.getCount() == 1 && receiver.stored() == 64, "readout does not inject or consume energy");
        box.toggleAxis(Direction.Axis.Y);
        player.messages.clear();
        click(helper, player, meter, pos, Direction.DOWN);
        helper.assertTrue(player.messages.size() == 5, "axle-only faces also have original diagnostics");
        side(helper, player.messages.get(0), "message.gt6ce.meter.output_side", "down");
        side(helper, player.messages.get(1), "message.gt6ce.meter.output_side", "up");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void handheldMetersRejectUnsupportedTargetsWithoutConsumption(GameTestHelper helper) {
        var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.STONE);
        var player = new RecordingPlayer(helper.getLevel());
        for (String id : new String[]{"electrometer", "tachometer"}) {
            var meter = item(id);
            for (var face : Direction.values())
                helper.assertTrue(click(helper, player, meter, pos, face) == InteractionResult.PASS, "unsupported target remains PASS");
            helper.assertTrue(meter.getCount() == 1 && player.getMainHandItem() == meter, "unsupported clicks preserve stack identity/count");
        }
        helper.assertTrue(player.messages.isEmpty(), "unsupported targets report no invented measurement");
        helper.succeed();
    }

    private static ItemStack item(String id) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
        if (item == Items.AIR) throw new AssertionError("missing registered meter " + id);
        return new ItemStack(item);
    }

    private static InteractionResult click(GameTestHelper helper, RecordingPlayer player, ItemStack stack, BlockPos pos, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var absolute = helper.absolutePos(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute), face, absolute, false);
        return stack.getItem().onItemUseFirst(stack, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static void reading(GameTestHelper helper, Component message, String key, long expected) {
        helper.assertTrue(message.getContents() instanceof TranslatableContents, "localized measurement component");
        var text = (TranslatableContents) message.getContents();
        helper.assertTrue(text.getKey().equals(key) && text.getArgs().length == 1
                && text.getArgs()[0] instanceof Number value && value.longValue() == expected,
                "measurement/unit contract " + key + " expected=" + expected + " actual=" + message);
    }

    private static void side(GameTestHelper helper, Component message, String key, String direction) {
        helper.assertTrue(message.getContents() instanceof TranslatableContents, "localized gearbox side component");
        var text = (TranslatableContents) message.getContents();
        helper.assertTrue(text.getKey().equals(key) && text.getArgs().length == 1
                && text.getArgs()[0] instanceof Component face && face.getContents() instanceof TranslatableContents translated
                && translated.getKey().equals("jade.gt6ce.direction." + direction), "original input/output face diagnostic " + direction);
    }

    private static final class RecordingPlayer extends Player {
        private final java.util.List<Component> messages = new java.util.ArrayList<>();
        RecordingPlayer(Level level) {
            super(level, BlockPos.ZERO, 0, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "MeterTest"));
        }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
        @Override public void displayClientMessage(Component message, boolean actionBar) { messages.add(message); }
    }
}
