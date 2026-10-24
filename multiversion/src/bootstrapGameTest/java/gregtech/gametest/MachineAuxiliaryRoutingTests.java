package gregtech.gametest;

import gregtech.internal.energy.FaceConfig;
import gregtech.internal.machine.BasicMachineSpec;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class MachineAuxiliaryRoutingTests {
    private static BasicMachineBlockEntity machine(GameTestHelper h, GregTechTags.Tag auxiliary) {
        var block = (BasicMachineBlock) BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof BasicMachineBlock basic && basic.basicSpec().machineName().equals("crusher"))
                .findFirst().orElseThrow();
        var pos = h.absolutePos(new BlockPos(2, 3, 2));
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        var machine = (BasicMachineBlockEntity) h.getLevel().getBlockEntity(pos);
        machine.setSpec(BasicMachineSpec.builder("aux_routing_fixture", block.basicSpec().material())
                .machineType("crusher").recipes(block.basicSpec().recipeMap())
                .energy(GregTechTags.Energy.EU, 8).energyRange(4, 32)
                .faces(FaceConfig.builder().energyIn(FaceConfig.LEFT).build())
                .auxiliaryEnergy(auxiliary, 16, 8, 64, 128, 1 << FaceConfig.RIGHT).build());
        return machine;
    }

    private static long stored(BasicMachineBlockEntity machine, String name) {
        try {
            var field = BasicMachineBlockEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getLong(machine);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Actual energy buffer missing", failure);
        }
    }

    @GameTest(template="test_empty")
    public static void sameTypeAuxiliaryUsesDisjointFacesAndConservesBuffers(GameTestHelper h) {
        var machine = machine(h, GregTechTags.Energy.EU);
        var eu = GregTechTags.Energy.EU;
        h.assertTrue(machine.isEnergyAcceptingFrom(eu, Direction.EAST, false)
                && machine.isEnergyAcceptingFrom(eu, Direction.WEST, false)
                && !machine.isEnergyAcceptingFrom(eu, Direction.NORTH, false), "both declared faces accept, undeclared face rejects");
        h.assertTrue(machine.getEnergySizeInputRecommended(eu, Direction.EAST) == 8
                && machine.getEnergySizeInputMin(eu, Direction.EAST) == 4
                && machine.getEnergySizeInputMax(eu, Direction.EAST) == 32
                && machine.getEnergySizeInputRecommended(eu, Direction.WEST) == 16
                && machine.getEnergySizeInputMin(eu, Direction.WEST) == 8
                && machine.getEnergySizeInputMax(eu, Direction.WEST) == 64, "packet metadata follows selected face");
        h.assertTrue(machine.doInject(eu, Direction.EAST, 8, 2, false) == 2
                && machine.doInject(eu, Direction.WEST, 16, 2, false) == 2
                && stored(machine, "mEnergy") == 0 && stored(machine, "auxEnergy") == 0, "simulation never changes either buffer");
        h.assertTrue(machine.doInject(eu, Direction.EAST, 8, 2, true) == 2
                && stored(machine, "mEnergy") == 16 && stored(machine, "auxEnergy") == 0, "primary face credits only primary buffer");
        h.assertTrue(machine.doInject(eu, Direction.WEST, 16, 2, true) == 2
                && stored(machine, "mEnergy") == 16 && stored(machine, "auxEnergy") == 32, "auxiliary face credits only auxiliary buffer");
        h.assertTrue(machine.getEnergyDemanded(eu, Direction.EAST, 8) == 2
                && machine.getEnergyDemanded(eu, Direction.WEST, 16) == 6
                && machine.getEnergyDemanded(eu, Direction.NORTH, 8) == 0
                && machine.doInject(eu, Direction.NORTH, 8, 1, true) == 0, "demand and rejection agree with injection routing");
        h.assertTrue(machine.isEnergyAcceptingFrom(eu, null, false)
                && machine.getEnergySizeInputRecommended(eu, null) == 16
                && machine.doInject(eu, null, 16, 1, true) == 1
                && stored(machine, "mEnergy") == 16 && stored(machine, "auxEnergy") == 48, "unsided calls retain previous auxiliary priority, never double credit");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void differentTypeAuxiliaryRetainsTypeAndFaceIsolation(GameTestHelper h) {
        var machine = machine(h, GregTechTags.Energy.KU);
        h.assertTrue(machine.doInject(GregTechTags.Energy.EU, Direction.WEST, 8, 1, true) == 0
                && machine.doInject(GregTechTags.Energy.KU, Direction.EAST, 16, 1, true) == 0,
                "wrong type on a valid other-input face is rejected");
        h.assertTrue(machine.doInject(GregTechTags.Energy.EU, Direction.EAST, 8, 1, true) == 1
                && machine.doInject(GregTechTags.Energy.KU, Direction.WEST, 16, 1, true) == 1
                && stored(machine, "mEnergy") == 8 && stored(machine, "auxEnergy") == 16,
                "different types keep original separate buffers");
        h.succeed();
    }
}
