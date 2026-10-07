/* GregTech-6 Team (2024) visual book/map recipes, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.data.*;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

public final class VisualDocumentRecipes {
    private VisualDocumentRecipes() {}
    public static Recipe scan(Level level,BlockEntity machine,ItemStack special,List<ItemStack> items,List<FluidStack> fluids) {
        if(level.isClientSide)return null;
        ItemStack usb=null,object=null;
        for(var stack:items)if(!stack.isEmpty()) {
            if(usb==null&&UsbDataMedia.stickTier(stack)>=1)usb=stack;
            else if(object==null)object=stack;
        }
        if(usb==null||object==null)return null;
        CompoundTag data=VisualDocumentData.bookData(level,object);
        int ticks=VisualDocumentRules.BOOK_SCAN_TICKS;
        if(data==null){data=VisualDocumentData.mapData(VisualDocumentData.mapId(object));ticks=VisualDocumentRules.MAP_TICKS;}
        if(data==null)return null;
        var written=usb.copyWithCount(1);UsbDataMedia.writeStick(written,1,data);
        return exact(new Recipe(new ItemStack[]{object.copyWithCount(1),usb.copyWithCount(1)},new ItemStack[]{written,object.copyWithCount(1)},null,null,null,null,ticks,VisualDocumentRules.POWER,0));
    }
    public static Recipe print(Level level,BlockEntity machine,ItemStack special,List<ItemStack> items,List<FluidStack> fluids) {
        if(level.isClientSide)return null;
        ItemStack medium=null,paper=null;
        for(var stack:items)if(!stack.isEmpty()) {
            if(medium==null&&(UsbDataMedia.stickTier(stack)>=1||UsbDataMedia.cableTier(stack)>=1))medium=stack;
            else if(paper==null&&(stack.is(Items.MAP)||stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","book_binding/paper")))))paper=stack;
        }
        if(medium==null||paper==null)return null;
        var data=UsbDataMedia.cableTier(medium)>=1?UsbDataCable.readAdjacent(machine,medium,1):UsbDataMedia.readStick(medium,1);
        if(data==null)return null;
        if(paper.is(Items.MAP)) {
            var output=VisualDocumentData.filledMap(VisualDocumentData.mapId(data));if(output.isEmpty())return null;
            var dyes=new ArrayList<FluidStack>();
            for(String color:List.of("Black","Cyan","Magenta","Yellow")){var dye=GTFluids.stack("Dye_Chemical_"+color,VisualDocumentRules.MAP_DYE_MB);if(dye==null||dye.isEmpty())return null;dyes.add(dye);}
            return exact(new Recipe(new ItemStack[]{paper.copyWithCount(1),medium.copyWithCount(1)},new ItemStack[]{output},null,null,dyes.toArray(FluidStack[]::new),null,VisualDocumentRules.MAP_TICKS,VisualDocumentRules.POWER,0).withCatalystInputs(1));
        }
        if(VisualDocumentData.title(data).isBlank()||data.getString("author").isBlank())return null;
        data=VisualDocumentData.resolve(level,data);
        var plan=VisualDocumentRules.bookPrint(VisualDocumentData.pages(data));if(paper.getCount()<plan.sheets())return null;
        var output=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",plan.path())));
        if(!VisualDocumentData.applyBook(level,output,data))return null;
        var dye=GTFluids.stack("Dye_Chemical_Black",plan.blackDye());if(dye==null||dye.isEmpty())return null;
        return exact(new Recipe(new ItemStack[]{paper.copyWithCount(plan.sheets()),medium.copyWithCount(1)},new ItemStack[]{output},null,null,new FluidStack[]{dye},null,plan.ticks(),VisualDocumentRules.POWER,0).withCatalystInputs(1));
    }
    private static Recipe exact(Recipe recipe){recipe.mExactItemInputs=true;recipe.mExplicitCatalystsOnly=true;recipe.mCanBeBuffered=false;return recipe;}
}
