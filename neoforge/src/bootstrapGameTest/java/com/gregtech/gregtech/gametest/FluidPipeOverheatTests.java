package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.fluid.PipeIgnition;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_fluid_channels")
@PrefixGameTestTemplate(false)
public final class FluidPipeOverheatTests {
    private static final class Pipe extends FluidPipeBlockEntity {
        final boolean destroy;
        int heatRolls;
        Pipe(BlockPos pos, BlockState state, boolean destroy) { super(pos, state); this.destroy = destroy; }
        @Override protected boolean overheatDestroysPipe() { heatRolls++; return destroy; }
    }

    private static BlockPos arena(GameTestHelper h, int test) {
        var center = h.absolutePos(new BlockPos(2, 0, 2)).atY(240 + test * 8);
        // Separate vertical arenas, within build height; reset contents before any break callbacks.
        for (var pos : BlockPos.betweenClosed(center.offset(-1,-2,-1), center.offset(2,2,1))) {
            if (h.getLevel().getBlockEntity(pos) instanceof FluidPipeBlockEntity p) {
                for (int i = 0; i < p.getTanks(); i++) p.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
            }
        }
        for (var pos : BlockPos.betweenClosed(center.offset(-1,-2,-1), center.offset(2,2,1))) {
            h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        return center;
    }

    private static Pipe pipe(GameTestHelper h, BlockPos pos, String kind, boolean destroy) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", "pipe_" + kind));
        h.assertTrue(block instanceof FluidPipeBlock, "registered fixture " + kind);
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.OBSIDIAN.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var p = new Pipe(pos, block.defaultBlockState(), destroy);
        h.getLevel().setBlockEntity(p);
        return p;
    }

    private static void tick(GameTestHelper h, Pipe p) {
        FluidPipeBlockEntity.serverTick(h.getLevel(), p.getBlockPos(), p.getBlockState(), p);
    }

    private static void temperature(GameTestHelper h, Pipe p, long value) {
        var tag = p.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.putLong("gt.temperature", value);
        p.loadWithComponents(tag, h.getLevel().registryAccess());
    }

    @GameTest(template = "test_empty", timeoutTicks = 60)
    public static void overheatedWoodIgnitesAllSixAirNeighbors(GameTestHelper h) {
        var pos = arena(h, 0);
        var p = pipe(h, pos, "medium_wood", false);
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        p.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE);
        tick(h, p);
        for (Direction side : Direction.values()) h.assertTrue(h.getLevel().getBlockState(pos.relative(side)).is(Blocks.FIRE),
                "wood supports fire on neighbor " + side);
        h.assertTrue(p.heatRolls == 1 && h.getLevel().getBlockEntity(pos) == p, "failed destruction roll retains pipe");
        h.assertTrue(p.getFluidInTank(0).getAmount() == 100, "ignition alone does not consume safe liquid");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 60)
    public static void neighborShapeLavaFireAndGtFluidProtection(GameTestHelper h) {
        var pos = arena(h, 1);
        var p = pipe(h, pos, "medium_wood", false);
        var level = h.getLevel();
        level.setBlockAndUpdate(pos.west(), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(pos.east().below(), Blocks.OBSIDIAN.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Blocks.WHITE_CARPET.defaultBlockState());
        level.setBlockAndUpdate(pos.north(), Blocks.LAVA.defaultBlockState());
        var fluid = GTFluids.still("Swampwater").get().defaultFluidState().createLegacyBlock();
        h.assertTrue(BuiltInRegistries.BLOCK.getKey(fluid.getBlock()).getNamespace().equals("gregtech")
                && fluid.getCollisionShape(level, pos.south()).isEmpty(), "noncolliding registered GT fluid fixture");
        level.setBlockAndUpdate(pos.south(), fluid);
        level.setBlockAndUpdate(pos.above(), Blocks.FIRE.defaultBlockState().setValue(FireBlock.AGE, 7));
        p.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(level.getBlockState(pos.west()).is(Blocks.OAK_PLANKS), "solid flammable block is not directly replaced");
        h.assertTrue(level.getBlockState(pos.east()).is(Blocks.FIRE), "carpet is eligible despite a thin collision shape");
        h.assertTrue(level.getBlockState(pos.north()).is(Blocks.LAVA), "lava remains");
        h.assertTrue(level.getBlockState(pos.south()).is(fluid.getBlock()), "nonflammable GT fluid remains protected");
        h.assertTrue(level.getBlockState(pos.above()).getValue(FireBlock.AGE) == 7, "existing fire is not reset");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 60)
    public static void integrationProtectionTagPreventsReplacement(GameTestHelper h) {
        var pos = arena(h, 2);
        var p = pipe(h, pos, "medium_wood", false);
        var target = pos.east();
        h.getLevel().setBlockAndUpdate(target.below(), Blocks.OBSIDIAN.defaultBlockState());
        h.getLevel().setBlockAndUpdate(target, Blocks.TORCH.defaultBlockState());
        h.assertTrue(h.getLevel().getBlockState(target).is(PipeIgnition.PROTECTED), "test-only protection tag loaded");
        p.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.TORCH), "tagged noncolliding target is protected");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 60)
    public static void burnoutClearsAllContentsBeforeBreakAndTransfer(GameTestHelper h) {
        var pos = arena(h, 3);
        var b = pipe(h, pos.east(), "quadruple_wood", false);
        var a = pipe(h, pos, "quadruple_wood", true);
        a.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE);
        a.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        h.getLevel().setBlockAndUpdate(pos, a.getBlockState().setValue(FluidPipeBlock.EAST, true));
        h.getLevel().setBlockAndUpdate(pos.east(), b.getBlockState().setValue(FluidPipeBlock.WEST, true));
        tick(h, a);
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.FIRE) && a.heatRolls == 1, "burnout replaces center with ordinary fire and returns");
        for (int i = 0; i < a.getTanks(); i++) h.assertTrue(a.getFluidInTank(i).isEmpty() && b.getFluidInTank(i).isEmpty(),
                "contents destroyed before distribution and break callback");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 60)
    public static void exactLimitIsSafeButHotEmptyPipeBurnsBeforeCooling(GameTestHelper h) {
        var pos = arena(h, 4);
        var p = pipe(h, pos, "medium_wood", true);
        temperature(h, p, 340);
        tick(h, p);
        h.assertTrue(h.getLevel().getBlockEntity(pos) == p && p.heatRolls == 0, "strictly greater than threshold required");
        temperature(h, p, 341);
        tick(h, p);
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.FIRE) && p.heatRolls == 1, "hot empty pipe checks burnout before cooling to threshold");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 60)
    public static void emptyQuadrupleChecksEveryChannelThenCoolsOnce(GameTestHelper h) {
        var pos = arena(h, 5);
        var p = pipe(h, pos, "quadruple_wood", false);
        temperature(h, p, 341);
        tick(h, p);
        h.assertTrue(p.heatRolls == 4 && p.getTemperature() == 340, "four channel checks precede one kelvin cooling");
        tick(h, p);
        h.assertTrue(p.heatRolls == 4, "threshold tick does not repeat heat rolls");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 60)
    public static void plasmaProofDoesNotBypassMeltingLimit(GameTestHelper h) {
        var pos = arena(h, 6);
        var p = pipe(h, pos, "medium_netherite", true);
        var plasma = GTFluids.still("HeliumPlasma").get();
        h.assertTrue(p.spec().plasmaProof() && GTFluids.entryForFluid(plasma).temperature() > p.spec().maxTemperature(), "proof but over-limit plasma fixture");
        p.fill(new FluidStack(plasma, 100), FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.FIRE) && p.getFluidInTank(0).isEmpty(), "plasma proof does not prevent thermal burnout");
        h.assertTrue(p.getTransferredAmount() == 0, "destroyed bulk is not counted as plasma leakage");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 60)
    public static void channelOrderDecidesWhetherColdFluidCanReplaceOldHeat(GameTestHelper h) {
        var pos = arena(h, 7);
        var p = pipe(h, pos, "quadruple_wood", true);
        p.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        temperature(h, p, 1000);
        tick(h, p);
        h.assertTrue(p.heatRolls == 0 && h.getLevel().getBlockEntity(pos) == p, "first occupied cold channel replaces old heat before check");
        p.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        p.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE);
        p.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        p.drain(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE);
        temperature(h, p, 1000);
        tick(h, p);
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.FIRE) && p.heatRolls == 1,
                "earlier empty channel checks retained heat before later cold fluid");
        h.succeed();
    }
}

