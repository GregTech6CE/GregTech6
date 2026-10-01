package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.content.energy.ZpmEnergy;
import com.gregtech.gregtech.block.energy.ZpmDischargerBlock;
import com.gregtech.gregtech.blockentity.energy.*;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class ZpmRegressionTests {
    @GameTest(template="test_blueprint_empty")
    public static void zpmDischargesWithoutDuplicationAndSurvivesRemoval(GameTestHelper h){
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,GTLasers.ZPM_DISCHARGER_ADVANCED.get().defaultBlockState().setValue(ZpmDischargerBlock.FACING,Direction.EAST));
        var z=(ZpmDischargerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        var module=ZpmEnergy.charged();
        h.assertTrue(z.inventory().insertItem(0,new ItemStack(Items.DIAMOND),false).is(Items.DIAMOND),"only modules accepted");
        h.assertTrue(z.inventory().insertItem(0,module,true).isEmpty()&&z.totalEnergy()==0,"simulated insert cannot create energy");
        h.assertTrue(z.inventory().insertItem(0,module,false).isEmpty(),"artifact inserts");
        z.tick();h.assertTrue(z.totalEnergy()==ZpmEnergy.CAPACITY,"disconnected output retains energy across buffering");
        h.assertTrue(z.doEnergyInjection(GregTechTags.Energy.QU,Direction.WEST,131072,1,true)==0,"cannot recharge artifact or converter");
        var receiverBlock=com.gregtech.gregtech.api.machine.MachineRegistry.basicMachines().get(0).get();
        h.setBlock(pos.east(),receiverBlock);
        var receiver=(com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos.east()));
        receiver.setSpec(com.gregtech.gregtech.api.machine.BasicMachineSpec.builder("zpm_test",receiverBlock.basicSpec().material())
                .machineType(receiverBlock.basicSpec().machineName()).energy(GregTechTags.Energy.EU,ZpmEnergy.PACKET)
                .recipes(receiverBlock.basicSpec().recipeMap())
                .faces(com.gregtech.gregtech.api.energy.FaceConfig.ALL_SIDES).build());
        z.tick();h.assertTrue(receiver.getEnergyTick()==ZpmEnergy.PACKET&&z.totalEnergy()==ZpmEnergy.CAPACITY-ZpmEnergy.PACKET,"front converts exactly one QU packet to EU");
        z.toggleStopped();long before=z.totalEnergy();z.tick();h.assertTrue(z.totalEnergy()==before,"stop prevents emission");
        var drops=z.getBlockState().getBlock().getDrops(z.getBlockState(),new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(z.getBlockPos()))
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,ItemStack.EMPTY)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY,z));
        var restored=new ZpmDischargerBlockEntity(z.getBlockPos(),z.getBlockState());
        restored.load(drops.get(0).getTag().getCompound("BlockEntityTag"));
        h.assertTrue(restored.totalEnergy()==before&&restored.stopped(),"drop retains module, buffer and switch exactly once");
        var extracted=restored.inventory().extractItem(0,1,false);
        h.assertTrue(ZpmEnergy.stored(extracted)+restored.totalEnergy()==before,"extracting module conserves buffered charge");
        var cached=z.inventory();z.setRemoved();
        h.assertTrue(cached.extractItem(0,1,false).isEmpty()&&cached.insertItem(0,ZpmEnergy.charged(),false).getCount()==1,"cached item handler cannot duplicate or swallow artifacts after removal");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void zpmManufacturingRecipesLoad(GameTestHelper h){
        for(String name:java.util.List.of("basic","advanced")){
            var recipe=h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","zpm/"+name));
            h.assertTrue(recipe.isPresent(),"ZPM "+name+" crafting is registered");
            h.assertTrue(!recipe.orElseThrow().getResultItem(h.getLevel().registryAccess()).isEmpty(),"ZPM recipe resolves its result");
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void zpmPlacementPreservesChargeAndLegacyKinds(GameTestHelper h){
        var module=ZpmEnergy.charged();ZpmEnergy.set(module,123456789);
        var pos=new BlockPos(1,1,1);h.setBlock(pos,GTLasers.ZPM.get());
        var z=(ZpmModuleBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        z.load(module.getTag().getCompound("BlockEntityTag"));
        h.assertTrue(z.energy()==123456789,"placed artifact reads item charge");
        var saved=new ZpmModuleBlockEntity(z.getBlockPos(),z.getBlockState());saved.load(z.saveWithoutMetadata());
        h.assertTrue(saved.energy()==123456789,"placed artifact retains charge on reload");
        h.assertTrue(!((ZpmDischargerBlock)GTLasers.ZPM_DISCHARGER_BASIC.get()).electric()&&((ZpmDischargerBlock)GTLasers.ZPM_DISCHARGER_ADVANCED.get()).electric()&&!((ZpmDischargerBlock)GTLasers.ZPM_DISCHARGER_ELITE.get()).electric(),"legacy IDs map to actual QU and EU variants");
        h.succeed();
    }
}
