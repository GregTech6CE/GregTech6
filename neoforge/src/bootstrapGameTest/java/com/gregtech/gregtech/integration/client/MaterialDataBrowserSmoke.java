package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.content.recipe.*;
import com.gregtech.gregtech.content.transport.*;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.generated.MaterialDataFacts;
import com.gregtech.gregtech.jei.RecipeMapCategory;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Three actual browser pages and their registered input indexes, in an isolated fresh world. */
public final class MaterialDataBrowserSmoke {
    private static final boolean EMI=net.neoforged.fml.ModList.get().isLoaded("emi");
    private static int stage,frames;
    private static JsonObject receipt;
    private static Recipe selected;
    private static RecipeMap map;
    private static ItemStack pipe;
    private static Throwable failure;
    private static boolean separated;
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Material data browser: "+message);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregtech:"+id)));}
    private static RecipeMap currentMap(){return stage==1?MachineRecipeMaps.ScannerMolecular:stage==2?MachineRecipeMaps.Printer:MachineRecipeMaps.Replicator;}
    private static List<Recipe> currentRows(){return stage==1?MaterialDataViewerRecipes.scans():stage==2?MaterialDataViewerRecipes.prints():MaterialDataViewerRecipes.replications();}
    public static boolean frame(Minecraft minecraft,JsonObject result){
        if(failure!=null)throw new IllegalStateException("Material data browser capture",failure);
        if(stage==4){require(separated,"blank and different material USB files collapse to the same recipe focus");return true;}
        if(stage>0||RecipePresentationJeiSmoke.runtime==null)return false;
        receipt=result;
        require(UsbRecipeDisplayBinding.sticks().size()==4,"four registered USB stick tiers");
        var stackHelper=RecipePresentationJeiSmoke.runtime.getJeiHelpers().getStackHelper();
        for(var item:UsbRecipeDisplayBinding.sticks()){
            var first=new ItemStack(item);var reordered=new ItemStack(item);var changed=new ItemStack(item);var tierChanged=new ItemStack(item);
            var data=new net.minecraft.nbt.CompoundTag();data.putString("Aa","first");data.putString("BB","second");
            var reverse=new net.minecraft.nbt.CompoundTag();reverse.putString("BB","second");reverse.putString("Aa","first");
            var different=data.copy();different.putString("BB","different");
            com.gregtech.gregtech.content.data.UsbDataMedia.writeStick(first,1,data);
            com.gregtech.gregtech.content.data.UsbDataMedia.writeStick(reordered,1,reverse);
            com.gregtech.gregtech.content.data.UsbDataMedia.writeStick(changed,1,different);
            com.gregtech.gregtech.content.data.UsbDataMedia.writeStick(tierChanged,0,data);
            var blank=new ItemStack(item);var named=first.copy();named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Same file, custom name"));
            require(stackHelper.isEquivalent(first,reordered,UidContext.Recipe)&&stackHelper.isEquivalent(first,named,UidContext.Recipe)
                    &&!stackHelper.isEquivalent(first,changed,UidContext.Recipe)&&!stackHelper.isEquivalent(first,tierChanged,UidContext.Recipe)
                    &&!stackHelper.isEquivalent(first,blank,UidContext.Recipe),"registered JEI file identity "+item);
            if(EMI)require(MaterialDataEmiBrowserSmoke.identities(first,reordered,changed,tierChanged,blank,named),"registered EMI file identity "+item);
        }
        result.addProperty("materialDataBrowserUsbPhysicalTiersChecked",UsbRecipeDisplayBinding.sticks().size());

        var pipes=new ArrayList<ItemStack>();
        for(var spec:FluidTransportDefinitions.pipes())pipes.add(item(spec.id()));
        for(var material:ItemPipeCatalog.ITEM_PIPE_MATS)for(var size:ItemPipeSpec.ItemPipeSize.values())
            pipes.add(item("item_pipe_"+size.name().toLowerCase(Locale.ROOT)+"_"+material.idSuffix()));
        int accepted=0,rejected=0;
        for(var stack:pipes){
            var form=MaterialEquivalence.form(stack);
            require(form!=null,"native pipe form "+stack);
            boolean valid=MaterialDataFacts.scannable(form.prefix());
            var scans=MaterialDataViewerRecipes.scans().stream().filter(r->GTMaterialDataRecipes.scannedMaterial(r.mOutputs[0]).resolve()==form.material().resolve()).toList();
            long found;
            if(EMI)found=MaterialDataEmiBrowserSmoke.scanUses(stack);
            else found=lookup(MachineRecipeMaps.ScannerMolecular,stack).stream().filter(scans::contains).count();
            require(found==(valid?1:0),"actual pipe input index "+stack+" "+found);
            if(valid){accepted++;if(pipe==null&&form.material().resolve()==GTMaterialRegistry.get("Steel").resolve())pipe=stack;}else rejected++;
        }
        require(accepted==303&&rejected==103&&pipe!=null,"source pipe focus matrix");
        receipt.addProperty("materialDataBrowserScannablePipeFocuses",accepted);
        receipt.addProperty("materialDataBrowserRejectedPipeFocuses",rejected);
        var print=MaterialDataViewerRecipes.prints().stream().filter(r->GTMaterialDataRecipes.scannedMaterial(r.mInputs[1]).resolve()==GTMaterialRegistry.get("Steel").resolve()).findFirst().orElseThrow();
        var usb=print.mInputs[1];var blank=new ItemStack(usb.getItem());
        var other=MaterialDataViewerRecipes.prints().stream().map(r->r.mInputs[1]).filter(s->GTMaterialDataRecipes.scannedMaterial(s).resolve()!=GTMaterialDataRecipes.scannedMaterial(usb).resolve()).findFirst().orElseThrow();
        var named=usb.copy();named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Same file, custom name"));
        if(EMI){
            separated=MaterialDataEmiBrowserSmoke.fileFocus(print,usb,blank,other,named);
        }else{
            var helper=RecipePresentationJeiSmoke.runtime.getJeiHelpers().getStackHelper();
            separated=!helper.isEquivalent(usb,blank,UidContext.Recipe)&&!helper.isEquivalent(usb,other,UidContext.Recipe)
                    &&helper.isEquivalent(usb,named,UidContext.Recipe)
                    &&lookup(MachineRecipeMaps.Printer,usb).contains(print)&&!lookup(MachineRecipeMaps.Printer,blank).contains(print)
                    &&!lookup(MachineRecipeMaps.Printer,other).contains(print)&&lookup(MachineRecipeMaps.Printer,named).contains(print);
        }
        receipt.addProperty("materialDataBrowserUsbFileFocusSeparated",separated);
        com.mojang.logging.LogUtils.getLogger().info("MATERIAL_DATA_BROWSER_FOCUS {}",receipt);
        stage=1;frames=0;show();return false;
    }
    private static List<Recipe> lookup(RecipeMap map,ItemStack stack){
        var runtime=RecipePresentationJeiSmoke.runtime;
        var focus=runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.INPUT,VanillaTypes.ITEM_STACK,stack);
        return runtime.getRecipeManager().createRecipeLookup(RecipeMapCategory.recipeType(map)).limitFocus(List.of(focus)).get().toList();
    }
    private static void show(){
        map=currentMap();
        selected=currentRows().stream().filter(r->GTMaterialDataRecipes.scannedMaterial(stage==1?r.mOutputs[0]:r.mInputs[stage==2?1:0]).resolve()
                ==GTMaterialRegistry.get(stage==3?"Iron":"Steel").resolve()).findFirst().orElseThrow();
        if(EMI){MaterialDataEmiBrowserSmoke.show(map,selected,stage==1?pipe:ItemStack.EMPTY);}
        else{
            var runtime=RecipePresentationJeiSmoke.runtime;
            var category=runtime.getRecipeManager().getRecipeCategory(RecipeMapCategory.recipeType(map));
            var layout=runtime.getRecipeManager().createRecipeLayoutDrawable(category,selected,runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
            if(stage==1){var alternatives=layout.getRecipeSlotsView().findSlotByName("INPUT_item_0").orElseThrow().getItemStacks().toList();
                require(alternatives.size()==selected.viewerInputAlternatives(0).size()&&alternatives.stream().anyMatch(s->s.is(pipe.getItem())),"JEI real builder retains every grouped form");}
            var focuses=stage==1?List.of(runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.INPUT,VanillaTypes.ITEM_STACK,pipe)):List.<mezz.jei.api.recipe.IFocus<ItemStack>>of();
            runtime.getRecipesGui().showRecipes(category,List.of(selected),new ArrayList<>(focuses));
        }
    }
    public static void screen(Minecraft minecraft,net.minecraft.client.gui.screens.Screen screen){
        if(stage<1||stage>3||frames<0||failure!=null||!screen.getClass().getName().contains(EMI?"RecipeScreen":"RecipesGui"))return;
        try{if(EMI&&!WorldCreationSmoke.emiReady()){frames=0;return;}}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        if(++frames<30)return;
        try{
            int slots=0,fluids=0;boolean medium=false;
            if(EMI){
                var counts=MaterialDataEmiBrowserSmoke.slots(map,selected,screen);slots=counts[0];fluids=counts[1];medium=counts[2]>0;
            }else{
                var field=screen.getClass().getDeclaredField("layouts");field.setAccessible(true);var layouts=field.get(screen);
                var list=layouts.getClass().getDeclaredField("recipeLayoutsWithButtons");list.setAccessible(true);
                for(var wrapper:(List<?>)list.get(layouts)){
                    var layoutField=Arrays.stream(wrapper.getClass().getDeclaredFields()).filter(f->IRecipeLayoutDrawable.class.isAssignableFrom(f.getType())).findFirst().orElseThrow();
                    layoutField.setAccessible(true);var layout=(IRecipeLayoutDrawable<?>)layoutField.get(wrapper);
                    if(layout.getRecipe()!=selected)continue;
                    for(var slot:layout.getRecipeSlotsView().getSlotViews()){
                        var name=slot.getSlotName().orElse("");if(name.isEmpty())continue;
                        require(!slot.isEmpty(),"real JEI slot");slots++;
                        for(var stack:slot.getItemStacks().toList())if(GTMaterialDataRecipes.scannedMaterial(stack)!=null)medium=true;
                        if(name.contains("_fluid_")){require(slot.getItemStacks().findAny().isPresent(),"JEI fluid item icon");fluids++;}
                    }
                }
            }
            require(slots>=3&&medium,"actual file and input/output slots on stage "+stage);
            require(fluids==selected.mFluidInputs.length+selected.mFluidOutputs.length,"actual visible fluid slots");
            receipt.addProperty("materialDataBrowserSlots"+stage,slots);receipt.addProperty("materialDataBrowserFluidIcons"+stage,fluids);
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}

        var medium=stage==1?selected.mOutputs[0]:selected.mInputs[stage==2?1:0];
        var material=GTMaterialDataRecipes.scannedMaterial(medium);
        var tooltip=medium.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(minecraft.level),minecraft.player,net.minecraft.world.item.TooltipFlag.Default.NORMAL).stream().map(net.minecraft.network.chat.Component::getString).toList();
        require(tooltip.stream().anyMatch(line->line.contains(material.getLocalName()))&&tooltip.stream().anyMatch(line->line.contains("3.0")),"actual USB item tooltip has file material and tier");
        var tooltipJson=new com.google.gson.JsonArray();tooltip.forEach(tooltipJson::add);receipt.add("materialDataBrowserUsbTooltip"+stage,tooltipJson);
        int captured=stage;frames=-1;String file="material-data-"+(EMI?"emi":"jei")+"-"+stage+"-"+UUID.randomUUID()+".png";
        Screenshot.grab(minecraft.gameDirectory,file,minecraft.getMainRenderTarget(),ignored->{
            try{
                var path=minecraft.gameDirectory.toPath().resolve("screenshots").resolve(file).toAbsolutePath();require(java.nio.file.Files.isRegularFile(path),"screenshot");
                receipt.addProperty("materialDataBrowserScreenshot"+captured,path.toString());
                minecraft.execute(()->{stage=captured+1;frames=0;if(stage<4)show();else minecraft.setScreen(null);});
            }catch(Throwable e){failure=e;minecraft.execute(()->minecraft.setScreen(null));}
        });
    }
}
