package gregtech.gametest;

import gregtech.content.hazard.BreathingGasEvents;
import gregtech.content.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Drives the real loader immersion collection, travel path and independent gas hazard sampling. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class GasMotionRegressionTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void gasUsesAirMotionWithoutRemovingHazards(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 4, 1));
        var gas = GTFluids.still("Gas_Natural").get();
        var type = gas.getFluidType();
        helper.assertTrue(type.getDensity() == -500 && type.getViscosity() == 200,
                "native gas registration uses original density and viscosity");
        helper.assertTrue(GTFluids.still("Oil_Soulsand").get().getFluidType().getDensity() == 650,
                "native liquid registration preserves the specific source override");
        var plasma = GTFluids.still("HeliumPlasma").get().getFluidType();
        helper.assertTrue(plasma.getDensity() == -100000 && plasma.getViscosity() == 10,
                "native plasma registration is distinct from liquid and gas");
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) for (int z = -1; z <= 1; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), gas.defaultFluidState().createLegacyBlock());
        Cow inGas = EntityType.COW.create(level);
        Cow inAir = EntityType.COW.create(level);
        helper.assertTrue(inGas != null && inAir != null, "test cows created");
        inGas.setNoAi(true);
        inAir.setNoAi(true);
        inGas.setPos(origin.getX() + 0.5, origin.getY() + 0.1, origin.getZ() + 0.5);
        BlockPos air = origin.offset(5, 0, 0);
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) for (int z = -1; z <= 1; z++)
            level.setBlockAndUpdate(air.offset(x, y, z), Blocks.AIR.defaultBlockState());
        inAir.setPos(air.getX() + 0.5, air.getY() + 0.1, air.getZ() + 0.5);
        inGas.fallDistance = 8;
        inGas.baseTick();
        inAir.baseTick();
        helper.assertTrue(inGas.getFluidTypeHeight(type) == 0 && !inGas.isInFluidType(),
                "GT gas does not enter loader immersion, including jump/sink selection");
        helper.assertTrue(inGas.fallDistance == 8, "gas does not reset falling distance");
        helper.assertTrue(!type.canPushEntity(inGas) && !type.canSwim(inGas), "gas cannot push or swim");
        inGas.setDeltaMovement(0.1, -0.1, 0.1);
        inAir.setDeltaMovement(0.1, -0.1, 0.1);
        inGas.travel(new Vec3(0.2, 0, 0.2));
        inAir.travel(new Vec3(0.2, 0, 0.2));
        helper.assertTrue(inGas.getDeltaMovement().subtract(inAir.getDeltaMovement()).lengthSqr() < 1.0E-12,
                "gas travel keeps ordinary air acceleration, drag and gravity");
        helper.assertTrue(BreathingGasEvents.headFluid(inGas) == gas, "head sampling still sees actual gas");
        float health = inGas.getHealth();
        helper.assertTrue(BreathingGasEvents.breatheIn(inGas) && inGas.getHealth() < health,
                "gas retains its head-inside damage despite no liquid immersion");
        // Same entity and collection hook: actual water must still take liquid immersion.
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) for (int z = -1; z <= 1; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.WATER.defaultBlockState());
        inGas.baseTick();
        helper.assertTrue(inGas.getFluidTypeHeight(Fluids.WATER.getFluidType()) > 0 && inGas.isInWater(),
                "ordinary water remains swimmable immersion");
        helper.assertTrue(!GTFluids.isNonLiquidType(GTFluids.still("Oil_Light").get().getFluidType()),
                "oil is not excluded by the gas-only policy");
        helper.succeed();
    }
}
