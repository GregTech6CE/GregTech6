package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.energy.item.IItemEnergy;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.misc.*;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity;
import com.gregtech.gregtech.client.gui.AdvancedCraftingMenu;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class ChargingCraftingRepairTests {
    private static ChargingCraftingTableBlockEntity place(GameTestHelper h,BlockPos pos){h.setBlock(pos,GTMiscBlocks.CHARGING_CRAFTING_TABLE.get());return (ChargingCraftingTableBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));}
    private static void batteries(ChargingCraftingTableBlockEntity table){for(int i=16;i<=20;i++)table.items().setStackInSlot(i,new ItemStack(GTElectricItems.BATTERY_LV.get()));}
    private static long charge(ItemStack stack){return stack.getItem() instanceof IItemEnergy energy?energy.getEnergyStored(stack,GregTechTags.Energy.EU):0;}
    private static ItemStack tool(GTToolType type){return com.gregtech.gregtech.item.GTToolItem.create(type,Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));}
    private static ElectricWireBlock cable(){return GTWires.allCables().stream().map(net.minecraftforge.registries.RegistryObject::get).filter(b->b.spec().material()==Materials.Copper&&b.spec().size()==12).findFirst().orElseThrow();}
    private static ElectricWireBlockEntity wire(GameTestHelper h,BlockPos pos,Direction... faces){var block=cable();var state=block.defaultBlockState();for(var face:faces)state=state.setValue(ElectricWireBlock.propFor(face),true);h.setBlock(pos,state);return (ElectricWireBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));}
    @GameTest(template="test_empty")
    public static void chargesOnePacketPerToolSlotAndNeverInventsBlockStorage(GameTestHelper h){
        var table=place(h,new BlockPos(1,1,1));batteries(table);var before=table.saveWithoutMetadata();
        h.assertTrue(table.getEnergyDemanded(GregTechTags.Energy.EU,null,32)==5&&table.doEnergyInjection(GregTechTags.Energy.EU,Direction.DOWN,32,2,false)==2,"five slots demand five packets; two-packet simulation");
        h.assertTrue(before.equals(table.saveWithoutMetadata()),"simulation neither changes charge nor creates NBT");
        h.assertTrue(table.doEnergyInjection(GregTechTags.Energy.EU,Direction.EAST,32,2,true)==2,"actual two packets accepted");
        for(int i=16;i<=20;i++)h.assertTrue(charge(table.items().getStackInSlot(i))==(i<18?32:0),"original slot-order charging "+i);
        h.assertTrue(table.doEnergyInjection(GregTechTags.Energy.EU,Direction.UP,32,1000,true)==5,"at most five packets per call");
        for(int i=16;i<=20;i++)h.assertTrue(charge(table.items().getStackInSlot(i))==(i<18?64:32),"one further packet per slot "+i);
        h.assertTrue(table.getEnergyStored(GregTechTags.Energy.EU,null)==0&&table.getEnergyCapacity(GregTechTags.Energy.EU,null)==0&&table.doEnergyExtraction(GregTechTags.Energy.EU,null,32,5,true)==0,"no hidden buffer or output");
        h.assertTrue(table.doEnergyInjection(GregTechTags.Energy.HU,null,32,5,true)==0&&table.doEnergyInjection(GregTechTags.Energy.EU,null,0,5,true)==0&&table.doEnergyInjection(GregTechTags.Energy.EU,null,Long.MIN_VALUE,5,true)==0,"items reject HU; invalid packet sizes rejected");
        h.assertTrue(table.doEnergyInjection(GregTechTags.Energy.EU,null,Long.MAX_VALUE,Long.MAX_VALUE,true)==0,"huge packets cannot overflow battery capacity");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void skipsFullItemsAndSynchronizesChargeToOpenCraftingMenu(GameTestHelper h){
        var table=place(h,new BlockPos(1,1,1));var battery=new ItemStack(GTElectricItems.BATTERY_LV.get());battery.getOrCreateTag().putLong("gt.charge",100000);table.items().setStackInSlot(16,battery);
        table.items().setStackInSlot(17,new ItemStack(Items.STICK));table.items().setStackInSlot(18,new ItemStack(GTElectricItems.ELECTRIC_DRILL.get()));
        table.items().setStackInSlot(0,new ItemStack(GTElectricItems.BATTERY_LV.get()));table.items().setStackInSlot(70,new ItemStack(GTElectricItems.BATTERY_LV.get()));
        var player=h.makeMockSurvivalPlayer();var menu=new AdvancedCraftingMenu(1,player.getInventory(),table);long[] synced={-1};
        menu.addSlotListener(new ContainerListener(){public void slotChanged(AbstractContainerMenu m,int slot,ItemStack stack){if(slot==19)synced[0]=charge(stack);}public void dataChanged(AbstractContainerMenu m,int id,int value){}});
        h.assertTrue((menu.modes()&32)!=0,"same menu carries charging GUI selector");
        h.assertTrue(table.doEnergyInjection(GregTechTags.Energy.EU,null,128,5,true)==0,"LV tool refuses MV charging packet");
        h.assertTrue(table.doEnergyInjection(GregTechTags.Energy.EU,null,32,5,true)==1,"full battery and non-energy item skipped, drill accepts one");menu.broadcastChanges();
        h.assertTrue(synced[0]==32&&charge(table.items().getStackInSlot(18))==32,"open GUI gets changed tool energy NBT");
        h.assertTrue(charge(table.items().getStackInSlot(0))==0&&charge(table.items().getStackInSlot(70))==0,"storage batteries never charge");
        table.load(table.saveWithoutMetadata());h.assertTrue(charge(table.items().getStackInSlot(18))==32,"charge survives save/load");
        var flux=table.getCapability(ForgeCapabilities.ENERGY,Direction.NORTH);var handler=flux.orElseThrow(IllegalStateException::new);
        h.assertTrue(handler.canReceive()&&!handler.canExtract()&&handler.receiveEnergy(500,false)==0&&handler.getEnergyStored()==0,"Forge RF is not silently converted to EU");
        table.invalidateCaps();h.assertTrue(!flux.isPresent()&&!table.getCapability(ForgeCapabilities.ENERGY,null).isPresent()&&handler.receiveEnergy(100,false)==0,"invalidated energy interface cannot resurrect or charge");
        table.reviveCaps();h.assertTrue(table.getCapability(ForgeCapabilities.ENERGY,null).isPresent()&&table.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"both inherited inventory and energy revive");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void realThreeCableRouteChargesAndRejectsDisconnectedInputs(GameTestHelper h){
        var table=place(h,new BlockPos(4,1,1));batteries(table);
        var a=wire(h,new BlockPos(1,1,1),Direction.WEST,Direction.EAST);wire(h,new BlockPos(2,1,1),Direction.WEST,Direction.EAST);wire(h,new BlockPos(3,1,1),Direction.WEST,Direction.EAST);
        var before=table.saveWithoutMetadata();long oldWattage=a.lastWattage();
        h.assertTrue(a.doEnergyInjection(GregTechTags.Energy.HU,Direction.WEST,32,2,true)==0&&a.doEnergyInjection(GregTechTags.Energy.EU,Direction.UP,32,2,true)==0&&a.doEnergyInjection(GregTechTags.Energy.EU,null,32,2,true)==0,"wrong type, unconnected and unknown input faces reject");
        h.assertTrue(a.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,2,false)==2&&before.equals(table.saveWithoutMetadata())&&a.lastWattage()==oldWattage,"GT6 simulation reports local acceptance and changes no wire or receiver state");
        h.assertTrue(a.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,2,true)==2,"three consecutive segments deliver packets");
        long expected=32-3*cable().spec().lossPerBlock();h.assertTrue(expected>0&&charge(table.items().getStackInSlot(16))==expected&&charge(table.items().getStackInSlot(17))==expected,"loss applied exactly once per cable");
        var middle=h.absolutePos(new BlockPos(2,1,1));var state=h.getLevel().getBlockState(middle);h.getLevel().setBlock(middle,state.setValue(ElectricWireBlock.WEST,false),3);
        h.assertTrue(a.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,1,true)==0,"downstream input must also be connected");h.getLevel().setBlock(middle,state,3);
        h.getLevel().setBlock(middle,state.setValue(ElectricWireBlock.EAST,false),3);h.assertTrue(a.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,1,true)==0,"broken downstream output also stops transfer");
        h.assertTrue(charge(table.items().getStackInSlot(16))==expected,"broken line cannot leak another packet");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void cableCycleAndBranchConserveAcceptedPackets(GameTestHelper h){
        var first=place(h,new BlockPos(3,1,1));var second=place(h,new BlockPos(1,1,3));batteries(first);batteries(second);
        var root=wire(h,new BlockPos(1,1,1),Direction.WEST,Direction.EAST,Direction.SOUTH);
        wire(h,new BlockPos(2,1,1),Direction.WEST,Direction.SOUTH,Direction.EAST);wire(h,new BlockPos(2,1,2),Direction.NORTH,Direction.WEST);wire(h,new BlockPos(1,1,2),Direction.NORTH,Direction.EAST,Direction.SOUTH);
        h.assertTrue(root.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,8,true)==8,"cycle terminates and two branches accept eight packets total");
        int charged=0;long total=0;for(var table:new ChargingCraftingTableBlockEntity[]{first,second})for(int i=16;i<=20;i++){long amount=charge(table.items().getStackInSlot(i));if(amount>0)charged++;total+=amount;h.assertTrue(amount<32,"lossy route never amplifies packet voltage");}
        h.assertTrue(charged==8&&total>0&&total<256,"each accepted packet charged one tool once, including cycle");
        for(var table:new ChargingCraftingTableBlockEntity[]{first,second})for(int i=16;i<=20;i++){var stack=table.items().getStackInSlot(i).copy();stack.getOrCreateTag().putLong("gt.charge",100000);table.items().setStackInSlot(i,stack);}
        h.assertTrue(root.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,8,true)==0,"saturated receivers report no consumption");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void allChargingMaterialsHaveOriginalUpgradeRecipeAndDropChargedItems(GameTestHelper h){
        var table=place(h,new BlockPos(1,1,1));var player=h.makeMockSurvivalPlayer();var pos=table.getBlockPos();
        var gold=GTWires.allCables().stream().map(net.minecraftforge.registries.RegistryObject::get).filter(b->b.spec().material()==Materials.Gold&&b.spec().size()==4).findFirst().orElseThrow();
        h.assertTrue(GTMiscBlocks.CHARGING_CRAFTING_TABLES.size()==60,"all 60 original charging table materials");
        for(var spec:GTStorageMetals.ALL){var block=GTMiscBlocks.chargingCraftingTable(spec.material());
            var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(GregTech.id("hand/storage/charging_crafting_table_"+spec.suffix())).orElseThrow();
            var grid=new TransientCraftingContainer(new CraftingMenu(1,player.getInventory()),3,3);
            for(int slot:new int[]{0,2,6,8})grid.setItem(slot,new ItemStack(gold));for(int slot:new int[]{1,7})grid.setItem(slot,GTItems.getStack(MaterialPrefix.screw,spec.material(),1));
            grid.setItem(3,tool(GTToolType.SCREWDRIVER));grid.setItem(4,new ItemStack(GTMiscBlocks.advancedCraftingTable(spec.material())));grid.setItem(5,tool(GTToolType.WIRE_CUTTER));
            h.assertTrue(recipe.matches(grid,h.getLevel())&&recipe.getResultItem(h.getLevel().registryAccess()).is(block.asItem())&&block.asItem().getDefaultInstance().getMaxStackSize()==16,"same-material table and original 4x gold cables: "+spec.suffix());
            h.assertTrue(GTBlockEntities.CHARGING_CRAFTING_TABLE.get().isValid(block.defaultBlockState()),"correct entity type for every charging variant");
        }
        table.items().setStackInSlot(16,new ItemStack(GTElectricItems.BATTERY_LV.get()));
        h.assertTrue(table.doEnergyInjection(GregTechTags.Energy.EU,null,512,1,true)==0,"LV battery rejects HV voltage before drop test");
        h.assertTrue(table.doEnergyInjection(GregTechTags.Energy.EU,null,32,1,true)==1,"LV battery charges at valid voltage before drop test");
        var menu=table.storageMenu(1,player.getInventory());menu.getSlot(35).set(new ItemStack(Items.DIAMOND,7));
        h.assertTrue(table.items().getStackInSlot(70).getCount()==7&&table.getType()==GTBlockEntities.CHARGING_CRAFTING_TABLE.get(),"charging inherits rear inventory with correct entity id");
        h.getLevel().destroyBlock(pos,true);table.dropContents();h.runAfterDelay(1,()->{
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));int batteries=0,diamonds=0;for(var e:drops){if(e.getItem().is(GTElectricItems.BATTERY_LV.get())){batteries+=e.getItem().getCount();h.assertTrue(charge(e.getItem())==32,"dropped battery retains charge NBT");}if(e.getItem().is(Items.DIAMOND))diamonds+=e.getItem().getCount();}
            h.assertTrue(batteries==1&&diamonds==7,"charged item and normal inventory drop exactly once");h.succeed();});
    }
}
