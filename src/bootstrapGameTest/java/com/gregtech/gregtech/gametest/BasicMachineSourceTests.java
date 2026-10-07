package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.machine.OriginalBasicMachineRules;
import com.gregtech.gregtech.api.recipe.Recipe;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
@net.minecraftforge.gametest.GameTestHolder("gregtech_basic_source")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class BasicMachineSourceTests {
    private static java.util.List<BasicMachineBlock> machines() {return java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(),false)
        .filter(b->b instanceof BasicMachineBlock).map(b->(BasicMachineBlock)b).filter(b->OriginalBasicMachineRules.handles(b.basicSpec().machineName(),b.basicSpec().tier())).toList();}
    private static Object value(BasicMachineBlockEntity be,String method) throws ReflectiveOperationException {
        var m=BasicMachineBlockEntity.class.getDeclaredMethod(method);m.setAccessible(true);return m.invoke(be);
    }
    private static void tick(GameTestHelper h,BasicMachineBlockEntity be) {BasicMachineBlockEntity.serverTick(h.getLevel(),be.getBlockPos(),be.getBlockState(),be);}
    private static net.minecraft.nbt.CompoundTag save(GameTestHelper h,BasicMachineBlockEntity be) {return be.saveWithoutMetadata();}
    @GameTest(template="test_empty",timeoutTicks=160)
    public static void original_flags_costs_and_ignition(GameTestHelper h) throws Exception {
        var machines=machines();h.assertTrue(machines.size()==247,"actual247 source registered machines");
        for(var block:machines) {
            var spec=block.basicSpec();var be=(BasicMachineBlockEntity)block.newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            h.assertTrue(value(be,"efficiency").equals(OriginalBasicMachineRules.efficiency(spec.machineName(),spec.tier())),"actual factory source efficiency "+spec.id());
            h.assertTrue(value(be,"cheapOverclocking").equals(OriginalBasicMachineRules.cheapOverclocking(spec.machineName(),spec.tier())),"actual factory source overclock "+spec.id());
            h.assertTrue(value(be,"requiresConstantEnergy").equals(!spec.energyType().equals("TU")&&!OriginalBasicMachineRules.noConstantPower(spec.machineName(),spec.tier())),"actual factory source constant power "+spec.id());
        }
        var main=h.absolutePos(new BlockPos(1,1,1));
        for(var name:java.util.List.of("electricmixer","roaster","burnmixer")) {
            var block=machines.stream().filter(b->b.basicSpec().machineName().equals(name)&&b.basicSpec().tier()==1).findFirst().orElseThrow();
            h.getLevel().setBlockAndUpdate(main,block.defaultBlockState());var be=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(main);
            var target=main.relative(be.getBlockState().getValue(BasicMachineBlock.FACING).getCounterClockWise());
            if(name.equals("burnmixer"))h.getLevel().setBlockAndUpdate(target,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
            var map=be.spec().recipeMap();int itemCount=Math.min(map.mInputItemsCount,Math.max(1,map.mMinimalInputs));
            int fluidCount=Math.max(map.mMinimalInputFluids,map.mMinimalInputs-itemCount);
            Item[] markers={Items.BEDROCK,Items.BARRIER,Items.COMMAND_BLOCK,Items.STRUCTURE_BLOCK,Items.DEBUG_STICK,Items.JIGSAW};
            var inputs=new ItemStack[itemCount];for(int i=0;i<itemCount;i++)inputs[i]=new ItemStack(markers[i]);
            var fluids=new FluidStack[fluidCount];for(int i=0;i<fluidCount;i++)fluids[i]=new FluidStack(Fluids.WATER,1000);
            var recipe=map.addRecipe(false,inputs,new ItemStack[]{new ItemStack(Items.STICK)},null,new long[]{10000},fluids,new FluidStack[0],name.equals("burnmixer")?40:10,name.equals("burnmixer")?32:8,0);
            h.assertTrue(recipe!=null,"isolated marker fixture accepted "+name);
            try {
                for(int i=0;i<itemCount;i++)be.inventory().setStackInSlot(i,inputs[i].copy());
                for(int i=0;i<fluidCount;i++)be.getTanksInput()[i].setFluid(fluids[i].copy());
                if(name.equals("burnmixer")) {
                    be.doInject(be.spec().energyTag(),null,32,1,true);tick(h,be);
                    h.assertTrue(be.inventory().getStackInSlot(0).getCount()==1&&save(h,be).getLong("gt.max_progress")==0,"original unignited burner refuses reservation");
                    h.assertTrue(be.onIgnite(h.getLevel(),main,Direction.NORTH,null,ItemStack.EMPTY,false,0,0,0)==10000,"real igniter method cost");
                    var saved=save(h,be);h.assertTrue(saved.getByte("gt.ignite")==40,"source ignition byte key40");
                    be.load(saved);
                    h.assertTrue(save(h,be).getByte("gt.ignite")==40,"native save/load ignition preserved");
                } else h.assertTrue(be.onIgnite(h.getLevel(),main,Direction.NORTH,null,ItemStack.EMPTY,false,0,0,0)==0,"ordinary machine does not claim ignition");
                be.doInject(be.spec().energyTag(),null,32,1,true);tick(h,be);
                long expected=name.equals("electricmixer")?320:name.equals("roaster")?80:1280;
                h.assertTrue(save(h,be).getLong("gt.max_progress")==expected&&be.inventory().getStackInSlot(0).isEmpty(),"actual reserved job source total work "+name);
                if(name.equals("burnmixer")) {
                    for(int i=1;i<40;i++){be.doInject(be.spec().energyTag(),null,32,1,true);tick(h,be);}
                    var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(target);
                    h.assertTrue(chest.getItem(0).is(Items.STICK)&&save(h,be).getByte("gt.ignite")==40,"40 real explicit work ticks deliver to actual chest and renew original ignition; timer="+save(h,be).getByte("gt.ignite"));
                }
            } finally {map.mRecipeList.remove(recipe);h.getLevel().removeBlock(main,false);if(name.equals("burnmixer"))h.getLevel().removeBlock(target,false);}
        }
        h.succeed();
    }
}
