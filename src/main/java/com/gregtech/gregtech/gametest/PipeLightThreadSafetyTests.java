package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.block.machine.*;
import com.gregtech.gregtech.block.energy.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class PipeLightThreadSafetyTests {
    @GameTest(template="test_empty")
    public static void pipeLightQueriesNeverLoadNeighbourChunks(GameTestHelper h) {
        BlockGetter forbidden=new BlockGetter() {
            public BlockEntity getBlockEntity(BlockPos p){throw new AssertionError("light query requested a block entity");}
            public BlockState getBlockState(BlockPos p){throw new AssertionError("light query requested neighbouring chunk state at "+p);}
            public FluidState getFluidState(BlockPos p){throw new AssertionError("light query requested neighbouring fluid");}
            public int getHeight(){return 384;}
            public int getMinBuildHeight(){return -64;}
        };
        int tested=0;
        for(Block b:ForgeRegistries.BLOCKS) {
            double diameter;
            net.minecraft.world.level.block.state.properties.BooleanProperty[] connections;
            if(b instanceof ItemPipeBlock pipe){diameter=pipe.spec().diameter();connections=ItemPipeBlock.CONNECTIONS;}
            else if(b instanceof FluidPipeBlock pipe){diameter=pipe.spec().diameter();connections=FluidPipeBlock.CONNECTIONS;}
            else if(b instanceof ElectricWireBlock wire){diameter=wire.spec().halfThickness()/8;connections=ElectricWireBlock.CONNECTIONS;}
            else if(b instanceof AxleBlock axle){diameter=axle.spec().halfThickness()/8;connections=AxleBlock.CONNECTIONS;}
            else continue;
            for(int mask=0;mask<64;mask++)for(boolean wet:new boolean[]{false,true}) {
                var state=b.defaultBlockState().setValue(GTWaterloggable.WATERLOGGED,wet);
                for(int i=0;i<6;i++)state=state.setValue(connections[i],(mask&(1<<i))!=0);
                boolean expected=diameter<1&&!wet;
                h.assertTrue(state.propagatesSkylightDown(forbidden,new BlockPos(15,64,15))==expected,"pipe skylight geometry/fluid semantics: "+b);
                h.assertTrue(state.getLightBlock(forbidden,new BlockPos(15,64,15))==(expected?0:1),"pipe opacity remains equivalent to noOcclusion vanilla logic");
                tested++;
            }
        }
        h.assertTrue(tested>256,"registered pipes, wires, axles and all connection masks exercised");h.succeed();
    }
}
