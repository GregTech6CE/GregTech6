package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.cover.*;
import com.gregtech.gregtech.content.data.*;
import com.gregtech.gregtech.content.recipe.CanvasRecipes;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.*;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Three finite native cases; actual registered recipe lookup, powered machines and cover storage. */
@GameTestHolder("gregtech_canvas") @PrefixGameTestTemplate(false)
public final class CanvasPortTests {
    private static ItemStack item(String path){var item=GTTechnological.get(path);if(item==null)throw new IllegalStateException("Missing "+path);return new ItemStack(item);}
    private static List<FluidStack> dyes(){return CanvasRules.printingDyes().stream().map(c->GTFluids.stack("Dye_Chemical_"+c,16)).toList();}
    private static Recipe lookup(RecipeMap map,GameTestHelper h,List<ItemStack> items,List<FluidStack> fluids){return map.findRecipe(items,fluids,false,map.mInputItemsCount,map.mOutputItemsCount,h.getLevel(),null,ItemStack.EMPTY);}
    @GameTest(template="test_empty",timeoutTicks=40)
    public static void everyColorHasSourceBathAndExactScanPrintData(GameTestHelper h){
        var white=item("canvas_white");int tested=0;
        for(var variant:CanvasRules.VARIANTS){
            var canvas=item(variant.path());
            h.assertTrue(CanvasData.isCanvas(canvas)&&CoverItems.isCover(canvas),"all sixteen identities are real covers");
            var material=com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(canvas).orElseThrow();h.assertTrue(material.material().getName().equals("Paper")&&material.amount()==com.gregtech.gregtech.api.material.GTValues.U,"canvas retains original one-unit paper composition");
            String field=variant.name().replace(" Canvas","").replace(" ","");
            for(String category:List.of("Water","Flower","Chemical")){
                var dye=GTFluids.stack("Dye_"+category+"_"+field,144);
                var recipe=lookup(MachineRecipeMaps.Bath,h,List.of(variant.dye().equals("white")?new ItemStack(Items.PAPER):white),List.of(dye));
                h.assertTrue(recipe!=null&&recipe.mOutputs[0].is(canvas.getItem())&&recipe.mDuration==16&&recipe.mEUt==0,"registered bath recipe "+variant.path()+" "+category);tested++;
            }
        }
        h.assertTrue(tested==48,"48 source dye routes executed through actual map lookup");
        var log=new ItemStack(Items.OAK_LOG);var root=new net.minecraft.nbt.CompoundTag();var props=new net.minecraft.nbt.CompoundTag();props.putString("axis","x");root.put("BlockStateTag",props);CoverStackData.write(log,root);
        var usb=item("usb1_stick");
        var scanned=lookup(MachineRecipeMaps.ScannerVisuals,h,List.of(usb,log),List.of());
        h.assertTrue(scanned!=null&&scanned.mDuration==512&&scanned.mEUt==16,"ordinary block scan uses original cost");
        var written=scanned.mOutputs[0];var image=CanvasData.read(UsbDataMedia.readStick(written,1));
        h.assertTrue(image.block().equals("minecraft:oak_log")&&CanvasData.state(image).getValue(BlockStateProperties.AXIS)==Direction.Axis.X,"scan saves stable block and property identity");
        h.assertTrue(ItemStack.isSameItemSameTags(log,scanned.mOutputs[1]),"scanned object is returned with its data");
        var printed=lookup(MachineRecipeMaps.Printer,h,List.of(written,white),dyes());
        h.assertTrue(printed!=null&&printed.mDuration==64&&printed.mEUt==16&&printed.mFluidInputs.length==4,"printing needs CMYK at source cost");
        var remaining=RecipeInputs.consume(printed,List.of(written,white),dyes(),1);
        h.assertTrue(remaining!=null&&ItemStack.isSameItemSameTags(written,remaining.items().get(0))&&remaining.items().get(1).isEmpty()&&remaining.fluids().stream().allMatch(FluidStack::isEmpty),"printing consumes exactly 16 mB per dye and retains USB");
        var missing=new ArrayList<>(dyes());missing.set(3,FluidStack.EMPTY);
        h.assertTrue(lookup(MachineRecipeMaps.Printer,h,List.of(written,white),missing)==null,"missing yellow cannot print");
        var canvas=printed.mOutputs[0];h.assertTrue(image.equals(CanvasData.read(canvas)),"printed canvas carries same image");
        h.assertTrue(lookup(MachineRecipeMaps.Printer,h,List.of(written,canvas),dyes())==null,"already printed canvas cannot be overwritten");
        var copy=lookup(MachineRecipeMaps.ScannerVisuals,h,List.of(canvas,item("usb4_stick")),List.of());
        h.assertTrue(copy!=null&&copy.mDuration==64&&image.equals(CanvasData.read(UsbDataMedia.readStick(copy.mOutputs[0],1))),"printed canvas is scanned quickly into a higher-tier stick");
        h.assertTrue(lookup(MachineRecipeMaps.ScannerVisuals,h,List.of(white,usb),List.of())==null,"blank canvas has no image to scan");
        h.assertTrue(lookup(MachineRecipeMaps.ScannerVisuals,h,List.of(new ItemStack(Items.DIAMOND),usb),List.of())==null,"unmapped non-block items cannot invent images");h.succeed();
    }
    private static BasicMachineBlockEntity machine(GameTestHelper h,BlockPos pos,RecipeMap map){
        var block=java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(),false).filter(b->b instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine&&machine.basicSpec().recipeMap()==map).map(b->(com.gregtech.gregtech.block.machine.BasicMachineBlock)b).findFirst().orElseThrow();
        h.setBlock(pos,Blocks.AIR);h.setBlock(pos,block);for(var side:Direction.values())h.setBlock(pos.relative(side),Blocks.STONE);return (BasicMachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    private static void runPowered(GameTestHelper h,BasicMachineBlockEntity machine,int ticks){
        for(int i=0;i<ticks;i++){machine.doEnergyInjection(GregTechTags.Energy.EU,null,machine.spec().energyInMax(),1,true);BasicMachineBlockEntity.serverTick(h.getLevel(),machine.getBlockPos(),machine.getBlockState(),machine);}
    }
    @GameTest(template="test_empty",timeoutTicks=80)
    public static void poweredScannerAndCablePrinterProcessActualInventories(GameTestHelper h){
        var scanner=machine(h,new BlockPos(3,2,3),MachineRecipeMaps.ScannerVisuals);
        scanner.inventory().setStackInSlot(0,new ItemStack(Items.CRAFTING_TABLE));scanner.inventory().setStackInSlot(1,item("usb1_stick"));runPowered(h,scanner,550);
        var written=scanner.inventory().getStackInSlot(MachineRecipeMaps.ScannerVisuals.mInputItemsCount);
        h.assertTrue(UsbDataMedia.readStick(written,1)!=null,"powered registered scanner writes output USB");
        h.assertTrue(CanvasData.read(UsbDataMedia.readStick(written,1)).block().equals("minecraft:crafting_table"),"actual machine output is the scanned block");
        var printer=machine(h,new BlockPos(10,2,3),MachineRecipeMaps.Printer);
        var portPos=printer.getBlockPos().north();h.getLevel().setBlockAndUpdate(portPos,BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","hdd_switch")).defaultBlockState());
        var port=(com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity)h.getLevel().getBlockEntity(portPos);
        var drive=item("usb3_hdd");UsbDataMedia.writeDrive(drive,7,1,UsbDataMedia.readStick(written,1));port.items().setStackInSlot(0,drive);port.setMode(7);
        var cable=item("usb3_cable");var cableData=CoverStackData.readOrEmpty(cable);cableData.putByte(UsbDataCable.NBT_DIRECTION,(byte)Direction.NORTH.ordinal());CoverStackData.write(cable,cableData);
        printer.inventory().setStackInSlot(0,item("canvas_cyan"));printer.inventory().setStackInSlot(1,cable.copy());
        var inks=dyes();for(int i=0;i<4;i++)printer.getTanksInput()[i].setFluid(inks.get(i));runPowered(h,printer,100);
        var canvas=printer.inventory().getStackInSlot(MachineRecipeMaps.Printer.mInputItemsCount);
        h.assertTrue(CanvasData.isCanvas(canvas)&&CanvasData.read(canvas).block().equals("minecraft:crafting_table"),"powered cable printer yields cyan canvas with scanned image");
        h.assertTrue(printer.inventory().getStackInSlot(0).isEmpty()&&ItemStack.isSameItemSameTags(cable,printer.inventory().getStackInSlot(1)),"printer consumes canvas and retains cable");
        h.assertTrue(Arrays.stream(printer.getTanksInput()).allMatch(t->t.getFluid().isEmpty())&&port.readUsbData(Direction.SOUTH,1)!=null,"CMYK consumed exactly, HDD source file retained");h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=40)
    public static void imageCoverSurvivesNativeSaveButCrowbarResetsIt(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(4,2,10));var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","drum_steel"));h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        var owner=h.getLevel().getBlockEntity(pos);var host=(PanelCoverHost)owner;var canvas=item("canvas_red");
        var image=CanvasData.fromState(Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.Z));CanvasData.write(canvas,image);
        h.assertTrue(host.attachCover(Direction.EAST,canvas),"decorative canvas attaches to native container");var saved=owner.saveWithoutMetadata();host.removeCover(Direction.EAST);owner.load(saved);
        h.assertTrue(image.equals(CanvasData.read(host.getCover(Direction.EAST))),"native cover save/load preserves stable image state");
        var returned=host.removeCover(Direction.EAST);h.assertTrue(returned.is(canvas.getItem())&&!CanvasData.hasImage(returned),"crowbar returns default stackable colored canvas");
        var invalid=CanvasRules.image("missingmod:deleted_block",Map.of());h.assertTrue(CanvasData.state(invalid)==null,"removed external block cannot resolve to a random runtime ID");h.succeed();
    }
}
