package gregtech.gametest;

import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.content.data.GregTechTags;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.platform.Mc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
public final class EnergyDeviceBehaviorTests {
    private EnergyDeviceBehaviorTests() {}

    @GameTest(template = "test_empty")
    public static void rotationTransformerWaitsForWorldBeforeEmitting(GameTestHelper helper) {
        var block = BuiltInRegistries.BLOCK.get(Mc.gregtechId("rotation_transformer_wood"));
        var state = block.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH);
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var node = new EnergyNodeBlockEntity(pos, state);
        var ru = GregTechTags.Energy.RU;
        long size = node.spec().inputRate();
        helper.assertTrue(node.doEnergyInjection(ru, Direction.NORTH, -size, 1, true) == 1,
                "rotation transformer accepts the signed input packet");
        EnergyNodeBlockEntity.serverTick(helper.getLevel(), pos, state, node);
        helper.assertTrue(node.stored() == size, "a detached transformer must keep its input");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void customCoolerUsesDeclaredBehavior(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = BuiltInRegistries.BLOCK.get(Mc.gregtechId("electric_cooler_lv"));
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var state = block.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH);
        level.setBlockAndUpdate(pos, state);
        var node = (EnergyNodeBlockEntity) level.getBlockEntity(pos);
        var oldControl = node.machineControl(null);
        var eu = GregTechTags.Energy.EU;
        var cold = GregTechTags.Energy.CU;
        var heat = GregTechTags.Energy.HU;
        var spec = EnergyNodeSpec.builder("test_conditioner", node.spec().material())
                .deviceType(EnergyNodeSpec.DeviceType.COOLER)
                .texture(node.spec().textureFolder())
                .input(eu, 32)
                .output(cold, 8)
                .capacity(64)
                .name("Test Conditioner")
                .build();
        node.setSpec(spec);

        helper.assertTrue(node.isEnergyEmittingTo(cold, Direction.NORTH, false)
                        && node.isEnergyEmittingTo(heat, Direction.SOUTH, false)
                        && !node.isEnergyEmittingTo(heat, Direction.NORTH, false),
                "the cooler has distinct front cold and rear heat outputs");
        oldControl.setMode(4);
        helper.assertTrue(node.machineControl(null).mode() == 4, "the retained control updates the new converter");
        helper.assertTrue(node.doEnergyInjection(eu, Direction.SOUTH, 32, 1, true) == 0
                        && node.doEnergyInjection(eu, Direction.EAST, 32, 1, false) == 1 && node.stored() == 0,
                "only lateral inputs accept simulated packets");
        helper.assertTrue(node.doEnergyInjection(eu, Direction.EAST, 32, 1, true) == 1 && node.stored() == 32,
                "an actual packet fills the converter buffer");
        EnergyNodeBlockEntity.serverTick(level, pos, state, node);
        helper.assertTrue(oldControl.running() && node.stored() == 0,
                "the converter consumes its input even without an output receiver");
        helper.succeed();
    }
}
