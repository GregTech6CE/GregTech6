package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.data.*;
import com.gregtech.gregtech.content.book.*;
import com.gregtech.gregtech.content.recipe.GTMaterialDataRecipes;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Three finite source-bound cases, real recipe lookup/binding and actual powered inventories. */
@GameTestHolder("gregtech_documents") @PrefixGameTestTemplate(false)
public final class VisualDocumentPortTests {
    private static ItemStack item(String path){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",path)));}
    private static List<FluidStack> black(int amount){return List.of(GTFluids.stack("Dye_Chemical_Black",amount));}
    private static List<FluidStack> cmyk(){return List.of("Black","Cyan","Magenta","Yellow").stream().map(c->GTFluids.stack("Dye_Chemical_"+c,16)).toList();}
    private static Recipe lookup(RecipeMap map,GameTestHelper h,List<ItemStack> items,List<FluidStack> fluids){return map.findRecipe(items,fluids,false,map.mInputItemsCount,map.mOutputItemsCount,h.getLevel(),null,ItemStack.EMPTY);}
    private static CompoundTag data(int count){
        var tag=new CompoundTag();tag.putString("title","Source document");tag.putString("author","Gregorius Techneticies");tag.putInt("generation",1);tag.putBoolean("resolved",true);
        var pages=new ListTag();for(int i=0;i<count;i++)pages.add(StringTag.valueOf("{\"text\":\"Page "+i+"\"}"));tag.put("pages",pages);
        var filtered=new CompoundTag();filtered.putString("0","{\"text\":\"Filtered page\"}");tag.put("filtered_pages",filtered);tag.putString("filtered_title","Filtered title");return tag;
    }
    private static ItemStack bind(GameTestHelper h,ItemStack pages,boolean many){
        var input=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,-1){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}},3,3);
        input.setItem(0,pages.copy());input.setItem(1,new ItemStack(Items.LEATHER));input.setItem(2,new ItemStack(Items.RED_DYE));
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();var output=recipe.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(output.is(item(many?"large_book_red":"book_red").getItem()),"actual registered leather/red dye binding retains correct size");
        return output;
    }
    @GameTest(template="test_empty",timeoutTicks=50)
    public static void fiftyPageBoundaryBindingAndDictionary(GameTestHelper h){
        for(int count:new int[]{50,51}) {
            var source=data(count);var book=GTColoredBooks.stack(count>50?1004:4);h.assertTrue(VisualDocumentData.applyBook(h.getLevel(),book,source),"native book content accepted");
            var usb=item("usb4_stick");var scan=lookup(MachineRecipeMaps.ScannerVisuals,h,List.of(book,usb),List.of());
            h.assertTrue(scan!=null&&scan.mDuration==512&&scan.mEUt==16&&ItemStack.isSameItemSameTags(book,scan.mOutputs[1]),"source book scan costs and returns exact book");
            var written=scan.mOutputs[0];var stored=UsbDataMedia.readStick(written,1);
            h.assertTrue(VisualDocumentData.pages(stored)==count&&stored.getInt("generation")==1&&stored.getCompound("filtered_pages").getString("0").contains("Filtered page"),"USB retains pages, filtered content and generation");
            int sheets=count>50?6:3,dye=count>50?144:72;var paper=new ItemStack(Items.PAPER,sheets);
            var recipe=lookup(MachineRecipeMaps.Printer,h,List.of(written,paper),black(dye));
            h.assertTrue(recipe!=null&&recipe.mDuration==(count>50?1024:512)&&recipe.mEUt==16&&recipe.mOutputs[0].is(item(count>50?"many_printed_pages":"printed_pages").getItem()),"50/51 threshold outputs original paper bundle with original costs");
            var remaining=RecipeInputs.consume(recipe,List.of(written,paper),black(dye),1);
            h.assertTrue(remaining!=null&&ItemStack.isSameItemSameTags(written,remaining.items().get(0))&&remaining.items().get(1).isEmpty()&&remaining.fluids().get(0).isEmpty(),"paper/dye conserved and exact USB retained");
            h.assertTrue(lookup(MachineRecipeMaps.Printer,h,List.of(written,new ItemStack(Items.PAPER,sheets-1)),black(dye))==null,"insufficient paper rejected");
            h.assertTrue(lookup(MachineRecipeMaps.Printer,h,List.of(written,paper),black(dye-1))==null,"insufficient black dye rejected");
            var bound=bind(h,recipe.mOutputs[0],count>50);var payload=VisualDocumentData.bookData(h.getLevel(),bound);
            h.assertTrue(payload.getList("pages",8).equals(stored.getList("pages",8))&&payload.getInt("generation")==1&&payload.getString("filtered_title").equals("Filtered title"),"printed bundle binds without changing content or generation");
            var again=lookup(MachineRecipeMaps.ScannerVisuals,h,List.of(recipe.mOutputs[0],item("usb1_stick")),List.of());h.assertTrue(again!=null&&again.mDuration==512,"printed bundles are also scannable");
        }
        var material=com.gregtech.gregtech.content.material.Materials.Iron;var usb=GTMaterialDataRecipes.withMaterialData(item("usb3_stick"),material);
        var dictionary=lookup(MachineRecipeMaps.Printer,h,List.of(usb,new ItemStack(Items.PAPER,3)),black(72));
        h.assertTrue(dictionary!=null&&dictionary.mDuration==512&&dictionary.mEUt==16&&VisualDocumentData.isPages(dictionary.mOutputs[0]),"material dictionary uses source paper output, duration and power");
        h.assertTrue(GTMaterialDictionary.materialOf(bind(h,dictionary.mOutputs[0],false))==material,"bound dictionary retains material mapping");
        var mapping=new CompoundTag();mapping.putString("title","Mapped manual");mapping.putString("author","Gregorius");mapping.putString("book","Manual_Printer");UsbDataMedia.writeStick(usb,1,mapping);
        var mapped=lookup(MachineRecipeMaps.Printer,h,List.of(usb,new ItemStack(Items.PAPER,6)),black(144));h.assertTrue(mapped!=null&&VisualDocumentData.pages(VisualDocumentData.bookData(h.getLevel(),mapped.mOutputs[0]))==GTBooks.pagesOf("Manual_Printer").size(),"catalog-only book resolves actual manual pages");
        var lines=new ArrayList<net.minecraft.network.chat.Component>();com.gregtech.gregtech.item.behavior.BehaviorDataStorage.dataTooltip(mapping,lines,true);h.assertTrue(lines.size()==2&&lines.get(0).getString().equals("Book: Mapped manual"),"USB title and author source tooltip");lines.clear();com.gregtech.gregtech.item.behavior.BehaviorDataStorage.dataTooltip(mapping,lines,false);h.assertTrue(lines.size()==1,"HDD file tooltip omits verbose author");h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=50)
    public static void nativeMapsUseSameSavedDataAndUntruncatedIds(GameTestHelper h){
        var map=MapItem.create(h.getLevel(),12,34,(byte)2,true,false);var id=VisualDocumentData.mapId(map);var original=MapItem.getSavedData(map,h.getLevel());
        h.assertTrue(id>=0&&original!=null,"native map has actual saved-world map data");original.colors[0]=(byte)7;
        var scan=lookup(MachineRecipeMaps.ScannerVisuals,h,List.of(item("usb1_stick"),map),List.of());h.assertTrue(scan!=null&&scan.mDuration==64&&scan.mEUt==16,"native map scan source costs");
        var usb=scan.mOutputs[0];var empty=new ItemStack(Items.MAP);var printed=lookup(MachineRecipeMaps.Printer,h,List.of(empty,usb),cmyk());
        h.assertTrue(printed!=null&&printed.mDuration==64&&printed.mEUt==16&&VisualDocumentData.mapId(printed.mOutputs[0])==id,"printed map retains exact native map ID");
        h.assertTrue(MapItem.getSavedData(printed.mOutputs[0],h.getLevel())==original&&original.colors[0]==7,"copy references original explored terrain instead of new empty map");
        var remaining=RecipeInputs.consume(printed,List.of(empty,usb),cmyk(),1);h.assertTrue(remaining!=null&&remaining.items().get(0).isEmpty()&&ItemStack.isSameItemSameTags(usb,remaining.items().get(1))&&remaining.fluids().stream().allMatch(FluidStack::isEmpty),"four exact CMYK amounts and USB retention");
        UsbDataMedia.writeStick(usb,1,VisualDocumentData.mapData(70000));printed=lookup(MachineRecipeMaps.Printer,h,List.of(empty,usb),cmyk());h.assertTrue(printed!=null&&VisualDocumentData.mapId(printed.mOutputs[0])==70000,"modern IDs never wrap at short range");
        var malformed=new CompoundTag();malformed.putInt("map_id",-1);UsbDataMedia.writeStick(usb,1,malformed);h.assertTrue(lookup(MachineRecipeMaps.Printer,h,List.of(empty,usb),cmyk())==null,"invalid map ID rejected");
        var missing=new ArrayList<>(cmyk());missing.set(2,FluidStack.EMPTY);UsbDataMedia.writeStick(usb,1,VisualDocumentData.mapData(id));h.assertTrue(lookup(MachineRecipeMaps.Printer,h,List.of(empty,usb),missing)==null,"missing magenta rejected");
        h.assertTrue(lookup(MachineRecipeMaps.ScannerVisuals,h,List.of(new ItemStack(Items.WRITABLE_BOOK),item("usb1_stick")),List.of())==null,"unsigned writable book has no scannable title");h.succeed();
    }
    private static BasicMachineBlockEntity machine(GameTestHelper h,BlockPos pos,RecipeMap map){
        var block=java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(),false).filter(b->b instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine&&machine.basicSpec().recipeMap()==map).map(b->(com.gregtech.gregtech.block.machine.BasicMachineBlock)b).findFirst().orElseThrow();
        h.setBlock(pos,Blocks.AIR);h.setBlock(pos,block);for(var side:Direction.values())h.setBlock(pos.relative(side),Blocks.STONE);return (BasicMachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    private static void power(GameTestHelper h,BasicMachineBlockEntity machine,int ticks){for(int i=0;i<ticks;i++){machine.doEnergyInjection(GregTechTags.Energy.EU,null,machine.spec().energyInMax(),1,true);BasicMachineBlockEntity.serverTick(h.getLevel(),machine.getBlockPos(),machine.getBlockState(),machine);}}
    @GameTest(template="test_empty",timeoutTicks=80)
    public static void poweredScannerAndHddPrinterProduceBoundBook(GameTestHelper h){
        var book=GTBooks.bookStack("Manual_Printer");var scanner=machine(h,new BlockPos(3,2,3),MachineRecipeMaps.ScannerVisuals);scanner.inventory().setStackInSlot(0,book);scanner.inventory().setStackInSlot(1,item("usb1_stick"));power(h,scanner,550);
        var usb=scanner.inventory().getStackInSlot(MachineRecipeMaps.ScannerVisuals.mInputItemsCount);var data=UsbDataMedia.readStick(usb,1);h.assertTrue(data!=null&&VisualDocumentData.pages(data)==GTBooks.pagesOf("Manual_Printer").size(),"powered registered scanner reads actual manual");
        var printer=machine(h,new BlockPos(10,2,3),MachineRecipeMaps.Printer);var portPos=printer.getBlockPos().north();h.getLevel().setBlockAndUpdate(portPos,BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","hdd_switch")).defaultBlockState());
        var port=(com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity)h.getLevel().getBlockEntity(portPos);var drive=item("usb3_hdd");UsbDataMedia.writeDrive(drive,9,1,data);port.items().setStackInSlot(0,drive);port.setMode(9);
        var cable=item("usb3_cable");var cableData=com.gregtech.gregtech.content.cover.CoverStackData.readOrEmpty(cable);cableData.putByte(UsbDataCable.NBT_DIRECTION,(byte)Direction.NORTH.ordinal());com.gregtech.gregtech.content.cover.CoverStackData.write(cable,cableData);
        printer.inventory().setStackInSlot(0,new ItemStack(Items.PAPER,3));printer.inventory().setStackInSlot(1,cable.copy());printer.getTanksInput()[0].setFluid(black(72).get(0));power(h,printer,550);
        var pages=printer.inventory().getStackInSlot(MachineRecipeMaps.Printer.mInputItemsCount);h.assertTrue(VisualDocumentData.isPages(pages)&&ItemStack.isSameItemSameTags(cable,printer.inventory().getStackInSlot(1)),"actual cable printer produces paper pages and retains cable");
        h.assertTrue(printer.inventory().getStackInSlot(0).isEmpty()&&printer.getTanksInput()[0].getFluid().isEmpty()&&port.readUsbData(Direction.SOUTH,1)!=null,"exact input amounts consumed, selected source file retained");
        var bound=bind(h,pages,false);h.assertTrue(VisualDocumentData.pages(VisualDocumentData.bookData(h.getLevel(),bound))==GTBooks.pagesOf("Manual_Printer").size(),"powered scanner/HDD/printer chain gives complete bound manual");h.succeed();
    }
}
