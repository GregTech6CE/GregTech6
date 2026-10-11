package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.MachineControl;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.block.misc.*;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.logistics.ExtenderSpec;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class UniversalExtenderTests {
    private static final BlockPos CENTER=new BlockPos(4,3,4);
    private static ExtenderBlockEntity relay(GameTestHelper h,BlockPos pos,ExtenderSpec spec,Direction front,Direction secondary){
        h.setBlock(pos,Blocks.AIR);
        h.setBlock(pos,GTMiscBlocks.SOURCE_EXTENDERS.get(spec).get().defaultBlockState().setValue(SourceExtenderBlock.FACING,front).setValue(SourceExtenderBlock.SECONDARY,secondary));
        return (ExtenderBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    private static BasicMachineBlockEntity machine(GameTestHelper h,BlockPos pos){
        h.setBlock(pos,Blocks.AIR);h.setBlock(pos,MachineRegistry.basicMachines().iterator().next().get());
        return (BasicMachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    @GameTest(template="test_blueprint_empty") public static void liveControlsSixSidesAndCycles(GameTestHelper h){
        for(var spec:new ExtenderSpec[]{ExtenderSpec.UNIVERSAL,ExtenderSpec.UNIVERSAL_BRIDGE}){
            var relay=relay(h,CENTER,spec,Direction.UP,Direction.EAST);
            var machines=new java.util.EnumMap<Direction,BasicMachineBlockEntity>(Direction.class);
            for(var side:Direction.values())machines.put(side,machine(h,CENTER.relative(side)));
            for(var side:Direction.values()){
                machines.values().forEach(m->m.machineControl(null).setEnabled(true));
                var control=relay.machineControl(side);h.assertTrue(control.available(),"supported target detected");
                var exit=spec.bridge?side.getOpposite():side==Direction.UP?Direction.EAST:Direction.UP;
                h.assertTrue(!control.setEnabled(false),"disable returns actual disabled state");
                for(var entry:machines.entrySet())h.assertTrue(entry.getValue().machineControl(null).enabled()==(entry.getKey()!=exit),"only selected machine changes: "+spec+" / "+side);
                h.assertTrue(control.setEnabled(true),"target can restart");
            }
            var retained=relay.machineControl(Direction.DOWN);
            var removedControl=machines.get(Direction.UP).machineControl(null);
            h.setBlock(CENTER.above(),Blocks.AIR);
            h.assertTrue(!removedControl.available()&&!removedControl.setEnabled(true),"retained direct control of removed machine cannot affect its replacement");
            h.assertTrue(!retained.available()&&!retained.setEnabled(true)&&retained.progressMax()==0,"retained interface cannot control removed machine");
            var replacement=machine(h,CENTER.above());
            h.assertTrue(retained.available(),"same interface resolves replacement");retained.setEnabled(false);
            h.assertTrue(!replacement.machineControl(null).enabled(),"replacement receives command");
        }
        var a=relay(h,CENTER,ExtenderSpec.UNIVERSAL,Direction.EAST,Direction.EAST);
        relay(h,CENTER.east(),ExtenderSpec.UNIVERSAL,Direction.WEST,Direction.WEST);
        var cycle=a.machineControl(Direction.WEST);
        h.assertTrue(!cycle.available()&&!cycle.setEnabled(true)&&!cycle.running()&&cycle.progress()==0,"control cycle terminates");
        var ordinary=relay(h,CENTER,ExtenderSpec.COMBINED,Direction.NORTH,Direction.SOUTH);
        h.assertTrue(ordinary.machineControl(Direction.NORTH)==null,"ordinary extenders do not gain universal control");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void stopSaveResumePendingOutput(GameTestHelper h){
        var block=MachineRegistry.basicMachines().stream().map(e->e.get()).filter(b->b.basicSpec().recipeMap().mOutputItemsCount>0).findFirst().orElseThrow();
        h.setBlock(CENTER,block);var machine=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(CENTER));
        var recipe=new com.gregtech.gregtech.api.recipe.Recipe(null,new ItemStack[]{new ItemStack(Items.DIAMOND,4)},null,null,null,null,16,16,0);
        var data=machine.saveWithoutMetadata();data.putLong("gt.progress",16);data.putLong("gt.max_progress",16);
        data.put("gt.pending_outputs",com.gregtech.gregtech.api.recipe.MachineWorkOutputs.roll(recipe,1,bound->0).save());machine.load(data);
        var relay=relay(h,CENTER.south(),ExtenderSpec.UNIVERSAL,Direction.NORTH,Direction.DOWN);
        var control=relay.machineControl(null);
        h.assertTrue(control.progress()==16&&control.progressMax()==16,"progress reaches relay unrounded");
        control.setEnabled(false);
        for(int i=0;i<3;i++)BasicMachineBlockEntity.serverTick(h.getLevel(),machine.getBlockPos(),machine.getBlockState(),machine);
        h.assertTrue(control.progress()==16&&machine.saveWithoutMetadata().contains("gt.pending_outputs"),"stopped machine retains finished but undelivered output");
        var saved=machine.saveWithoutMetadata();machine.load(saved);
        h.assertTrue(!control.enabled(),"stop state survives save/load");
        control.setEnabled(true);BasicMachineBlockEntity.serverTick(h.getLevel(),machine.getBlockPos(),machine.getBlockState(),machine);
        h.assertTrue(!machine.saveWithoutMetadata().contains("gt.pending_outputs"),"restart delivers pending work");
        int count=0;for(int i=machine.inputSlots();i<machine.inventory().getSlots();i++)if(machine.inventory().getStackInSlot(i).is(Items.DIAMOND))count+=machine.inventory().getStackInSlot(i).getCount();
        count+=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(machine.getBlockPos()).inflate(2)).stream().map(net.minecraft.world.entity.item.ItemEntity::getItem).filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
        h.assertTrue(count==4,"restart neither loses nor duplicates output");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void sourceRedstoneAndComparatorRouting(GameTestHelper h){
        for(var front:Direction.values()){
            for(var side:Direction.values())h.setBlock(CENTER.relative(side),Blocks.AIR);
            var relay=relay(h,CENTER,ExtenderSpec.UNIVERSAL,front,front.getOpposite());
            h.setBlock(CENTER.relative(front),Blocks.REDSTONE_BLOCK);
            for(var query:Direction.values())h.assertTrue(h.getLevel().getSignal(h.absolutePos(CENTER),query)==(query.getOpposite()==front?0:15),"source main-face redstone fanout: "+front+" / "+query);
            h.setBlock(CENTER.relative(front),Blocks.AIR);h.setBlock(CENTER.relative(front.getOpposite()),Blocks.REDSTONE_BLOCK);
            for(var query:Direction.values())h.assertTrue(relay.signal(query)==(query.getOpposite()==front?15:0),"other inputs return only to main face");
            h.setBlock(CENTER.relative(front.getOpposite()),Blocks.AIR);h.setBlock(CENTER.relative(front),Blocks.BARREL);
            var chest=(net.minecraft.world.level.block.entity.BarrelBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(CENTER.relative(front)));chest.setItem(0,new ItemStack(Items.DIAMOND,64));
            h.assertTrue(relay.comparator(null)==1,"comparator reads source main inventory");
            chest.clearContent();h.assertTrue(relay.comparator(null)==0,"live contents do not require reopening GUI");
        }
        for(var side:Direction.values())h.setBlock(CENTER.relative(side),Blocks.AIR);
        var bridge=relay(h,CENTER,ExtenderSpec.UNIVERSAL_BRIDGE,Direction.NORTH,Direction.SOUTH);
        h.setBlock(CENTER.east(),Blocks.REDSTONE_BLOCK);
        for(var query:Direction.values())h.assertTrue(bridge.signal(query)==(query==Direction.WEST?15:0),"bridge follows original opposite-query incoming rule");
        h.setBlock(CENTER.east(),Blocks.AIR);
        var a=relay(h,CENTER,ExtenderSpec.UNIVERSAL,Direction.EAST,Direction.EAST);
        relay(h,CENTER.east(),ExtenderSpec.UNIVERSAL,Direction.WEST,Direction.WEST);
        for(var side:Direction.values())h.assertTrue(a.signal(side)==0&&a.comparator(side)==0,"unpowered relay cycle terminates without phantom power");h.succeed();
    }
}
