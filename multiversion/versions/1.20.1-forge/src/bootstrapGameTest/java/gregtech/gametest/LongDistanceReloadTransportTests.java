package gregtech.gametest;

import gregtech.content.block.misc.LongDistEndpointBlock;
import gregtech.content.block.misc.LongDistEndpointBlockEntity;
import gregtech.content.registry.GTLongDistance;
import gregtech.content.registry.GTTanks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real endpoint NBT/world rebinding, not a simulated link or same-object reload. */
@GameTestHolder("gt6ce_long_distance_reload")
@PrefixGameTestTemplate(false)
public final class LongDistanceReloadTransportTests {
    private static final BlockPos S=new BlockPos(2,2,2), R=new BlockPos(5,2,2);
    private static final BlockPos INPUT=S.west(), OUTPUT=R.east(), BREAK=S.east();

    @GameTest(template="test_blueprint_empty")
    public static void itemReloadThenTransferThenBreak(GameTestHelper h){item(h,true);}
    @GameTest(template="test_blueprint_empty")
    public static void itemReloadThenBreakBeforeFirstTransfer(GameTestHelper h){item(h,false);}
    @GameTest(template="test_blueprint_empty")
    public static void fluidReloadThenTransferThenBreak(GameTestHelper h){fluid(h,true);}
    @GameTest(template="test_blueprint_empty")
    public static void fluidReloadThenBreakBeforeFirstTransfer(GameTestHelper h){fluid(h,false);}

    private static void build(GameTestHelper h,boolean fluid){
        var endpoint=GTLongDistance.block(fluid?"long_dist_endpoint_fluid":"long_dist_endpoint_item");
        var pipe=GTLongDistance.block(fluid?"long_dist_pipe_fluid":"long_dist_pipe_item");
        h.setBlock(S,endpoint.defaultBlockState().setValue(LongDistEndpointBlock.FACING,Direction.WEST));
        h.setBlock(R,endpoint.defaultBlockState().setValue(LongDistEndpointBlock.FACING,Direction.WEST));
        h.setBlock(S.east(),pipe);h.setBlock(S.east(2),pipe);
        h.setBlock(INPUT,fluid?GTTanks.DRUM_STEEL.get():Blocks.CHEST);
        h.setBlock(OUTPUT,fluid?GTTanks.DRUM_STEEL.get():Blocks.CHEST);
    }
    private static LongDistEndpointBlockEntity endpoint(GameTestHelper h,BlockPos p){
        return (LongDistEndpointBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
    }
    private static void reload(GameTestHelper h){
        var source=endpoint(h,S);var receiver=endpoint(h,R);
        var saved=source.saveWithoutMetadata();var receiverSaved=receiver.saveWithoutMetadata();
        h.assertTrue(saved.contains("gt.target")&&saved.getLong("gt.target")==h.absolutePos(R).asLong(),"precondition: established non-self route actually persisted");
        source.setRemoved();receiver.setRemoved();
        var freshSource=new LongDistEndpointBlockEntity(source.getBlockPos(),source.getBlockState());
        var freshReceiver=new LongDistEndpointBlockEntity(receiver.getBlockPos(),receiver.getBlockState());
        freshSource.load(saved);freshReceiver.load(receiverSaved);
        h.getLevel().setBlockEntity(freshReceiver);h.getLevel().setBlockEntity(freshSource);
        h.assertTrue(freshSource!=source&&freshReceiver!=receiver&&endpoint(h,S)==freshSource&&endpoint(h,R)==freshReceiver
                &&!freshSource.isRemoved()&&!freshReceiver.isRemoved(),"fresh NBT-loaded endpoints are bound to the actual world");
    }
    private static IItemHandler items(GameTestHelper h,BlockPos p){
        return h.getLevel().getBlockEntity(h.absolutePos(p)).getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
    }
    private static IFluidHandler fluids(GameTestHelper h,BlockPos p){
        return h.getLevel().getBlockEntity(h.absolutePos(p)).getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
    }
    private static int itemCount(IItemHandler h){int n=0;for(int i=0;i<h.getSlots();i++)n+=h.getStackInSlot(i).getCount();return n;}
    /** Debit the real source inventory only by the actual destination acceptance. */
    private static int moveItem(IItemHandler from,IItemHandler route){
        var offered=from.extractItem(0,1,true);
        var remainder=route.insertItem(0,offered,false);
        int accepted=offered.getCount()-remainder.getCount();
        if(accepted>0)from.extractItem(0,accepted,false);
        return accepted;
    }
    private static int moveFluid(IFluidHandler from,IFluidHandler route){
        var offered=from.drain(100,IFluidHandler.FluidAction.SIMULATE);
        int accepted=route.fill(offered,IFluidHandler.FluidAction.EXECUTE);
        if(accepted>0)from.drain(accepted,IFluidHandler.FluidAction.EXECUTE);
        return accepted;
    }
    private static void item(GameTestHelper h,boolean transferFirst){
        build(h,false);var input=items(h,INPUT);var output=items(h,OUTPUT);
        h.assertTrue(input.insertItem(0,new ItemStack(Items.DIAMOND,16),false).isEmpty(),"real upstream inventory seeded");
        h.assertTrue(moveItem(input,items(h,S))==1&&itemCount(input)==15&&itemCount(output)==1,"live item route positive control");
        reload(h);var restored=items(h,S);
        if(transferFirst)h.assertTrue(moveItem(input,restored)==1,"intact path transfers after fresh reload without wrench");
        h.setBlock(BREAK,Blocks.AIR);
        int beforeSource=itemCount(input),beforeTarget=itemCount(output);
        h.assertTrue(moveItem(input,restored)==0&&itemCount(input)==beforeSource&&itemCount(output)==beforeTarget,
                "broken restored item route transfers nothing and consumes nothing without wrench");
        h.setBlock(BREAK,GTLongDistance.block("long_dist_pipe_item"));
        h.assertTrue(moveItem(input,restored)==1&&itemCount(input)==beforeSource-1&&itemCount(output)==beforeTarget+1,"repair rebuilds item route without wrench");
        reload(h);h.assertTrue(moveItem(input,items(h,S))==1,"complete item route survives another fresh reload");
        h.succeed();
    }
    private static void fluid(GameTestHelper h,boolean transferFirst){
        build(h,true);var input=fluids(h,INPUT);var output=fluids(h,OUTPUT);
        h.assertTrue(input.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE)==1000,"real upstream tank seeded");
        h.assertTrue(moveFluid(input,fluids(h,S))==100&&input.getFluidInTank(0).getAmount()==900&&output.getFluidInTank(0).getAmount()==100,"live fluid route positive control");
        reload(h);var restored=fluids(h,S);
        if(transferFirst)h.assertTrue(moveFluid(input,restored)==100,"intact path transfers after fresh reload without wrench");
        h.setBlock(BREAK,Blocks.AIR);
        int beforeSource=input.getFluidInTank(0).getAmount(),beforeTarget=output.getFluidInTank(0).getAmount();
        h.assertTrue(moveFluid(input,restored)==0&&input.getFluidInTank(0).getAmount()==beforeSource&&output.getFluidInTank(0).getAmount()==beforeTarget,
                "broken restored fluid route transfers nothing and consumes nothing without wrench");
        h.setBlock(BREAK,GTLongDistance.block("long_dist_pipe_fluid"));
        h.assertTrue(moveFluid(input,restored)==100&&input.getFluidInTank(0).getAmount()==beforeSource-100&&output.getFluidInTank(0).getAmount()==beforeTarget+100,"repair rebuilds fluid route without wrench");
        reload(h);h.assertTrue(moveFluid(input,fluids(h,S))==100,"complete fluid route survives another fresh reload");
        h.succeed();
    }
    /** Detached native adapter guard, NOT a real chunk unload/reload lifecycle claim. */
    @GameTest(template="test_blueprint_empty")
    public static void unvalidatedHintsDoNotLoadMissingSourceChunks(GameTestHelper h){
        var level=h.getLevel();var far=h.absolutePos(S).offset(1_000_000,0,1_000_000);
        h.assertTrue(!level.hasChunkAt(far)&&!level.hasChunkAt(far.east()),"guard coordinates start unloaded");
        for(boolean fluid:new boolean[]{false,true}){
            build(h,fluid);
            if(fluid)h.assertTrue(fluids(h,S).fill(new FluidStack(Fluids.WATER,1),IFluidHandler.FluidAction.SIMULATE)==1,"real fluid hint established");
            else h.assertTrue(items(h,S).insertItem(0,new ItemStack(Items.DIAMOND),true).isEmpty(),"real item hint established");
            var source=endpoint(h,S);var tag=source.saveWithoutMetadata();
            h.assertTrue(tag.contains("gt.target"),"native route serialized before guard");
            source.setRemoved();
            var detached=new LongDistEndpointBlockEntity(far,source.getBlockState());detached.load(tag);detached.setLevel(level);
            if(fluid){
                var handler=detached.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
                h.assertTrue(handler.fill(new FluidStack(Fluids.WATER,100),IFluidHandler.FluidAction.EXECUTE)==0
                        &&fluids(h,OUTPUT).getFluidInTank(0).isEmpty(),"unvalidated fluid hint cannot transfer through unloaded origin");
            }else{
                var handler=detached.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
                h.assertTrue(handler.insertItem(0,new ItemStack(Items.DIAMOND,3),false).getCount()==3
                        &&itemCount(items(h,OUTPUT))==0,"unvalidated item hint cannot transfer through unloaded origin");
            }
            h.assertTrue(!level.hasChunkAt(far)&&!level.hasChunkAt(far.east()),"route validation does not force source chunks loaded");
            detached.setRemoved();
        }
        var state=GTLongDistance.block("long_dist_transformer_ulv").defaultBlockState()
                .setValue(gregtech.content.block.misc.LongDistanceTransformerBlock.FACING,Direction.WEST);
        h.setBlock(S,state);h.setBlock(R,state);
        h.setBlock(S.east(),GTLongDistance.block("long_dist_wire_ev"));h.setBlock(S.east(2),GTLongDistance.block("long_dist_wire_ev"));
        var source=(gregtech.content.block.misc.LongDistanceTransformerBlockEntity)level.getBlockEntity(h.absolutePos(S));
        h.assertTrue(source.doEnergyInjection(gregtech.content.data.GregTechTags.Energy.EU,Direction.WEST,2048,1,false)==1,"real wire hint established");
        var tag=source.saveWithoutMetadata();h.assertTrue(tag.contains("gt.target"),"wire route serialized");source.setRemoved();
        var detached=new gregtech.content.block.misc.LongDistanceTransformerBlockEntity(far,state);detached.load(tag);detached.setLevel(level);
        h.assertTrue(detached.doEnergyInjection(gregtech.content.data.GregTechTags.Energy.EU,Direction.WEST,2048,1,false)==0
                &&detached.doEnergyInjection(gregtech.content.data.GregTechTags.Energy.EU,Direction.WEST,2048,1,true)==0,
                "unvalidated wire hint cannot accept energy through unloaded origin");
        h.assertTrue(!level.hasChunkAt(far)&&!level.hasChunkAt(far.east()),"wire validation does not force source chunks loaded");
        detached.setRemoved();h.succeed();
    }

}
