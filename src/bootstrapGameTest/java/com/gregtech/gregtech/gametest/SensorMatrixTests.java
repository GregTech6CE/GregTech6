package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput;
import com.gregtech.gregtech.api.sensor.SensorMeasurements;
import com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity;
import com.gregtech.gregtech.data.MachineRecipeIngredients;
import com.gregtech.gregtech.data.SensorRecipePack;
import com.gregtech.gregtech.data.generated.GTSensorRecipesGen;
import com.gregtech.gregtech.registry.GTSensors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * GT6 registers <b>20 sensor machines</b> ({@code Loader_MultiTileEntities.sensors}, {@code :1979-1998},
 * machine ids 31000-31022). §3 of the porting notes called this a "20 vs 7" gap, but that count was
 * taken from <em>files</em>: GT6's 20 sensors live in 20 small classes under
 * {@code gregtech/tileentity/sensors/}, while the port implements all of them in one
 * {@code SensorBlockEntity} with a {@code Kind} enum. Counted by machine the port had <b>16</b>:
 *
 * <ul>
 *   <li>the four weight-o-meters of {@code :1988-1991} were collapsed into one block
 *       ({@code sensor_weightometric}), which is GT6's <em>Heavy</em> one - tonnes, maximum 65535;</li>
 *   <li>the TPS meter of {@code :1992} was missing entirely.</li>
 * </ul>
 *
 * §108 adds those four, so the port now ships exactly GT6's twenty - and the four weight-o-meters are
 * one measurement with four scales ({@code MultiTileEntityWeightometer*.java:43,62}):
 * {@code gramm} (x1000), {@code kilogramm} (x1), {@code ton} (/1000) and {@code kiloton} (/1000000),
 * each saturating at {@code B[16]-1 = 65535}.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class SensorMatrixTests {
    private static final int BASE_X = 37000;
    private static final int BASE_Z = 37000;
    private static final int BASE_Y = 100;

    private static BlockPos at(int dx, int dy, int dz) {
        return new BlockPos(BASE_X + dx, BASE_Y + dy, BASE_Z + dz);
    }

    /** GT6 sensor machine id, GT6 name, port block path - {@code Loader_MultiTileEntities:1979-1998}. */
    private static final String[][] GT6_SENSORS = {
            {"31000", "Thermometer Sensor", "sensor_thermometer"},
            {"31001", "Gibbl-O-Meter Sensor", "sensor_gibblometer"},
            {"31002", "Luminometer Sensor", "sensor_luminometer"},
            {"31003", "Chronometer Sensor", "sensor_chronometer"},
            {"31004", "Item-O-Meter Sensor", "sensor_itemometer"},
            {"31005", "Stack-O-Meter Sensor", "sensor_stackometer"},
            {"31006", "Fluid-O-Meter Sensor", "sensor_fluidometer"},
            {"31007", "Bucket-O-Meter Sensor", "sensor_bucketometer"},
            {"31010", "Light Weight-O-Meter Sensor", "sensor_weightometer_light"},
            {"31011", "Medium Weight-O-Meter Sensor", "sensor_weightometer_medium"},
            {"31012", "Heavy Weight-O-Meter Sensor", "sensor_weightometric"},
            {"31013", "Super Heavy Weight-O-Meter Sensor", "sensor_weightometer_super_heavy"},
            {"31015", "Electrometer Sensor", "sensor_electrometer"},
            {"31016", "TPS Sensor", "sensor_tpsmeter"},
            {"31017", "Player Counter Sensor", "sensor_playercounter"},
            {"31018", "Progress Sensor", "sensor_progressmeter"},
            {"31019", "Tachometer Sensor", "sensor_tachometer"},
            {"31020", "Geiger Counter Sensor", "sensor_geiger"},
            {"31021", "Laser-O-Meter Sensor", "sensor_laserometer"},
            {"31022", "Kilo-Bucket-O-Meter Sensor", "sensor_kilobucketometer"},
            {"31023", "Kilo-Gibbl-O-Meter Sensor", "sensor_kilogibblometer"},
    };

    private static Block block(GameTestHelper helper, String path) {
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", path));
        helper.assertTrue(block != null, "gregtech:" + path + " is a registered block, got null");
        return block;
    }

    private static SensorBlockEntity placeSensor(GameTestHelper helper, String path, BlockPos pos,
                                                 Direction facing) {
        ServerLevel level = helper.getLevel();
        level.removeBlock(pos, false);
        level.setBlock(pos, block(helper, path).defaultBlockState()
                .setValue(DirectionalBlock.FACING, facing), 3);
        helper.assertTrue(level.getBlockEntity(pos) instanceof SensorBlockEntity,
                "the sensor at " + pos + " has its block entity");
        return (SensorBlockEntity) level.getBlockEntity(pos);
    }

    // ---------------------------------------------------------------------------------------------
    // 1) The set itself
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void theSensorSetIsGt6sTwentyMachines(GameTestHelper helper) {
        StringBuilder registered = new StringBuilder();
        for (String[] row : GT6_SENSORS) {
            helper.assertTrue(ForgeRegistries.BLOCKS.containsKey(
                            ResourceLocation.fromNamespaceAndPath("gregtech", row[2])),
                    "GT6's " + row[1] + " (" + row[0] + ") has a port block gregtech:" + row[2]);
            registered.append(row[2]).append(' ');
        }
        int count = 0;
        for (RegistryObject<com.gregtech.gregtech.block.sensor.SensorBlock> entry : GTSensors.all()) {
            if (entry.isPresent()) count++;
        }
        helper.assertTrue(count == GT6_SENSORS.length,
                "the port ships one sensor block per GT6 sensor machine (" + GT6_SENSORS.length
                        + "), got " + count);
        GregTech.LOGGER.info("[sensor] {} sensor blocks for GT6's {} machines: {}", count,
                GT6_SENSORS.length, registered.toString().trim());
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // 2) The four weight-o-meter scales
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void theFourWeighometersDifferOnlyInScale(GameTestHelper helper) {
        helper.assertTrue(SensorMeasurements.MAX_COUNT == 65535,
                "GT6's B[16]-1 is 65535, got " + SensorMeasurements.MAX_COUNT);

        // The same 1000 kg read on all four scales (MultiTileEntityWeightometer*:62).
        helper.assertTrue(SensorMeasurements.grams(1000.0) == 65535,
                "the light meter would read 1,000,000 g and saturates at 65535, got "
                        + SensorMeasurements.grams(1000.0));
        helper.assertTrue(SensorMeasurements.kilograms(1000.0) == 1000,
                "the medium meter reads kilogrammes, got " + SensorMeasurements.kilograms(1000.0));
        helper.assertTrue(SensorMeasurements.tonnes(1000.0) == 1,
                "the heavy meter reads tonnes, got " + SensorMeasurements.tonnes(1000.0));
        helper.assertTrue(SensorMeasurements.kilotonnes(1000.0) == 0,
                "the super heavy meter reads kilotonnes, got "
                        + SensorMeasurements.kilotonnes(1000.0));

        // Each one saturates at its own maximum mass: (B[16]-1) x the scale.
        helper.assertTrue(SensorMeasurements.grams(65.535) == 65535,
                "65.535 kg is the light meter's maximum, got " + SensorMeasurements.grams(65.535));
        helper.assertTrue(SensorMeasurements.kilograms(65535.0) == 65535,
                "65,535 kg is the medium meter's maximum, got "
                        + SensorMeasurements.kilograms(65535.0));
        helper.assertTrue(SensorMeasurements.tonnes(65_535_000.0) == 65535,
                "65,535,000 kg is the heavy meter's maximum, got "
                        + SensorMeasurements.tonnes(65_535_000.0));
        helper.assertTrue(SensorMeasurements.kilotonnes(6.5535E10) == 65535,
                "65,535,000,000 kg is the super heavy meter's maximum, got "
                        + SensorMeasurements.kilotonnes(6.5535E10));

        // Nothing weighs less than nothing.
        for (double negative : new double[]{-1.0, -1000.0}) {
            helper.assertTrue(SensorMeasurements.grams(negative) == 0
                            && SensorMeasurements.kilograms(negative) == 0
                            && SensorMeasurements.tonnes(negative) == 0
                            && SensorMeasurements.kilotonnes(negative) == 0,
                    "a negative weight reads 0 on every scale, got "
                            + SensorMeasurements.grams(negative));
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // 3) One chest, four meters
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void aChestIsWeighedOnAllFourScales(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos chestPos = at(0, 0, 0);
        level.removeBlock(chestPos, false);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockEntity(chestPos) instanceof ChestBlockEntity,
                "the chest at " + chestPos + " has its block entity");
        ItemStack payload = new ItemStack(Items.RAW_IRON, 4);
        ((ChestBlockEntity) level.getBlockEntity(chestPos)).setItem(0, payload);

        // The weight the sensors are supposed to see, computed through the same parser they use.
        double kilograms = 0;
        for (var material : CrucibleItemInput.parse(payload)) kilograms += material.weightKg() * 4;
        helper.assertTrue(kilograms > 0, "raw iron has a known weight, got " + kilograms);

        // A sensor reads the block on the side opposite its facing, so these four all look at the
        // chest in the middle.
        String[] meters = {"sensor_weightometer_light", "sensor_weightometer_medium",
                "sensor_weightometric", "sensor_weightometer_super_heavy"};
        String[] units = {"gramm", "kilogramm", "ton", "kiloton"};
        Direction[] facings = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
        BlockPos[] spots = {at(1, 0, 0), at(-1, 0, 0), at(0, 0, 1), at(0, 0, -1)};
        long[] expected = {SensorMeasurements.grams(kilograms), SensorMeasurements.kilograms(kilograms),
                SensorMeasurements.tonnes(kilograms), SensorMeasurements.kilotonnes(kilograms)};

        for (int i = 0; i < meters.length; i++) {
            SensorBlockEntity meter = placeSensor(helper, meters[i], spots[i], facings[i]);
            SensorBlockEntity.measure(level, spots[i], level.getBlockState(spots[i]), meter);
            helper.assertTrue(meter.value() == expected[i],
                    meters[i] + " reads " + expected[i] + " for " + kilograms + " kg, got "
                            + meter.value());
            var cells = meter.displayCells();
            helper.assertTrue(cells[5] != null && units[i].equals(cells[5].texture()),
                    meters[i] + " shows the " + units[i] + " glyph, got "
                            + (cells[5] == null ? "none" : cells[5].texture()));
        }

        // The four meters are one measurement on four scales, so the readings never grow with the
        // coarser scale - true whether or not the finer ones saturate at GT6's 65535. (The payload of
        // 4 raw iron is a four-figure kilogram mass in this table: an ore stack carries a material
        // unit, not the item's own weight, so nothing here assumes a magnitude.)
        helper.assertTrue(expected[0] >= expected[1] && expected[1] >= expected[2]
                        && expected[2] >= expected[3],
                "the four scales are non-increasing for " + kilograms + " kg, got "
                        + java.util.Arrays.toString(expected));
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // 4) The TPS meter
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void theTpsMeterReadsHundredthsOfATickPerSecond(GameTestHelper helper) {
        // MultiTileEntityTPSmeter:46 - (tickRate * 100000) / elapsedMillis, sampled every 20 ticks.
        helper.assertTrue(SensorBlockEntity.tpsFromElapsed(1000, 20) == 2000,
                "20 ticks in 1000 ms is a full 20 TPS shown as 2000, got "
                        + SensorBlockEntity.tpsFromElapsed(1000, 20));
        helper.assertTrue(SensorBlockEntity.tpsFromElapsed(2000, 20) == 1000,
                "20 ticks in 2000 ms is 10 TPS shown as 1000, got "
                        + SensorBlockEntity.tpsFromElapsed(2000, 20));
        helper.assertTrue(SensorBlockEntity.tpsFromElapsed(500, 20) == 4000,
                "20 ticks in 500 ms is 40 TPS shown as 4000, got "
                        + SensorBlockEntity.tpsFromElapsed(500, 20));
        helper.assertTrue(SensorBlockEntity.tpsFromElapsed(0, 20) == SensorBlockEntity.TPS_DEFAULT,
                "no elapsed time falls back to GT6's 2000, got "
                        + SensorBlockEntity.tpsFromElapsed(0, 20));

        // The meter measures the server, not a neighbour: it works in mid air.
        BlockPos pos = at(4, 0, 0);
        SensorBlockEntity meter = placeSensor(helper, "sensor_tpsmeter", pos, Direction.UP);
        helper.assertTrue(meter.kind() == SensorBlockEntity.Kind.TPS,
                "the TPS meter reports Kind.TPS, got " + meter.kind());
        helper.assertTrue(meter.tpsValue() == SensorBlockEntity.TPS_DEFAULT,
                "a fresh meter starts at GT6's 2000, got " + meter.tpsValue());
        meter.sampleTps();
        meter.sampleTps();
        SensorBlockEntity.measure(level(helper), pos, helper.getLevel().getBlockState(pos), meter);
        helper.assertTrue(meter.value() == meter.tpsValue(),
                "the display shows the sampled value, " + meter.value() + " vs " + meter.tpsValue());
        helper.assertTrue(meter.tpsValue() > 0, "and it is a real reading, got " + meter.tpsValue());
        helper.assertTrue(meter.displayCells()[5] != null
                        && "clock".equals(meter.displayCells()[5].texture()),
                "the TPS meter uses the clock glyph, got "
                        + (meter.displayCells()[5] == null ? "none" : meter.displayCells()[5].texture()));
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // 5) The twenty original GT6 crafting recipes (§109)
    // ---------------------------------------------------------------------------------------------

    /**
     * §109: the port ships GT6's own crafting recipe for each of the twenty sensors.
     * <p>
     * The table itself is generated from {@code Loader_MultiTileEntities:1979-1998}, so this test
     * checks the things a generated table can still get wrong: that every pattern symbol of every row
     * resolves to an ingredient that <em>matches something</em> (rather than to the resolver's silent
     * "machine casing block" fallback, or to an empty tag), and that {@link SensorRecipePack} really
     * emitted twenty recipe files.
     * </p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void everySensorHasItsOriginalGt6Recipe(GameTestHelper helper) {
        var steel = com.gregtech.gregtech.api.material.GTMaterialRegistry.get("StainlessSteel");
        List<String> broken = new ArrayList<>();
        int resolved = 0;
        int unusedKeys = 0;

        // Snapshot the substitution log first: a key that silently fell back (a material form the port
        // lacks) shows up as a new entry, which is a much better diagnostic than "it resolved to
        // *something*". index*() helpers never note, so only real fallbacks appear.
        Map<String, String> before = new HashMap<>(MachineRecipeIngredients.substitutions());

        for (GTSensorRecipesGen.Row row : GTSensorRecipesGen.ROWS) {
            // ForgeRegistries.*.getValue(an unknown id) returns AIR rather than null in 1.20.1.
            Block block = ForgeRegistries.BLOCKS.getValue(
                    ResourceLocation.fromNamespaceAndPath("gregtech", row.blockId()));
            if (block == null || block == Blocks.AIR) {
                broken.add(row.name() + " (" + row.machineId() + "): no block gregtech:" + row.blockId());
                continue;
            }
            if (block.asItem() == Items.AIR) {
                broken.add(row.name() + ": block gregtech:" + row.blockId() + " has no item form");
                continue;
            }
            if (row.pattern().size() != 3 || row.pattern().stream().allMatch(String::isBlank)) {
                broken.add(row.name() + ": pattern is not a 3x3 grid: " + row.pattern());
                continue;
            }

            // GT6 declares keys its pattern never spells; vanilla rejects the surplus, so the pack
            // emits the used ones only (ShapedRecipe.java:174-175) and this test counts the same set.
            Set<Character> used = SensorRecipePack.usedSymbols(row.pattern());
            unusedKeys += row.keys().size() - used.size();
            for (char symbol : used) {
                if (!row.keys().containsKey(symbol)) {
                    broken.add(row.name() + ": pattern uses '" + symbol + "' with no key");
                }
            }
            Map<Character, String> usedKeys = new java.util.TreeMap<>();
            for (var entry : row.keys().entrySet()) {
                if (used.contains(entry.getKey())) usedKeys.put(entry.getKey(), entry.getValue());
            }

            Map<Character, Object> ingredients =
                    MachineRecipeIngredients.resolveAll(usedKeys, steel, SensorRecipePack.SENSOR_TIER);
            if (ingredients.size() != usedKeys.size()) {
                broken.add(row.name() + ": " + ingredients.size() + " of " + usedKeys.size()
                        + " used keys resolved");
                continue;
            }
            for (var pair : ingredients.entrySet()) {
                if (pair.getValue() == null) {
                    broken.add(row.name() + " key " + pair.getKey() + " resolved to null");
                    continue;
                }
                for (Object option : pair.getValue() instanceof List<?> list
                        ? list : List.of(pair.getValue())) {
                    if (!(option instanceof Map<?, ?> map)) {
                        broken.add(row.name() + " key " + pair.getKey() + " -> " + option);
                        continue;
                    }
                    Object item = map.get("item");
                    Object tag = map.get("tag");
                    if (item != null) {
                        Item resolvedItem = ForgeRegistries.ITEMS.getValue(
                                ResourceLocation.parse(item.toString()));
                        if (resolvedItem == null || resolvedItem == Items.AIR) {
                            broken.add(row.name() + " key " + pair.getKey() + " -> missing item " + item);
                        }
                    } else if (tag != null) {
                        // A tag ingredient is fine only if it matches something: an unknown or empty
                        // tag would make the recipe uncraftable while still "resolving".
                        TagKey<Item> tagKey = TagKey.create(Registries.ITEM,
                                ResourceLocation.parse(tag.toString()));
                        if (Ingredient.of(tagKey).getItems().length == 0) {
                            broken.add(row.name() + " key " + pair.getKey() + " -> empty tag " + tag);
                        }
                    } else {
                        broken.add(row.name() + " key " + pair.getKey()
                                + " is neither an item nor a tag: " + map);
                    }
                }
            }
            resolved++;
        }

        List<String> fellBack = new ArrayList<>();
        for (var entry : MachineRecipeIngredients.substitutions().entrySet()) {
            if (!Objects.equals(before.get(entry.getKey()), entry.getValue())) {
                fellBack.add(entry.getKey() + " -> " + entry.getValue());
            }
        }

        // The one tier-sensitive key in the twenty rows: IL.SENSORS[1] is GT6's LV compact sensor
        // (Loader_MultiTileEntities:1998, the Laser-O-Meter's centre), which is why the pack resolves
        // the whole table at tier 1.
        Object laser = MachineRecipeIngredients.resolve("il:SENSORS", steel, SensorRecipePack.SENSOR_TIER);
        helper.assertTrue(laser instanceof Map<?, ?> sensorMap
                        && "gregtech:compact_sensor_lv".equals(sensorMap.get("item")),
                "IL.SENSORS[1] resolves to the LV compact sensor, got " + laser);

        Set<String> emitted = SensorRecipePack.emittedBlockIds();
        GregTech.LOGGER.info("[sensor] recipe rows={} resolved={} missing={} emitted={} unusedKeys={}",
                GTSensorRecipesGen.ROWS.size(), resolved, GTSensorRecipesGen.ROWS.size() - resolved,
                emitted.size(), unusedKeys);

        // Both lists go into one message so a single gate run shows every problem at once.
        helper.assertTrue(broken.isEmpty() && fellBack.isEmpty(),
                "sensor recipe problems: rows=" + broken + " substitutions=" + fellBack);
        helper.assertTrue(GTSensorRecipesGen.ROWS.size() == 21,
                "GT6's twenty sensor rows, got " + GTSensorRecipesGen.ROWS.size());
        helper.assertTrue(resolved == GTSensorRecipesGen.ROWS.size(),
                "rows fully resolved: " + resolved + " of " + GTSensorRecipesGen.ROWS.size());
        for (GTSensorRecipesGen.Row row : GTSensorRecipesGen.ROWS) {
            helper.assertTrue(emitted.contains(row.blockId()),
                    "SensorRecipePack emitted no recipe for gregtech:" + row.blockId());
        }
        helper.assertTrue(emitted.size() == GTSensorRecipesGen.ROWS.size(),
                "SensorRecipePack emitted " + emitted.size() + " recipes: " + emitted);
        // Every emitted recipe must have loaded: a JSON that vanilla's ShapedRecipe rejects (an unused
        // key, a pattern symbol without a key) fails at pack load, not at crafting time.
        for (GTSensorRecipesGen.Row row : GTSensorRecipesGen.ROWS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gregtech",
                    "machines/sensors/" + row.blockId());
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id).isPresent(),
                    "recipe " + id + " loaded from the sensor data pack");
        }
        helper.succeed();
    }

    private static ServerLevel level(GameTestHelper helper) {
        return helper.getLevel();
    }
}
