package gregtech.gametest;

import gregtech.content.cover.CoverAttachmentBehaviors;
import gregtech.content.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.capability.templates.FluidTank;
//?}

/** Issue 24: native world boundaries missed by the initial cover regression suite. */
@GameTestHolder("gregtech_drain_regression") @PrefixGameTestTemplate(false)
public final class CoverDrainRegressionTests {
    private static BlockPos site(GameTestHelper h, int offset) {
        var pos = new BlockPos(114200 + offset, 220, 114200);
        h.getLevel().getChunkAt(pos);
        for (var nearby : BlockPos.betweenClosed(pos.offset(-2,-2,-2), pos.offset(2,3,2)))
            h.getLevel().setBlockAndUpdate(nearby, Blocks.AIR.defaultBlockState());
        return pos;
    }

    @GameTest(template="test_empty",timeoutTicks=40)
    public static void solidWaterloggedHostsAndPartialTanksArePreserved(GameTestHelper h) {
        var level=h.getLevel(); var pos=site(h,0); var front=pos.east();
        var tank=new FluidTank(32000);
        var slab=Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true);
        level.setBlockAndUpdate(front,slab);
        h.assertTrue(!CoverAttachmentBehaviors.tickDrain(level,pos,Direction.EAST,tank,5)
                &&level.getBlockState(front).equals(slab)&&tank.isEmpty(),"drain leaves a waterlogged solid host intact");
        level.setBlockAndUpdate(front,Blocks.WATER.defaultBlockState());
        var small=new FluidTank(999);
        h.assertTrue(!CoverAttachmentBehaviors.tickDrain(level,pos,Direction.EAST,small,5)
                &&small.isEmpty()&&level.getFluidState(front).isSource(),"insufficient capacity preserves source and tank");
        h.assertTrue(CoverAttachmentBehaviors.tickDrain(level,pos,Direction.EAST,tank,5)
                &&tank.getFluidAmount()==1000&&level.getBlockState(front).isAir(),"ordinary source yields one bucket and disappears");
        level.setBlockAndUpdate(pos.below(),Blocks.WATER.defaultBlockState());
        h.assertTrue(!CoverAttachmentBehaviors.tickDrain(level,pos,Direction.DOWN,tank,5)
                &&level.getFluidState(pos.below()).isSource(),"ordinary water is not lifted against gravity");
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=40)
    public static void bulkWatersUseOriginalProductsAndKeepFlowingBlocks(GameTestHelper h) {
        var level=h.getLevel(); var pos=site(h,8);
        String[] fields={"River_Water","Ocean","Swampwater"};
        var expected=new net.minecraft.world.level.material.Fluid[]{Fluids.WATER,GTFluids.still("Ocean").get(),GTFluids.still("Dirty_Water").get()};
        for(int i=0;i<fields.length;i++) for(var side:new Direction[]{Direction.EAST,Direction.DOWN}) {
            var front=pos.relative(side);
            var source=GTFluids.still(fields[i]).get().defaultFluidState().createLegacyBlock();
            for(int depth:new int[]{0,3}) {
                var state=source.setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,depth);
                level.setBlockAndUpdate(front,state);
                var tank=new FluidTank(32000);
                h.assertTrue(CoverAttachmentBehaviors.tickDrain(level,pos,side,tank,5)
                        &&tank.getFluidAmount()==16000&&tank.getFluid().getFluid()==expected[i]
                        &&level.getBlockState(front).equals(state),"bulk water product/amount/source retained: "+fields[i]+" "+side+" "+depth);
            }
        }
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=40)
    public static void rainNeedsAnOpenFaceAndSideSupport(GameTestHelper h) {
        var level=h.getLevel(); var pos=site(h,16); var front=pos.east();
        h.assertTrue(CoverAttachmentBehaviors.canCollectRainAt(level,pos,Direction.UP),"top receives unobstructed rain");
        h.assertTrue(!CoverAttachmentBehaviors.canCollectRainAt(level,pos,Direction.EAST),"side needs ground below rain catchment");
        level.setBlockAndUpdate(front.below(),Blocks.STONE.defaultBlockState());
        h.assertTrue(CoverAttachmentBehaviors.canCollectRainAt(level,pos,Direction.EAST),"side collects on an exposed solid floor");
        level.setBlockAndUpdate(front,Blocks.GLASS.defaultBlockState());
        h.assertTrue(!CoverAttachmentBehaviors.canCollectRainAt(level,pos,Direction.EAST),"transparent solid blocks rain entry");
        level.setBlockAndUpdate(front,Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(front.above(),Blocks.STONE.defaultBlockState());
        h.assertTrue(!CoverAttachmentBehaviors.canCollectRainAt(level,pos,Direction.EAST),"roof stops collection");
        h.assertTrue(!CoverAttachmentBehaviors.canCollectRainAt(level,pos,Direction.DOWN),"bottom never collects rain");
        h.succeed();
    }
}
