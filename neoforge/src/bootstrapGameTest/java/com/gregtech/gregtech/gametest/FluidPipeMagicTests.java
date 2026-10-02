package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.fluid.FluidHazards;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_fluid_channels")
@PrefixGameTestTemplate(false)
public final class FluidPipeMagicTests {
    private static final class Pipe extends FluidPipeBlockEntity {
        private final boolean destroy;
        Pipe(BlockPos pos, BlockState state, boolean destroy) { super(pos, state); this.destroy = destroy; }
        @Override protected boolean magicDestroysPipe() { return destroy; }
    }

    private static Pipe pipe(GameTestHelper h, int test, int x, String kind, boolean destroy) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(x, 120 + 14 * test, 2));
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity previous) {
            for (int i = 0; i < previous.getTanks(); i++) previous.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        }
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", "pipe_" + kind));
        h.assertTrue(block instanceof FluidPipeBlock, "registered pipe " + kind);
        var state = block.defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        level.setBlockAndUpdate(pos, state);
        var result = new Pipe(pos, state, destroy);
        level.setBlockEntity(result);
        return result;
    }

    private static FluidStack holy(GameTestHelper h, int amount) {
        var holder = GTFluids.still("Holywater");
        h.assertTrue(holder != null && FluidHazards.isMagic(holder.get()), "registered magical holy water");
        return new FluidStack(holder.get(), amount);
    }

    private static void tick(GameTestHelper h, FluidPipeBlockEntity p) {
        FluidPipeBlockEntity.serverTick(h.getLevel(), p.getBlockPos(), p.getBlockState(), p);
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void declaredMagicAndFlowingFormsAreRecognized(GameTestHelper h) {
        for (String field : new String[]{"Holywater", "XP", "XP_Molten", "Mob", "Sap_Rainbow"}) {
            var still = GTFluids.still(field);
            var flowing = GTFluids.flowing(field);
            h.assertTrue(still != null && flowing != null, "registered forms of " + field);
            h.assertTrue(FluidHazards.isMagic(still.get()) && FluidHazards.isMagic(flowing.get()), "both magic forms " + field);
        }
        h.assertTrue(!FluidHazards.isMagic(null) && !FluidHazards.isMagic(Fluids.EMPTY)
                && !FluidHazards.isMagic(Fluids.WATER) && !FluidHazards.isMagic(Fluids.LAVA), "ordinary fluids are not magical");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void magicLiquidLeaksAndPoisonsWithinThreeBlocks(GameTestHelper h) {
        var p = pipe(h, 0, 2, "medium_steel", false);
        var near = h.spawn(EntityType.COW, new BlockPos(5, 120, 2));
        var far = h.spawn(EntityType.COW, new BlockPos(8, 120, 2));
        p.fill(holy(h, 100), FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(p.getFluidInTank(0).getAmount() == 96 && p.getTransferredAmount() == 4, "liquid magic loss and statistics");
        var poison = near.getEffect(MobEffects.POISON);
        h.assertTrue(poison != null && poison.getDuration() == 1200 && poison.getAmplifier() == 1,
                "original 1200 tick poison II reaches radius-three entity");
        h.assertTrue(!far.hasEffect(MobEffects.POISON), "entity outside radius is unaffected");
        near.discard(); far.discard();
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void magicProofPreventsLeakPoisonAndDestructionRoll(GameTestHelper h) {
        var p = pipe(h, 1, 2, "medium_desh", true);
        var cow = h.spawn(EntityType.COW, new BlockPos(3, 134, 2));
        h.assertTrue(p.spec().magicProof(), "original magic-proof Desh");
        p.fill(holy(h, 100), FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(h.getLevel().getBlockEntity(p.getBlockPos()) == p, "proof pipe never reaches forced destruction roll");
        h.assertTrue(p.getFluidInTank(0).getAmount() == 100 && p.getTransferredAmount() == 0, "proof retains liquid");
        h.assertTrue(!cow.hasEffect(MobEffects.POISON), "magic proof prevents poison");
        cow.discard();
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void smallMagicRemainderStillPoisonsAndCountsActualLoss(GameTestHelper h) {
        var p = pipe(h, 2, 2, "medium_steel", false);
        var cow = h.spawn(EntityType.COW, new BlockPos(3, 148, 2));
        p.fill(holy(h, 3), FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(p.getFluidInTank(0).isEmpty() && p.getTransferredAmount() == 3, "limited fluid only counts three");
        h.assertTrue(cow.hasEffect(MobEffects.POISON), "emptying the tank does not skip the magic consequence");
        tick(h, p);
        h.assertTrue(p.getTransferredAmount() == 0, "next empty tick resets statistics");
        cow.discard();
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void magicDestructionClearsAllChannelsBeforeRemoval(GameTestHelper h) {
        var b = pipe(h, 3, 3, "quadruple_steel", false);
        var a = pipe(h, 3, 2, "quadruple_steel", true);
        a.fill(holy(h, 100), FluidAction.EXECUTE);
        a.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        h.getLevel().setBlockAndUpdate(a.getBlockPos(), a.getBlockState().setValue(FluidPipeBlock.EAST, true));
        h.getLevel().setBlockAndUpdate(b.getBlockPos(), b.getBlockState().setValue(FluidPipeBlock.WEST, true));
        tick(h, a);
        h.assertTrue(h.getLevel().getBlockState(a.getBlockPos()).isAir(), "absent Thaumcraft uses original air fallback");
        h.assertTrue(a.getTransferredAmount() == 4, "magic leak is counted before contents are destroyed");
        for (int i = 0; i < a.getTanks(); i++) {
            h.assertTrue(a.getFluidInTank(i).isEmpty() && b.getFluidInTank(i).isEmpty(), "no destroyed contents dumped into neighbor");
        }
        h.succeed();
    }
}

