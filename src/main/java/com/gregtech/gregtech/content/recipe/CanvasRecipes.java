/* GregTech-6 Team visual scan/print and canvas bath recipes, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.cover.*;
import com.gregtech.gregtech.content.data.*;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

public final class CanvasRecipes {
    private CanvasRecipes() {}
    private static boolean registered;
    public static synchronized void register() {
        if (registered) return; registered=true;
        var usb=new ItemStack(GTTechnological.get("usb1_stick"));
        var scannedUsb=usb.copy();
        scannedUsb.setHoverName(net.minecraft.network.chat.Component.literal("Containing scanned Block"));
        var white=new ItemStack(GTTechnological.get("canvas_white"));
        for(var variant:CanvasRules.VARIANTS) {
            var canvas=new ItemStack(GTTechnological.get(variant.path()));
            String color=variant.dye().replace("_", "");
            String suffix=Character.toUpperCase(color.charAt(0))+color.substring(1);
            // Fluid field suffixes retain the original spelling LightGray / LightBlue.
            if(variant.dye().equals("light_gray"))suffix="LightGray";
            if(variant.dye().equals("light_blue"))suffix="LightBlue";
            for(String category:List.of("Water","Flower","Chemical")) bath(variant.originalId()==7045?new ItemStack(Items.PAPER):white,canvas,"Dye_"+category+"_"+suffix,144);
            if(variant.dye().equals("black"))bath(white,canvas,"InkSquid",144);
            if(variant.dye().equals("blue")) { bath(white,canvas,"Indigo",144);bath(white,canvas,"Juice_Blueberry",250); }
            MachineRecipeMaps.ScannerVisuals.addFakeRecipe(false,new ItemStack[]{canvas,usb.copy()},new ItemStack[]{scannedUsb.copy(),canvas.copy()},null,null,null,64,16,0);
            var row=MachineRecipeMaps.Printer.addFakeRecipe(false,new ItemStack[]{canvas,scannedUsb.copy()},new ItemStack[]{canvas.copy()},null,dyes(),null,64,16,0);
            if(row!=null)row.withCatalystInputs(1);
        }
    }
    private static void bath(ItemStack input,ItemStack output,String field,int amount) {
        var dye=GTFluids.stack(field,amount); if(dye==null||dye.isEmpty())return;
        MachineRecipeMaps.Bath.addRecipe(false,new ItemStack[]{input.copy()},new ItemStack[]{output.copy()},null,null,new FluidStack[]{dye},null,16,0,0);
    }
    private static FluidStack[] dyes() {
        var dyes=new ArrayList<FluidStack>();
        for(String color:CanvasRules.printingDyes()){var dye=GTFluids.stack("Dye_Chemical_"+color,CanvasRules.PRINT_DYE_AMOUNT);if(dye==null||dye.isEmpty())return null;dyes.add(dye);}
        return dyes.toArray(FluidStack[]::new);
    }
    public static Recipe scan(Level level,BlockEntity machine,ItemStack special,List<ItemStack> items,List<FluidStack> fluids) {
        if(level.isClientSide)return null;
        ItemStack usb=null,object=null;
        for(var stack:items)if(!stack.isEmpty()) {
            if(usb==null&&UsbDataMedia.stickTier(stack)>=1)usb=stack;
            else if(object==null)object=stack;
        }
        if(usb==null||object==null)return null;
        var image=CanvasData.scanned(object);if(image==null||CanvasData.state(image)==null)return null;
        var written=usb.copyWithCount(1);UsbDataMedia.writeStick(written,1,CanvasData.write(image));
        return exact(new Recipe(new ItemStack[]{object.copyWithCount(1),usb.copyWithCount(1)},new ItemStack[]{written,object.copyWithCount(1)},null,null,null,null,CanvasData.isCanvas(object)?CanvasRules.CANVAS_SCAN_TICKS:CanvasRules.BLOCK_SCAN_TICKS,CanvasRules.POWER,0));
    }
    public static Recipe print(Level level,BlockEntity machine,ItemStack special,List<ItemStack> items,List<FluidStack> fluids) {
        if(level.isClientSide)return null;
        ItemStack medium=null,canvas=null;
        for(var stack:items)if(!stack.isEmpty()) {
            if(medium==null&&(UsbDataMedia.stickTier(stack)>=1||UsbDataMedia.cableTier(stack)>=1))medium=stack;
            else if(canvas==null&&CanvasData.isCanvas(stack))canvas=stack;
        }
        if(medium==null||canvas==null||CanvasData.hasImage(canvas))return null;
        var data=UsbDataMedia.cableTier(medium)>=1?UsbDataCable.readAdjacent(machine,medium,1):UsbDataMedia.readStick(medium,1);
        var image=CanvasData.read(data);if(CanvasData.state(image)==null)return null;
        var dyes=dyes();if(dyes==null)return null;
        var printed=canvas.copyWithCount(1);CanvasData.write(printed,image);
        return exact(new Recipe(new ItemStack[]{canvas.copyWithCount(1),medium.copyWithCount(1)},new ItemStack[]{printed},null,null,dyes,null,CanvasRules.PRINT_TICKS,CanvasRules.POWER,0).withCatalystInputs(1));
    }
    private static Recipe exact(Recipe recipe) {recipe.mExactItemInputs=true;recipe.mExplicitCatalystsOnly=true;recipe.mCanBeBuffered=false;return recipe;}
}
