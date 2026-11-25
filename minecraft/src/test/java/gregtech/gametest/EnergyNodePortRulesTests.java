package gregtech.gametest;

import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.content.blockentity.machine.BoilerTankBlockEntity;
import gregtech.content.data.GregTechTags;
import gregtech.content.material.Materials;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.platform.Mc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class EnergyNodePortRulesTests {
    private EnergyNodePortRulesTests() {}

    @GameTest(template = "test_empty")
    public static void coolerReportsPrimaryPacketSizesAndEmitsRearHeat(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var cooler = BuiltInRegistries.BLOCK.get(Mc.gregtechId("electric_cooler_lv"));
        var state = cooler.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH);
        level.setBlockAndUpdate(pos, state);
        var boiler = BuiltInRegistries.BLOCK.get(Mc.gregtechId("steam_boiler_bronze"));
        level.setBlockAndUpdate(pos.south(), boiler.defaultBlockState());
        var node = (EnergyNodeBlockEntity) level.getBlockEntity(pos);
        var receiver = (BoilerTankBlockEntity) level.getBlockEntity(pos.south());
        var cold = GregTechTags.Energy.CU;
        var heat = GregTechTags.Energy.HU;
        long rated = node.spec().outputRate();
        helper.assertTrue(node.isEnergyEmittingTo(cold, Direction.NORTH, true)
                        && node.getEnergySizeOutputMin(cold, Direction.NORTH) == rated / 2
                        && node.getEnergySizeOutputRecommended(cold, Direction.NORTH) == rated
                        && node.getEnergySizeOutputMax(cold, Direction.NORTH) == rated * 2,
                "the front cold port reports the primary packet range");
        helper.assertTrue(node.isEnergyEmittingTo(heat, Direction.SOUTH, true)
                        && node.getEnergySizeOutputMin(heat, Direction.SOUTH) == 0
                        && node.getEnergySizeOutputRecommended(heat, Direction.SOUTH) == 0
                        && node.getEnergySizeOutputMax(heat, Direction.SOUTH) == 0,
                "the original auxiliary heat port advertises its type without primary packet ratings");
        helper.assertTrue(node.doEnergyInjection(GregTechTags.Energy.EU, Direction.EAST, node.spec().inputRate(), 1, true) == 1,
                "the cooler accepts one rated input packet");
        long before = receiver.getEnergyStored(heat, Direction.NORTH);
        EnergyNodeBlockEntity.serverTick(level, pos, state, node);
        helper.assertTrue(receiver.getEnergyStored(heat, Direction.NORTH) - before == rated,
                "the rear boiler receives the rated heat output");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void zeroOutputStorageKeepsAcceptedEnergy(GameTestHelper helper) {
        var eu = GregTechTags.Energy.EU;
        var spec = EnergyNodeSpec.builder("test_input_only_storage", Materials.Steel)
                .kind(EnergyNodeSpec.Kind.STORAGE).deviceType(EnergyNodeSpec.DeviceType.BUFFERED)
                .texture("test").input(eu, 8).output(eu, 0).capacity(16).name("Test Storage").build();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var state = Blocks.STONE.defaultBlockState();
        var node = new EnergyNodeBlockEntity(pos, state);
        node.setSpec(spec);
        node.setLevel(helper.getLevel());
        helper.assertTrue(node.doEnergyInjection(eu, Direction.SOUTH, 8, 1, true) == 1,
                "a zero-output store still accepts input");
        helper.assertTrue(node.getEnergyOffered(eu, Direction.NORTH, 1) == 0
                        && node.doEnergyExtraction(eu, Direction.NORTH, 1, 1, true) == 0,
                "a zero-output store offers and extracts no energy");
        EnergyNodeBlockEntity.serverTick(helper.getLevel(), pos, state, node);
        helper.assertTrue(node.stored() == 8, "the tick preserves energy when output is disabled");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void retunedTransformerTracksTheActiveDirectionAtUnitVoltage(GameTestHelper helper) {
        var eu = GregTechTags.Energy.EU;
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var transformer = BuiltInRegistries.BLOCK.get(Mc.gregtechId("transformer_ulv_lv"));
        var state = transformer.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH);
        level.setBlockAndUpdate(pos, state);
        var node = (EnergyNodeBlockEntity) level.getBlockEntity(pos);
        node.toggleInverted();
        node.setSpec(EnergyNodeSpec.builder("test_unit_transformer", node.spec().material())
                .deviceType(EnergyNodeSpec.DeviceType.ELECTRIC_TRANSFORMER)
                .texture("test").input(eu, 1).output(eu, 1).capacity(2).name("Test Transformer").build());
        EnergyNodeBlockEntity.serverTick(level, pos, state, node);
        helper.assertTrue(node.getEnergySizeOutputMin(eu, Direction.NORTH) == 1
                        && node.doEnergyInjection(eu, Direction.SOUTH, 1, 1, true) == 1,
                "the empty step-up transformer still accepts its first packet");
        EnergyNodeBlockEntity.serverTick(level, pos, state, node);
        helper.assertTrue(!node.isEnergyAcceptingFrom(eu, Direction.SOUTH, false),
                "the step-up circuit waits for its unaccepted output");
        node.toggleInverted();
        helper.assertTrue(node.isEnergyAcceptingFrom(eu, Direction.NORTH, false),
                "the unused step-down circuit remains ready after retuning in step-up mode");
        helper.succeed();
    }
}
