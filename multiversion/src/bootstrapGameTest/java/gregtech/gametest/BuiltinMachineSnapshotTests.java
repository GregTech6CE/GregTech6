package gregtech.gametest;

import gregtech.internal.multiblock.MultiblockLayout;
import gregtech.internal.multiblock.MultiblockPortOwner;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.LargeRecipeMachineBlockEntity;
import gregtech.client.MachineTooltips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

//? if >=1.21.1 {
/*@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_builtin_snapshot")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
@net.minecraftforge.gametest.GameTestHolder("gregtech_builtin_snapshot")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
/**
 * Locks the processing result of every built-in basic machine before those results move onto the spec.
 * The numbers come from the block entity methods, not from the spec fields, because several of those
 * fields disagree with processing today.
 */
public final class BuiltinMachineSnapshotTests {
    private BuiltinMachineSnapshotTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void processing_matches_snapshot(GameTestHelper helper) throws Exception {
        var rows = new ArrayList<Row>();
        for (var block : BuiltInRegistries.BLOCK) {
            var key = BuiltInRegistries.BLOCK.getKey(block);
            if (!"gt6ce".equals(key.getNamespace())) continue;
            // The implosion controller is a dedicated block on Forge, not a BasicMachineBlock.
            if (!(block instanceof BasicMachineBlock) && !key.getPath().equals("implosion_compressor_main")) continue;
            var state = block.defaultBlockState();
            if (state.hasProperty(BasicMachineBlock.FACING)) state = state.setValue(BasicMachineBlock.FACING, Direction.NORTH);
            else if (state.hasProperty(HorizontalDirectionalBlock.FACING)) state = state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
            var created = ((EntityBlock) block).newBlockEntity(BlockPos.ZERO, state);
            if (!(created instanceof BasicMachineBlockEntity entity)) continue;
            rows.add(Row.of(key.getPath(), entity));
        }
        rows.sort(Comparator.comparing(row -> row.path));
        helper.assertTrue(rows.size() > 250, "expected every built-in basic machine, got " + rows.size());
        check(helper, rows, "distillationtower", row -> row.inputMaximum == 1024 && row.energySources.split(";").length == 9);
        check(helper, rows, "cryodistillationtower", row -> row.inputMinimum == 1 && row.inputMaximum == 1024);
        check(helper, rows, "implosioncompressor", row -> row.inputMinimum == 1 && row.inputMaximum == 16 && !row.scalesDuration && row.timed);
        check(helper, rows, "largemassfab", row -> !row.constantEnergy && row.energySources.isEmpty());
        check(helper, rows, "largefermenter", row -> row.itemOutput.equals("0,1,5,FRONT") && row.fluidOutput.equals("0,0,5,FRONT"));
        check(helper, rows, "largecrusher", row -> row.energySources.equals("-3,1,2,RIGHT;3,1,2,LEFT"));
        check(helper, rows, "fusionreactor", row -> row.portMinimum == 1 && row.portMaximum == 16384);
        var actual = new StringBuilder();
        for (var row : rows) actual.append(row.line()).append('\n');
        var text = actual.toString().replace("\r\n", "\n");
        var resource = BuiltinMachineSnapshotTests.class.getResourceAsStream("/builtin-machine-snapshot.txt");
        var written = Path.of(System.getProperty("user.dir"), "builtin-machine-snapshot.txt");
        if (resource == null) {
            Files.writeString(written, text, StandardCharsets.UTF_8);
            helper.assertTrue(false, "wrote the processing snapshot to " + written.toAbsolutePath());
            return;
        }
        var expected = new String(resource.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        if (!expected.equals(text)) {
            Files.writeString(written, text, StandardCharsets.UTF_8);
            helper.assertTrue(false, "processing snapshot drifted; new text is " + written.toAbsolutePath()
                    + ". " + firstDifference(expected, text));
        }
        helper.succeed();
    }

    private static void check(GameTestHelper helper, List<Row> rows, String name, java.util.function.Predicate<Row> expected) {
        var found = false;
        for (var row : rows) {
            if (!row.name.equals(name)) continue;
            found = true;
            helper.assertTrue(expected.test(row), "processing snapshot sentinel failed for " + row.path + ": " + row.line());
        }
        helper.assertTrue(found, "missing built-in machine " + name);
    }

    private static String firstDifference(String expected, String actual) {
        var left = expected.split("\n", -1);
        var right = actual.split("\n", -1);
        int lines = Math.min(left.length, right.length);
        for (int i = 0; i < lines; i++) {
            if (!left[i].equals(right[i])) return "first different line " + (i + 1);
        }
        return "line count " + left.length + " vs " + right.length;
    }

    private record Row(String path, String name, long inputMinimum, long inputMaximum, boolean constantEnergy,
                       boolean scalesDuration, boolean timed, String itemOutput, String fluidOutput, String energySources,
                       long portMinimum, long portMaximum, String line) {
        static Row of(String path, BasicMachineBlockEntity entity) throws ReflectiveOperationException {
            var spec = entity.spec();
            int efficiency = ((Number) call(entity, "efficiency")).intValue();
            boolean constant = (Boolean) call(entity, "requiresConstantEnergy");
            int parallel = ((Number) call(entity, "parallelLimit")).intValue();
            boolean scales = (Boolean) call(entity, "parallelScalesDuration");
            long minimum = ((Number) call(entity, "inputMinimum")).longValue();
            long maximum = ((Number) call(entity, "inputMaximum")).longValue();
            boolean cheap = (Boolean) call(entity, "cheapOverclocking");
            boolean timed = (Boolean) call(entity, "usesTimeEnergy");
            String itemOutput = entity instanceof LargeRecipeMachineBlockEntity ? (String) callBoolean(entity, "outputCellLabel", true) : "";
            String fluidOutput = entity instanceof LargeRecipeMachineBlockEntity ? (String) callBoolean(entity, "outputCellLabel", false) : "";
            String sources = entity instanceof LargeRecipeMachineBlockEntity ? (String) call(entity, "energySourceText") : "";
            boolean named = (Boolean) call(entity, "originalProcessingController", false);
            boolean unlimited = (Boolean) call(entity, "sourceUnlimitedOutputs", false);
            long tank = entity.getTanksOutput().length == 0 ? -1 : entity.getTanksOutput()[0].capacity();
            long portMinimum = 0;
            long portMaximum = 0;
            if (entity instanceof MultiblockPortOwner owner) {
                var type = spec.energyTag();
                portMinimum = owner.portEnergyInputMin(MultiblockLayout.Role.ENERGY_INPUT, type);
                portMaximum = owner.portEnergyInputMax(MultiblockLayout.Role.ENERGY_INPUT, type);
            }
            var tooltip = new ArrayList<Component>();
            MachineTooltips.appendBasicMachine(spec, tooltip);
            var shown = new StringBuilder();
            for (var line : tooltip) {
                if (!shown.isEmpty()) shown.append(" / ");
                shown.append(line.getString().replace('\n', ' ').replace('|', '/'));
            }
            String text = String.join("|", path, entity.getClass().getSimpleName(), spec.energyType(),
                    Integer.toString(efficiency), Boolean.toString(constant), Integer.toString(parallel), Boolean.toString(scales),
                    Long.toString(minimum), Long.toString(maximum), Boolean.toString(cheap), Boolean.toString(timed),
                    itemOutput, fluidOutput, sources, Boolean.toString(named), Boolean.toString(unlimited), Long.toString(tank),
                    Long.toString(portMinimum), Long.toString(portMaximum), shown.toString());
            return new Row(path, spec.machineName(), minimum, maximum, constant, scales, timed, itemOutput, fluidOutput, sources,
                    portMinimum, portMaximum, text);
        }
    }

    private static Object call(Object target, String name) throws ReflectiveOperationException {
        return call(target, name, true);
    }

    /** {@code required} is false for methods only the large controllers declare; those return false when absent. */
    private static Object call(Object target, String name, boolean required) throws ReflectiveOperationException {
        Class<?> type = target.getClass();
        while (type != null) {
            for (var method : type.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != 0) continue;
                method.setAccessible(true);
                return method.invoke(target);
            }
            type = type.getSuperclass();
        }
        if (!required) return false;
        throw new NoSuchMethodException(target.getClass().getName() + "#" + name);
    }

    private static Object callBoolean(Object target, String name, boolean argument) throws ReflectiveOperationException {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                var method = type.getDeclaredMethod(name, boolean.class);
                method.setAccessible(true);
                return method.invoke(target, argument);
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "#" + name);
    }
}
