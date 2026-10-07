package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.data.*;
import com.gregtech.gregtech.content.recipe.GTMaterialDataRecipes;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.data.generated.MaterialDataFacts;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

@GameTestHolder("gregtech_material_data") @PrefixGameTestTemplate(false)
public final class MolecularDataPortTests {
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",path))); }
    private static GTMaterial material(int id) { return GTMaterialRegistry.get(id).resolve(); }
    private static ItemStack usb(int id) { return GTMaterialDataRecipes.withMaterialData(item("usb3_stick"),material(id)); }
    private static List<FluidStack> matter(int id) {
        var material=material(id);var fluids=new ArrayList<FluidStack>();
        if(material.getNeutrons()>0)fluids.add(GTFluids.stack("MatterNeutral",(int)material.getNeutrons()));
        if(material.getProtons()>0)fluids.add(GTFluids.stack("MatterCharged",(int)material.getProtons()));
        return fluids;
    }
    private static Recipe lookup(GameTestHelper h,RecipeMap map,List<ItemStack> items,List<FluidStack> fluids) {
        return map.findRecipe(items,fluids,false,3,3,h.getLevel(),null,ItemStack.EMPTY);
    }
    @GameTest(template="test_empty",timeoutTicks=60)
    public static void sourceFlagsScannerAndRetainedMedia(GameTestHelper h) {
        h.assertTrue(material(260).has(MaterialProperty.UUM)&&material(8630).has(MaterialProperty.UUM),"source element/alloy UUM flags loaded");
        h.assertTrue(!material(8214).has(MaterialProperty.UUM),"obsidian setMcfg is not uumMcfg");
        var dust=GTItems.getStack(MaterialPrefix.dust,material(260),1);var stick=item("usb3_stick");
        var scan=lookup(h,MachineRecipeMaps.ScannerMolecular,List.of(dust,stick),List.of());
        h.assertTrue(scan!=null&&!scan.mCanBeBuffered&&scan.mEUt==512&&scan.mDuration==(material(260).getProtons()+material(260).getNeutrons())*512L,"source scan cost and no buffering");
        var consumed=RecipeInputs.consume(scan,List.of(dust,stick),List.of(),1);
        h.assertTrue(consumed!=null&&consumed.items().stream().allMatch(ItemStack::isEmpty)&&GTMaterialDataRecipes.scannedMaterial(scan.mOutputs[0]).resolve()==material(260),"scanned object and blank USB consumed, file rebuilt");
        h.assertTrue(lookup(h,MachineRecipeMaps.ScannerMolecular,List.of(new ItemStack(Items.IRON_AXE),stick),List.of())==null,"finished tool composition is not a SCANNABLE prefix");
        var scrap=GTItems.getStack(MaterialPrefix.scrapGt,material(260),1);
        h.assertTrue(!scrap.isEmpty()&&!MaterialDataFacts.scannable(MaterialPrefix.scrapGt)&&lookup(h,MachineRecipeMaps.ScannerMolecular,List.of(scrap,stick),List.of())==null,"non-scannable material form rejected");
        h.assertTrue(lookup(h,MachineRecipeMaps.Replicator,List.of(usb(8214)),matter(8214))==null,"non-UUM material cannot replicate");
        var recipe=lookup(h,MachineRecipeMaps.Replicator,List.of(usb(260)),matter(260));
        h.assertTrue(recipe!=null&&recipe.mEUt==1&&recipe.mDuration==(material(260).getProtons()+material(260).getNeutrons())*256L&&!recipe.mCanBeBuffered,"replicator duration/power source fields");
        consumed=RecipeInputs.consume(recipe,List.of(usb(260)),matter(260),1);
        h.assertTrue(consumed!=null&&same(consumed.items().get(0),usb(260))&&consumed.fluids().stream().allMatch(FluidStack::isEmpty),"matter consumed exactly, tagged USB retained");
        var shortMatter=new ArrayList<>(matter(260));shortMatter.get(0).setAmount(shortMatter.get(0).getAmount()-1);
        h.assertTrue(lookup(h,MachineRecipeMaps.Replicator,List.of(usb(260)),shortMatter)==null,"insufficient neutral matter rejected");
        var lines=new ArrayList<net.minecraft.network.chat.Component>();
        com.gregtech.gregtech.item.behavior.BehaviorDataStorage.dataTooltip(UsbDataMedia.readStick(usb(260),3),lines,true);
        h.assertTrue(lines.size()==5&&lines.get(4).getString().contains(Long.toString((material(260).getProtons()+material(260).getNeutrons())*65536L))&&lines.get(4).getString().endsWith(" QU"),"original tooltip estimate and unit");
        lines.clear();com.gregtech.gregtech.item.behavior.BehaviorDataStorage.dataTooltip(UsbDataMedia.readStick(usb(8214),3),lines,false);
        h.assertTrue(lines.size()==1&&lines.get(0).getString().contains("Not Replicatable"),"non-UUM data keeps original refusal hint");h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=60)
    public static void priorityPrefixesAndAmbientFluids(GameTestHelper h) {
        for(int id:new int[]{140,160,8341}) {
            var recipe=lookup(h,MachineRecipeMaps.Replicator,List.of(usb(id)),matter(id));var expected=MaterialDataFacts.priority(material(id));
            h.assertTrue(recipe!=null&&recipe.mOutputs.length==1&&com.gregtech.gregtech.item.MaterialItem.getPrefix(recipe.mOutputs[0])==expected&&recipe.mOutputs[0].getCount()==1,"source priority output for "+material(id).getName());
        }
        for(int id:new int[]{10,800,9800}) {
            var recipe=lookup(h,MachineRecipeMaps.Replicator,List.of(usb(id)),matter(id));
            h.assertTrue(recipe!=null&&recipe.mOutputs.length==0&&recipe.mFluidOutputs.length==1&&recipe.mFluidOutputs[0].getAmount()==1000,"ambient phase yields one fluid unit for "+material(id).getName());
            var expected=com.gregtech.gregtech.loaders.c.GTGeneratedChem.materialFluid(material(id).getName(),1000);
            h.assertTrue(recipe.mFluidOutputs[0].isFluidEqual(expected),"fluid preserves registered material identity");
        }
        h.assertTrue(MaterialDataFacts.scannable(MaterialPrefix.toolHeadSword),"source tool-head prefix stays scannable");h.succeed();
    }
    private static BasicMachineBlockEntity machine(GameTestHelper h,BlockPos pos) {
        var block=java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(),false)
            .filter(b->b instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock m&&m.basicSpec().recipeMap()==MachineRecipeMaps.Replicator&&m.basicSpec().tier()==1)
            .findFirst().orElseThrow();
        h.setBlock(pos,block);for(var side:Direction.values())h.setBlock(pos.relative(side),Blocks.STONE);
        return (BasicMachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    @GameTest(template="test_empty",timeoutTicks=80)
    public static void lowTierQuantumMachineAndSelectedHddFile(GameTestHelper h) {
        var machine=machine(h,new BlockPos(8,2,3));var portPos=machine.getBlockPos().north();
        h.getLevel().setBlockAndUpdate(portPos,BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","hdd_switch")).defaultBlockState());
        var port=(com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity)h.getLevel().getBlockEntity(portPos);
        var drive=item("usb3_hdd");UsbDataMedia.writeDrive(drive,9,3,UsbDataMedia.readStick(usb(260),3));port.items().setStackInSlot(0,drive);port.setMode(9);
        var cable=item("usb3_cable");var direction=com.gregtech.gregtech.content.cover.CoverStackData.readOrEmpty(cable);direction.putByte(UsbDataCable.NBT_DIRECTION,(byte)Direction.NORTH.ordinal());com.gregtech.gregtech.content.cover.CoverStackData.write(cable,direction);
        machine.inventory().setStackInSlot(0,cable.copy());var fluids=matter(260);for(int i=0;i<fluids.size();i++)machine.getTanksInput()[i].setFluid(fluids.get(i));
        for(int i=0;i<2000;i++) { machine.doEnergyInjection(machine.spec().energyTag(),null,machine.spec().energyInMax(),1,true);BasicMachineBlockEntity.serverTick(h.getLevel(),machine.getBlockPos(),machine.getBlockState(),machine); }
        var output=machine.inventory().getStackInSlot(MachineRecipeMaps.Replicator.mInputItemsCount);
        h.assertTrue(!output.isEmpty()&&output.getCount()==1&&com.gregtech.gregtech.item.MaterialItem.getMaterial(output).resolve()==material(260),"actual T1 16..64 QU input completes iron replication");
        h.assertTrue(same(machine.inventory().getStackInSlot(0),cable)&&port.readUsbData(Direction.SOUTH,3)!=null&&Arrays.stream(machine.getTanksInput()).allMatch(t->t.getFluid().isEmpty()),"actual machine retains selected cable/HDD file and consumes exact matter");h.succeed();
    }
    private static boolean same(ItemStack a,ItemStack b) { return ItemStack.isSameItemSameComponents(a,b); }
}
