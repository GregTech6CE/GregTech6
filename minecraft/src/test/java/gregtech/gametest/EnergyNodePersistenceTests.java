package gregtech.gametest;

import gregtech.content.block.energy.EnergyNodeBlock;
import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.energy.ChemicalBatterySpec;
import gregtech.content.material.Materials;
import gregtech.content.registry.GTChemicalBatteries;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyNodeSpec.DeviceType;
import gregtech.engine.energy.EnergyNodeSpec.Kind;
import gregtech.engine.stack.NbtCompat;
import gregtech.platform.Mc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class EnergyNodePersistenceTests {
    private EnergyNodePersistenceTests() {}

    @GameTest(template = "test_empty", batch = "energy_node_persistence")
    public static void currentDeviceTagsRoundTrip(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        for (DeviceType type : DeviceType.values()) {
            if (type == DeviceType.BUFFERED) continue;
            var node = registered(type);
            var expected = commonTag();
            switch (type) {
                case MOTOR, STEAM_TURBINE -> {
                    putConverter(expected);
                    expected.putBoolean("gt.reversed", true);
                    expected.putBoolean("gt.visual", true);
                    if (type == DeviceType.STEAM_TURBINE) putTurbine(expected, node, lookup);
                }
                case DYNAMO, HEATER, COOLER -> putConverter(expected);
                case MAGNET -> {
                    putConverter(expected);
                    putMagnet(expected, 3);
                }
                case SOLAR -> {
                    expected.putBoolean("gt.solar_stopped", true);
                    expected.putBoolean("gt.solar_active", true);
                    expected.putBoolean("gt.solar_emitting", true);
                }
                case ELECTRIC_TRANSFORMER -> {
                    expected.putBoolean("gt.transformer_stopped", true);
                    expected.putInt("gt.transformer_mode", 4);
                    expected.putBoolean("gt.transformer_possible", true);
                    expected.putBoolean("gt.transformer_emitted", true);
                }
                case ROTATION_TRANSFORMER -> expected.putBoolean("gt.rotation_negative_input", true);
                case BATTERY_BOX -> {
                    expected.putBoolean("gt.battery_stopped", true);
                    expected.putInt("gt.battery_mode", 3);
                    var slots = new ListTag();
                    var cell = GTChemicalBatteries.item(ChemicalBatterySpec.Chemistry.LEAD_ACID, 1);
                    for (int i = 0; i < node.spec().batterySlots(); i++) {
                        slots.add(NbtCompat.saveItem(i == 2 ? new ItemStack(cell) : ItemStack.EMPTY, lookup));
                    }
                    expected.put("gt.batteries", slots);
                }
                case BUFFERED -> throw new AssertionError(type);
            }
            assertRestored(helper, node, expected, expected);
        }
        for (Kind kind : new Kind[] {Kind.CONVERTER, Kind.STORAGE, Kind.MAGNET, Kind.TURBINE}) {
            var node = buffered(kind);
            var expected = commonTag();
            if (kind == Kind.MAGNET) putMagnet(expected, 0);
            if (kind == Kind.TURBINE) putTurbine(expected, node, lookup);
            assertRestored(helper, node, expected, expected);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "energy_node_persistence")
    public static void legacyMagnetKeysMigrate(GameTestHelper helper) {
        var magnet = registered(DeviceType.MAGNET);
        var legacy = commonTag();
        legacy.remove("gt.converter_stopped");
        putMagnet(legacy, 3);
        var expected = commonTag();
        putMagnet(expected, 3);
        putConverter(expected);
        expected.putBoolean("gt.active.energy", false);
        expected.putBoolean("gt.can.energy", false);
        assertRestored(helper, magnet, legacy, expected);

        var conflict = expected.copy();
        conflict.putBoolean("gt.magnet_stopped", false);
        conflict.putByte("gt.magnet_mode", (byte) 8);
        assertRestored(helper, magnet, conflict, expected);

        var buffered = buffered(Kind.MAGNET);
        var bufferedExpected = commonTag();
        putMagnet(bufferedExpected, 0);
        assertRestored(helper, buffered, legacy, bufferedExpected);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "energy_node_persistence")
    public static void updatesReplaceExistingNodeState(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        var server = registered(DeviceType.SOLAR);
        var replica = registered(DeviceType.SOLAR);
        var old = commonTag();
        old.putLong("gt.buffer", Long.MAX_VALUE);
        old.put("gt.battery_cover_1", NbtCompat.saveItem(
                new ItemStack(BuiltInRegistries.ITEM.get(Mc.gregtechId("item_filter"))), lookup));
        NbtCompat.loadBlockEntity(server, old, lookup);
        applyUpdate(replica, NbtCompat.getUpdateTag(server, lookup), lookup);
        helper.assertTrue(replica.stored() == replica.spec().outputRate() && !replica.getCover(Direction.UP).isEmpty(),
                "an old solar accumulator is limited to one rated tick and its cover is synchronized");
        NbtCompat.loadBlockEntity(server, new CompoundTag(), lookup);
        applyUpdate(replica, NbtCompat.getUpdateTag(server, lookup), lookup);
        helper.assertTrue(replica.stored() == 0 && replica.getCover(Direction.UP).isEmpty(),
                "the next update clears the previous solar energy and cover");
        helper.succeed();
    }

    private static EnergyNodeBlockEntity registered(DeviceType type) {
        var block = BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof EnergyNodeBlock node && node.spec().deviceType() == type)
                .findFirst().orElseThrow(() -> new AssertionError("No registered block for " + type));
        return new EnergyNodeBlockEntity(BlockPos.ZERO, block.defaultBlockState());
    }

    private static EnergyNodeBlockEntity buffered(Kind kind) {
        var node = registered(DeviceType.MOTOR);
        var spec = node.spec();
        node.setSpec(EnergyNodeSpec.builder("test_buffer", spec.material())
                .kind(kind).texture(spec.textureFolder()).input(spec.inType(), 32)
                .output(spec.outType(), 16).capacity(64).name("Test buffer").build());
        return node;
    }

    private static CompoundTag commonTag() {
        var tag = new CompoundTag();
        tag.putLong("gt.buffer", 5L);
        tag.putBoolean("gt.inverted", true);
        tag.putBoolean("gt.converter_stopped", true);
        tag.putLong("gt.turbine_pending", 0L);
        tag.putLong("gt.steam_remainder", 0L);
        tag.putBoolean("gt.painted", false);
        return tag;
    }

    private static void putConverter(CompoundTag tag) {
        tag.putByte("gt.mode", (byte) 3);
        tag.putBoolean("gt.active.energy", true);
        tag.putBoolean("gt.can.energy", true);
    }

    private static void putMagnet(CompoundTag tag, int mode) {
        tag.putBoolean("gt.magnet_stopped", true);
        tag.putByte("gt.magnet_mode", (byte) mode);
        tag.putInt("gt.magnet_overloads", 17);
    }

    private static void putTurbine(CompoundTag tag, EnergyNodeBlockEntity node, HolderLookup.Provider lookup) {
        tag.putLong("gt.turbine_pending", 13L);
        tag.putLong("gt.steam_remainder", 19L);
        tag.put("gt.rotor", NbtCompat.saveItem(GTItems.getStack(MaterialPrefix.rotor, Materials.StainlessSteel, 1), lookup));
        var tank = new CompoundTag();
        tank.putLong("Amount", 7L);
        tank.putLong("Capacity", node.spec().inputRate() * 4);
        tank.put("Fluid", NbtCompat.saveFluid(GTFluids.stack("Steam", 7), lookup));
        tag.put("gt.steam", tank);
    }

    private static void assertRestored(GameTestHelper helper, EnergyNodeBlockEntity node, CompoundTag input,
            CompoundTag expected) {
        var lookup = helper.getLevel().registryAccess();
        NbtCompat.loadBlockEntity(node, input, lookup);
        var saved = NbtCompat.saveWithoutMetadata(node, lookup);
        // Forge and NeoForge may add their own metadata alongside the energy node's keys.
        var deviceTag = new CompoundTag();
        for (String key : saved.getAllKeys()) {
            if (key.startsWith("gt.")) deviceTag.put(key, saved.get(key));
        }
        var label = node.spec().deviceType() + "/" + node.spec().kind();
        helper.assertTrue(deviceTag.equals(expected), label + " expected " + expected + ", saved " + deviceTag);
        helper.assertTrue(node.stored() == expected.getLong("gt.buffer"), label + " restores its working buffer");
    }

    private static void applyUpdate(EnergyNodeBlockEntity node, CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
        /*node.handleUpdateTag(tag, lookup);
*///?} else {
        node.handleUpdateTag(tag);
//?}
    }
}
